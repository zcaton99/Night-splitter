package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Quaternionf;
import org.joml.Vector3f;
/** All string geometry is white, except the black knight silhouette and purple puppet outlines. */
public final class StringRenderer {
    private static final Identifier WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png"),BLACK=Identifier.withDefaultNamespace("textures/block/black_concrete.png"),PURPLE=Identifier.withDefaultNamespace("textures/block/purple_concrete.png");
    private static ModelPart bake(MeshDefinition m){return LayerDefinition.create(m,16,16).bakeRoot();}
    private static final ModelPart BEAM=beam(false),CORE=beam(true),CAGE=cage(),WEB=web(),KNIGHT=knight(false),OUTLINE=knight(true);
    private static ModelPart beam(boolean core){MeshDefinition m=new MeshDefinition();m.getRoot().addOrReplaceChild("thread",CubeListBuilder.create().texOffs(0,0).addBox(core?-4:-8,0,core?-4:-8,core?8:16,16,core?8:16),PartPose.ZERO);return bake(m);}
    private static void line(PartDefinition root,String name,Vector3f a,Vector3f b,float width){Vector3f d=new Vector3f(b).sub(a);float length=d.length();Vector3f angles=new Quaternionf().rotationTo(new Vector3f(0,1,0),d.normalize()).getEulerAnglesXYZ(new Vector3f());root.addOrReplaceChild(name,CubeListBuilder.create().texOffs(0,0).addBox(-width/2,0,-width/2,width,length,width),PartPose.offsetAndRotation(a.x,a.y,a.z,angles.x,angles.y,angles.z));}
    private static Vector3f sphere(double a,double b){return new Vector3f((float)(16*Math.sin(b)*Math.cos(a)),(float)(16*Math.cos(b)),(float)(16*Math.sin(b)*Math.sin(a)));}
    private static ModelPart cage(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();int n=0;
        for(int i=0;i<32;i++)for(int j=0;j<32;j++)line(r,"c"+n++,sphere(i*Math.PI/16,j*Math.PI/32),sphere(i*Math.PI/16,(j+1)*Math.PI/32),.012f);
        for(int j=1;j<12;j++)for(int i=0;i<96;i++)line(r,"c"+n++,sphere(i*Math.PI/48,j*Math.PI/12),sphere((i+1)*Math.PI/48,j*Math.PI/12),.009f);
        return bake(m);}
    private static ModelPart web(){MeshDefinition m=new MeshDefinition();var r=m.getRoot();int n=0;
        for(int i=0;i<16;i++){double a=i*Math.PI/8;line(r,"w"+n++,new Vector3f(),new Vector3f((float)Math.cos(a)*16,(float)Math.sin(a)*16,0),.15f);}
        for(int ring=1;ring<=4;ring++)for(int i=0;i<16;i++){double a=i*Math.PI/8,b=(i+1)*Math.PI/8;float radius=ring*4;line(r,"w"+n++,new Vector3f((float)Math.cos(a)*radius,(float)Math.sin(a)*radius,0),new Vector3f((float)Math.cos(b)*radius,(float)Math.sin(b)*radius,0),.12f);}return bake(m);}
    private static ModelPart knight(boolean outline){MeshDefinition m=new MeshDefinition();var r=m.getRoot();float[][] parts={{-4,24,-4,8,8,8},{-4,12,-2,8,12,4},{-8,12,-2,4,12,4},{4,12,-2,4,12,4},{-4,0,-2,4,12,4},{0,0,-2,4,12,4}};int n=0;
        for(float[] p:parts){if(outline)r.addOrReplaceChild("outline"+n++,CubeListBuilder.create().texOffs(0,0).addBox(p[0]-.2f,p[1]-.2f,p[2]-.2f,p[3]+.4f,p[4]+.4f,p[5]+.4f),PartPose.ZERO);
            else {for(float y=0;y<p[4];y+=1.2f)r.addOrReplaceChild("weave"+n++,CubeListBuilder.create().texOffs(0,0).addBox(p[0]-.25f,p[1]+y,p[2]-.25f,p[3]+.5f,.65f,p[5]+.5f),PartPose.ZERO);}}
        return bake(m);}
    private static void draw(ModelPart model,Identifier texture,PoseStack poses,SubmitNodeCollector nodes,boolean glow){nodes.submitModelPart(model,poses,glow?RenderTypes.eyes(texture):RenderTypes.entityTranslucent(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    public static void submit(int kind,PoseStack poses,SubmitNodeCollector nodes,float age){
        if(kind==4){draw(CAGE,WHITE,poses,nodes,true);return;}if(kind==5){poses.pushPose();poses.rotate(new Quaternionf().rotationY(age*.03f));draw(WEB,WHITE,poses,nodes,true);poses.popPose();return;}
        if(kind==6){draw(OUTLINE,BLACK,poses,nodes,false);draw(KNIGHT,WHITE,poses,nodes,true);return;}
        draw(BEAM,kind==2?PURPLE:kind==3?BLACK:WHITE,poses,nodes,kind!=3);if(kind==2||kind==3)draw(CORE,WHITE,poses,nodes,true);
    }
    public static int kind(String name){return name.equals("cb_string_cage")?4:name.equals("cb_string_web")?5:name.equals("cb_string_knight")?6:name.equals("cb_string_beam_0")?1:name.equals("cb_string_beam_1")?2:name.equals("cb_string_beam_2")?3:0;}
    public static void validate(){if(kind("cb_string_cage")!=4||kind("ordinary")!=0)throw new AssertionError("String visual names");BEAM.getChild("thread");CAGE.getChild("c0");WEB.getChild("w0");KNIGHT.getChild("weave0");OUTLINE.getChild("outline0");}
}
