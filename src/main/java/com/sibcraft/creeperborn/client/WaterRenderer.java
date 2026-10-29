package com.sibcraft.creeperborn.client;

import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.core.Direction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Continuous round water tubes with translucent cyan surfaces and a narrow glossy highlight. */
public final class WaterRenderer {
    private static final Identifier WATER=Identifier.withDefaultNamespace("textures/block/light_blue_stained_glass.png"),SHINE=Identifier.withDefaultNamespace("textures/block/white_stained_glass.png");
    private record Key(int kind,int length,int frame){}
    private static final Map<Key,ModelPart[]> cache=new LinkedHashMap<>(24,.75f,true){protected boolean removeEldestEntry(Map.Entry<Key,ModelPart[]> e){return size()>20;}};
    public static int kind(String name){return name.startsWith("cb_water_1|")?1:name.startsWith("cb_water_2|")?2:0;}
    public static float length(String name){if(kind(name)==0)return 1;try{float value=Float.parseFloat(name.substring(name.lastIndexOf('|')+1));return Float.isFinite(value)?Math.max(.25f,Math.min(64,value)):1;}catch(RuntimeException ignored){return 1;}}
    public static void followOwner(net.minecraft.world.entity.Display.BlockDisplay display,net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState state,float partial){
        if(display.getCustomName()==null)return;String name=display.getCustomName().getString();int kind=kind(name);if(kind==0)return;
        try{var client=net.minecraft.client.Minecraft.getInstance();var owner=client.level==null?null:client.level.getPlayerByUUID(UUID.fromString(name.split("\\|",3)[1]));if(owner==null)return;Vec3 at=owner.getPosition(partial);
            if(kind==1){double yaw=Math.toRadians(owner.getViewYRot(partial)),side=owner.getMainArm()==net.minecraft.world.entity.HumanoidArm.LEFT?-1:1;at=owner.getEyePosition(partial).add(-Math.cos(yaw)*side*.32,-.38,-Math.sin(yaw)*side*.32).add(owner.getViewVector(partial).scale(.4));}
            state.x=at.x;state.y=at.y;state.z=at.z;
        }catch(IllegalArgumentException ignored){}
    }
    public static Vec3 whipPoint(double t,double length,double age){double stretch=Math.min(1,.15+age/6),recoil=age<=14?1:Math.max(.1,1-(age-14)/7),envelope=Math.sin(Math.PI*t),wave=t*Math.PI*2-age*.35,amplitude=Math.min(2.4,length*.12);return new Vec3(Math.sin(wave)*amplitude*envelope,Math.cos(wave*.8)*amplitude*.4*envelope,t*length*stretch*recoil);}
    public static Vec3 guardPoint(int arm,double t,double age){double rise=Math.min(1,age/6),sweep=Math.max(0,Math.min(1,(age-6)/12)),fade=age<=26?1:Math.max(0,(34-age)/8),angle=arm*Math.PI/4+Math.sin(age*.12)*.08+Math.sin(t*Math.PI)*.15;double radius=1.3+Math.pow(t,1.1)*(2.2+4.2*sweep)*fade,height=.55+Math.sin(t*Math.PI/2)*rise*(2.8-2.35*sweep)*fade+.3*Math.sin(t*Math.PI*2);return new Vec3(Math.cos(angle)*radius,height,Math.sin(angle)*radius);}
    private static ModelPart.Vertex vertex(Vec3 v){return new ModelPart.Vertex((float)v.x*16,(float)v.y*16,(float)v.z*16,.5f,.5f);}
    private static void face(List<ModelPart.Cube> faces,Vec3 a,Vec3 b,Vec3 c,Vec3 d){Vec3 normal=b.subtract(a).cross(c.subtract(a)).normalize();var cube=new ModelPart.Cube(0,0,0,0,0,1,1,1,0,0,0,false,16,16,Set.of(Direction.NORTH));cube.polygons[0]=new ModelPart.Polygon(new ModelPart.Vertex[]{vertex(a),vertex(b),vertex(c),vertex(d)},new Vector3f((float)normal.x,(float)normal.y,(float)normal.z));faces.add(cube);}
    private static void tube(List<Vec3> path,double base,double tip,List<ModelPart.Cube> faces){int sides=8;Vec3[][] rings=new Vec3[path.size()][sides];
        for(int i=0;i<path.size();i++){Vec3 tangent=path.get(Math.min(i+1,path.size()-1)).subtract(path.get(Math.max(0,i-1))).normalize(),reference=Math.abs(tangent.y)>.9?new Vec3(1,0,0):new Vec3(0,1,0),right=tangent.cross(reference).normalize(),up=right.cross(tangent).normalize();double radius=base+(tip-base)*i/(path.size()-1.0);for(int j=0;j<sides;j++){double angle=j*Math.PI*2/sides;rings[i][j]=path.get(i).add(right.scale(Math.cos(angle)*radius)).add(up.scale(Math.sin(angle)*radius));}}
        for(int i=0;i<path.size()-1;i++)for(int j=0;j<sides;j++){int next=(j+1)%sides;face(faces,rings[i][j],rings[i+1][j],rings[i+1][next],rings[i][next]);}
        for(int j=0;j<sides;j++){int next=(j+1)%sides;face(faces,path.getFirst(),rings[0][next],rings[0][j],path.getFirst());int last=path.size()-1;face(faces,path.getLast(),rings[last][j],rings[last][next],path.getLast());}
    }
    private static void waterTube(List<Vec3> points,double base,double tip,List<ModelPart.Cube> body,List<ModelPart.Cube> shine){tube(points,base,tip,body);List<Vec3> highlight=new ArrayList<>();for(int i=0;i<points.size();i++){double radius=base+(tip-base)*i/(points.size()-1.0);highlight.add(points.get(i).add(0,radius*.8,0));}tube(highlight,base*.15,tip*.15,shine);}
    private static ModelPart[] mesh(Key key){List<ModelPart.Cube> body=new ArrayList<>(),shine=new ArrayList<>();
        if(key.kind==1){List<Vec3> points=new ArrayList<>();for(int i=0;i<=32;i++)points.add(whipPoint(i/32.0,key.length/4.0,key.frame));waterTube(points,.17,.035,body,shine);}
        else{List<Vec3> ring=new ArrayList<>();for(int i=0;i<=48;i++){double a=i*Math.PI/24;ring.add(new Vec3(Math.cos(a)*1.3,.55,Math.sin(a)*1.3));}waterTube(ring,.23,.23,body,shine);for(int arm=0;arm<8;arm++){List<Vec3> points=new ArrayList<>();for(int i=0;i<=24;i++)points.add(guardPoint(arm,i/24.0,key.frame));waterTube(points,.29,.045,body,shine);}}
        return new ModelPart[]{new ModelPart(body,Map.of()),new ModelPart(shine,Map.of())};
    }
    public static void submit(int kind,float length,float age,PoseStack poses,SubmitNodeCollector nodes){if(kind!=1&&kind!=2)return;Key key=new Key(kind,Math.max(1,Math.min(256,Math.round(length*4))),Math.max(0,Math.min(kind==1?19:33,(int)age)));ModelPart[] model=cache.computeIfAbsent(key,WaterRenderer::mesh);poses.pushPose();if(kind==2){float scale=length/7.7f;poses.scale(scale,1,scale);}nodes.submitModelPart(model[0],poses,RenderTypes.entityTranslucent(WATER),0xF000F0,OverlayTexture.NO_OVERLAY,null);nodes.submitModelPart(model[1],poses,RenderTypes.entityTranslucent(SHINE),0xF000F0,OverlayTexture.NO_OVERLAY,null);poses.popPose();}
    public static void validate(){if(kind("cb_water_1|owner|16")!=1||length("cb_water_1|owner|NaN")!=1)throw new AssertionError("Water effect metadata");for(int arm=0;arm<8;arm++)for(int age=0;age<=33;age++){Vec3 tip=guardPoint(arm,1,age);if(!Double.isFinite(tip.lengthSqr())||tip.length()>10)throw new AssertionError("Water tentacle bounds");}if(whipPoint(0,16,8).lengthSqr()!=0||Math.abs(whipPoint(1,16,8).z-16)>.001)throw new AssertionError("Water whip hand and strike endpoints");}
}
