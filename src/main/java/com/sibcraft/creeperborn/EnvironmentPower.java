package com.sibcraft.creeperborn;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

public final class EnvironmentPower {
    private EnvironmentPower() {}
    public static double strength(Player p, Element e) {
        boolean wet = p.isInWater() || p.level().getFluidState(p.blockPosition()).is(FluidTags.WATER);
        return switch (e) {
            case EARTH -> !wet && p.level().getBlockState(p.blockPosition().below()).isSolidRender() ? 1 : .55;
            case WATER -> wet || Abilities.waterSource(p) ? 1 : .55;
            case FIRE -> wet ? .35 : hot(p) ? 1 : p.isInWaterOrRain() ? .45 : .70;
            case AIR -> wet ? .55 : p.onGround() ? .75 : 1.25;
        };
    }
    private static boolean hot(Player p) {
        if (p.level().dimension() == Level.NETHER || p.level().getBiome(p.blockPosition()).value().getBaseTemperature() >= 1) return true;
        BlockPos center = p.blockPosition();
        for (BlockPos pos : BlockPos.betweenClosed(center.offset(-4, -2, -4), center.offset(4, 2, 4))) {
            int x = pos.getX(), z = pos.getZ();
            if (p.level().hasChunksAt(x, z, x, z) && p.level().getFluidState(pos).is(FluidTags.LAVA)) return true;
        }
        return false;
    }
}
