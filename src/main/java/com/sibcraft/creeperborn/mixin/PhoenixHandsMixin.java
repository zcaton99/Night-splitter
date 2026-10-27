package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.PhoenixVisuals;
import com.sibcraft.creeperborn.client.LightningVisuals;
import com.sibcraft.creeperborn.client.DarkVisuals;
import com.sibcraft.creeperborn.client.ShadowRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class PhoenixHandsMixin{
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void creeperborn$birdHands(float partial,PoseStack poses,SubmitNodeCollector nodes,PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,CallbackInfo ci){
        if(player.avatarRenderState!=null&&(DarkVisuals.flags(player.avatarRenderState.id)&3)!=0){if((DarkVisuals.flags(player.avatarRenderState.id)&2)==0)ShadowRenderer.submitHand(poses,nodes);ci.cancel();return;}
        if(player.avatarRenderState!=null&&((PhoenixVisuals.flags(player.avatarRenderState.id)&2)!=0||(LightningVisuals.flags(player.avatarRenderState.id)&1)!=0))ci.cancel();
    }
}
