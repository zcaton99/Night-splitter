package com.rdt.nightsplitters.mixin;

import net.minecraft.world.entity.monster.Creeper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(Creeper.class)
public interface CreeperAccess {
    @Accessor("explosionRadius") void nightSplitters$setExplosionRadius(int radius);
    @Invoker("explodeCreeper") void nightSplitters$explode();
}
