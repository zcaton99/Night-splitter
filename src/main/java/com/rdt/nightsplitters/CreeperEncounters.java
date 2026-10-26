package com.rdt.nightsplitters;

import com.rdt.nightsplitters.mixin.CreeperAccess;
import com.rdt.nightsplitters.mixin.MobGoalsAccessor;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public final class CreeperEncounters {
    public static final String MARK = "ns_creeper", PARENT = "ns_creeper_parent", CHILD = "ns_creeper_child";
    private final Map<UUID, Creeper> loaded = new HashMap<>();
    private final List<Brood> pending = new ArrayList<>();
    private record Brood(ServerLevel level, Vec3 position, long due) {}

    public void track(Entity entity) {
        if (entity instanceof Creeper creeper && creeper.entityTags().contains(MARK)) {
            loaded.put(creeper.getUUID(), creeper);
            ((MobGoalsAccessor)creeper).nightSplitters$getGoalSelector()
                .addGoal(1, new CreeperHuntGoal(creeper));
        }
    }
    public void unload(Entity entity) { loaded.remove(entity.getUUID()); }
    public int activeCount() {
        return (int)loaded.values().stream().filter(c -> c.isAlive() && !c.isRemoved()).count();
    }
    public int pendingCount() { return pending.size(); }
    public boolean active() { return activeCount() > 0 || !pending.isEmpty(); }
    public int reservedSlots() {
        return loaded.values().stream().filter(c -> c.isAlive() && !c.isRemoved())
            .mapToInt(c -> c.entityTags().contains(PARENT) ? 2 : 1).sum() + pending.size() * 2;
    }

    public Creeper make(ServerLevel level, boolean parent) {
        SwarmConfig cfg = NightSplitters.settings();
        Creeper creeper = EntityTypes.CREEPER.create(level, EntitySpawnReason.EVENT);
        if (creeper == null) throw new IllegalStateException("Vanilla creeper unavailable");
        creeper.addTag(MARK);
        creeper.addTag(parent ? PARENT : CHILD);
        creeper.addTag("ns_exp_" + (level.getGameTime() + cfg.lifetimeSeconds * 20L));
        creeper.getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(parent ? 0.30 : 0.36);
        creeper.getAttribute(Attributes.MAX_HEALTH).setBaseValue(parent ? 20 : 10);
        creeper.setHealth(parent ? 20 : 10);
        if (!parent) creeper.getAttribute(Attributes.SCALE).setBaseValue(0.5);
        ((CreeperAccess)creeper).nightSplitters$setExplosionRadius(parent ? 3 : 1);
        return creeper;
    }

    public boolean spawnNear(ServerLevel level, ServerPlayer player) {
        if (!NightSplitters.settings().enabled || !NightSplitters.settings().creeperEnabled || active()) return false;
        for (int attempt = 0; attempt < 8; attempt++) {
            double angle = level.getRandom().nextDouble() * Math.PI * 2;
            double radius = 24 + level.getRandom().nextDouble() * 8;
            int x = (int)Math.floor(player.getX() + Math.cos(angle) * radius);
            int z = (int)Math.floor(player.getZ() + Math.sin(angle) * radius);
            if (!level.hasChunksAt(x - 2, z - 2, x + 2, z + 2)
                    || !level.areEntitiesActuallyLoadedAndTicking(new net.minecraft.world.level.ChunkPos(x >> 4, z >> 4))) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos pos = new BlockPos(x, y, z);
            if (Math.abs(y - player.getY()) > 16 || !level.getWorldBorder().isWithinBounds(pos)
                    || !level.canSeeSky(pos) || level.getBrightness(LightLayer.BLOCK, pos) > 7
                    || !level.getFluidState(pos).isEmpty()
                    || !level.getBlockState(pos.below()).isSolidRender()
                    || level.players().stream().anyMatch(p -> p.distanceToSqr(x + 0.5, y, z + 0.5) < 16 * 16)) continue;
            Creeper parent = make(level, true);
            parent.snapTo(x + 0.5, y, z + 0.5);
            if (level.noCollision(parent, parent.getBoundingBox()) && level.addFreshEntity(parent)) return true;
        }
        return false;
    }

    public void exploded(Creeper parent) {
        if (!(parent.level() instanceof ServerLevel level)
                || !parent.entityTags().contains(MARK) || !parent.entityTags().contains(PARENT)) return;
        // Consume once. Children never carry the parent tag.
        parent.removeTag(PARENT);
        loaded.remove(parent.getUUID());
        if (allowed(level)) pending.add(new Brood(level, parent.position(), level.getGameTime() + 40));
    }

    public void tick(MinecraftServer server) {
        for (Creeper c : List.copyOf(loaded.values())) {
            if (c.isRemoved() || !c.isAlive()) { loaded.remove(c.getUUID()); continue; }
            ServerLevel level = (ServerLevel)c.level();
            if (!allowed(level) || expired(c, level.getGameTime())) {
                c.discard(); loaded.remove(c.getUUID());
            }
        }
        Iterator<Brood> iterator = pending.iterator();
        while (iterator.hasNext()) {
            Brood brood = iterator.next();
            ServerLevel level = brood.level();
            long now = level.getGameTime();
            if (!allowed(level) || now > brood.due() + 200) { iterator.remove(); continue; }
            if (now < brood.due()) continue;
            // Search opposite sides of the blast, with safe footing and no chunk loading.
            Creeper first = null, second = null;
            double startAngle = level.getRandom().nextDouble() * Math.PI * 2;
            for (int attempt = 0; attempt < 8 && first == null; attempt++) {
                double angle = startAngle + attempt * Math.PI / 4;
                double dx = Math.cos(angle) * 2.5, dz = Math.sin(angle) * 2.5;
                Creeper left = childAt(level, brood.position().add(dx, 0, dz));
                Creeper right = childAt(level, brood.position().add(-dx, 0, -dz));
                if (left != null && right != null) { first = left; second = right; }
            }
            if (first == null) continue; // Retry until the existing ten-second deadline.
            if (!level.addFreshEntity(first)) continue;
            if (!level.addFreshEntity(second)) { first.discard(); loaded.remove(first.getUUID()); continue; }
            iterator.remove();
        }
    }

    private Creeper childAt(ServerLevel level, Vec3 position) {
        int x = (int)Math.floor(position.x), z = (int)Math.floor(position.z);
        if (!level.hasChunksAt(x - 1, z - 1, x + 1, z + 1)) return null;
        Creeper child = make(level, false);
        for (int dy : new int[] {0, -1, 1, -2, 2, -3}) {
            BlockPos pos = BlockPos.containing(position.x, position.y + dy, position.z);
            child.snapTo(position.x, pos.getY(), position.z);
            if (level.getWorldBorder().isWithinBounds(pos)
                    && level.getFluidState(pos).isEmpty()
                    && level.getBlockState(pos.below()).isSolidRender()
                    && level.noCollision(child, child.getBoundingBox())) return child;
        }
        return null;
    }

    private boolean allowed(ServerLevel level) {
        SwarmConfig cfg = NightSplitters.settings();
        long time = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return cfg.enabled && cfg.creeperEnabled && level.getDifficulty() != Difficulty.PEACEFUL
            && (!cfg.cleanupAtDawn || (time >= 13000 && time < 23000));
    }
    private boolean expired(Creeper c, long now) {
        for (String tag : c.entityTags()) if (tag.startsWith("ns_exp_")) {
            try { return now >= Long.parseLong(tag.substring(7)); }
            catch (NumberFormatException ignored) { return true; }
        }
        return true;
    }
    public int clear() {
        int count = activeCount();
        loaded.values().forEach(Entity::discard);
        loaded.clear(); pending.clear();
        return count;
    }
}
