package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
/** Original paw pads, compressed air rings and pulsing extracted pain. */
public final class PawRenderer {
    private static final Identifier PINK=Identifier.withDefaultNamespace("textures/block/pink_concrete.png"),RED=Identifier.withDefaultNamespace("textures/block/red_concrete.png"),WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final ModelPart PAW=paw(),RING=ring();
    private static void rounded(PartDefinition r,String name,float x,float y,float w,float h){
        var p=r.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(x-w/2+1,y-h/2,-1,w-2,h,2).addBox(x-w/2,y-h/2+1,-1,w,h-2,2),PartPose.ZERO);
    }
    private static ModelPart paw(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();rounded(r,"palm",0,-2,12,10);float[][] toes={{-7,6,5,6},{-2.5f,10,5,6},{3,10,5,6},{7.5f,6,5,6}};for(int i=0;i<4;i++){var a=toes[i];rounded(r,"toe"+i,a[0],a[1],a[2],a[3]);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static ModelPart ring(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();for(int i=0;i<32;i++){double a=i*Math.PI/16;float x=(float)Math.cos(a)*13,y=(float)Math.sin(a)*13;r.addOrReplaceChild("air"+i,CubeListBuilder.create().texOffs(0,0).addBox(x-1,y-1,-.3f,2,2,.6f),PartPose.ZERO);}return LayerDefinition.create(m,16,16).bakeRoot();}
    private static void draw(ModelPart part,Identifier texture,PoseStack poses,SubmitNodeCollector nodes){nodes.submitModelPart(part,poses,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static int kind(String name){return name.equals("cb_paw_1")?1:name.equals("cb_paw_2")?2:name.equals("cb_paw_3")?3:name.equals("cb_paw_4")?4:name.equals("cb_paw_5")?5:name.equals("cb_paw_6")?6:0;}
    public static void submit(int kind,PoseStack poses,SubmitNodeCollector nodes,float age){
        if(kind==6){
            poses.pushPose();float growth=Math.max(.05f,Math.min(1,age/12f));poses.scale(growth,growth,growth);
            draw(RING,WHITE,poses,nodes);poses.pushPose();poses.rotate(new Quaternionf().rotationX((float)Math.PI/2));draw(RING,WHITE,poses,nodes);poses.popPose();
            poses.pushPose();poses.rotate(new Quaternionf().rotationY((float)Math.PI/2));draw(RING,WHITE,poses,nodes);poses.popPose();poses.popPose();return;
        }
        poses.pushPose();float pulse=kind==4?1+(float)Math.sin(age*.35)*.09f:1;poses.scale(pulse,pulse,pulse);
        poses.pushPose();draw(PAW,kind==4?RED:kind==2||kind==5?WHITE:PINK,poses,nodes);poses.popPose();
        if(kind==2||kind==3||kind==4){poses.pushPose();poses.rotate(new Quaternionf().rotationZ(age*(kind==4?.018f:.05f)));float radius=1+(float)Math.sin(age*.16)*.1f;poses.scale(radius,radius,1);draw(RING,kind==4?RED:WHITE,poses,nodes);poses.popPose();}
        poses.popPose();
    }
    public static void validate(){PAW.getChild("palm");PAW.getChild("toe3");RING.getChild("air31");for(int i=1;i<=6;i++)if(kind("cb_paw_"+i)!=i)throw new AssertionError("Paw effect mapping");}
}
