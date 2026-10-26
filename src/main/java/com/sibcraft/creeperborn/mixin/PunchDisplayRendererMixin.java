package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.Punches;
import com.sibcraft.creeperborn.client.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.player.PlayerModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(DisplayRenderer.BlockDisplayRenderer.class)
public abstract class PunchDisplayRendererMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/Display$BlockDisplay;Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;F)V",at=@At("TAIL"))
    private void creeperborn$extractSkin(Display.BlockDisplay display,BlockDisplayEntityRenderState state,float partial,CallbackInfo ci){
        PunchVisualState visual=(PunchVisualState)state;visual.creeperborn$punch(null);
        var tag=display.getCustomName()==null?null:Punches.visual(display.getCustomName().getString());
        if(tag==null)return;
        var client=Minecraft.getInstance();
        var owner=client.level==null?null:client.level.getPlayerByUUID(tag.owner());
        PunchRenderer.SkinData data=new PunchRenderer.SkinData(null,tag.left(),tag.fist(),false,false);
        if(owner instanceof AbstractClientPlayer player){
            var skin=player.getSkin();
            data=new PunchRenderer.SkinData(skin.body().texturePath(),tag.left(),tag.fist(),skin.model().getSerializedName().equals("slim"),
                player.isModelPartShown(tag.left()?PlayerModelPart.LEFT_SLEEVE:PlayerModelPart.RIGHT_SLEEVE));
        }
        visual.creeperborn$punch(data);
    }
    @Inject(method="submitInner(Lnet/minecraft/client/renderer/entity/state/BlockDisplayEntityRenderState;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;IF)V",at=@At("HEAD"),cancellable=true)
    private void creeperborn$submitArm(BlockDisplayEntityRenderState state,PoseStack poses,SubmitNodeCollector collector,int light,float partial,CallbackInfo ci){
        var data=((PunchVisualState)state).creeperborn$punch();
        if(data!=null){PunchRenderer.submit(data,poses,collector,light);ci.cancel();}
    }
}

