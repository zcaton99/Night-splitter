package com.sibcraft.creeperborn.client;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
/** Original rubber limbs, inflated silhouette, white smoke scarf and stretched return orb. */
public final class LuffyRenderer {
 private static Identifier t(String n){return Identifier.withDefaultNamespace("textures/block/"+n+".png");}
 private static final Identifier SKIN=t("white_terracotta"),WHITE=t("white_concrete"),RED=t("red_concrete"),BLACK=t("netherite_block"),GOLD=t("yellow_concrete"),FIRE=t("orange_concrete"),PURPLE=t("purple_concrete"),STONE=t("stone"),CYAN=t("light_blue_concrete");
 private static void box(PartDefinition r,String n,float x,float y,float z,float w,float h,float d){r.addOrReplaceChild(n,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
 private static ModelPart bake(MeshDefinition m){return LayerDefinition.create(m,16,16).bakeRoot();}
 private static final ModelPart BALL=ball(),ARM=arm(),FIST=fist(),FOOT=foot(),WRAP=wrap(),RING=ring(),HAIR=hair(),FACE=face(),BOLT=bolt();
 private static ModelPart ball(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"center",-7,-6,-6,14,12,12);box(r,"sides",-8,-4,-4,16,8,8);box(r,"top",-5,-8,-5,10,16,10);return bake(m);}
 private static ModelPart arm(){MeshDefinition m=new MeshDefinition();box(m.getRoot(),"rubber",-1.3f,-1.3f,0,2.6f,2.6f,16);return bake(m);}
 private static ModelPart fist(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"palm",-4,-4,-3,8,8,7);for(int i=0;i<4;i++)box(r,"knuckle"+i,-4+i*2,-3,3,1.9f,6,2);box(r,"thumb",3,-5,-1,3,5,3);return bake(m);}
 private static ModelPart foot(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"leg",-2,-2,-16,4,4,16);box(r,"foot",-4,-2,-1,8,5,11);for(int i=0;i<4;i++)box(r,"toe"+i,-4+i*2,-1,9,1.9f,3,3);return bake(m);}
 private static ModelPart wrap(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<8;i++){var q=r.addOrReplaceChild("limb"+i,CubeListBuilder.create(),PartPose.rotation(0,i*(float)Math.PI/4,i%2==0?.5f:-.5f));box(q,"arm",-1,0,-8,2,2,16);box(q,"hand",-2,0,6,4,3,4);}return bake(m);}
 private static ModelPart ring(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<24;i++){double a=i*Math.PI/12;var q=r.addOrReplaceChild("puff"+i,CubeListBuilder.create(),PartPose.offset((float)Math.cos(a)*8,(float)Math.sin(a)*8,0));box(q,"cloud",-1.7f,-1.7f,-1.5f,3.4f,3.4f,3);}return bake(m);}
 private static ModelPart hair(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<14;i++){double a=i*2.4;box(r,"curl"+i,(float)Math.cos(a)*3-1.6f,29+(i%3)*1.1f,(float)Math.sin(a)*3-1.6f,3.2f,3.2f,3.2f);}return bake(m);}
 private static ModelPart bolt(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<8;i++){float side=i%2==0?1:-1;var q=r.addOrReplaceChild("zig"+i,CubeListBuilder.create(),PartPose.offsetAndRotation(side*1.5f,0,i*3-12,0,side*.65f,0));box(q,"energy",-1.1f,-1,0,2.2f,2,5);}return bake(m);}
 private static ModelPart face(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"head",-4,35,-4,8,8,8);for(int side:new int[]{-1,1}){box(r,"hand"+side,side*19-2,14,-2,4,7,4);box(r,"foot"+side,side*7-2,0,0,4,4,8);}return bake(m);}
 private static void draw(ModelPart model,Identifier tex,PoseStack p,SubmitNodeCollector n,boolean glow){n.submitModelPart(model,p,glow?RenderTypes.eyes(tex):RenderTypes.entityTranslucent(tex),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
 public static void effect(int kind,float age,PoseStack p,SubmitNodeCollector n){p.pushPose();if(kind==40){p.rotate(new Quaternionf().rotationZ((float)Math.sin(age*.25)*.2f));draw(BOLT,GOLD,p,n,true);p.scale(.5f,.5f,1);draw(BOLT,WHITE,p,n,true);}else if(kind==41){draw(BALL,FIRE,p,n,true);p.scale(.7f,.7f,.7f);draw(BALL,GOLD,p,n,true);}else if(kind==42){float squash=1+(float)Math.sin(age*.3)*.15f;p.scale(squash,1/squash,1);draw(BALL,STONE,p,n,false);}else if(kind==43){draw(BALL,CYAN,p,n,true);p.rotate(new Quaternionf().rotationY(age*.15f));draw(RING,WHITE,p,n,true);}else if(kind==33)draw(ARM,SKIN,p,n,false);else if(kind==34){draw(FIST,BLACK,p,n,false);for(int i=0;i<5;i++){p.pushPose();p.translate(Math.sin(age*.3+i)*.3,Math.cos(age*.3+i)*.3,-i*.16);p.scale(.25f,.35f,.25f);draw(BALL,FIRE,p,n,true);p.popPose();}}else if(kind==35)draw(FIST,SKIN,p,n,false);else if(kind==36)draw(FOOT,SKIN,p,n,false);else if(kind==37){p.rotate(new Quaternionf().rotationY(age*.08f));draw(WRAP,SKIN,p,n,false);}else if(kind==38){float stretch=1+(float)Math.sin(age*.4)*.2f;p.scale(stretch,1/stretch,1);draw(BALL,PURPLE,p,n,true);p.scale(.7f,.7f,.7f);draw(BALL,GOLD,p,n,true);p.rotate(new Quaternionf().rotationZ(age*.2f));draw(RING,WHITE,p,n,true);}else if(kind==39){p.rotate(new Quaternionf().rotationX((float)Math.PI/2));p.scale(1,1,.035f);draw(RING,WHITE,p,n,false);}p.popPose();}
 public static void avatar(AvatarRenderState state,PoseStack p,SubmitNodeCollector n,int flags){p.pushPose();p.rotate(new Quaternionf().rotationY((float)Math.toRadians(-state.bodyRot)));if((flags&2)!=0){p.pushPose();p.translate(0,1.4,0);p.scale(2.7f,2.4f,2.3f);draw(BALL,(flags&1)!=0?WHITE:RED,p,n,false);p.popPose();draw(FACE,SKIN,p,n,false);}if((flags&1)!=0){p.pushPose();if((flags&2)!=0)p.translate(0,.75,0);draw(HAIR,WHITE,p,n,false);p.popPose();p.pushPose();p.translate(0,1.4,0);p.rotate(new Quaternionf().rotationY(state.ageInTicks*.025f));p.scale(1.4f,.6f,1);draw(RING,WHITE,p,n,false);p.popPose();for(int side:new int[]{-1,1}){p.pushPose();p.translate(side*.6,1.5,-.2);p.scale(.22f,.7f,.22f);p.rotate(new Quaternionf().rotationZ(side*.4f));draw(RING,WHITE,p,n,false);p.popPose();}}p.popPose();}
 public static float spin(float age){return Math.max(0,Math.min(12,age))*30;}
 public static List<Identifier> textures(){return List.of(SKIN,WHITE,RED,BLACK,GOLD,FIRE,PURPLE,STONE,CYAN);}
 public static void validate(){FIST.getChild("knuckle3");WRAP.getChild("limb7");HAIR.getChild("curl13");if(spin(12)!=360||spin(6)!=180)throw new AssertionError("One complete spin before release");}
}
