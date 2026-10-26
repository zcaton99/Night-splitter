package com.sibcraft.creeperborn.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

public final class PunchRenderer {
    public record SkinData(Identifier texture,boolean left,boolean fist,boolean slim,boolean sleeve){}
    private static final ModelPart[][][] MODELS=new ModelPart[2][2][2];
    private static final ModelPart[][] SLEEVES=new ModelPart[2][2];
    static{
        for(int left=0;left<2;left++)for(int slim=0;slim<2;slim++){
            float width=slim==1?3:4;
            for(int fist=0;fist<2;fist++){
                MeshDefinition mesh=new MeshDefinition();
                mesh.getRoot().addOrReplaceChild("part",CubeListBuilder.create().texOffs(left==1?32:40,fist==1?(left==1?56:24):(left==1?48:16))
                    .addBox(-width/2,fist==1?-2:0,-2,width,fist==1?4:12,4),PartPose.ZERO);
                MODELS[left][slim][fist]=LayerDefinition.create(mesh,64,64).bakeRoot().getChild("part");
            }
            MeshDefinition sleeve=new MeshDefinition();
            sleeve.getRoot().addOrReplaceChild("part",CubeListBuilder.create().texOffs(left==1?48:40,left==1?48:32)
                .addBox(-width/2,0,-2,width,12,4,new CubeDeformation(.25f)),PartPose.ZERO);
            SLEEVES[left][slim]=LayerDefinition.create(sleeve,64,64).bakeRoot().getChild("part");
        }
    }
    private PunchRenderer(){}
    public static void submit(SkinData data,PoseStack poses,SubmitNodeCollector collector,int light){
        if(data.texture()==null)return;
        int l=data.left()?1:0,s=data.slim()?1:0,f=data.fist()?1:0;
        poses.pushPose();if(!data.fist())poses.scale(1,4f/3,1);
        var type=RenderTypes.entityTranslucent(data.texture());
        collector.submitModelPart(MODELS[l][s][f],poses,type,light,OverlayTexture.NO_OVERLAY,null);
        if(!data.fist()&&data.sleeve())collector.submitModelPart(SLEEVES[l][s],poses,type,light,OverlayTexture.NO_OVERLAY,null);
        poses.popPose();
    }
}
