package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.*;
import net.minecraft.client.model.player.PlayerModel;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(PlayerModel.class)
public abstract class RasenganPoseMixin {
    @Inject(method="setupAnim(Lnet/minecraft/client/renderer/entity/state/AvatarRenderState;)V",at=@At("TAIL"))
    private void creeperborn$rasenganArm(AvatarRenderState state,CallbackInfo ci){RasenganRenderer.pose((PlayerModel)(Object)this,state,ZoanVisuals.flags(state.id));}
}
