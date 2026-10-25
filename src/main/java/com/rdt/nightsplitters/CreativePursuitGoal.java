package com.rdt.nightsplitters;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.spider.CaveSpider;

/** Vanilla melee goals reject Creative players; this goal owns their pursuit. */
public final class CreativePursuitGoal extends Goal {
    private final CaveSpider spider;
    private ServerPlayer target;
    private int repath;
    private int attackCooldown;
    private final SwarmPounce pounce = new SwarmPounce();

    public CreativePursuitGoal(CaveSpider spider) {
        this.spider = spider;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    private boolean valid(ServerPlayer player) {
        return player != null && player.isAlive() && player.isCreative()
            && !player.isSpectator() && player.level() == spider.level()
            && spider.distanceToSqr(player) < 48 * 48;
    }

    @Override public boolean canUse() {
        if (!spider.entityTags().contains("ns_swarm")) return false;
        // Respect an existing Survival/Adventure target.
        if (spider.getTarget() != null && spider.getTarget().isAlive()
                && !(spider.getTarget() instanceof ServerPlayer p && p.isCreative())) return false;
        target = ((ServerLevel)spider.level()).players().stream()
            .filter(this::valid)
            .min(Comparator.comparingDouble(spider::distanceToSqr)).orElse(null);
        return target != null;
    }

    @Override public boolean canContinueToUse() { return valid(target); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { repath = 0; }

    @Override public void tick() {
        spider.setTarget(target);
        spider.getLookControl().setLookAt(target, 30.0F, 30.0F);
        boolean airborne = pounce.isInFlight(spider);
        if (!airborne && pounce.tryLaunch(spider, target)) airborne = true;
        // Do not steer in flight: players can dodge the sampled launch position.
        if (!airborne && --repath <= 0) {
            repath = 10;
            // Coordinate navigation avoids vanilla's Creative entity-target filters.
            spider.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
        }
        if (attackCooldown > 0) attackCooldown--;
        if (attackCooldown == 0 && spider.isWithinMeleeAttackRange(target)
                && spider.getSensing().hasLineOfSight(target)) {
            attackCooldown = 20;
            spider.doHurtTarget((ServerLevel)spider.level(), target);
        }
    }

    @Override public void stop() {
        if (spider.getTarget() == target) spider.setTarget(null);
        target = null;
        spider.getNavigation().stop();
    }
}

