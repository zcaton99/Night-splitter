package com.sibcraft.creeperborn.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;
import org.joml.Quaternionf;

/** Rounded hot core with layered, curving flame tails instead of a line of flame particles. */
public final class FireRenderer {
    private static final Identifier YELLOW=Identifier.withDefaultNamespace("textures/block/yellow_concrete.png"),ORANGE=Identifier.withDefaultNamespace("textures/block/orange_concrete.png"),RED=Identifier.withDefaultNamespace("textures/block/red_concrete.png"),WHITE=Identifier.withDefaultNamespace("textures/block/white_concrete.png");
    private static final ModelPart BALL=sphere(),TAIL=tail(),RING=ring();
    public static int kind(String name){return name.startsWith("cb_fireball|")?1:name.startsWith("cb_fire_ring|")?2:0;}
    public static boolean matches(String name){return kind(name)>0;}
    public static float length(String name){if(!matches(name))return 1;try{float value=Float.parseFloat(name.substring(name.indexOf('|')+1));return Float.isFinite(value)?Math.max(.25f,Math.min(64,value)):1;}catch(RuntimeException ignored){return 1;}}
    private static ModelPart.Vertex vertex(Vec3 v){return new ModelPart.Vertex((float)v.x*16,(float)v.y*16,(float)v.z*16,.5f,.5f);}
    private static void face(List<ModelPart.Cube> faces,Vec3 a,Vec3 b,Vec3 c,Vec3 d){Vec3 normal=b.subtract(a).cross(c.subtract(a));if(normal.lengthSqr()<.000001)normal=c.subtract(a).cross(d.subtract(a));Vec3 n=normal.normalize();var cube=new ModelPart.Cube(0,0,0,0,0,1,1,1,0,0,0,false,16,16,Set.of(Direction.NORTH));cube.polygons[0]=new ModelPart.Polygon(new ModelPart.Vertex[]{vertex(a),vertex(b),vertex(c),vertex(d)},new Vector3f((float)n.x,(float)n.y,(float)n.z));faces.add(cube);}
    private static Vec3 spherePoint(int lat,int lon){double theta=lat*Math.PI/12,phi=lon*Math.PI/10;return new Vec3(Math.sin(theta)*Math.cos(phi),Math.cos(theta),Math.sin(theta)*Math.sin(phi));}
    private static ModelPart sphere(){List<ModelPart.Cube> faces=new ArrayList<>();for(int lat=0;lat<12;lat++)for(int lon=0;lon<20;lon++)face(faces,spherePoint(lat,lon),spherePoint(lat,lon+1),spherePoint(lat+1,lon+1),spherePoint(lat+1,lon));return new ModelPart(faces,Map.of());}
    private static Vec3 tailPoint(int step,int side){double t=step/12.0,radius=Math.pow(1-t,.8),a=side*Math.PI/4;return new Vec3(Math.cos(a)*radius+.35*t*t,Math.sin(a)*radius+.12*Math.sin(t*Math.PI),-t);}
    private static ModelPart tail(){List<ModelPart.Cube> faces=new ArrayList<>();for(int step=0;step<12;step++)for(int side=0;side<8;side++)face(faces,tailPoint(step,side),tailPoint(step+1,side),tailPoint(step+1,side+1),tailPoint(step,side+1));return new ModelPart(faces,Map.of());}
    private static Vec3 ringPoint(int around,int side){double a=around*Math.PI/32,b=side*Math.PI/4,r=1+.04*Math.cos(b);return new Vec3(Math.cos(a)*r,.04*Math.sin(b),Math.sin(a)*r);}
    private static ModelPart ring(){List<ModelPart.Cube> faces=new ArrayList<>();for(int a=0;a<64;a++)for(int b=0;b<8;b++)face(faces,ringPoint(a,b),ringPoint(a,b+1),ringPoint(a+1,b+1),ringPoint(a+1,b));return new ModelPart(faces,Map.of());}
    private static void draw(ModelPart part,Identifier texture,PoseStack poses,SubmitNodeCollector nodes,float x,float y,float z){poses.pushPose();poses.scale(x,y,z);nodes.submitModelPart(part,poses,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);poses.popPose();}
    public static float travel(float length,float age){return length*Math.max(0,Math.min(1,age/6));}
    public static void submit(float length,float age,PoseStack poses,SubmitNodeCollector nodes){
        if(!Float.isFinite(length)||length<.25f||length>64)return;float head=travel(length,age),burst=Math.max(0,Math.min(1,(age-8)/4)),size=(1+.35f*(float)Math.sin(age*.9))*(1-burst*.7f);float trail=Math.min(2.8f,Math.max(.1f,head))*(1-burst);
        poses.pushPose();poses.translate(0,0,head);poses.rotate(new Quaternionf().rotationZ(age*.17f));
        for(int i=0;i<4;i++){poses.pushPose();double a=i*Math.PI/2+age*.3;poses.translate(Math.cos(a)*.2,Math.sin(a)*.2,-.05);poses.rotate(new Quaternionf().rotationZ((float)a));draw(TAIL,i%2==0?RED:ORANGE,poses,nodes,.16f*size,.16f*size,trail*(.8f+i*.12f));poses.popPose();}
        draw(TAIL,ORANGE,poses,nodes,.26f*size,.26f*size,trail*.8f);
        poses.pushPose();poses.translate(0,.13,-.06);draw(TAIL,YELLOW,poses,nodes,.09f*size,.09f*size,trail*.6f);poses.popPose();
        draw(BALL,YELLOW,poses,nodes,.32f*size,.32f*size,.4f*size);
        for(int i=0;i<5;i++){poses.pushPose();double a=i*Math.PI*2/5-age*.2;poses.translate(Math.cos(a)*.28,Math.sin(a)*.28,-.03);draw(TAIL,ORANGE,poses,nodes,.15f*size,.15f*size,.6f*size);poses.popPose();}
        poses.pushPose();poses.translate(0,0,-.3*size);draw(BALL,WHITE,poses,nodes,.12f*size,.12f*size,.14f*size);poses.popPose();poses.popPose();
    }
    public static float ringRadius(float maximum,float age){float expand=Math.max(0,Math.min(1,(age-4)/17));return 1.2f+(maximum-1.2f)*expand;}
    public static void submitRing(float maximum,float age,PoseStack poses,SubmitNodeCollector nodes){
        float radius=ringRadius(maximum,age),rise=Math.min(1,age/4),fade=age<21?1:Math.max(0,(28-age)/7);
        poses.pushPose();
        poses.pushPose();poses.translate(0,.12,0);draw(RING,ORANGE,poses,nodes,radius,1.2f*fade,radius);poses.popPose();
        for(int i=0;i<32;i++){double a=i*Math.PI/16;float flicker=.8f+.2f*(float)Math.sin(age*.8+i*1.7),height=(.7f+.65f*flicker)*rise*fade;
            poses.pushPose();poses.translate(Math.cos(a)*radius,.15,Math.sin(a)*radius);poses.rotate(new Quaternionf().rotationY((float)-a).rotateX((float)Math.PI/2));
            draw(TAIL,RED,poses,nodes,.33f*fade,.33f*fade,height*1.25f);draw(TAIL,ORANGE,poses,nodes,.25f*fade,.25f*fade,height);draw(TAIL,YELLOW,poses,nodes,.12f*fade,.12f*fade,height*.75f);poses.popPose();
            poses.pushPose();poses.translate(Math.cos(a)*radius,.13,Math.sin(a)*radius);draw(BALL,ORANGE,poses,nodes,.27f*fade,.13f*fade,.27f*fade);poses.popPose();
        }
        poses.popPose();
    }
    public static void validate(){if(!matches("cb_fireball|16")||length("cb_fireball|NaN")!=1||travel(16,6)!=16||travel(16,0)!=0)throw new AssertionError("Fireball animation metadata and travel");if(kind("cb_fire_ring|8")!=2||ringRadius(8,4)!=1.2f||ringRadius(8,21)!=8)throw new AssertionError("Flame Ring must expand outward after its initial circle");}
}
