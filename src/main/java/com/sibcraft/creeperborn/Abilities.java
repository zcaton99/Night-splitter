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
    public final Rocks rocks = new Rocks();
    private static final class Push {
        LivingEntity target; Vec3 direction, previous; double remaining; int ticks = 35;
        Push(LivingEntity target, Vec3 direction, double distance) {
            this.target = target; this.direction = direction; this.remaining = distance; this.previous = target.position();
        }
    }
    private final Map<UUID, Push> pushes = new HashMap<>();
    public double pushRemaining(LivingEntity target) { Push p = pushes.get(target.getUUID()); return p == null ? 0 : p.remaining; }
    public void clear() { cooldowns.clear(); moves.clear(); rocks.clear(); pushes.clear(); }
    public void tick() {
        rocks.tick();
        Iterator<Push> iterator = pushes.values().iterator();
        while (iterator.hasNext()) {
            Push push = iterator.next(); LivingEntity t = push.target;
            Vec3 delta = t.position().subtract(push.previous);
            push.remaining -= Math.max(0, delta.x * push.direction.x + delta.z * push.direction.z);
            push.previous = t.position();
            if (!t.isAlive() || t.isRemoved() || --push.ticks <= 0 || push.remaining <= .05) {
                if (t.isAlive() && !t.isRemoved()) t.setDeltaMovement(0, t.getDeltaMovement().y, 0);
                iterator.remove(); continue;
            }
            double speed = Math.min(.8, push.remaining);
            t.setDeltaMovement(push.direction.x * speed, t.getDeltaMovement().y, push.direction.z * speed);
        }
    }
    public int move(Player p) { return moves.getOrDefault(p.getUUID(), 0); }
    public void move(Player p, int index) { moves.put(p.getUUID(), index); }
    public String cast(Player p) {
        if (!(p.level() instanceof ServerLevel level) || p.isSpectator() || !p.isAlive()) return "Cannot cast now.";
        if(WaterRules.suppressed(p))return "Fruit weakness: powers are disabled while submerged. Master all four elements to overcome it.";
        Element e = Progress.selected(p);
        if (e == null) return "Consume an elemental creeper heart first.";
        int move = move(p);
        if (!Mastery.allowed(p, e, move)) return e.moves[move] + " locked: move 2 needs 10 meaningful uses + 5 active minutes; move 3 needs 40 uses + 20 active minutes.";
        double power = EnvironmentPower.strength(p, e);
        double harmony = Mastery.multiplier(p);
        String key = e.key() + move;
        Map<String, Long> timers = cooldowns.computeIfAbsent(p.getUUID(), id -> new HashMap<>());
        long now = level.getGameTime(), due = timers.getOrDefault(key, 0L);
        if (now < due) return "Cooling down: " + ((due - now + 19) / 20) + "s.";
        timers.put(key, now + e.cooldowns[move]);
        if (move == 0 && e == Element.EARTH) rocks.throwBoulder(p, power);
        else if (move == 0) bolt(level, p, e, power);
        else switch (e) {
            case EARTH -> {
                if (move == 1) rocks.guard(p, (int)Math.round(100 * power * harmony));
                else pulse(level, p, e, 5, (float)(5 * power), 10 * power);
            }
            case WATER -> {
                if (move == 1) {
                    effect(p, MobEffects.RESISTANCE, (int)Math.round(80 * power * harmony), 0); pulse(level, p, e, 5, 0, 5 * power);
                } else {
                    int foodBefore=p.getFoodData().getFoodLevel();
                    float before = p.getHealth(); p.heal((float)(4 * power * harmony)); p.clearFire();
                    p.getFoodData().setFoodLevel(20);
                    if (p.getHealth() > before || foodBefore<20) Mastery.credit(p, e);
                }
            }
            case FIRE -> {
                if (move == 1) area(level, p, e, 4, (float)(5 * power), .35 * power);
                else { effect(p, MobEffects.FIRE_RESISTANCE, (int)Math.round(160 * power * harmony), 0); effect(p, MobEffects.RESISTANCE, (int)Math.round(80 * power * harmony), 0); }
            }
            case AIR -> {
                if (move == 1) {
                    Vec3 direction = p.getLookAngle();
                    p.setIgnoreFallDamageFromCurrentImpulse(true, p.position());
                    p.setDeltaMovement(direction.x * 1.2 * power, .65 * power, direction.z * 1.2 * power);
                    if (p instanceof ServerPlayer sp) {
                        ((com.sibcraft.creeperborn.mixin.AirStepConnectionAccessor)sp.connection).creeperborn$resetAirStepCounter(0);
                        sp.connection.send(new ClientboundSetEntityMotionPacket(p));
                    }
                } else area(level, p, e, 6, (float)(2 * power), -.5 * power);
            }
        }
        particles(level, e, p.position().add(0, 1, 0), 20);
        return e.moves[move] + "!";
    }
    public static boolean waterSource(Player p) {
        for (int i = 0; i < p.getInventory().getContainerSize(); i++)
            if (p.getInventory().getItem(i).is(Items.WATER_BUCKET)) return true;
        BlockPos center = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            int x = pos.getX(), z = pos.getZ();
            if (p.level().hasChunksAt(x, z, x, z) && p.level().getFluidState(pos).is(net.minecraft.tags.FluidTags.WATER)) return true;
        }
        return false;
    }
    private static void bolt(ServerLevel level, Player p, Element e, double power) {
        Vec3 origin = p.getEyePosition(), direction = p.getLookAngle();
        double maximum=Mastery.harmony(p)?24:16;
        Vec3 end = origin.add(direction.scale(maximum));
        var block = level.clip(new ClipContext(origin, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        double range = block.getType() == HitResult.Type.MISS ? maximum : origin.distanceTo(block.getLocation());
        LivingEntity best = null; double closest = range;
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, p.getBoundingBox().inflate(maximum+1))) {
            if (!(target instanceof Enemy) || !target.isAlive() || !p.hasLineOfSight(target)) continue;
            var intersection = target.getBoundingBox().inflate(0.35).clip(origin, origin.add(direction.scale(range)));
            if (intersection.isPresent()) {
                double d = origin.distanceTo(intersection.get());
                if (d < closest) { closest = d; best = target; }
            }
        }
        for (double d = 0; d < closest; d += 0.5) particles(level, e, origin.add(direction.scale(d)), 1);
        if (best != null) strike(level, p, best, e, (float)((e == Element.AIR ? 2 : 6) * power), (e == Element.AIR ? 1.1 : .35) * power);
    }
    private void pulse(ServerLevel level, LivingEntity caster, Element e, double radius, float damage, double distance) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius))) {
            if (!(target instanceof Enemy) || !target.isAlive() || target.distanceToSqr(caster) > radius * radius || !caster.hasLineOfSight(target)) continue;
            strike(level, caster, target, e, damage, 0);
            Vec3 delta = target.position().subtract(caster.position());
            Vec3 direction = new Vec3(delta.x, 0, delta.z).normalize();
            pushes.put(target.getUUID(), new Push(target, direction, distance));
            target.setDeltaMovement(direction.x * .8, .15, direction.z * .8);
        }
    }
    private static void area(ServerLevel level, LivingEntity caster, Element e, double radius, float damage, double push) {
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, caster.getBoundingBox().inflate(radius))) {
            if (target instanceof Enemy && target.isAlive() && target.distanceToSqr(caster) <= radius * radius
                    && caster.hasLineOfSight(target)) strike(level, caster, target, e, damage, push);
        }
    }
    public static void strike(ServerLevel level, LivingEntity caster, LivingEntity target, Element e, float damage, double push) {
        if (caster instanceof Player p && target instanceof Enemy && target.isAlive()) Mastery.credit(p, e);
        if (caster instanceof Player p) damage *= (float)Mastery.multiplier(p);
        if (damage > 0) {
            Rubber.fireBending(e == Element.FIRE);
            try { target.hurtServer(level, caster instanceof Player p ? caster.damageSources().playerAttack(p)
                : caster.damageSources().mobAttack(caster), damage); }
            finally { Rubber.fireBending(false); }
        }
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


