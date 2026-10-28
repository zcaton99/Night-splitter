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
        ((BoltVisualState)state).creeperborn$azureKind(AzureRenderer.kind(display.getCustomName()==null?"":display.getCustomName().getString()));
        ((BoltVisualState)state).creeperborn$climateKind(ClimateRenderer.kind(display.getCustomName()==null?"":display.getCustomName().getString()));
        ((BoltVisualState)state).creeperborn$pairKind(PairRenderer.kind(display.getCustomName()==null?"":display.getCustomName().getString()));
        ((BoltVisualState)state).creeperborn$tremorKind(TremorRenderer.kind(display.getCustomName()==null?"":display.getCustomName().getString()));
        ((BoltVisualState)state).creeperborn$pawKind(PawRenderer.kind(display.getCustomName()==null?"":display.getCustomName().getString()));
        ((BoltVisualState)state).creeperborn$stringKind(StringRenderer.kind(display.getCustomName()==null?"":display.getCustomName().getString()));
        String gravityName=display.getCustomName()==null?"":display.getCustomName().getString();
        ((BoltVisualState)state).creeperborn$gravity(gravityName.equals("cb_gravity_meteor")?1:gravityName.equals("cb_gravity_rock")?2:0);
        ((BoltVisualState)state).creeperborn$opRoom(display.getCustomName()!=null&&display.getCustomName().getString().equals("cb_op_room"));
        ((BoltVisualState)state).creeperborn$darkGround(display.getCustomName()!=null&&display.getCustomName().getString().startsWith("cb_dark_ground|")?display.getCustomName().getString():"");
        ((BoltVisualState)state).creeperborn$darkVoid(display.getCustomName()!=null&&display.getCustomName().getString().equals("cb_dark_void"));
        PunchVisualState visual=(PunchVisualState)state;visual.creeperborn$punch(null);
        ((BoltVisualState)state).creeperborn$bolt(display.getCustomName()==null?-1:com.sibcraft.creeperborn.LightningBolts.variant(display.getCustomName().getString()));
        ((BoltVisualState)state).creeperborn$boltLength(display.getCustomName()==null?1:com.sibcraft.creeperborn.LightningBolts.length(display.getCustomName().getString()));
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
        int azureKind=((BoltVisualState)state).creeperborn$azureKind();if(azureKind>0){AzureRenderer.submit(azureKind,poses,collector,state.ageInTicks);ci.cancel();return;}
        int climateKind=((BoltVisualState)state).creeperborn$climateKind();if(climateKind>0){ClimateRenderer.submit(climateKind,poses,collector,state.ageInTicks);ci.cancel();return;}
        int pairKind=((BoltVisualState)state).creeperborn$pairKind();if(pairKind>0){PairRenderer.submit(pairKind,poses,collector,state.ageInTicks);ci.cancel();return;}
        int tremorKind=((BoltVisualState)state).creeperborn$tremorKind();if(tremorKind>0){TremorRenderer.submit(tremorKind,poses,collector,state.ageInTicks);ci.cancel();return;}
        int pawKind=((BoltVisualState)state).creeperborn$pawKind();if(pawKind>0){PawRenderer.submit(pawKind,poses,collector,state.ageInTicks);ci.cancel();return;}
        int stringKind=((BoltVisualState)state).creeperborn$stringKind();if(stringKind>0){StringRenderer.submit(stringKind,poses,collector,state.ageInTicks);ci.cancel();return;}
        int gravity=((BoltVisualState)state).creeperborn$gravity();if(gravity>0){GravityRenderer.submit(gravity,poses,collector,state.ageInTicks);ci.cancel();return;}
        if(((BoltVisualState)state).creeperborn$opRoom()){RoomRenderer.submit(poses,collector);ci.cancel();return;}
        if(!((BoltVisualState)state).creeperborn$darkGround().isEmpty()){DarkGroundRenderer.submit(((BoltVisualState)state).creeperborn$darkGround(),poses,collector);ci.cancel();return;}
        if(((BoltVisualState)state).creeperborn$darkVoid()){VoidRenderer.submit(poses,collector,state.ageInTicks);ci.cancel();return;}
        int bolt=((BoltVisualState)state).creeperborn$bolt();
        if(bolt>=0){BoltRenderer.submit(bolt,((BoltVisualState)state).creeperborn$boltLength(),poses,collector);ci.cancel();return;}
        var data=((PunchVisualState)state).creeperborn$punch();
        if(data!=null){PunchRenderer.submit(data,poses,collector,light);ci.cancel();}
    }
}
