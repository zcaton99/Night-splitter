package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Guardians {
    private final Map<UUID, Villager> villagers = new HashMap<>();
    private final Map<UUID, Long> attacks = new HashMap<>();
    public void clear() { villagers.clear(); attacks.clear(); }
    public void track(Entity entity) {
        if (entity instanceof Villager v && element(v) != null) villagers.put(v.getUUID(), v);
    }
    public void unload(Entity entity) { villagers.remove(entity.getUUID()); attacks.remove(entity.getUUID()); }
    public static Element element(Entity entity) {
        for (Element e : Element.values()) if (entity.entityTags().contains("cb_faction_" + e.key())) return e;
        return null;
    }
    public static boolean owns(Player player, Villager v) { return v.entityTags().contains("cb_owner_" + player.getUUID()); }
    public boolean assign(Player player, Villager v, Element e) {
        if (!Progress.all(player) || player.isSpectator() || v.isBaby()) return false;
        if (v.entityTags().stream().anyMatch(t -> t.startsWith("cb_owner_")) && !owns(player, v)) return false;
        for (Element old : Element.values()) v.removeTag("cb_faction_" + old.key());
        v.addTag("cb_faction_" + e.key()); v.addTag("cb_owner_" + player.getUUID());
        post(v, v.blockPosition()); villagers.put(v.getUUID(), v); return true;
    }
    public void post(Villager v, BlockPos pos) {
        for (String tag : Set.copyOf(v.entityTags())) if (tag.startsWith("cb_post_")) v.removeTag(tag);
        v.addTag("cb_post_" + pos.getX() + "_" + pos.getY() + "_" + pos.getZ());
    }
    public BlockPos post(Villager v) {
        for (String tag : v.entityTags()) if (tag.startsWith("cb_post_")) {
            try { String[] xyz = tag.substring(8).split("_");
                return new BlockPos(Integer.parseInt(xyz[0]), Integer.parseInt(xyz[1]), Integer.parseInt(xyz[2]));
            } catch (RuntimeException ignored) {}
        }
        return v.blockPosition();
    }
    public boolean release(Player player, Villager v) {
        if (!owns(player, v)) return false;
        for (String tag : Set.copyOf(v.entityTags())) if (tag.startsWith("cb_faction_") || tag.startsWith("cb_owner_") || tag.startsWith("cb_post_")) v.removeTag(tag);
        villagers.remove(v.getUUID()); attacks.remove(v.getUUID()); v.getNavigation().stop(); return true;
    }
    public void tick() {
        for (Villager v : List.copyOf(villagers.values())) {
            if (!v.isAlive() || v.isRemoved()) { unload(v); continue; }
            if (!(v.level() instanceof ServerLevel level)) continue;
            Element e = element(v); if (e == null) { unload(v); continue; }
            long now = level.getGameTime();
            LivingEntity enemy = level.getEntitiesOfClass(LivingEntity.class, v.getBoundingBox().inflate(12)).stream()
                .filter(t -> t instanceof Enemy && t.isAlive() && v.hasLineOfSight(t))
                .min(Comparator.comparingDouble(v::distanceToSqr)).orElse(null);
            if (enemy != null) {
                v.getLookControl().setLookAt(enemy, 30, 30);
                if (now >= attacks.getOrDefault(v.getUUID(), 0L)) {
                    attacks.put(v.getUUID(), now + 60);
                    Abilities.strike(level, v, enemy, e, 4, e == Element.AIR ? 1 : .3);
                }
            } else if (now % 40 == 0) {
                BlockPos anchor = post(v);
                // Homes/work schedules remain vanilla. Bring distant guards back toward their post.
                if (v.position().distanceToSqr(Vec3.atCenterOf(anchor)) > 16 * 16
                        && level.hasChunksAt(anchor.getX(), anchor.getZ(), anchor.getX(), anchor.getZ()))
                    v.getNavigation().moveTo(anchor.getX() + .5, anchor.getY(), anchor.getZ() + .5, .6);
            }
            if (now % 40 == 0) Abilities.particles(level, e, v.position().add(0, 1.8, 0), 2);
        }
    }
}
