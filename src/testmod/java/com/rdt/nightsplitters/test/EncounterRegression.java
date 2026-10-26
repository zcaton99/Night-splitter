package com.rdt.nightsplitters.test;

import com.rdt.nightsplitters.*;
import com.rdt.nightsplitters.mixin.CreeperAccess;
import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class EncounterRegression {
    private ServerLevel level;
    private long started;
    private double y;
    private int checks;
    private final CreeperEncounters encounters = NightSplitters.encounters();

    public void begin(MinecraftServer server) {
        level = server.overworld();
        checkCommands(server);
        checkDarkness();
        SwarmConfig cfg = NightSplitters.settings();
        cfg.enabled = true; cfg.creeperEnabled = true; cfg.creeperBlockDamage = false;
        cfg.babyCreeperBlockDamage = false; cfg.cleanupAtDawn = false;
        encounters.clear();
        // Keep this fixture loaded: production intentionally waits instead of force-loading.
        for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++)
            level.setChunkForced(x, z, true);
        y = level.getMaxY() - 40;
        for (int x = -4; x <= 4; x++) for (int z = -4; z <= 4; z++)
            level.setBlockAndUpdate(new BlockPos(x, (int)y - 1, z), Blocks.STONE.defaultBlockState());
        Creeper parent = encounters.make(level, true);
        parent.snapTo(0.5, y, 0.5);
        check(level.addFreshEntity(parent), "Parent added to actual server world");
        check(encounters.activeCount() == 1 && encounters.reservedSlots() == 2, "Parent reserves two children");
        ((CreeperAccess)parent).nightSplitters$explode();
        check(parent.isRemoved(), "Vanilla explosion removes parent");
        check(encounters.activeCount() == 0 && encounters.pendingCount() == 1,
            "Explosion schedules one delayed brood");
        check(encounters.active() && encounters.reservedSlots() == 2,
            "Encounter stays active during the delay");
        check(level.getBlockState(new BlockPos(0, (int)y - 1, 0)).is(Blocks.STONE),
            "Parent block-damage off preserves blocks");
        ((CreeperAccess)parent).nightSplitters$explode();
        check(encounters.pendingCount() == 1, "Repeated explosion callback cannot double-split");
        started = level.getGameTime();
        ServerTickEvents.END_SERVER_TICK.register(this::tick);
    }

    private void tick(MinecraftServer server) {
        checks++;
        if (checks > 200) throw new AssertionError("Encounter regression timeout");
        long elapsed = level.getGameTime() - started;
        if (elapsed < 40) {
            check(encounters.activeCount() == 0 && encounters.pendingCount() == 1,
                "No babies before two seconds");
            return;
        }
        check(elapsed == 40, "Brood appears at exactly 40 server ticks");
        check(encounters.pendingCount() == 0 && encounters.activeCount() == 2,
            "Exactly two children spawned after delay; active=" + encounters.activeCount() + ", pending=" + encounters.pendingCount());
        var children = level.getEntitiesOfClass(Creeper.class,
            new AABB(-5, y - 5, -5, 5, y + 5, 5),
            c -> c.entityTags().contains(CreeperEncounters.CHILD));
        check(children.size() == 2, "Both offspring present in the real world");
        check(children.get(0).distanceToSqr(children.get(1)) >= 24.9, "Babies spawn five blocks apart");
        for (Creeper child : children) {
            check(child.getAttributeValue(Attributes.SCALE) == 0.5, "Offspring scaled to baby size");
            check(!child.entityTags().contains(CreeperEncounters.PARENT), "Child never inherits parent flag");
        }
        ((CreeperAccess)children.get(0)).nightSplitters$explode();
        check(children.get(1).isAlive() && children.get(1).getHealth() == 10,
            "Sibling survives first baby explosion at spawn");
        ((CreeperAccess)children.get(1)).nightSplitters$explode();
        check(encounters.pendingCount() == 0, "Exploding babies cannot create grandchildren");
        check(level.getBlockState(new BlockPos(0, (int)y - 1, 0)).is(Blocks.STONE),
            "Baby block-damage off preserves blocks");
        encounters.clear();
        Creeper next = encounters.make(level, true);
        next.snapTo(0.5, y, 0.5);
        level.addFreshEntity(next);
        ((CreeperAccess)next).nightSplitters$explode();
        check(encounters.pendingCount() == 1, "Second fixture has pending brood");
        command(server, "nightsplitters disable");
        check(encounters.pendingCount() == 0 && !encounters.active(), "Disable cancels delayed broods");
        command(server, "nightsplitters enable");
        System.out.println("NIGHT_SPLITTERS_NEXT_UPDATE_REGRESSION_PASSED");
        server.halt(false);
    }

    private static void checkCommands(MinecraftServer server) {
        SwarmConfig cfg = NightSplitters.settings();
        command(server, "nightsplitters wave multiplier 4");
        check(cfg.waveMultiplier == 4 && cfg.waveSize(3, false) == 12, "Multiplier changes parent wave plan");
        check(cfg.globalBudget() == 384 && cfg.localBudget() == 128, "Multiplier scales budgets");
        check(cfg.waveSize(3, true) == 6, "Baby-only horde remains exactly six");
        boolean rejected = false;
        try { server.getCommands().getDispatcher().execute("nightsplitters wave multiplier 17",
            server.createCommandSourceStack()); }
        catch (Exception expected) { rejected = true; }
        check(rejected && cfg.waveMultiplier == 4, "Out-of-range multiplier rejected");
        for (String name : new String[] {"darkness", "darknessonly", "babyhorde", "creeper",
                "creeperblockdamage", "babycreeperblockdamage"}) {
            command(server, "nightsplitters " + name + " on");
            command(server, "nightsplitters " + name + " off");
        }
        check(!cfg.darknessEnabled && !cfg.darknessOnly && !cfg.babyHordeEnabled && !cfg.creeperEnabled,
            "Feature toggles persist in live settings");
        check(cfg.darknessChance == 0.20, "One-in-five Darkness default");
        for (int roll = 0; roll < 3; roll++) {
            final int choice = roll;
            CreeperSchedule schedule = new CreeperSchedule();
            for (int wave = 1; wave <= 3 + roll; wave++)
                check(schedule.completeSpiderWave(() -> choice) == (wave == 3 + roll),
                    "Creeper due after exactly 3, 4, or 5 completed spider waves");
            check(schedule.completeSpiderWave(() -> choice), "Failed spawn remains due");
            schedule.reset();
            check(!schedule.completeSpiderWave(() -> choice), "New interval prevents back-to-back creepers");
        }
        command(server, "nightsplitters darknesschance 0.35");
        check(cfg.darknessChance == 0.35, "Darkness chance command updates live settings");
        command(server, "nightsplitters darknesschance 0.20");
        command(server, "nightsplitters wave multiplier 1");
        cfg.validate();
    }

    private void checkDarkness() {
        SwarmConfig cfg = NightSplitters.settings();
        cfg.enabled = true; cfg.darknessEnabled = true;
        CaveSpider dark = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
        dark.addTag("ns_swarm"); dark.addTag("ns_dark");
        dark.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4);
        Player creative = player(GameType.CREATIVE);
        check(dark.doHurtTarget(level, creative), "Dark spider hits Creative");
        check(creative.getHealth() == 18 && creative.hasEffect(MobEffects.DARKNESS),
            "Selected spider combines half Creative damage with Darkness");

        CaveSpider ordinarySwarm = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
        ordinarySwarm.addTag("ns_swarm");
        Player unaffected = player(GameType.SURVIVAL);
        ordinarySwarm.doHurtTarget(level, unaffected);
        check(!unaffected.hasEffect(MobEffects.DARKNESS), "Unselected spiders do not apply Darkness");

        cfg.darknessOnly = true;
        for (GameType mode : new GameType[] {GameType.CREATIVE, GameType.SURVIVAL}) {
            Player target = player(mode);
            check(ordinarySwarm.doHurtTarget(level, target), "Effect-only hit accepted");
            check(target.getHealth() == 20 && target.hasEffect(MobEffects.DARKNESS),
                "Global mode applies Darkness without direct damage");
            check(!target.hasEffect(MobEffects.POISON), "Global mode also suppresses poison");
        }
        Player spectator = player(GameType.SPECTATOR);
        check(!ordinarySwarm.doHurtTarget(level, spectator) && !spectator.hasEffect(MobEffects.DARKNESS),
            "Effect-only mode ignores Spectators");
        cfg.darknessOnly = false; cfg.darknessEnabled = false;
        Player restored = player(GameType.CREATIVE);
        dark.doHurtTarget(level, restored);
        check(restored.getHealth() == 18 && !restored.hasEffect(MobEffects.DARKNESS),
            "Off restores default damage without Darkness");
    }

    private Player player(GameType mode) {
        Player player = new Player(level, new GameProfile(UUID.randomUUID(), "Regression")) {
            @Override public GameType gameMode() { return mode; }
        };
        mode.updatePlayerAbilities(player.getAbilities());
        player.setHealth(20);
        return player;
    }
    private static void command(MinecraftServer server, String command) {
        try { server.getCommands().getDispatcher().execute(command, server.createCommandSourceStack()); }
        catch (Exception error) { throw new AssertionError("Command failed: " + command, error); }
    }
    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
