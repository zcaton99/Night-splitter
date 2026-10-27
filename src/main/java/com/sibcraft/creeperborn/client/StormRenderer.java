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

/** Original orbiting cloud puffs and branching emissive lightning, around a human-sized collision box. */
public final class StormRenderer{
    private static final Identifier DARK=Identifier.withDefaultNamespace("textures/block/gray_concrete.png");
    private static final Identifier SILVER=Identifier.withDefaultNamespace("textures/block/light_gray_concrete.png");
    private static final Identifier WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final ModelPart CORE=cloud(false),RIM=cloud(true),BOLTS=bolts();
    private static void box(PartDefinition root,String name,float x,float y,float z,float w,float h,float d){
        root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);
    }
    private static ModelPart cloud(boolean rim){
        MeshDefinition m=new MeshDefinition();var root=m.getRoot();
        box(root,"center",rim?-12:-18,rim?24:10,rim?-12:-18,rim?24:36,rim?5:16,rim?24:36);
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;float x=(float)Math.cos(a)*(rim?23:20),z=(float)Math.sin(a)*(rim?23:20);
            box(root,"puff_"+i,x-9,rim?18+(i%3):9+(i%3)*2,z-9,18,rim?8:15,18);
        }
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    private static ModelPart bolts(){
        MeshDefinition m=new MeshDefinition();var root=m.getRoot();
        for(int i=0;i<5;i++){
            var branch=root.addOrReplaceChild("bolt_"+i,CubeListBuilder.create(),PartPose.offset((i-2)*8,10,(i%2)*12-6));
            box(branch,"top",-1,-8,-1,2,10,2);
            var bend=branch.addOrReplaceChild("bend",CubeListBuilder.create(),PartPose.offsetAndRotation(0,-7,0,0,0,i%2==0?.6f:-.6f));
            box(bend,"middle",-1,-7,-1,2,8,2);
            var tip=bend.addOrReplaceChild("tip",CubeListBuilder.create(),PartPose.offsetAndRotation(0,-6,0,0,0,i%2==0?-1:1));
            box(tip,"end",-.6f,-7,-.6f,1.2f,8,1.2f);
        }
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    private static void part(ModelPart model,Identifier texture,PoseStack poses,SubmitNodeCollector nodes,boolean glow){
        nodes.submitModelPart(model,poses,glow?RenderTypes.eyes(texture):RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);
    }
    public static void submit(AvatarRenderState state,PoseStack poses,SubmitNodeCollector nodes,int flags){
        float time=state.ageInTicks;
        poses.pushPose();poses.translate(0,Math.sin(time*.1)*.08,0);
        poses.scale(3,1,3);
        poses.rotate(new Quaternionf().rotationY(time*.012f));
        part(CORE,DARK,poses,nodes,false);
        poses.rotate(new Quaternionf().rotationY(-time*.025f));
        part(RIM,SILVER,poses,nodes,false);
        if((flags&6)!=0||(int)time%25<9){
            for(int i=0;i<5;i++)BOLTS.getChild("bolt_"+i).xRot=(float)Math.sin(time*.7+i)*.12f;
            part(BOLTS,WHITE,poses,nodes,true);
        }
        poses.popPose();
    }
    public static void validate(){CORE.getChild("puff_7");RIM.getChild("center");BOLTS.getChild("bolt_4").getChild("bend").getChild("tip");}
}
