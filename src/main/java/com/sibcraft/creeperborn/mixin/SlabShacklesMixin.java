package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.Shackles;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class SlabShacklesMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void creeperborn$rootedTravel(Vec3 input,CallbackInfo ci){
        LivingEntity target=(LivingEntity)(Object)this;
        if(Shackles.rooted(target)){target.setDeltaMovement(0,0,0);ci.cancel();}
    }
}

