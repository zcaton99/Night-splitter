package com.sibcraft.creeperborn;

import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class TemporaryRockMixin {
    @Inject(method = "shouldBeSaved", at = @At("HEAD"), cancellable = true)
    private void creeperborn$temporary(CallbackInfoReturnable<Boolean> result) {
        if (((Entity)(Object)this).entityTags().contains("cb_temporary_rock")) result.setReturnValue(false);
    }
}
