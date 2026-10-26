package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.Shackles;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class SlabShacklesMoveMixin {
    @Inject(method="move",at=@At("HEAD"),cancellable=true)
    private void creeperborn$blockCustomMovement(MoverType type,Vec3 movement,CallbackInfo ci){
        if(type==MoverType.SELF&&(Object)this instanceof LivingEntity target&&Shackles.rooted(target)){
            target.setDeltaMovement(0,0,0);ci.cancel();
        }
    }
}

