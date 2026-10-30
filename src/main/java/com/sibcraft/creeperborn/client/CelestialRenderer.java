package com.sibcraft.creeperborn.client;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** Geometry-native light stars and woven straw, with articulated human/scarecrow silhouettes. */
public final class CelestialRenderer {
 private static Identifier tex(String name){return Identifier.withDefaultNamespace("textures/block/"+name+".png");}
 private static final Identifier GOLD=tex("yellow_concrete"),WHITE=tex("white_concrete"),STRAW=tex("hay_block_side"),WOOD=tex("stripped_oak_log"),BLACK=tex("black_concrete"),PURPLE=tex("purple_concrete");
 private static final ModelPart STAR=star(),BEAM=beam(),HUMAN=human(),SWORD=sword(),ROPE=rope(),MONSTER=monster(),DOLL=doll(),SCARE=scarecrow(),FACE=face(),CARD=card(),CLAWS=claws();
 private static void box(PartDefinition p,String n,float x,float y,float z,float w,float h,float d){p.addOrReplaceChild(n,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
 private static ModelPart bake(MeshDefinition m){return LayerDefinition.create(m,16,16).bakeRoot();}
 private static ModelPart.Vertex v(float x,float y,float z){return new ModelPart.Vertex(x,y,z,.5f,.5f);}
 private static void quad(List<ModelPart.Cube> list,ModelPart.Vertex a,ModelPart.Vertex b,ModelPart.Vertex c,ModelPart.Vertex d,Vector3f normal){var cube=new ModelPart.Cube(0,0,0,0,0,1,1,1,0,0,0,false,16,16,Set.of(Direction.NORTH));cube.polygons[0]=new ModelPart.Polygon(new ModelPart.Vertex[]{a,b,c,d},normal);list.add(cube);}
 private static ModelPart star(){List<ModelPart.Cube> faces=new ArrayList<>();for(int i=0;i<8;i++){double a=i*Math.PI/4,b=(i+1)*Math.PI/4;float ra=i%2==0?14:2.4f,rb=(i+1)%2==0?14:2.4f;quad(faces,v(0,0,0),v((float)Math.cos(a)*ra,(float)Math.sin(a)*ra,0),v((float)Math.cos(b)*rb,(float)Math.sin(b)*rb,0),v(0,0,0),new Vector3f(0,0,1));}return new ModelPart(faces,Map.of());}
 private static ModelPart beam(){List<ModelPart.Cube> faces=new ArrayList<>();for(int i=0;i<8;i++){double a=i*Math.PI/4,b=(i+1)*Math.PI/4;quad(faces,v((float)Math.cos(a)*1.3f,(float)Math.sin(a)*1.3f,0),v((float)Math.cos(b)*1.3f,(float)Math.sin(b)*1.3f,0),v((float)Math.cos(b)*1.3f,(float)Math.sin(b)*1.3f,16),v((float)Math.cos(a)*1.3f,(float)Math.sin(a)*1.3f,16),new Vector3f((float)Math.cos(a),(float)Math.sin(a),0));}return new ModelPart(faces,Map.of());}
 private static ModelPart human(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"torso",-4,10,-2,8,12,4);box(r,"head",-4,22,-4,8,8,8);for(int side:new int[]{-1,1}){var arm=r.addOrReplaceChild("arm"+side,CubeListBuilder.create(),PartPose.offset(side*5.5f,21,0));box(arm,"limb",-1.5f,-12,-1.5f,3,13,3);box(r,"leg"+side,side*2-1.8f,0,-1.8f,3.6f,11,3.6f);}return bake(m);}
 private static ModelPart sword(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"blade",-.55f,-.4f,1,1.1f,.8f,20);box(r,"guard",-3,-.7f,0,6,1.4f,1.5f);box(r,"grip",-.6f,-.6f,-5,1.2f,1.2f,5);return bake(m);}
 private static ModelPart rope(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<24;i++){double a=i*.85;box(r,"fiber"+i,(float)Math.cos(a)*1.2f-.35f,(float)Math.sin(a)*1.2f-.35f,i*16/24f,.7f,.7f,1.2f);}return bake(m);}
 private static ModelPart doll(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"body",-3,7,-2,6,9,4);box(r,"head",-3.5f,16,-3,7,7,6);for(int side:new int[]{-1,1}){box(r,"arm"+side,side*5-1,10,-1,2,7,2);box(r,"leg"+side,side*2-1,0,-1,2,8,2);}return bake(m);}
 private static ModelPart scarecrow(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"chest",-8,27,-4,16,22,8);box(r,"head",-6,49,-4.5f,12,14,9);box(r,"waist",-5,24,-3,10,8,6);for(int side:new int[]{-1,1}){var arm=r.addOrReplaceChild("arm"+side,CubeListBuilder.create(),PartPose.offset(side*10,46,0));box(arm,"straw",-2,-17,-2,4,18,4);var fore=arm.addOrReplaceChild("fore",CubeListBuilder.create(),PartPose.offset(0,-16,0));box(fore,"stalks",-2.5f,-14,-2.5f,5,15,5);for(int i=0;i<4;i++)box(fore,"claw"+i,-3+i*1.7f,-24+i%2,-1,1,11,1);var leg=r.addOrReplaceChild("leg"+side,CubeListBuilder.create(),PartPose.offset(side*4,27,0));box(leg,"stalks",-2.5f,-27,-2.5f,5,28,5);for(int i=0;i<4;i++)box(r,"fray"+side+i,side*(7+i),27-i*2,-3,1,10,1);}return bake(m);}
 private static ModelPart face(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int side:new int[]{-1,1}){box(r,"eye"+side,side*2.5f-1,57,4.55f,2,1, .2f);}box(r,"mouth",-4,52,4.55f,8,1,.2f);for(int i=0;i<5;i++)box(r,"stitch"+i,-3.5f+i*1.7f,51.4f,4.8f,.5f,2,.15f);return bake(m);}
 private static ModelPart monster(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"head",-7,-6,0,14,12,10);box(r,"mouth",-5,-4,10,10,7,1);for(int i=0;i<6;i++){box(r,"fang"+i,-5+i*1.8f,2,11,1,4,1);box(r,"jawfang"+i,-5+i*1.8f,-6,11,1,3,1);}return bake(m);}
 private static ModelPart card(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"paper",-4,-6,-.2f,8,12,.4f);return bake(m);}
 private static ModelPart claws(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<4;i++)box(r,"talon"+i,-3+i*2,-.4f,0,.7f,.8f,16);return bake(m);}
 private static void draw(ModelPart m,Identifier t,PoseStack p,SubmitNodeCollector n,boolean light){n.submitModelPart(m,p,light?RenderTypes.eyes(t):RenderTypes.entityTranslucent(t),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
 private static void star(PoseStack p,SubmitNodeCollector n,float age,float size){p.pushPose();p.rotate(new Quaternionf().rotationZ(age*.05f));p.scale(size,size,size);for(int axis=0;axis<3;axis++){p.pushPose();if(axis==1)p.rotate(new Quaternionf().rotationY((float)Math.PI/2));if(axis==2)p.rotate(new Quaternionf().rotationX((float)Math.PI/2));draw(STAR,GOLD,p,n,true);p.scale(.6f,.6f,.6f);draw(STAR,WHITE,p,n,true);p.popPose();}p.popPose();}
 public static int kind(String name){if(!name.startsWith("cb_celestial_"))return 0;try{int k=Integer.parseInt(name.substring(13,name.indexOf('|')));return k>=1&&k<=17?k:0;}catch(RuntimeException ignored){return 0;}}
 public static void submit(int kind,float age,PoseStack p,SubmitNodeCollector n){p.pushPose();
  if(kind!=3&&age>85){p.popPose();return;}
  if(kind==1||kind==5){draw(BEAM,GOLD,p,n,true);p.pushPose();p.scale(.45f,.45f,1);draw(BEAM,WHITE,p,n,true);p.popPose();star(p,n,age,.5f);p.pushPose();p.translate(0,0,1);star(p,n,age,.7f);p.popPose();}
  else if(kind==2){float burst=Math.max(.1f,(float)Math.sin(Math.min(1,age/16)*Math.PI));star(p,n,age,burst);}
  else if(kind==3){for(int side:new int[]{-1,1})HUMAN.getChild("arm"+side).xRot=.3f+(float)Math.sin(age*.09+side)*.12f;draw(HUMAN,GOLD,p,n,true);p.pushPose();p.translate(0,1.2,0);star(p,n,age,.25f);p.popPose();}
  else if(kind==4||kind==6){p.rotate(new Quaternionf().rotationY((kind==4?1:-1)*(-1.1f+age*.16f)));draw(SWORD,GOLD,p,n,true);star(p,n,age,.3f);}
  else if(kind==11){draw(CLAWS,STRAW,p,n,false);}
  else if(kind==12){float length=p.last().pose().getScale(new Vector3f()).z;draw(ROPE,STRAW,p,n,false);p.translate(0,0,1);p.scale(1,1,1/Math.max(1,length));draw(MONSTER,STRAW,p,n,false);}
  else if(kind==13){draw(ROPE,STRAW,p,n,false);}
  else if(kind==14){p.scale(.3f,.3f,.3f);draw(DOLL,STRAW,p,n,false);}
  else if(kind==15){for(int i=0;i<7;i++){p.pushPose();double a=i*Math.PI*2/7;p.translate(Math.cos(a)*.4,0,Math.sin(a)*.4);p.rotate(new Quaternionf().rotationX((float)-Math.PI/2));p.scale(1,1,.8f);draw(ROPE,STRAW,p,n,false);p.popPose();}}
  else if(kind==16){for(int i=0;i<14;i++){p.pushPose();p.rotate(new Quaternionf().rotationY(i*(float)Math.PI/7));p.scale(.4f,.4f,1);draw(ROPE,STRAW,p,n,false);p.popPose();}}
  else if(kind==17){p.rotate(new Quaternionf().rotationY(age*.1f));draw(CARD,PURPLE,p,n,false);p.scale(.45f,.45f,.45f);star(p,n,age,.4f);}
  p.popPose();}
 public static void avatar(AvatarRenderState s,PoseStack p,SubmitNodeCollector n,int flags,boolean overlay){boolean light=(flags&3)==1,form=(flags&4)!=0;float age=s.ageInTicks;p.pushPose();p.rotate(new Quaternionf().rotationY((float)Math.toRadians(-s.bodyRot)));
  if(!overlay){if(light){for(int side:new int[]{-1,1})HUMAN.getChild("arm"+side).xRot=(float)Math.sin(age*.12+side)*.18f;draw(HUMAN,GOLD,p,n,true);p.pushPose();p.translate(0,1.4,0);star(p,n,age,.25f);p.popPose();for(int i=0;i<5;i++){p.pushPose();double a=age*.05+i*Math.PI*2/5;p.translate(Math.cos(a)*.55,.3+i*.3,Math.sin(a)*.55);star(p,n,age,.1f);p.popPose();}}else if(form){for(int side:new int[]{-1,1}){SCARE.getChild("arm"+side).zRot=side*.2f;SCARE.getChild("arm"+side).getChild("fore").xRot=.1f+(float)Math.sin(age*.1+side)*.1f;SCARE.getChild("leg"+side).xRot=(float)Math.sin(age*.1+side)*.08f;}draw(SCARE,STRAW,p,n,false);draw(FACE,BLACK,p,n,false);}}
  if(!light){int count=(flags>>4)&15;for(int i=0;i<count;i++){p.pushPose();int row=i/5,col=i%5;double a=(col-2)*.32;p.translate(Math.sin(a)*(1.6+row*.15),.6+row*.5+(form?1.5:0),-Math.cos(a)*(1.6+row*.15));p.scale(.22f,.22f,.22f);draw(DOLL,STRAW,p,n,false);p.popPose();}}
  p.popPose();}
 public static List<Identifier> textures(){return List.of(GOLD,WHITE,STRAW,WOOD,BLACK,PURPLE);}
 public static void validate(){SCARE.getChild("arm1").getChild("fore");HUMAN.getChild("arm-1");if(kind("cb_celestial_17|owner")!=17||kind("bad")!=0)throw new AssertionError("Celestial metadata");}
}
