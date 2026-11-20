package com.rdt.connectedfreedom.mixin;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.GoalSelector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
@Mixin(Mob.class)
public interface MobGoalsAccessor {
    @Accessor("goalSelector") GoalSelector rdt$goals();
    @Accessor("targetSelector") GoalSelector rdt$targets();
}
