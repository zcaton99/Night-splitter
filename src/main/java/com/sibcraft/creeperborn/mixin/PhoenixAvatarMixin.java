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
    @Inject(method="submit(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;Lnet/minecraft/client/renderer/state/level/CameraRenderState;)V",at=@At("TAIL"))
    private void creeperborn$wings(LivingEntityRenderState state,PoseStack poses,SubmitNodeCollector nodes,CameraRenderState camera,CallbackInfo ci){
        if(state instanceof AvatarRenderState avatar){int celestial=CelestialVisuals.flags(avatar.id);if(((celestial&3)==2||(celestial&2048)!=0)&&(celestial&4)==0)CelestialRenderer.avatar(avatar,poses,nodes,celestial,true);int nature=NatureVisuals.flags(avatar.id);if((nature&4)==0)NatureRenderer.avatar(avatar,poses,nodes,nature,true);int azure=AzureVisuals.flags(avatar.id);if((azure&128)!=0&&(azure&3)==0)AzureRenderer.avatar(avatar,poses,nodes,azure);int flags=PhoenixVisuals.flags(avatar.id);
            if((flags&1)!=0&&(flags&2)==0)PhoenixRenderer.submit(avatar,poses,nodes,flags,false);
        }
    }
}
