package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class RasenganHandsMixin {
    @Inject(method="submitHandsWithItems",at=@At("HEAD"),cancellable=true)
    private void creeperborn$rasenganHands(float partial,PoseStack poses,SubmitNodeCollector nodes,PlayerRenderState player,FirstPersonHandsAndItemsRenderState hands,CallbackInfo ci){if(player.avatarRenderState==null)return;int flags=ZoanVisuals.flags(player.avatarRenderState.id);if((flags&3)==1&&((flags>>2)&3)!=3&&(flags&768)!=0){RasenganRenderer.hands(poses,nodes,flags,partial);ci.cancel();}}
}
