package com.sibcraft.creeperborn;

import java.util.*;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class Rocks {
    private static final class Boulder {
        Display.BlockDisplay visual; Player caster; Vec3 position, velocity; float damage; int life = 25;
        Boulder(Display.BlockDisplay visual, Player caster, Vec3 position, Vec3 velocity, float damage) {
            this.visual = visual; this.caster = caster; this.position = position; this.velocity = velocity; this.damage = damage;
        }
    }
    private record Guard(Player caster, long expires, List<Display.BlockDisplay> visuals) {}
    private final List<Boulder> boulders = new ArrayList<>();
    private final Map<UUID, Guard> guards = new HashMap<>();
    public int activeBoulders() { return boulders.size(); }
    public boolean guarded(Player p) { return guards.containsKey(p.getUUID()); }
    public void clear() {
        boulders.forEach(b -> b.visual.discard()); boulders.clear();
        guards.values().forEach(g -> g.visuals().forEach(v -> v.discard())); guards.clear();
    }
    private static Display.BlockDisplay visual(ServerLevel level, Vec3 position, float size) {
        Display.BlockDisplay display = EntityTypes.BLOCK_DISPLAY.create(level, EntitySpawnReason.EVENT);
        if (display == null) throw new IllegalStateException("Block display unavailable");
        display.addTag("cb_temporary_rock");
        display.setBlockState(Blocks.STONE.defaultBlockState());
        display.setTransformation(new Transformation(new Vector3f(-size / 2, -size / 2, -size / 2),
            new Quaternionf(), new Vector3f(size, size, size), new Quaternionf()));
        display.snapTo(position.x, position.y, position.z);
        level.addFreshEntity(display); return display;
    }
    public void throwBoulder(Player p, double power) {
        ServerLevel level = (ServerLevel)p.level(); Vec3 direction = p.getLookAngle();
        Vec3 origin = p.getEyePosition().add(direction.scale(1.2));
        boulders.add(new Boulder(visual(level, origin, (float)(1 + .5 * power)), p, origin, direction.scale(1.2), (float)(8 * power)));
    }
    public void guard(Player p, int duration) {
        Guard previous = guards.remove(p.getUUID()); if (previous != null) previous.visuals().forEach(v -> v.discard());
        ServerLevel level = (ServerLevel)p.level(); List<Display.BlockDisplay> visuals = new ArrayList<>();
        for (int i = 0; i < 3; i++) visuals.add(visual(level, p.getEyePosition(), .85f));
        guards.put(p.getUUID(), new Guard(p, level.getGameTime() + duration, visuals)); positionGuard(guards.get(p.getUUID()));
    }
    public boolean blocks(LivingEntity entity, DamageSource source) {
        Guard guard = guards.get(entity.getUUID());
        if (guard == null || !entity.isAlive() || entity.level() != guard.caster().level()
                || entity.level().getGameTime() >= guard.expires()) return false;
        Vec3 sourcePosition = source.getSourcePosition();
        if (sourcePosition == null || (source.getDirectEntity() == null && source.getEntity() == null)) return false;
        Vec3 toSource = sourcePosition.subtract(entity.position());
        Vec3 horizontal = new Vec3(toSource.x, 0, toSource.z).normalize();
        Vec3 look = entity.getLookAngle(); Vec3 facing = new Vec3(look.x, 0, look.z).normalize();
        return horizontal.dot(facing) >= .5; // Only the forward 120-degree arc.
    }
    private void positionGuard(Guard g) {
        Vec3 look = g.caster().getLookAngle(); Vec3 forward = new Vec3(look.x, 0, look.z).normalize();
        if (forward.lengthSqr() < .1) forward = new Vec3(0, 0, 1);
        Vec3 side = new Vec3(forward.z, 0, -forward.x);
        Vec3 center = g.caster().position().add(0, 1, 0).add(forward.scale(1.3));
        for (int i = 0; i < 3; i++) {
            Vec3 pos = center.add(side.scale((i - 1) * .85));
            g.visuals().get(i).snapTo(pos.x, pos.y, pos.z);
        }
    }
    public void tick() {
        Iterator<Guard> guardsIterator = guards.values().iterator();
        while (guardsIterator.hasNext()) {
            Guard g = guardsIterator.next();
            if (!g.caster().isAlive() || g.caster().isRemoved() || g.caster().isSpectator()
                    || g.visuals().get(0).level() != g.caster().level() || g.caster().level().getGameTime() >= g.expires()) {
                g.visuals().forEach(v -> v.discard()); guardsIterator.remove();
            } else positionGuard(g);
        }
        Iterator<Boulder> iterator = boulders.iterator();
        while (iterator.hasNext()) {
            Boulder b = iterator.next(); ServerLevel level = (ServerLevel)b.visual.level();
            Vec3 next = b.position.add(b.velocity);
            int x = (int)Math.floor(next.x), z = (int)Math.floor(next.z);
            if (--b.life < 0 || !b.caster.isAlive() || b.caster.level() != level
                    || !level.hasChunksAt(x - 1, z - 1, x + 1, z + 1)) { b.visual.discard(); iterator.remove(); continue; }
            var wall = level.clip(new ClipContext(b.position, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, b.caster));
            Vec3 end = wall.getType() == HitResult.Type.MISS ? next : wall.getLocation();
            LivingEntity victim = null; double closest = b.position.distanceTo(end);
            for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, new AABB(b.position, end).inflate(.8))) {
                if (!(target instanceof Enemy) || !target.isAlive() || !b.caster.hasLineOfSight(target)) continue;
                var hit = target.getBoundingBox().inflate(.65).clip(b.position, end);
                if (hit.isPresent() && b.position.distanceTo(hit.get()) <= closest) {
                    closest = b.position.distanceTo(hit.get()); victim = target;
                }
            }
            boolean volumeBlocked = !level.noCollision(b.visual,
                new AABB(next.x - .6, next.y - .6, next.z - .6, next.x + .6, next.y + .6, next.z + .6));
            if (victim != null || wall.getType() != HitResult.Type.MISS || volumeBlocked) {
                if (victim != null) Abilities.strike(level, b.caster, victim, Element.EARTH, b.damage, .4);
                Abilities.particles(level, Element.EARTH, end, 16); b.visual.discard(); iterator.remove();
            } else { b.position = next; b.visual.snapTo(next.x, next.y, next.z); }
        }
    }
}
