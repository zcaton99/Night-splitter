package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
/** Original hearts, stone patches, sand blades/vortices and assembling burial coffin. */
public final class PairRenderer {
    private static final Identifier PINK=Identifier.withDefaultNamespace("textures/block/pink_concrete.png"),RED=Identifier.withDefaultNamespace("textures/block/red_concrete.png"),STONE=Identifier.withDefaultNamespace("textures/block/stone.png"),SAND=Identifier.withDefaultNamespace("textures/block/sand.png");
    private static final ModelPart HEART=heart(),RING=ring(),STATUE=statue(false),PATCH=statue(true),COFFIN=coffin(),VORTEX=vortex(),BLADE=blade(),GRAINS=grains(),SWIRL=swirl();
    private static void box(PartDefinition r,String n,float x,float y,float z,float w,float h,float d){r.addOrReplaceChild(n,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
    private static ModelPart heart(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();String[] rows={"011100001110","111110011111","111111111111","111111111111","011111111110","001111111100","000111111000","000011110000","000001100000"};for(int y=0;y<rows.length;y++)for(int x=0;x<12;x++)if(rows[y].charAt(x)=='1')box(r,"heart"+x+"_"+y,(x-6)*1.2f,(4-y)*1.2f,-.7f,1.22f,1.22f,1.4f);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart ring(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<40;i++){double a=i*Math.PI/20;box(r,"ring"+i,(float)Math.cos(a)*7.4f-.4f,(float)Math.sin(a)*7.4f-.4f,-.2f,.8f,.8f,.4f);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart statue(boolean patch){MeshDefinition m=new MeshDefinition();var r=m.getRoot();if(patch){box(r,"patch0",-4,7,-2,2,3,4);box(r,"patch1",2,11,-2,2,3,4);box(r,"patch2",-2,0,-2,2,4,4);}else{box(r,"head",-2,12,-2,4,4,4);box(r,"body",-2.5f,6,-1.8f,5,6,3.6f);box(r,"left",-4,6,-1,1.6f,6,2);box(r,"right",2.4f,6,-1,1.6f,6,2);box(r,"leg0",-2.2f,0,-1.5f,2,6,3);box(r,"leg1",.2f,0,-1.5f,2,6,3);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart coffin(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"back",-7,0,4,14,16,2);box(r,"front",-7,0,-6,14,16,2);box(r,"left",-7,0,-4,2,16,8);box(r,"right",5,0,-4,2,16,8);box(r,"lid",-7,15,-6,14,1,12);box(r,"base",-7,0,-6,14,1,12);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart vortex(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int ring=0;ring<8;ring++)for(int i=0;i<12;i++){double a=i*Math.PI/6+ring*.4;float radius=2+ring*.7f;box(r,"sand"+ring+"_"+i,(float)Math.cos(a)*radius-.5f,ring*1.8f,(float)Math.sin(a)*radius-.5f,1.1f,1.1f,1.1f);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart grains(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<32;i++){double a=i*2.4;float rad=2+i%6;box(r,"grain"+i,(float)Math.cos(a)*rad,(i%9)-4,(float)Math.sin(a)*rad,.8f,.8f,.8f);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart swirl(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int arm=0;arm<3;arm++)for(int k=0;k<28;k++){double a=arm*Math.PI*2/3+k*.24;float radius=.7f+k*.25f;box(r,"swirl"+arm+"_"+k,(float)Math.cos(a)*radius-.6f,(k%3)*.14f,(float)Math.sin(a)*radius-.6f,1.3f,.4f,1.3f);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart blade(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<12;i++){double a=(i/11.0-.5)*2.5;box(r,"blade"+i,(float)Math.sin(a)*7-.6f,(float)Math.cos(a)*6-4,-.5f,1.5f,2,.8f);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static void draw(ModelPart part,Identifier texture,PoseStack poses,SubmitNodeCollector nodes){nodes.submitModelPart(part,poses,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static int kind(String name){for(int i=1;i<=12;i++)if(name.equals("cb_pair_"+i))return i;return 0;}
    public static void submit(int kind,PoseStack poses,SubmitNodeCollector nodes,float age){poses.pushPose();
        if(kind==1||kind==2){float pulse=kind==2?1+(float)Math.sin(age*.35)*.05f:1;poses.scale(pulse,pulse,pulse);draw(HEART,kind==2?RED:PINK,poses,nodes);}
        else if(kind==3||kind==4)draw(kind==3?PATCH:STATUE,STONE,poses,nodes);
        else if(kind==5){draw(HEART,PINK,poses,nodes);poses.pushPose();poses.rotate(new Quaternionf().rotationZ(age*.05f));draw(RING,RED,poses,nodes);poses.popPose();}
        else if(kind==6)draw(BLADE,SAND,poses,nodes);
        else if(kind==7||kind==11){poses.rotate(new Quaternionf().rotationY(age*.18f));draw(VORTEX,SAND,poses,nodes);poses.pushPose();poses.scale(.8f,.9f,.8f);poses.rotate(new Quaternionf().rotationY(-age*.3f));draw(GRAINS,SAND,poses,nodes);poses.popPose();}
        else if(kind==8){float assembly=Math.max(.05f,Math.min(1,age/10));poses.pushPose();poses.scale(1,assembly,1);draw(COFFIN,SAND,poses,nodes);poses.popPose();poses.pushPose();poses.rotate(new Quaternionf().rotationY(age*.3f));draw(VORTEX,SAND,poses,nodes);poses.popPose();}
        else if(kind==9){poses.rotate(new Quaternionf().rotationY(age*.17f));draw(GRAINS,SAND,poses,nodes);}
        else if(kind==10){poses.rotate(new Quaternionf().rotationY(age*.1f));draw(SWIRL,SAND,poses,nodes);poses.pushPose();poses.rotate(new Quaternionf().rotationY(-age*.27f));poses.scale(.9f,.12f,.9f);draw(GRAINS,SAND,poses,nodes);poses.popPose();}
        else if(kind==12){poses.rotate(new Quaternionf().rotationY(age*.3f));float spread=1+Math.min(1,age/12);poses.scale(spread,spread,spread);draw(GRAINS,STONE,poses,nodes);}
        poses.popPose();}
    public static void validate(){SWIRL.getChild("swirl2_27");HEART.getChild("heart1_0");RING.getChild("ring39");COFFIN.getChild("lid");VORTEX.getChild("sand7_11");for(int i=1;i<=12;i++)if(kind("cb_pair_"+i)!=i)throw new AssertionError("Love/Sand visual mapping");}
}
