package com.rdt.nightsplitters;

import java.util.Comparator;
import java.util.EnumSet;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.monster.Creeper;

public final class CreeperHuntGoal extends Goal {
    private final Creeper creeper;
    private ServerPlayer target;
    private int repath;
    public CreeperHuntGoal(Creeper creeper) {
        this.creeper = creeper;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }
    private boolean valid(ServerPlayer p) {
        return p != null && p.isAlive() && !p.isSpectator() && p.level() == creeper.level()
            && p.distanceToSqr(creeper) < 48 * 48;
    }
    @Override public boolean canUse() {
        if (!creeper.entityTags().contains(CreeperEncounters.MARK)) return false;
        target = ((ServerLevel)creeper.level()).players().stream().filter(this::valid)
            .min(Comparator.comparingDouble(creeper::distanceToSqr)).orElse(null);
        return target != null;
    }
    @Override public boolean canContinueToUse() { return valid(target); }
    @Override public boolean requiresUpdateEveryTick() { return true; }
    @Override public void start() { repath = 0; }
    @Override public void tick() {
        creeper.setTarget(target);
        creeper.getLookControl().setLookAt(target, 30, 30);
        if (creeper.distanceToSqr(target) < 3 * 3 && creeper.getSensing().hasLineOfSight(target))
            creeper.ignite();
        if (--repath <= 0) {
            repath = 10;
            creeper.getNavigation().moveTo(target.getX(), target.getY(), target.getZ(), 1.0);
        }
    }
    @Override public void stop() {
        if (creeper.getTarget() == target) creeper.setTarget(null);
        target = null; creeper.getNavigation().stop();
    }
}
