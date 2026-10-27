package com.sibcraft.creeperborn.client;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.*;
import net.minecraft.core.Direction;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import org.joml.Vector3f;

/** One closed spherical surface. Adjacent patches share their exact endpoint positions. */
public final class RoomRenderer{
    private static final int LAT=32,LON=64;
    private static final Identifier BLUE=Identifier.withDefaultNamespace("textures/block/light_blue_stained_glass.png");
    private static final List<ModelPart.Polygon> FACES=faces();
    private static final ModelPart SHELL=model();
    private static List<ModelPart.Polygon> faces(){
        ModelPart.Vertex[][] grid=new ModelPart.Vertex[LAT+1][LON+1];
        var top=new ModelPart.Vertex(0,16,0,.5f,.5f);var bottom=new ModelPart.Vertex(0,-16,0,.5f,.5f);
        for(int lat=0;lat<=LAT;lat++){
            double theta=lat*Math.PI/LAT;
            for(int lon=0;lon<LON;lon++){
                double phi=lon*Math.PI*2/LON;
                grid[lat][lon]=lat==0?top:lat==LAT?bottom:new ModelPart.Vertex(
                    (float)(Math.sin(theta)*Math.sin(phi)*16),(float)(Math.cos(theta)*16),
                    (float)(Math.sin(theta)*Math.cos(phi)*16),.5f,.5f);
            }
            grid[lat][LON]=grid[lat][0];
        }
        List<ModelPart.Polygon> faces=new ArrayList<>();
        for(int lat=0;lat<LAT;lat++)for(int lon=0;lon<LON;lon++){
            ModelPart.Vertex[] v={grid[lat][lon],grid[lat+1][lon],grid[lat+1][lon+1],grid[lat][lon+1]};
            Vector3f normal=new Vector3f();for(var p:v)normal.add(p.x(),p.y(),p.z());normal.normalize();
            faces.add(new ModelPart.Polygon(v,normal));
        }
        return List.copyOf(faces);
    }
    private static ModelPart model(){
        List<ModelPart.Cube> surface=new ArrayList<>();
        for(var face:FACES){
            var patch=new ModelPart.Cube(0,0,0,0,0,1,1,1,0,0,0,false,16,16,Set.of(Direction.NORTH));
            if(patch.polygons.length!=1)throw new IllegalStateException("Expected a single mesh face");
            patch.polygons[0]=face;surface.add(patch);
        }
        return new ModelPart(surface,Map.of());
    }
    public static void submit(PoseStack poses,SubmitNodeCollector nodes){
        nodes.submitModelPart(SHELL,poses,RenderTypes.entityTranslucent(BLUE),0xF000F0,OverlayTexture.NO_OVERLAY,null);
    }
    public static void validate(){
        Map<ModelPart.Vertex,Integer> ids=new HashMap<>();Map<Long,Integer> edges=new HashMap<>();
        for(var face:FACES){
            var v=face.vertices();
            for(int i=0;i<4;i++){
                var a=v[i];var b=v[(i+1)%4];
                if(Math.abs(a.x()*a.x()+a.y()*a.y()+a.z()*a.z()-256)>.001)throw new AssertionError("ROOM vertex outside sphere");
                int ai=ids.computeIfAbsent(a,k->ids.size()),bi=ids.computeIfAbsent(b,k->ids.size());
                if(ai==bi)continue;
                long edge=((long)Math.min(ai,bi)<<32)|(Math.max(ai,bi)&0xffffffffL);edges.merge(edge,1,Integer::sum);
            }
        }
        if(FACES.size()!=LAT*LON||edges.values().stream().anyMatch(n->n!=2))throw new AssertionError("ROOM shell has an open edge");
    }
}
