package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.LuffyVisuals;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public abstract class LuffySpinMixin {
 @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
 private void creeperborn$luffySpin(LivingEntity entity,LivingEntityRenderState state,float partial,CallbackInfo ci){if(state instanceof AvatarRenderState avatar)avatar.bodyRot+=LuffyVisuals.spin(avatar.id);}
}
