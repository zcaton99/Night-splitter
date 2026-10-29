package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
/** Fixed-budget native meshes for glaciers, lava fields, crystal prisons and ground-fed flight. */
public final class ClimateRenderer {
    private static final Identifier ICE=Identifier.withDefaultNamespace("textures/block/packed_ice.png"),BLUE=Identifier.withDefaultNamespace("textures/block/blue_ice.png"),LAVA=Identifier.withDefaultNamespace("textures/block/lava_still.png"),MAGMA=Identifier.withDefaultNamespace("textures/block/magma.png"),WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final ModelPart SPEAR=spear(),BIRD=bird(),PLATFORM=platform(),GLACIER=field(true),POOLS=field(false),FIST=fist(),HAND=hand(),MOLTEN=molten(),HOUND=hound(),SHELL=shell(),COFFIN=coffin(),AURA=aura(),COLUMN=column(),RING=ring(),SHARDS=shards();
    private static void box(PartDefinition r,String n,float x,float y,float z,float w,float h,float d){r.addOrReplaceChild(n,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
    private static ModelPart bake(MeshDefinition m){return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart spear(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"shaft",-1,-1,-8,2,2,12);box(r,"blade",-2,-2,4,4,4,4);box(r,"tip",-.8f,-.8f,8,1.6f,1.6f,3);return bake(m);}
    private static ModelPart bird(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"body",-2,-2,-3,4,4,7);box(r,"head",-1.7f,0,3,3.4f,3.4f,3);box(r,"beak",-.7f,.8f,6,1.4f,1.4f,3);for(int i=0;i<5;i++){box(r,"wingL"+i,-4-i*1.6f,1-i*.3f,-2-i*.8f,3,1,5);box(r,"wingR"+i,1+i*1.6f,1-i*.3f,-2-i*.8f,3,1,5);}box(r,"tail",-1.5f,-1,-7,3,1,5);return bake(m);}
    private static ModelPart platform(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int x=0;x<3;x++)for(int z=0;z<3;z++)box(r,"tile"+x+z,-8+x*5.3f,-.4f,-8+z*5.3f,5.3f,1,5.3f);return bake(m);}
    private static ModelPart field(boolean ice){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int ring=0;ring<7;ring++){int count=12+ring*6;float radius=ring+1;for(int i=0;i<count;i++){double a=i*Math.PI*2/count;float x=(float)Math.cos(a)*radius,z=(float)Math.sin(a)*radius;box(r,"surface"+ring+"_"+i,x-.7f,0,z-.7f,1.5f,ice?.18f:.12f,1.5f);if(ice&&i%4==0)box(r,"crystal"+ring+"_"+i,x-.13f,.1f,z-.13f,.26f,2+ring*.2f,.26f);}}box(r,"center",-1,0,-1,2,.15f,2);return bake(m);}
    private static ModelPart fist(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"palm",-5,-4,-2,10,8,7);for(int i=0;i<4;i++)box(r,"knuckle"+i,-5+i*2.5f,1,3,2.3f,3,4);box(r,"thumb",4,-3,0,3,5,4);box(r,"wrist",-3,-3,-6,6,6,4);return bake(m);}
    private static ModelPart hand(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"palm",-5,-3,-2,10,6,7);box(r,"wrist",-3,-2.5f,-7,6,5,5);for(int i=0;i<4;i++){float x=-5+i*2.65f,length=i==0||i==3?6:8;var finger=r.addOrReplaceChild("finger"+i,CubeListBuilder.create(),PartPose.offsetAndRotation(x+1,0,4,0,(i-1.5f)*.08f,0));box(finger,"digit",-1,-2,0,2,4,length);box(finger,"tip",-.8f,-1.6f,length,1.6f,3.2f,1.5f);}var thumb=r.addOrReplaceChild("thumb",CubeListBuilder.create(),PartPose.offsetAndRotation(4,-1,1,0,.65f,0));box(thumb,"digit",0,-1.5f,0,2.6f,3,6);return bake(m);}
    private static ModelPart molten(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"heart",-3.5f,-3.08f,-1,7,6.16f,5);for(int i=0;i<4;i++){float x=-4+i*2.65f;box(r,"vein"+i,x-.35f,-2.08f,4,.7f,4.16f,i==0||i==3?6:8);}return bake(m);}
    private static ModelPart hound(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"body",-2,3,-5,4,5,9);box(r,"head",-3,6,3,6,5,5);box(r,"snout",-2,6,8,4,2.5f,4);box(r,"ear0",-2.7f,10,4,1.5f,3,2);box(r,"ear1",1.2f,10,4,1.5f,3,2);for(int i=0;i<4;i++)box(r,"leg"+i,i%2==0?-2.5f:1,0,i<2?-4:2,1.5f,4,2);box(r,"tail",-.6f,6,-9,1.2f,1.2f,5);return bake(m);}
    private static ModelPart shell(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"helmet",-2.3f,12,-2.3f,4.6f,4.3f,4.6f);box(r,"chest",-2.8f,6,-2,5.6f,6,4);box(r,"arm0",-4.2f,6,-1.3f,1.8f,6,2.6f);box(r,"arm1",2.4f,6,-1.3f,1.8f,6,2.6f);box(r,"leg0",-2.4f,0,-1.8f,2.2f,6,3.6f);box(r,"leg1",.2f,0,-1.8f,2.2f,6,3.6f);return bake(m);}
    private static ModelPart coffin(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<8;i++){double a=i*Math.PI/4;box(r,"wall"+i,(float)Math.cos(a)*4-1,0,(float)Math.sin(a)*4-1,2,15+i%3,2);}box(r,"cap",-4.5f,15,-4.5f,9,1.5f,9);return bake(m);}
    private static ModelPart aura(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<12;i++){double a=i*Math.PI/6;box(r,"heat"+i,(float)Math.cos(a)*5-.5f,0,(float)Math.sin(a)*5-.5f,1,12+i%4,1);}return bake(m);}
    private static ModelPart column(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<12;i++){double a=i*Math.PI/6;box(r,"stream"+i,(float)Math.cos(a)*3-.4f,0,(float)Math.sin(a)*3-.4f,.8f,16,.8f);}return bake(m);}
    private static ModelPart ring(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<32;i++){double a=i*Math.PI/16;box(r,"r"+i,(float)Math.cos(a)*7-.4f,(float)Math.sin(a)*7-.4f,0,.8f,.8f,.4f);}return bake(m);}
    private static ModelPart shards(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<24;i++){double a=i*2.4;box(r,"shard"+i,(float)Math.cos(a)*(i%7+1),(i%8)-3,(float)Math.sin(a)*(i%7+1),.5f,1.4f,.5f);}return bake(m);}
    private static void draw(ModelPart model,Identifier texture,PoseStack poses,SubmitNodeCollector nodes){nodes.submitModelPart(model,poses,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static int kind(String name){for(int i=1;i<=18;i++)if(name.equals("cb_climate_"+i))return i;return 0;}
    public static void submit(int kind,PoseStack poses,SubmitNodeCollector nodes,float age){poses.pushPose();
        if(kind==1||kind==2)draw(SPEAR,kind==2?BLUE:ICE,poses,nodes);
        else if(kind==3){poses.rotate(new Quaternionf().rotationZ((float)Math.sin(age*.4)*.04f));draw(BIRD,BLUE,poses,nodes);}
        else if(kind==4)draw(PLATFORM,ICE,poses,nodes);
        else if(kind==5)draw(GLACIER,ICE,poses,nodes);
        else if(kind==6){draw(HAND,MAGMA,poses,nodes);draw(MOLTEN,LAVA,poses,nodes);for(int i=0;i<3;i++){poses.pushPose();poses.translate(Math.sin(age*.4+i)*.03,Math.cos(age*.4+i)*.03,-.4-i*.25);float size=.6f-i*.15f;poses.scale(size,size,size);draw(SHARDS,LAVA,poses,nodes);poses.popPose();}}
        else if(kind==7||kind==9){draw(FIST,MAGMA,poses,nodes);poses.pushPose();float pulse=.9f+(float)Math.sin(age*.5)*.03f;poses.scale(pulse,pulse,pulse);draw(FIST,LAVA,poses,nodes);poses.popPose();}
        else if(kind==8)draw(HOUND,MAGMA,poses,nodes);
        else if(kind==10){draw(POOLS,LAVA,poses,nodes);}
        else if(kind==11){draw(AURA,LAVA,poses,nodes);poses.pushPose();poses.rotate(new Quaternionf().rotationY(age*.12f));draw(SHARDS,MAGMA,poses,nodes);poses.popPose();}
        else if(kind==12||kind==17)draw(SHELL,kind==17?BLUE:ICE,poses,nodes);
        else if(kind==13){draw(COFFIN,ICE,poses,nodes);poses.pushPose();poses.scale(.8f,1.02f,.8f);draw(COFFIN,BLUE,poses,nodes);poses.popPose();}
        else if(kind==14){poses.rotate(new Quaternionf().rotationY(age*.2f));draw(SHARDS,ICE,poses,nodes);}
        else if(kind==15){poses.rotate(new Quaternionf().rotationZ(age*.1f));draw(RING,ICE,poses,nodes);poses.pushPose();poses.rotate(new Quaternionf().rotationY((float)Math.PI/2));draw(RING,WHITE,poses,nodes);poses.popPose();}
        else if(kind==16){poses.rotate(new Quaternionf().rotationY(age*.07f));draw(COLUMN,LAVA,poses,nodes);}
        else if(kind==18){float growth=.2f+Math.min(1,age/12);poses.scale(growth,growth,growth);draw(SHARDS,LAVA,poses,nodes);draw(RING,MAGMA,poses,nodes);}
        poses.popPose();}
    public static void validate(){SPEAR.getChild("tip");HAND.getChild("finger3").getChild("tip");HAND.getChild("thumb");HOUND.getChild("snout");GLACIER.getChild("center");COFFIN.getChild("cap");COLUMN.getChild("stream11");for(int i=1;i<=18;i++)if(kind("cb_climate_"+i)!=i)throw new AssertionError("Ice/Magma visual mapping");}
}
