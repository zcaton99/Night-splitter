package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
/** Branching glass-air fractures and curled, voxel water walls; no world blocks. */
public final class TremorRenderer {
    private static final Identifier WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png"),BLUE=Identifier.withDefaultNamespace("textures/block/light_blue_concrete.png"),DEEP=Identifier.withDefaultNamespace("textures/block/blue_concrete.png");
    private static final ModelPart CRACK=crack(),RING=ring(),WAVE=wave(),GROUND=ground();
    private static ModelPart crack(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();int n=0;for(int ray=0;ray<7;ray++){double a=ray*Math.PI*2/7;float px=0,py=0;for(int k=0;k<5;k++){float nx=(float)Math.cos(a+(k%2==0?.16:-.16))*(k+1)*1.4f,ny=(float)Math.sin(a+(k%2==0?.16:-.16))*(k+1)*1.4f;segment(r,"crack"+n++,px,py,nx,ny,.16f);if(k==2||k==3)segment(r,"branch"+n++,nx,ny,nx+(float)Math.cos(a+.9)*2,ny+(float)Math.sin(a+.9)*2,.1f);px=nx;py=ny;}}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static void segment(PartDefinition r,String name,float x,float y,float xx,float yy,float width){float dx=xx-x,dy=yy-y,length=(float)Math.sqrt(dx*dx+dy*dy);r.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(-width/2,0,-.08f,width,length,.16f),PartPose.offsetAndRotation(x,y,0,0,0,(float)Math.atan2(-dx,dy)));}
    private static ModelPart ring(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<40;i++){double a=i*Math.PI/20;r.addOrReplaceChild("ring"+i,CubeListBuilder.create().texOffs(0,0).addBox((float)Math.cos(a)*7.5f-.4f,(float)Math.sin(a)*7.5f-.4f,-.2f,.8f,.8f,.4f),PartPose.ZERO);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart wave(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int x=0;x<12;x++){float crest=13+(float)Math.sin(x*.8)*1.3f;r.addOrReplaceChild("water"+x,CubeListBuilder.create().texOffs(0,0).addBox(-8+x*1.33f,0,-1,1.4f,crest,2),PartPose.ZERO);r.addOrReplaceChild("curl"+x,CubeListBuilder.create().texOffs(0,0).addBox(-8+x*1.33f,crest-2,-3,1.5f,2,4),PartPose.ZERO);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart ground(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int x=0;x<3;x++)for(int z=0;z<3;z++)r.addOrReplaceChild("tile"+x+z,CubeListBuilder.create().texOffs(0,0).addBox(-8+x*5.4f,-.8f,-8+z*5.4f,5,1.2f,5),PartPose.ZERO);return LayerDefinition.create(m,16,16).bakeRoot();}
    private static void draw(ModelPart model,Identifier texture,PoseStack poses,SubmitNodeCollector nodes){nodes.submitModelPart(model,poses,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static int kind(String name){for(int i=1;i<=6;i++)if(name.equals("cb_tremor_"+i))return i;return 0;}
    public static void submit(int kind,PoseStack poses,SubmitNodeCollector nodes,float age){poses.pushPose();
        if(kind==6){float lift=(float)Math.sin(Math.min(1,age/30)*Math.PI);poses.translate(0,lift*.7f,0);poses.rotate(new Quaternionf().rotationXYZ(lift*.3f,0,lift*.2f));draw(GROUND,Identifier.withDefaultNamespace("textures/block/stone.png"),poses,nodes);}
        else if(kind==1){float growth=Math.min(1,.15f+age/5);poses.scale(growth,growth,1);draw(CRACK,WHITE,poses,nodes);}
        else if(kind==3){draw(WAVE,DEEP,poses,nodes);poses.pushPose();poses.translate(0,.03f,-.08f);poses.scale(1,1.02f,1);draw(WAVE,BLUE,poses,nodes);poses.popPose();poses.pushPose();poses.translate(0,.87f,-.12f);poses.scale(1,.12f,1.2f);draw(RING,WHITE,poses,nodes);poses.popPose();}
        else{float pulse=kind==4?Math.min(1,.05f+age/20):1+(float)Math.sin(age*.45)*.08f;poses.scale(pulse,pulse,pulse);draw(RING,WHITE,poses,nodes);poses.pushPose();poses.rotate(new Quaternionf().rotationY((float)Math.PI/2));draw(RING,BLUE,poses,nodes);poses.popPose();if(kind==2||kind==5){poses.pushPose();poses.rotate(new Quaternionf().rotationX((float)Math.PI/2));draw(RING,WHITE,poses,nodes);poses.popPose();}}
        poses.popPose();}
    public static void validate(){GROUND.getChild("tile22");CRACK.getChild("crack0");WAVE.getChild("water11");RING.getChild("ring39");for(int i=1;i<=6;i++)if(kind("cb_tremor_"+i)!=i)throw new AssertionError("Tremor visual mapping");}
}
