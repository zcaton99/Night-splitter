package com.sibcraft.creeperborn.client;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;

/** A branching white core with a blue corona; segment thickness stays constant at every range. */
public final class BoltRenderer{
    private static final Identifier WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final Identifier BLUE=Identifier.withDefaultNamespace("textures/block/light_blue_concrete.png");
    private static final ModelPart UNIT=unit();
    private static ModelPart unit(){
        MeshDefinition mesh=new MeshDefinition();
        mesh.getRoot().addOrReplaceChild("segment",CubeListBuilder.create().texOffs(0,0).addBox(-8,0,-8,16,16,16),PartPose.ZERO);
        return LayerDefinition.create(mesh,16,16).bakeRoot();
    }
    private static void layer(PoseStack poses,SubmitNodeCollector nodes,Identifier texture,float width,float length){
        poses.pushPose();poses.scale(width,length,width);
        nodes.submitModelPart(UNIT,poses,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);
        poses.popPose();
    }
    private static void segment(PoseStack poses,SubmitNodeCollector nodes,float x,float y,float z,float tx,float ty,float tz,float width){
        float dx=tx-x,dy=ty-y,dz=tz-z,length=(float)Math.sqrt(dx*dx+dy*dy+dz*dz);
        if(length<.001f)return;
        poses.pushPose();poses.translate(x,y,z);
        poses.rotate(new Quaternionf().rotationTo(0,1,0,dx/length,dy/length,dz/length));
        layer(poses,nodes,BLUE,width*2.8f,length);layer(poses,nodes,WHITE,width,length);
        poses.popPose();
    }
    public static void submit(int variant,float length,PoseStack poses,SubmitNodeCollector nodes){
        if(!Float.isFinite(length)||length<.01f||length>128)return;
        int phase=Math.floorMod(variant,3);
        poses.pushPose();poses.scale(1,1/length,1);
        float px=0,py=0,pz=0;
        for(int i=1;i<=12;i++){
            float x=i==12?0:(float)Math.sin(i*2.1+phase*2)*.24f;
            float y=i*length/12,z=i==12?0:(float)Math.cos(i*1.8+phase)*.14f;
            segment(poses,nodes,px,py,pz,x,y,z,.035f);
            if(i==4||i==8)segment(poses,nodes,x,y,z,x+(i==4?-.7f:.7f),y+Math.min(1.5f,length*.08f),z+.3f,.022f);
            px=x;py=y;pz=z;
        }
        poses.popPose();
    }
    public static void validate(){UNIT.getChild("segment");}
}
