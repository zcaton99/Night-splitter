package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
public final class GravityRenderer{
    private static final Identifier STONE=Identifier.withDefaultNamespace("textures/block/deepslate.png"),GLOW=Identifier.withDefaultNamespace("textures/block/purple_concrete.png");
    private static final ModelPart METEOR=meteor(),ROCK=rock(),RUNE=rune(),FIRE=fire(false),HOT=fire(true);
    private static final Identifier ORANGE=Identifier.withDefaultNamespace("textures/block/orange_concrete.png"),YELLOW=Identifier.withDefaultNamespace("textures/block/yellow_concrete.png");
    private static ModelPart meteor(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        for(int i=-2;i<=2;i++){float width=(float)Math.sqrt(1-i*i/9.0)*20;
            r.addOrReplaceChild("mass_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-width/2,i*4-2,-width/2,width,4,width),PartPose.ZERO);}
        r.addOrReplaceChild("crag",CubeListBuilder.create().texOffs(0,0).addBox(-5,-3,-5,10,8,10),PartPose.offsetAndRotation(6,3,5,.3f,.4f,.2f));
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    private static ModelPart rock(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        r.addOrReplaceChild("slab",CubeListBuilder.create().texOffs(0,0).addBox(-8,-1,-8,16,5,16),PartPose.ZERO);
        r.addOrReplaceChild("base",CubeListBuilder.create().texOffs(0,0).addBox(-5,-4,-5,10,4,10),PartPose.ZERO);
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    private static ModelPart rune(){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        for(int i=0;i<4;i++)r.addOrReplaceChild("vein_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-.4f,-1,-8.1f,.8f,8,.3f),
            PartPose.offsetAndRotation(0,-2,0,0,i*(float)Math.PI/2,0));
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    private static ModelPart fire(boolean hot){
        MeshDefinition m=new MeshDefinition();var r=m.getRoot();
        float width=hot?12:21;float y=hot?-14:-12;
        r.addOrReplaceChild("fireball",CubeListBuilder.create().texOffs(0,0).addBox(-width/2,y,-width/2,width,hot?6:8,width),PartPose.ZERO);
        for(int i=0;i<8;i++){
            double a=i*Math.PI/4;float radius=hot?4:8;
            r.addOrReplaceChild("flame_"+i,CubeListBuilder.create().texOffs(0,0).addBox(-1.5f,-18,-1.5f,3,8+i%3,3),
                PartPose.offsetAndRotation((float)Math.cos(a)*radius,0,(float)Math.sin(a)*radius,0,0,(float)Math.sin(a)*.2f));
        }
        return LayerDefinition.create(m,16,16).bakeRoot();
    }
    public static void submit(int kind,PoseStack poses,SubmitNodeCollector nodes,float age){
        poses.pushPose();poses.rotate(new Quaternionf().rotationY(age*(kind==1?.06f:.02f)));
        poses.pushPose();if(kind==1)poses.rotate(new Quaternionf().rotationZ(age*.04f));
        nodes.submitModelPart(kind==1?METEOR:ROCK,poses,RenderTypes.entityTranslucent(STONE),0xF000F0,OverlayTexture.NO_OVERLAY,null);
        nodes.submitModelPart(RUNE,poses,RenderTypes.eyes(GLOW),0xF000F0,OverlayTexture.NO_OVERLAY,null);poses.popPose();
        if(kind==1){
            nodes.submitModelPart(FIRE,poses,RenderTypes.eyes(ORANGE),0xF000F0,OverlayTexture.NO_OVERLAY,null);
            nodes.submitModelPart(HOT,poses,RenderTypes.eyes(YELLOW),0xF000F0,OverlayTexture.NO_OVERLAY,null);
        }poses.popPose();
    }
    public static void validate(){METEOR.getChild("mass_0");ROCK.getChild("slab");RUNE.getChild("vein_3");FIRE.getChild("fireball");HOT.getChild("flame_7");}
}
