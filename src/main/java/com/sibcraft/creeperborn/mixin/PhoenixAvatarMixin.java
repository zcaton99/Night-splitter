package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public abstract class PhoenixAvatarMixin{
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("HEAD"),cancellable=true)
    private void creeperborn$bird(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector nodes,CameraRenderState camera,CallbackInfo ci){
        if(state instanceof AvatarRenderState avatar){
            int lightning=LightningVisuals.flags(avatar.id);
            if((lightning&1)!=0){StormRenderer.submit(avatar,poses,nodes,lightning);ci.cancel();return;}
            int flags=PhoenixVisuals.flags(avatar.id);
            if((flags&2)!=0){PhoenixRenderer.submit(avatar,poses,nodes,flags,true);ci.cancel();}
        }
    }
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("TAIL"))
    private void creeperborn$wings(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector nodes,CameraRenderState camera,CallbackInfo ci){
        if(state instanceof AvatarRenderState avatar){int flags=PhoenixVisuals.flags(avatar.id);
            if((flags&1)!=0&&(flags&2)==0)PhoenixRenderer.submit(avatar,poses,nodes,flags,false);
        }
    }
}
