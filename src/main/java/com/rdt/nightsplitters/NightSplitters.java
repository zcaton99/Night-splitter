package com.rdt.nightsplitters;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class NightSplitters implements ModInitializer {
    private static NightSplitters instance;
    private static final SwarmConfig DEFAULT_SETTINGS = new SwarmConfig();
    public static SwarmConfig settings() { return instance == null ? DEFAULT_SETTINGS : instance.config; }
    private static final Logger LOG = LoggerFactory.getLogger("night_splitters");
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String MARK = "ns_swarm", PARENT = "ns_parent", CHILD = "ns_child", EXP = "ns_exp_";
    private final Map<UUID, CaveSpider> loaded = new HashMap<>();
    private final Path configPath = FabricLoader.getInstance().getConfigDir().resolve("night-splitters.json");
    private SwarmConfig config = new SwarmConfig();
    private long nextWave;
    private boolean waveActive;
    private boolean waitingForCooldown;
    private int rotation;
    private boolean intermissionUsed;
    private static final CreeperEncounters ENCOUNTERS = new CreeperEncounters();
    public static CreeperEncounters encounters() { return ENCOUNTERS; }

    @Override public void onInitialize() {
        instance = this;
        readConfig();
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            ENCOUNTERS.track(entity);
            if (entity.entityTags().contains(CreeperEncounters.MARK)) intermissionUsed = true;
            if (entity instanceof CaveSpider spider && spider.entityTags().contains(MARK)) {
                loaded.put(spider.getUUID(), spider);
                ((com.rdt.nightsplitters.mixin.MobGoalsAccessor)spider)
                    .nightSplitters$getGoalSelector().addGoal(1, new CreativePursuitGoal(spider));
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            loaded.remove(entity.getUUID()); ENCOUNTERS.unload(entity);
        });
        ServerLifecycleEvents.SERVER_STARTING.register(server -> { loaded.clear(); ENCOUNTERS.clear(); intermissionUsed = false; nextWave = 0; waveActive = false; waitingForCooldown = false; rotation = 0; readConfig(); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { loaded.clear(); ENCOUNTERS.clear(); intermissionUsed = false; nextWave = 0; waveActive = false; waitingForCooldown = false; rotation = 0; });
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damage) -> {
            if (!(entity instanceof CaveSpider parent) || !parent.entityTags().contains(PARENT)
                || !parent.entityTags().contains(MARK)) return;
            // Remove the parent flag before spawning: repeated callbacks cannot split twice.
            parent.removeTag(PARENT);
            loaded.remove(parent.getUUID());
            if (!config.enabled || !(parent.level() instanceof ServerLevel level)
                || level.getDifficulty() == Difficulty.PEACEFUL) return;
            var target = parent.getTarget();
            for (int i = 0; i < 2; i++) {
                CaveSpider child = make(level, false);
                child.snapTo(parent.getX(), parent.getY(), parent.getZ());
                // Both offspring fit within the parent's footprint; no block edits are needed.
                child.setTarget(target);
                if (level.addFreshEntity(child)) loaded.put(child.getUUID(), child);
                else LOG.warn("A split offspring could not be added at {}", parent.blockPosition());
            }
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
            dispatcher.register(Commands.literal("nightsplitters")
                .requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.literal("wave")
                    .then(Commands.literal("multiplier")
                        .then(Commands.argument("value", IntegerArgumentType.integer(1, 16)).executes(ctx -> {
                            config.waveMultiplier = IntegerArgumentType.getInteger(ctx, "value");
                            saveConfig();
                            ctx.getSource().sendSuccess(() -> Component.literal("Wave multiplier: " + config.waveMultiplier
                                + "; budgets " + config.localBudget() + " local / " + config.globalBudget() + " global."), true);
                            return config.waveMultiplier;
                        }))))
                .then(toggle("darkness", value -> config.darknessEnabled = value))
                .then(toggle("darknessonly", value -> config.darknessOnly = value))
                .then(toggle("babyhorde", value -> config.babyHordeEnabled = value))
                .then(toggle("creeper", value -> config.creeperEnabled = value))
                .then(Commands.literal("creeperchance")
                    .then(Commands.argument("chance", DoubleArgumentType.doubleArg(0, 1)).executes(ctx -> {
                        config.creeperChance = DoubleArgumentType.getDouble(ctx, "chance"); saveConfig();
                        ctx.getSource().sendSuccess(() -> Component.literal("Intermission creeper chance: " + config.creeperChance), true);
                        return 1;
                    })))
                .then(toggle("creeperblockdamage", value -> config.creeperBlockDamage = value))
                .then(toggle("babycreeperblockdamage", value -> config.babyCreeperBlockDamage = value))
                .then(Commands.literal("testcreeper").executes(ctx -> {
                    ServerPlayer player = ctx.getSource().getPlayerOrException();
                    ServerLevel level = (ServerLevel)player.level();
                    if (!config.enabled || !config.creeperEnabled || !night(level)
                            || level.getDifficulty() == Difficulty.PEACEFUL
                            || !level.getGameRules().get(GameRules.SPAWN_MOBS)
                            || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
                        ctx.getSource().sendFailure(Component.literal("Enable swarms and creepers, use nighttime, non-Peaceful difficulty, and enabled mob spawning."));
                        return 0;
                    }
                    clear(); waveActive = false; waitingForCooldown = false;
                    if (!ENCOUNTERS.spawnNear(level, player)) {
                        ctx.getSource().sendFailure(Component.literal("No valid loaded outdoor spawn found. Try open ground."));
                        return 0;
                    }
                    intermissionUsed = true; waveActive = true;
                    ctx.getSource().sendSuccess(() -> Component.literal("Test creeper encounter started; the previous loaded wave was cleared."), true);
                    return 1;
                }))
                .then(Commands.literal("status").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> Component.literal("Night Splitters: " +
                        (config.enabled ? "enabled" : "disabled") + "; loaded spiders=" + loaded.size()
                        + "; reserved slots=" + slots(null) + "/" + config.globalBudget()
                        + "; multiplier=" + config.waveMultiplier + "; darkness=" + config.darknessEnabled
                        + "; darknessOnly=" + config.darknessOnly + "; babyHorde=" + config.babyHordeEnabled
                        + "; creeper=" + config.creeperEnabled
                        + "; encounter creepers=" + ENCOUNTERS.activeCount() + "; pending broods=" + ENCOUNTERS.pendingCount()), false);
                    return 1;
                }))
                .then(Commands.literal("disable").executes(ctx -> {
                    config.enabled = false; saveConfig(); int count = clear(); waveActive = false; waitingForCooldown = false;
                    ctx.getSource().sendSuccess(() -> Component.literal("Swarm disabled. Removed " + count +
                        " loaded swarm entities; unloaded swarm entities are removed when loaded while disabled."), true);
                    return count;
                }))
                .then(Commands.literal("enable").executes(ctx -> {
                    config.enabled = true; saveConfig(); nextWave = 0; waveActive = false; waitingForCooldown = false;
                    ctx.getSource().sendSuccess(() -> Component.literal("Night swarms enabled."), true); return 1;
                }))
                .then(Commands.literal("reload").executes(ctx -> {
                    boolean ok = readConfig();
                    if (ok) ctx.getSource().sendSuccess(() -> Component.literal("Night Splitters configuration reloaded."), true);
                    else ctx.getSource().sendFailure(Component.literal("Invalid config: swarm disabled. See server log."));
                    return ok ? 1 : 0;
                }))
                .then(Commands.literal("clear").executes(ctx -> {
                    int count = clear(); waveActive = false; waitingForCooldown = false;
                    ctx.getSource().sendSuccess(() -> Component.literal("Removed " + count + " loaded swarm entities."), true);
                    return count;
                }))));
        LOG.info("Night Splitters initialized for Minecraft 26.3 (dedicated and integrated server)");
    }

    private com.mojang.brigadier.builder.LiteralArgumentBuilder<net.minecraft.commands.CommandSourceStack> toggle(
            String name, java.util.function.Consumer<Boolean> setter) {
        return Commands.literal(name)
            .then(Commands.literal("on").executes(ctx -> {
                setter.accept(true); saveConfig();
                ctx.getSource().sendSuccess(() -> Component.literal(name + " enabled."), true); return 1;
            }))
            .then(Commands.literal("off").executes(ctx -> {
                setter.accept(false); saveConfig();
                ctx.getSource().sendSuccess(() -> Component.literal(name + " disabled."), true); return 1;
            }));
    }

    private boolean night(ServerLevel level) {
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return time >= 13000 && time < 23000;
    }
    private boolean eligible(ServerPlayer player) {
        return player.isAlive() && !player.isSpectator();
    }
    private void tick(MinecraftServer server) {
        ENCOUNTERS.tick(server);
        if (server.getTickCount() % 20 != 0) return;
        for (CaveSpider spider : List.copyOf(loaded.values())) {
            if (spider.isRemoved() || !spider.isAlive()) { loaded.remove(spider.getUUID()); continue; }
            ServerLevel level = (ServerLevel) spider.level();
            // Creative players can be targeted; restore the target if vanilla AI drops it.
            if (spider.getTarget() == null || spider.getTarget().isSpectator()
                    || !spider.getTarget().isAlive()) {
                ServerPlayer nearest = level.players().stream()
                    .filter(this::eligible)
                    .filter(p -> p.distanceToSqr(spider) < 48 * 48)
                    .min(Comparator.comparingDouble(p -> p.distanceToSqr(spider)))
                    .orElse(null);
                if (nearest != null) spider.setTarget(nearest);
            }
            if (!config.enabled || level.getDifficulty() == Difficulty.PEACEFUL
                || (config.cleanupAtDawn && !night(level)) || expired(spider, level.getGameTime())) {
                spider.discard(); loaded.remove(spider.getUUID());
            }
        }
        if (!config.enabled) return;
        ServerLevel level = server.getLevel(Level.OVERWORLD);
        if (level == null || !night(level) || level.getDifficulty() == Difficulty.PEACEFUL
            || !level.getGameRules().get(GameRules.SPAWN_MOBS)
            || !level.getGameRules().get(GameRules.SPAWN_MONSTERS)) {
            // Do not carry a cooldown into the next night.
            if (loaded.isEmpty() && !ENCOUNTERS.active()) { waveActive = false; waitingForCooldown = false; nextWave = 0; }
            return;
        }
        // All tagged, tracked offspring remain part of the wave. No overlapping waves.
        if (!loaded.isEmpty() || ENCOUNTERS.active()) { waveActive = true; waitingForCooldown = false; return; }
        if (waveActive) {
            waveActive = false;
            if (!intermissionUsed && config.creeperEnabled) {
                intermissionUsed = true; // Exactly one chance per completed spider wave.
                if (level.getRandom().nextDouble() < config.creeperChance
                        && config.globalBudget() >= 2 && config.localBudget() >= 2) {
                    for (ServerPlayer player : level.players()) {
                        if (eligible(player) && level.canSeeSky(player.blockPosition())
                                && ENCOUNTERS.spawnNear(level, player)) { waveActive = true; return; }
                    }
                }
            }
            waitingForCooldown = true;
            int seconds = config.minWaveDelaySeconds + level.getRandom().nextInt(
                config.maxWaveDelaySeconds - config.minWaveDelaySeconds + 1);
            nextWave = (long)server.getTickCount() + seconds * 20L;
            return;
        }
        if (waitingForCooldown && server.getTickCount() < nextWave) return;
        waitingForCooldown = false;
        List<ServerPlayer> players = new ArrayList<>(level.players().stream().filter(this::eligible).toList());
        if (players.isEmpty()) return;
        Collections.rotate(players, rotation++ % players.size());
        // One global wave of 2-3 spiders; split offspring count toward completion.
        boolean babyWave = config.babyHordeEnabled && level.getRandom().nextDouble() < config.babyHordeChance;
        int baseSize = config.minWaveSize + level.getRandom().nextInt(
            config.parentsPerWave - config.minWaveSize + 1);
        int waveSize = config.waveSize(baseSize, babyWave);
        int slotCost = babyWave ? 1 : 2;
        int spawned = 0;
        for (ServerPlayer player : players) {
            if (!level.canSeeSky(player.blockPosition())) continue;
            while (spawned < waveSize && slots(null) + slotCost <= config.globalBudget()
                && slots(player) + slotCost <= config.localBudget()) {
                if (!spawnNear(level, player, !babyWave)) break;
                spawned++;
            }
            if (spawned == waveSize) break;
        }
        if (spawned > 0) { waveActive = true; intermissionUsed = false; }
    }
    private int slots(ServerPlayer player) {
        int total = 0;
        for (CaveSpider spider : loaded.values()) {
            if (!spider.isAlive() || spider.isRemoved()) continue;
            if (player != null && (spider.level() != player.level() || spider.distanceToSqr(player) > 64*64)) continue;
            total += SwarmBudget.cost(spider.entityTags().contains(PARENT));
        }
        return total;
    }
    private boolean spawnNear(ServerLevel level, ServerPlayer player, boolean parent) {
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double radius = 24 + level.getRandom().nextDouble() * 16;
            int x = (int)Math.floor(player.getX() + Math.cos(angle)*radius);
            int z = (int)Math.floor(player.getZ() + Math.sin(angle)*radius);
            // Check the surrounding collision area BEFORE any block/height access.
            if (!level.hasChunksAt(x-2, z-2, x+2, z+2)
                || !level.areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(x>>4,z>>4))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x,y,z);
            if (Math.abs(y-player.getY()) > 16 || !level.getWorldBorder().isWithinBounds(pos)
                || !level.canSeeSky(pos) || level.getBrightness(LightLayer.BLOCK,pos) > 7
                || !level.getFluidState(pos).isEmpty()
                || !level.getBlockState(pos.below()).isSolidRender()) continue;
            boolean close = level.players().stream().anyMatch(p -> p.distanceToSqr(x+.5,y,z+.5) < 16*16);
            if (close) continue;
            CaveSpider spider = make(level,parent);
            spider.snapTo(x+.5,y,z+.5);
            if (!level.noCollision(spider,spider.getBoundingBox())
                || !level.getEntitiesOfClass(CaveSpider.class, new AABB(pos).inflate(2)).isEmpty()) continue;
            spider.setTarget(player);
            if (level.addFreshEntity(spider)) { loaded.put(spider.getUUID(),spider); return true; }
        }
        return false;
    }
    private CaveSpider make(ServerLevel level, boolean parent) {
        CaveSpider spider = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
        if (spider == null) throw new IllegalStateException("Vanilla cave spider unavailable");
        spider.addTag(MARK); spider.addTag(parent ? PARENT : CHILD);
        if (level.getRandom().nextDouble() < config.darknessChance) spider.addTag("ns_dark");
        spider.addTag(EXP + (level.getGameTime() + config.lifetimeSeconds * 20L));
        spider.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(.3 *
            (parent ? config.parentSpeedMultiplier : config.childSpeedMultiplier));
        spider.getAttribute(Attributes.MAX_HEALTH).setBaseValue(parent ? config.parentHealth : config.childHealth);
        spider.setHealth((float)(parent ? config.parentHealth : config.childHealth));
        if (!parent) spider.getAttribute(Attributes.SCALE).setBaseValue(config.childScale);
        return spider;
    }
    private boolean expired(CaveSpider spider, long now) {
        for (String tag : spider.entityTags()) if (tag.startsWith(EXP)) {
            try { return now >= Long.parseLong(tag.substring(EXP.length())); }
            catch (NumberFormatException ignored) { return true; }
        }
        return true; // Malformed/old marked mobs are cleaned up rather than persisting forever.
    }
    private int clear() {
        var copy = List.copyOf(loaded.values());
        copy.forEach(Entity::discard); loaded.clear(); intermissionUsed = false;
        return copy.size() + ENCOUNTERS.clear();
    }
    private boolean readConfig() {
        try {
            if (!Files.exists(configPath)) { config = new SwarmConfig(); saveConfig(); return true; }
            SwarmConfig next = JSON.fromJson(Files.readString(configPath),SwarmConfig.class);
            if (next == null) throw new IllegalArgumentException("Empty config");
            next.validate(); config = next; return true;
        } catch (Exception error) {
            config.enabled = false; LOG.error("Invalid Night Splitters configuration; disabled",error); return false;
        }
    }
    private void saveConfig() {
        try { Files.createDirectories(configPath.getParent()); Files.writeString(configPath,JSON.toJson(config)+"\n"); }
        catch (Exception error) { LOG.error("Could not save Night Splitters config",error); }
    }
}

