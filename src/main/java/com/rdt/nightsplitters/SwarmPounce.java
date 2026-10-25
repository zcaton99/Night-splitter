package com.rdt.nightsplitters;

import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** One ballistic launch toward a snapshot of an airborne Creative target. */
public final class SwarmPounce {
    public static final double MAX_HEIGHT = 25.0;
    public static final double MAX_HORIZONTAL_DISTANCE = 24.0;
    private long nextLaunchTick;
    private boolean inFlight;

    public boolean isInFlight(CaveSpider spider) {
        if (inFlight && (spider.onGround() || spider.isInWater() || spider.isInLava())) {
            inFlight = false;
        }
        return inFlight;
    }

    public int cooldownTicksRemaining(CaveSpider spider) {
        return (int)Math.max(0L, nextLaunchTick - spider.level().getGameTime());
    }

    public boolean tryLaunch(CaveSpider spider, Player target) {
        if (!spider.entityTags().contains("ns_swarm") || !spider.isAlive()
                || !target.isAlive() || !target.isCreative() || target.isSpectator()
                || target.level() != spider.level() || target.onGround()
                || !spider.onGround() || spider.isInWater() || spider.isInLava()
                || isInFlight(spider) || cooldownTicksRemaining(spider) > 0
                || !spider.getSensing().hasLineOfSight(target)
                || !spider.level().noCollision(spider, spider.getBoundingBox().move(0, 1, 0))) {
            return false;
        }
        Vec3 velocity = calculateVelocity(spider.position(), target.position(), spider.getGravity());
        if (velocity == null) return false;
        spider.getNavigation().stop();
        spider.getMoveControl().strafe(0.0F, 0.0F);
        spider.resetFallDistance();
        // Vanilla impulse protection discounts the pounce height, then expires on landing.
        // Falling below the launch height can still cause ordinary cliff-fall damage.
        spider.setIgnoreFallDamageFromCurrentImpulse(true, spider.position());
        spider.setOnGround(false);
        spider.setDeltaMovement(velocity);
        inFlight = true;
        nextLaunchTick = spider.level().getGameTime() + 100 + spider.getRandom().nextInt(61);
        return true;
    }

    public static Vec3 calculateVelocity(Vec3 origin, Vec3 target, double gravity) {
        Vec3 offset = target.subtract(origin);
        double horizontal = Math.hypot(offset.x, offset.z);
        if (!Double.isFinite(horizontal) || !Double.isFinite(offset.y)
                || !Double.isFinite(gravity) || gravity <= 0 || gravity > 1
                || offset.y < 2.0 || offset.y > MAX_HEIGHT
                || horizontal > MAX_HORIZONTAL_DISTANCE) return null;
        double apex = Math.min(MAX_HEIGHT, offset.y + 0.75);
        // Solve the normal Minecraft air trajectory, including vertical drag.
        double low = 0, high = 10;
        for (int i = 0; i < 40; i++) {
            double middle = (low + high) * 0.5;
            if (apexHeight(middle, gravity) > apex) high = middle;
            else low = middle;
        }
        double vertical = low;
        double y = 0, vy = vertical;
        int ticks = 0;
        while (ticks < 200 && vy > 0) {
            y += vy;
            ticks++;
            if (y >= offset.y - 0.01) break;
            vy = (vy - gravity) * 0.98;
        }
        // Horizontal air drag: land at the sampled X/Z near target height.
        double travel = (1.0 - Math.pow(0.91, ticks)) / (1.0 - 0.91);
        return new Vec3(offset.x / travel, vertical, offset.z / travel);
    }

    private static double apexHeight(double velocity, double gravity) {
        double height = 0;
        for (int tick = 0; tick < 1000 && velocity > 0; tick++) {
            height += velocity;
            velocity = (velocity - gravity) * 0.98;
        }
        return height;
    }
}
