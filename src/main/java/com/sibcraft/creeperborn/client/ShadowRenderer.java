package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
public final class ShadowRenderer{
    private static final Identifier BLACK=Identifier.withDefaultNamespace("textures/block/black_concrete.png"),VIOLET=Identifier.withDefaultNamespace("textures/block/purple_concrete.png");
    private static final ModelPart BODY=body(),EYES=eyes(),SHADOW=shadow(),HAND=hand();
    private static void box(PartDefinition root,String name,float x,float y,float z,float w,float h,float d){root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
    private static ModelPart bake(MeshDefinition m){return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart body(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        box(r,"head",-4,24,-4,8,8,8);box(r,"torso",-4,12,-2,8,12,4);
        for(int side:new int[]{-1,1}){
            r.addOrReplaceChild("arm_"+side,CubeListBuilder.create().texOffs(0,0).addBox(-2,-11,-2,4,12,4),PartPose.offset(side*6,23,0));
            r.addOrReplaceChild("leg_"+side,CubeListBuilder.create().texOffs(0,0).addBox(-2,-12,-2,4,12,4),PartPose.offset(side*2,12,0));
        }return bake(m);
    }
    private static ModelPart eyes(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"eyes",-2.8f,28,4.05f,5.6f,.65f,.2f);box(r,"chest",-1,16,2.05f,2,5,.2f);return bake(m);}
    private static ModelPart shadow(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();box(r,"pool",-10,0,-14,20,.5f,28);box(r,"rim",-8,.2f,-12,16,.5f,24);return bake(m);}
    private static ModelPart hand(){MeshDefinition m=new MeshDefinition();box(m.getRoot(),"hand",-2,-12,-2,4,12,4);return bake(m);}
    private static void part(ModelPart part,Identifier texture,PoseStack poses,SubmitNodeCollector nodes,boolean glow){nodes.submitModelPart(part,poses,glow?RenderTypes.eyes(texture):RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static void submit(AvatarRenderState state,PoseStack poses,SubmitNodeCollector nodes,int flags){
        poses.pushPose();poses.rotate(new Quaternionf().rotationY((float)Math.toRadians(-state.bodyRot)));
        if((flags&2)!=0){part(SHADOW,BLACK,poses,nodes,false);}
        else{
            float swing=(float)Math.sin(state.ageInTicks*.15)*.12f;
            BODY.getChild("arm_-1").xRot=swing;BODY.getChild("arm_1").xRot=-swing;BODY.getChild("leg_-1").xRot=-swing;BODY.getChild("leg_1").xRot=swing;
            part(BODY,BLACK,poses,nodes,false);part(EYES,VIOLET,poses,nodes,true);
        }poses.popPose();
    }
    public static void submitHand(PoseStack poses,SubmitNodeCollector nodes){
        poses.pushPose();poses.translate(.42,-.35,-.55);poses.rotate(new Quaternionf().rotationX(1.15f));part(HAND,BLACK,poses,nodes,false);poses.popPose();
    }
    public static void validate(){BODY.getChild("arm_1");SHADOW.getChild("pool");HAND.getChild("hand");}
}
