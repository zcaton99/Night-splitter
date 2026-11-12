package com.sibcraft.creeperborn.client;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
/** Skin-preserving upper body with a layered translucent gas tail instead of legs. */
public final class GasRenderer {
 private static final Identifier GREEN=Identifier.fromNamespaceAndPath("creeperborn","textures/entity/gas_green.png"),PURPLE=Identifier.fromNamespaceAndPath("creeperborn","textures/entity/gas_purple.png"),ORANGE=Identifier.fromNamespaceAndPath("creeperborn","textures/entity/gas_orange.png");
 private static final ModelPart PUFF=puff(),UPPER=upper(false),SLIM=upper(true);
 private static ModelPart puff(){MeshDefinition m=new MeshDefinition();m.getRoot().addOrReplaceChild("puff",CubeListBuilder.create().texOffs(0,0).addBox(-5,-5,-5,10,10,10),PartPose.ZERO);return LayerDefinition.create(m,32,32).bakeRoot();}
 private static ModelPart upper(boolean slim){MeshDefinition m=new MeshDefinition();var r=m.getRoot();r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8).texOffs(32,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(.25f)),PartPose.ZERO);r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4).texOffs(16,32).addBox(-4,0,-2,8,12,4,new CubeDeformation(.25f)),PartPose.ZERO);for(int side:new int[]{-1,1}){boolean left=side==1;float w=slim?3:4;r.addOrReplaceChild("arm"+side,CubeListBuilder.create().texOffs(left?32:40,left?48:16).addBox(-w/2,-2,-2,w,12,4).texOffs(left?48:40,left?48:32).addBox(-w/2,-2,-2,w,12,4,new CubeDeformation(.25f)),PartPose.offset(side*(slim?5.5f:6),2,0));}return LayerDefinition.create(m,64,64).bakeRoot();}
 private static void draw(ModelPart part,Identifier texture,PoseStack p,SubmitNodeCollector n){n.submitModelPart(part,p,RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
 public static int kind(String name){if(name.equals("cb_gas_1"))return 1;if(name.equals("cb_gas_2"))return 2;if(name.equals("cb_gas_3"))return 3;return 0;}
 public static void submit(int kind,float time,PoseStack p,SubmitNodeCollector n){
  if(kind<1||kind>3)return;
  p.pushPose();int count=kind==2?5:14;
  for(int i=0;i<count;i++){p.pushPose();double angle=i*2.399+time*(i%2==0?.012:-.009),radius=kind==2?.2:(i%3==0?.15:.65);p.translate(Math.cos(angle)*radius,.35+(i%3)*.38+Math.sin(time*.07+i)*.09,Math.sin(angle)*radius);p.rotate(new Quaternionf().rotationY((float)angle));float size=kind==2?.8f:1.2f+(i%3)*.2f;p.scale(size,size,size);draw(PUFF,kind==3?ORANGE:i%3==0?PURPLE:GREEN,p,n);p.popPose();}p.popPose();
 }
 public static void avatar(AvatarRenderState state,PoseStack p,SubmitNodeCollector n,int flags){
  var c=Minecraft.getInstance();var owner=c.level==null?null:c.level.getEntity(state.id);boolean body=(flags&1)!=0;float time=state.ageInTicks;
  if(body&&owner instanceof AbstractClientPlayer player){var skin=player.getSkin();ModelPart model=skin.model().getSerializedName().equals("slim")?SLIM:UPPER;
   p.pushPose();p.rotate(new Quaternionf().rotationY((float)Math.toRadians(180-state.bodyRot)));p.translate(0,1.5+Math.sin(time*.08)*.035,0);p.scale(-1,-1,1);model.getChild("head").xRot=(float)Math.toRadians(state.xRot);draw(model.getChild("head"),skin.body().texturePath(),p,n);draw(model.getChild("body"),skin.body().texturePath(),p,n);for(int side:new int[]{-1,1}){var arm=model.getChild("arm"+side);arm.xRot=(float)Math.sin(time*.12)*.08f;arm.zRot=side*.12f;draw(arm,skin.body().texturePath(),p,n);}p.popPose();
  }
  p.pushPose();if(body){for(int i=0;i<12;i++){p.pushPose();double a=i*2.4+time*.045,rad=.15+(i%3)*.1;double h=.12+(i%5)*.17;p.translate(Math.cos(a)*rad,h,Math.sin(a)*rad);p.rotate(new Quaternionf().rotationY((float)a));float size=.55f+(i%5)*.12f;p.scale(size,size*.8f,size);draw(PUFF,i%3==0?PURPLE:GREEN,p,n);p.popPose();}}
  if((flags&2)!=0){p.scale(2,.8f,2);submit(1,time,p,n);}p.popPose();
 }
 public static List<Identifier> textures(){return List.of(GREEN,PURPLE,ORANGE);}
 public static void validate(){UPPER.getChild("head");SLIM.getChild("arm1");PUFF.getChild("puff");}
}
