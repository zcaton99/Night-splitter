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
        if(((AzureHiddenState)state).creeperborn$azureHidden()){ci.cancel();return;}
        if(state instanceof AvatarRenderState avatar){
            var local=net.minecraft.client.Minecraft.getInstance().player;if(local!=null&&avatar.id==local.getId()&&(ZoanVisuals.flags(avatar.id)&15)==13&&ZoanVisuals.hideLocalFoxBody()){ci.cancel();return;}
            int overlayLuffy=LuffyVisuals.flags(avatar.id);if((overlayLuffy&1)!=0&&(overlayLuffy&2)==0)LuffyRenderer.avatar(avatar,poses,nodes,overlayLuffy);int overlayZoan=ZoanVisuals.flags(avatar.id);if((overlayZoan&3)==2&&((overlayZoan>>2)&3)!=0&&!ZoanRenderer.full(overlayZoan))ZoanRenderer.avatar(avatar,poses,nodes,overlayZoan);
            int luffy=LuffyVisuals.flags(avatar.id);if((luffy&2)!=0){LuffyRenderer.avatar(avatar,poses,nodes,luffy);ci.cancel();return;}int zoan=ZoanVisuals.flags(avatar.id);if(ZoanRenderer.full(zoan)){ZoanRenderer.avatar(avatar,poses,nodes,zoan);ci.cancel();return;}
            int celestial=CelestialVisuals.flags(avatar.id);if((celestial&3)==1&&(celestial&12)!=0||(celestial&7)==6){CelestialRenderer.avatar(avatar,poses,nodes,celestial,false);ci.cancel();return;}
            int nature=NatureVisuals.flags(avatar.id);if((nature&16)!=0){ci.cancel();return;}if((nature&4)!=0){NatureRenderer.avatar(avatar,poses,nodes,nature,false);ci.cancel();return;}
            int azure=AzureVisuals.flags(avatar.id);if((azure&3)==2){ci.cancel();return;}if((azure&3)==1){AzureRenderer.avatar(avatar,poses,nodes,azure);ci.cancel();return;}
            int dark=DarkVisuals.flags(avatar.id);
            if((dark&3)!=0){ShadowRenderer.submit(avatar,poses,nodes,dark);ci.cancel();return;}
            int lightning=LightningVisuals.flags(avatar.id);
            if((lightning&1)!=0){StormRenderer.submit(avatar,poses,nodes,lightning);ci.cancel();return;}
            int flags=PhoenixVisuals.flags(avatar.id);
            if((flags&2)!=0){PhoenixRenderer.submit(avatar,poses,nodes,flags,true);ci.cancel();}
        }
    }
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At(value="INVOKE",target="Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V",shift=At.Shift.BEFORE))
    private void creeperborn$animatedFox(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector nodes,CameraRenderState camera,CallbackInfo ci){if(state instanceof AvatarRenderState avatar&&((LivingEntityRenderer<?,?,?>)(Object)this).getModel() instanceof net.minecraft.client.model.HumanoidModel<?> playerModel)FoxCloakRenderer.submit(avatar,poses,nodes,playerModel,ZoanVisuals.flags(avatar.id));}
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("TAIL"))
    private void creeperborn$wings(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector nodes,CameraRenderState camera,CallbackInfo ci){
        if(state instanceof AvatarRenderState avatar){int celestial=CelestialVisuals.flags(avatar.id);if(((celestial&3)==2||(celestial&2048)!=0)&&(celestial&4)==0)CelestialRenderer.avatar(avatar,poses,nodes,celestial,true);int nature=NatureVisuals.flags(avatar.id);if((nature&4)==0)NatureRenderer.avatar(avatar,poses,nodes,nature,true);int azure=AzureVisuals.flags(avatar.id);if((azure&128)!=0&&(azure&3)==0)AzureRenderer.avatar(avatar,poses,nodes,azure);int flags=PhoenixVisuals.flags(avatar.id);
            if((flags&1)!=0&&(flags&2)==0)PhoenixRenderer.submit(avatar,poses,nodes,flags,false);
        }
    }
}
