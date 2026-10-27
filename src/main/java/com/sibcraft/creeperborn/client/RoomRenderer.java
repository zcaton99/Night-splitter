package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
public final class RoomRenderer{
    private static final Identifier BLUE=Identifier.withDefaultNamespace("textures/block/light_blue_stained_glass.png");
    private static final ModelPart SHELL=model();
    private static ModelPart model(){
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();
        for(int lat=0;lat<20;lat++){
            double theta=(lat+.5)*Math.PI/20;float radius=(float)Math.sin(theta)*16,y=(float)Math.cos(theta)*16;
            for(int lon=0;lon<48;lon++){
                float angle=(float)(lon*Math.PI/24);float width=(float)(2*Math.PI*radius/48+.03);
                root.addOrReplaceChild("panel_"+lat+"_"+lon,CubeListBuilder.create().texOffs(0,0).addBox(-width/2,-1.28f,-.035f,width,2.56f,.07f),
                    PartPose.offsetAndRotation((float)Math.sin(angle)*radius,y,(float)Math.cos(angle)*radius,0,angle,0));
            }
        }return LayerDefinition.create(mesh,16,16).bakeRoot();
    }
    public static void submit(PoseStack poses,SubmitNodeCollector nodes){nodes.submitModelPart(SHELL,poses,RenderTypes.entityTranslucent(BLUE),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static void validate(){SHELL.getChild("panel_0_0");SHELL.getChild("panel_19_47");}
}
