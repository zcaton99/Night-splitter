package com.sibcraft.creeperborn.client;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import org.joml.Quaternionf;
/** Skin-matched articulated mirages dissolve into clock fragments. */
public final class TimeRenderer {
 public record Data(int kind,Identifier skin,boolean slim){}
 private static final Identifier CYAN=Identifier.withDefaultNamespace("textures/block/light_blue_concrete.png"),GOLD=Identifier.withDefaultNamespace("textures/block/yellow_concrete.png");
 private static final ModelPart[] PEOPLE={human(false),human(true)};
 private static final ModelPart CLOCK=clock(),HAND=hand(),SHARD=shard();
 private static ModelPart human(boolean slim){MeshDefinition m=new MeshDefinition();var r=m.getRoot();r.addOrReplaceChild("head",CubeListBuilder.create().texOffs(0,0).addBox(-4,-8,-4,8,8,8).texOffs(32,0).addBox(-4,-8,-4,8,8,8,new CubeDeformation(.25f)),PartPose.ZERO);r.addOrReplaceChild("body",CubeListBuilder.create().texOffs(16,16).addBox(-4,0,-2,8,12,4).texOffs(16,32).addBox(-4,0,-2,8,12,4,new CubeDeformation(.25f)),PartPose.ZERO);for(int side:new int[]{-1,1}){boolean left=side==1;float w=slim?3:4;r.addOrReplaceChild("arm"+side,CubeListBuilder.create().texOffs(left?32:40,left?48:16).addBox(-w/2,-2,-2,w,12,4).texOffs(left?48:40,left?48:32).addBox(-w/2,-2,-2,w,12,4,new CubeDeformation(.25f)),PartPose.offset(side*(slim?5.5f:6),2,0));r.addOrReplaceChild("leg"+side,CubeListBuilder.create().texOffs(left?16:0,left?48:16).addBox(-2,0,-2,4,12,4).texOffs(left?0:0,left?48:32).addBox(-2,0,-2,4,12,4,new CubeDeformation(.25f)),PartPose.offset(side*2,12,0));}return LayerDefinition.create(m,64,64).bakeRoot();}
 private static void box(PartDefinition r,String name,float x,float y,float z,float w,float h,float d){r.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
 private static ModelPart clock(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<12;i++){double a=i*Math.PI/6;var q=r.addOrReplaceChild("tick"+i,CubeListBuilder.create(),PartPose.offsetAndRotation((float)Math.sin(a)*11,(float)Math.cos(a)*11,0,0,0,(float)-a));box(q,"mark",-.5f,-1.6f,-.25f,1,3.2f,.5f);}return LayerDefinition.create(m,16,16).bakeRoot();}
 private static ModelPart hand(){MeshDefinition m=new MeshDefinition();box(m.getRoot(),"hand",-.4f,0,-.3f,.8f,8,.6f);return LayerDefinition.create(m,16,16).bakeRoot();}
 private static ModelPart shard(){MeshDefinition m=new MeshDefinition();box(m.getRoot(),"shard",-1,-1,-.3f,2,2,.6f);return LayerDefinition.create(m,16,16).bakeRoot();}
 public static Data extract(Display.BlockDisplay d){if(d.getCustomName()==null)return null;String name=d.getCustomName().getString();if(!name.startsWith("cb_time_"))return null;try{String[] a=name.split("\\|",2);int k=Integer.parseInt(a[0].substring(8));if(k<1||k>5)return null;var c=Minecraft.getInstance();if(c.player!=null&&c.options.getCameraType().isFirstPerson()){var delta=d.position().add(0,1,0).subtract(c.player.getEyePosition());if(delta.lengthSqr()<9||k==1&&delta.dot(c.player.getLookAngle())>0&&delta.lengthSqr()<100)k=0;}var owner=c.level==null?null:c.level.getPlayerByUUID(UUID.fromString(a[1]));if(owner instanceof AbstractClientPlayer p)return new Data(k,p.getSkin().body().texturePath(),p.getSkin().model().getSerializedName().equals("slim"));return new Data(k,null,false);}catch(RuntimeException ignored){return null;}}
 private static void draw(ModelPart m,Identifier tex,PoseStack p,SubmitNodeCollector n,boolean glow){n.submitModelPart(m,p,glow?RenderTypes.eyes(tex):RenderTypes.entityTranslucent(tex),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
 public static void submit(Data data,float age,PoseStack p,SubmitNodeCollector n){p.pushPose();int kind=data.kind();if(kind==0){p.popPose();return;}if(kind==1||kind==5){if(kind==1&&age>=24){p.popPose();return;}var model=PEOPLE[data.slim()?1:0];p.pushPose();p.translate(0,1.5,0);p.scale(-1,-1,1);for(String part:new String[]{"head","body","arm-1","arm1","leg-1","leg1"}){var q=model.getChild(part);q.xRot=part.startsWith("arm")?(float)Math.sin(Math.min(age,6)*.3)*(part.endsWith("-1")?-.35f:.35f):part.startsWith("leg")?(float)Math.sin(Math.min(age,6)*.3)*(part.endsWith("-1")?.4f:-.4f):0;if(kind==5||age<16+(part.hashCode()&7))draw(q,data.skin()==null?CYAN:data.skin(),p,n,data.skin()==null);}p.popPose();for(int i=0;i<9;i++)if(kind==1&&age>10+i){p.pushPose();double a=i*2.4;p.translate(Math.cos(a)*(age-10)*.04,.3+i*.17,Math.sin(a)*(age-10)*.04);p.rotate(new Quaternionf().rotationZ(age*.12f+i));p.scale(.6f,.6f,.6f);draw(SHARD,CYAN,p,n,true);p.popPose();}}
 else{if(kind==3){p.translate(0,.08,0);p.rotate(new Quaternionf().rotationX((float)Math.PI/2));}if(kind==4){p.translate(0,1,0);p.scale(.65f,.65f,.65f);}p.rotate(new Quaternionf().rotationZ(age*.015f));draw(CLOCK,CYAN,p,n,true);p.pushPose();p.rotate(new Quaternionf().rotationZ(age*.12f));draw(HAND,GOLD,p,n,true);p.popPose();p.pushPose();p.rotate(new Quaternionf().rotationZ(-age*.055f));p.scale(.65f,.65f,1);draw(HAND,CYAN,p,n,true);p.popPose();if(kind==2)for(int i=0;i<6;i++){p.pushPose();double a=i*Math.PI/3+age*.025;p.translate(Math.cos(a)*1.05,Math.sin(a)*1.05,Math.sin(age*.04+i)*.2);p.rotate(new Quaternionf().rotationZ(age*.1f+i));draw(SHARD,GOLD,p,n,true);p.popPose();}}
 p.popPose();}
 public static List<Identifier> textures(){return List.of(CYAN,GOLD);}
 public static void validate(){PEOPLE[0].getChild("arm-1");PEOPLE[1].getChild("leg1");CLOCK.getChild("tick11");}
}
