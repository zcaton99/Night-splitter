package com.sibcraft.creeperborn.mixin;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class TemporaryRockMixin {
    @Inject(method="baseTick",at=@At("HEAD"))
    private void creeperborn$pawExpiry(CallbackInfo ci){com.sibcraft.creeperborn.Paw.tickVisual((Entity)(Object)this);com.sibcraft.creeperborn.Tremor.tickVisual((Entity)(Object)this);}
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void creeperborn$temporary(CallbackInfoReturnable<Boolean> result) {
        if (((Entity)(Object)this).entityTags().contains("cb_temporary_rock")) result.setReturnValue(false);
    }
}
