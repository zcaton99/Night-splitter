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

/** Original articulated model: a body, head, crown, talons, layered wing feathers and a forked tail. */
public final class PhoenixRenderer{
    private static final Identifier RED=Identifier.withDefaultNamespace("textures/block/red_concrete.png");
    private static final Identifier FIRE=Identifier.withDefaultNamespace("textures/block/orange_concrete.png");
    private static final Identifier GOLD=Identifier.withDefaultNamespace("textures/block/gold_block.png");
    private static final Identifier WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final ModelPart BODY=body(),WINGS=wings(false),FEATHERS=wings(true),CREST=crest(),GOLD_PARTS=gold(),EYES=eyes(),FIRE_TIPS=fireTips();
    private static ModelPart bake(MeshDefinition mesh){return LayerDefinition.create(mesh,16,16).bakeRoot();}
    private static void box(PartDefinition root,String name,float x,float y,float z,float w,float h,float d){
        root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);
    }
    private static ModelPart body(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        box(r,"breast",-4,8,-6,8,10,12);box(r,"shoulders",-5,14,-3,10,5,8);
        box(r,"neck",-2.5f,17,2,5,5,5);box(r,"head",-3,20,3,6,6,7);
        return bake(m);
    }
    private static ModelPart wings(boolean accents){
        MeshDefinition m=new MeshDefinition();var root=m.getRoot();
        for(int side:new int[]{-1,1}){
            String name=side==1?"left":"right";
            var wing=root.addOrReplaceChild(name,CubeListBuilder.create(),PartPose.offset(side*4,16,0));
            if(!accents){
                box(wing,"shoulder",side==1?0:-14,-1,-4,14,2,9);
                var outer=wing.addOrReplaceChild("outer",CubeListBuilder.create(),PartPose.offset(side*13,0,1));
                box(outer,"edge",side==1?0:-15,-.5f,-3,15,1,7);
                for(int i=0;i<5;i++)box(outer,"flight_"+i,side==1?i*3:-i*3-3,-.7f,-8-i,3,1.4f,10+i);
            }else{
                var outer=wing.addOrReplaceChild("outer",CubeListBuilder.create(),PartPose.offset(side*13,0,1));
                for(int i=0;i<5;i++)box(outer,"flame_"+i,side==1?i*3+.5f:-i*3-2.5f,0,-10-i,2,1,8+i);
                for(int i=0;i<3;i++)box(wing,"cover_"+i,side==1?i*4+.5f:-i*4-4.5f,.15f,-5-i,4,.8f,7);
            }
        }
        return bake(m);
    }
    /** Layered orange/yellow tongues sit above and below the red leading feathers. */
    private static ModelPart fireTips(){
        MeshDefinition m=new MeshDefinition();var root=m.getRoot();
        for(int side:new int[]{-1,1}){
            var wing=root.addOrReplaceChild(side==1?"left":"right",CubeListBuilder.create(),PartPose.offset(side*4,16,0));
            var outer=wing.addOrReplaceChild("outer",CubeListBuilder.create(),PartPose.offset(side*13,0,1));
            for(int i=0;i<5;i++){
                float x=side==1?i*3+1:-i*3-2;
                box(outer,"ember_"+i,x,-1.1f,-11-i,1,2.4f,5+i);
            }
            for(int i=0;i<3;i++)box(wing,"ember_cover_"+i,side==1?i*4+1.5f:-i*4-2.5f,-1.2f,-6-i,1,2.8f,4);
        }
        return bake(m);
    }
    private static ModelPart crest(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        for(int i=-1;i<=1;i++)box(r,"crown_"+i,i*1.7f-.5f,25,3-Math.abs(i),1,6-Math.abs(i)*2,2);
        // Orange flame plumage overlaps the red body, with raised tongues at the shoulders.
        for(int side:new int[]{-1,1}){
            for(int i=0;i<3;i++)box(r,"body_flame_"+side+"_"+i,side==1?3.7f:-4.6f,9+i*2,-4+i*2,.9f,5-i,3);
            box(r,"shoulder_flame_"+side,side==1?3.5f:-5,17,-1,1.5f,6,3);
            box(r,"cheek_flame_"+side,side==1?2.8f:-3.6f,21,5,.8f,4,4);
        }
        box(r,"chest_glow",-2.5f,9,5.8f,5,9,.6f);
        var tail=r.addOrReplaceChild("tail",CubeListBuilder.create(),PartPose.offset(0,10,-5));
        for(int i=-1;i<=1;i++)box(tail,"tail_"+i,i*2.5f-1,-.5f,-22+Math.abs(i)*4,2,1,23-Math.abs(i)*4);
        return bake(m);
    }
    private static ModelPart gold(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        box(r,"beak",-1.5f,21.5f,9,3,2,5);
        for(int i:new int[]{-1,1}){
            box(r,"leg_"+i,i*2-.6f,3,-.5f,1.2f,6,1.2f);
            for(int j=-1;j<=1;j++)box(r,"talon_"+i+"_"+j,i*2+j*.7f-.3f,2,0,.6f,1,4);
        }
        box(r,"breast_flame",-1.5f,10,6.4f,3,7,.4f);
        box(r,"heart_ember",-.6f,16,6.45f,1.2f,3,.5f);
        for(int side:new int[]{-1,1}){
            box(r,"crown_ember_"+side,side*1.7f-.3f,25.5f,3,.6f,3,1);
            box(r,"shoulder_ember_"+side,side==1?4:-4.8f,19,-.5f,.8f,5,1.2f);
        }
        return bake(m);
    }
    private static ModelPart eyes(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        box(r,"left_eye",2.9f,23,7,.25f,1.5f,1.5f);
        box(r,"right_eye",-3.15f,23,7,.25f,1.5f,1.5f);
        return bake(m);
    }
    private static void poseWings(ModelPart root,float flap){
        var left=root.getChild("left");var right=root.getChild("right");
        left.zRot=flap;right.zRot=-flap;
        left.getChild("outer").zRot=flap*.35f-.15f;right.getChild("outer").zRot=-flap*.35f+.15f;
    }
    private static void part(ModelPart model,Identifier texture,PoseStack poses,SubmitNodeCollector nodes,boolean glow){
        nodes.submitModelPart(model,poses,glow?RenderTypes.eyes(texture):RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);
    }
    public static void submit(AvatarRenderState state,PoseStack poses,SubmitNodeCollector nodes,int flags,boolean full){
        float time=state.ageInTicks,flap=(float)Math.sin(time*((flags&8)!=0?.8:.4))*.6f+.15f;
        if((flags&4)!=0)flap=(float)Math.sin(time*.18f)*.2f+.65f;
        poseWings(WINGS,flap);poseWings(FEATHERS,flap);poseWings(FIRE_TIPS,flap);
        CREST.getChild("tail").xRot=(float)Math.sin(time*.18f)*.12f;
        poses.pushPose();
        poses.rotate(new Quaternionf().rotationY((float)Math.toRadians(-state.bodyRot)));
        if(full){
            poses.translate(0,Math.sin(time*.15)*.06,0);
            poses.rotate(new Quaternionf().rotationX((float)Math.toRadians(state.xRot*.45f)));
            if((flags&8)!=0)poses.rotate(new Quaternionf().rotationX(.2f));
            part(BODY,RED,poses,nodes,false);part(CREST,FIRE,poses,nodes,true);
            part(GOLD_PARTS,GOLD,poses,nodes,true);part(EYES,WHITE,poses,nodes,true);
        }else poses.translate(0,.35,-.12);
        part(WINGS,RED,poses,nodes,false);part(FEATHERS,FIRE,poses,nodes,true);
        part(FIRE_TIPS,GOLD,poses,nodes,true);
        poses.popPose();
    }
    /** Client smoke tests initialize and validate the full mesh without entering a world. */
    public static void validate(){BODY.getChild("head");WINGS.getChild("left").getChild("outer");FEATHERS.getChild("right");CREST.getChild("tail");CREST.getChild("chest_glow");GOLD_PARTS.getChild("heart_ember");FIRE_TIPS.getChild("left").getChild("outer");}
}
