package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;

public final class Abilities {
    private final Map<UUID, Map<String, Long>> cooldowns = new HashMap<>();
    private final Map<UUID, Integer> moves = new HashMap<>();
    public void clear() { cooldowns.clear(); moves.clear(); }
    public int move(Player p) { return moves.getOrDefault(p.getUUID(), 0); }
    public void move(Player p, int index) { moves.put(p.getUUID(), index); }
    public String cast(Player p) {
        if (!(p.level() instanceof ServerLevel level) || p.isSpectator() || !p.isAlive()) return "Cannot cast now.";
        Element e = Progress.selected(p);
        if (e == null) return "Consume an elemental creeper heart first.";
        int move = move(p);
        if (e == Element.WATER && !waterSource(p)) return "Water requires a carried water bucket or nearby water within four blocks.";
        String key = e.key() + move;
        Map<String, Long> timers = cooldowns.computeIfAbsent(p.getUUID(), id -> new HashMap<>());
        long now = level.getGameTime(), due = timers.getOrDefault(key, 0L);
        if (now < due) return "Cooling down: " + ((due - now + 19) / 20) + "s.";
        timers.put(key, now + e.cooldowns[move]);
        if (move == 0) bolt(level, p, e);
        else switch (e) {
            case EARTH -> {
                if (move == 1) effect(p, MobEffects.RESISTANCE, 100, 1);
                else area(level, p, e, 5, 5, 0.5);
            }
            case WATER -> {
                if (move == 1) {
                    effect(p, MobEffects.RESISTANCE, 80, 0); area(level, p, e, 4, 0, 0.7);
                } else { p.heal(4); p.clearFire(); }
            }
            case FIRE -> {
                if (move == 1) area(level, p, e, 4, 5, 0.35);
                else { effect(p, MobEffects.FIRE_RESISTANCE, 160, 0); effect(p, MobEffects.RESISTANCE, 80, 0); }
            }
            case AIR -> {
                if (move == 1) {
                    Vec3 direction = p.getLookAngle();
                    p.setIgnoreFallDamageFromCurrentImpulse(true, p.position());
                    p.setDeltaMovement(direction.x * 1.2, 0.65, direction.z * 1.2);
                    if (p instanceof ServerPlayer sp) sp.connection.send(new ClientboundSetEntityMotionPacket(p));
                } else area(level, p, e, 6, 2, -0.5);
            }
        }
        particles(level, e, p.position().add(0, 1, 0), 20);
        return e.moves[move] + "!";
    }
    public static boolean waterSource(Player p) {
        if (p.getInventory().contains(new ItemStack(Items.WATER_BUCKET))) return true;
        BlockPos center = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            int x = pos.getX(), z = pos.getZ();
            if (p.level().hasChunksAt(x, z, x, z) && p.level().getFluidState(pos).is(Fluids.WATER)) return true;
        }
        return false;
    }
    private static void bolt(ServerLevel level, Player p, Element e) {
        Vec3 origin = p.getEyePosition(), direction = p.getLookAngle();
        Vec3 end = origin.add(direction.scale(16));
        var block = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double range = block.getType() == HitResult.Type.MISS ? 16 : origin.distanceTo(block.getLocation());
        LivingEntity best = null; double closest = range;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(17))) {
            if (!(target instanceof Enemy) || !target.isAlive() || !p.hasLineOfSight(target)) continue;
            var intersection = target.getBoundingBox().inflate(0.35).clip(origin, origin.add(direction.scale(range)));
            if (intersection.isPresent()) {
                double d = origin.distanceTo(intersection.get());
                if (d < closest) { closest = d; best = target; }
            }
        }
        for (double d = 0; d < closest; d += 0.5) particles(level, e, origin.add(direction.scale(d)), 1);
        if (best != null) strike(level, p, best, e, e == Element.AIR ? 2 : 6, e == Element.AIR ? 1.1 : 0.35);
    }
    private static void area(ServerLevel level, LivingEntity caster, Element e, double radius, float damage, double push) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius))) {
            if (target instanceof Enemy && target.isAlive() && target.distanceToSqr(caster) <= radius * radius
                    && caster.hasLineOfSight(target)) strike(level, caster, target, e, damage, push);
        }
    }
    public static void strike(ServerLevel level, LivingEntity caster, LivingEntity target, Element e, float damage, double push) {
        if (damage > 0) target.hurtServer(level, caster instanceof Player p ? caster.damageSources().playerAttack(p)
            : caster.damageSources().mobAttack(caster), damage);
        if (!target.isAlive()) return;
        if (e == Element.WATER || e == Element.EARTH) effect(target, MobEffects.SLOWNESS, 50, 0);
        if (e == Element.FIRE) target.setRemainingFireTicks(40);
        Vec3 delta = target.position().subtract(caster.position());
        Vec3 horizontal = new Vec3(delta.x, 0, delta.z).normalize().scale(push);
        target.setDeltaMovement(target.getDeltaMovement().add(horizontal.x, 0.15, horizontal.z));
        particles(level, e, target.position().add(0, 0.7, 0), 8);
    }
    private static void effect(LivingEntity p, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect, int duration, int amp) {
        p.addEffect(new MobEffectInstance(effect, duration, amp));
    }
    public static void particles(ServerLevel level, Element e, Vec3 pos, int count) {
        level.sendParticles(e.particle, pos.x, pos.y, pos.z, count, .2, .3, .2, .02);
    }
}
