package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
public final class VoidRenderer{
    private static final Identifier BLACK=Identifier.withDefaultNamespace("textures/block/black_concrete.png"),VIOLET=Identifier.withDefaultNamespace("textures/block/purple_concrete.png");
    private static final ModelPart CORE=model(false),RING=model(true);
    private static ModelPart model(boolean ring){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        if(ring)for(int i=0;i<24;i++){double a=i*Math.PI/12;
            r.addOrReplaceChild("rim_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-1,-.8f,-1,2,1.6f,2),PartPose.offset((float)Math.cos(a)*13,0,(float)Math.sin(a)*13));}
        else for(int i=-2;i<=2;i++){float width=(float)Math.sqrt(1-i*i/9.0)*22;
            r.addOrReplaceChild("slice_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-width/2,i*4-2,-width/2,width,4,width),PartPose.ZERO);}
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    public static void submit(PoseStack poses,SubmitNodeCollector nodes,float age){
        nodes.submitModelPart(CORE,poses,RenderTypes.entityTranslucent(BLACK),0xF000F0,OverlayTexture.NO_OVERLAY,null);
        poses.pushPose();poses.rotate(new Quaternionf().rotationY(age*.18f));poses.rotate(new Quaternionf().rotationZ(.45f));
        nodes.submitModelPart(RING,poses,RenderTypes.eyes(VIOLET),0xF000F0,OverlayTexture.NO_OVERLAY,null);poses.popPose();
    }
    public static void validate(){CORE.getChild("slice_0");RING.getChild("rim_23");}
}
