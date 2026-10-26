package com.sibcraft.creeperborn.mixin;
import net.minecraft.world.entity.Display;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Display.class)
public interface PunchDisplayAccessor {
    @Invoker("setTransformationInterpolationDuration") void creeperborn$duration(int ticks);
    @Invoker("setTransformationInterpolationDelay") void creeperborn$delay(int ticks);
    @Invoker("setPosRotInterpolationDuration") void creeperborn$positionDuration(int ticks);
    @Invoker("setViewRange") void creeperborn$view(float range);
    @Invoker("setWidth") void creeperborn$width(float width);
    @Invoker("setHeight") void creeperborn$height(float height);
}

