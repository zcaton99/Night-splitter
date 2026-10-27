package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.StringVisuals;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(Minecraft.class)
public abstract class StringClientInteractionMixin {
    @Inject(method="startAttack",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringAttack(CallbackInfoReturnable<Boolean> cir){if(StringVisuals.controlling())cir.setReturnValue(false);}
    @Inject(method="continueAttack",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringBreak(boolean held,CallbackInfo ci){if(StringVisuals.controlling())ci.cancel();}
    @Inject(method="startUseItem",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringUse(CallbackInfo ci){if(StringVisuals.controlling())ci.cancel();}
}
