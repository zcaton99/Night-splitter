package com.sibcraft.creeperborn.client;
import java.util.*;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;

/** One cached mesh coats sampled block tops; terrain, collision and lighting data stay untouched. */
public final class DarkGroundRenderer{
    private static final Identifier BLACK=Identifier.withDefaultNamespace("textures/block/black_concrete.png");
    private static final Map<String,ModelPart> models=new LinkedHashMap<>(16,.75f,true){
        @Override protected boolean removeEldestEntry(Map.Entry<String,ModelPart> e){return size()>8;}
    };
    private static ModelPart mesh(String data){
        if(!data.startsWith("cb_dark_ground|")||data.length()>32000)return null;
        MeshDefinition mesh=new MeshDefinition();var root=mesh.getRoot();int count=0;
        for(String cell:data.substring("cb_dark_ground|".length()).split(";")){
            if(cell.isEmpty())continue;
            String[] values=cell.split(",");if(values.length!=3)return null;
            try{
                float x=Float.parseFloat(values[0]),y=Float.parseFloat(values[1]),z=Float.parseFloat(values[2]);
                if(!Float.isFinite(x)||!Float.isFinite(y)||!Float.isFinite(z)||Math.abs(x)>12||Math.abs(z)>12||y< -16||y>4||count>=500)return null;
                root.addOrReplaceChild("tile_"+count,CubeListBuilder.create().texOffs(0,0).addBox((x-.495f)*16,y*16,(z-.495f)*16,15.84f,.096f,15.84f),PartPose.ZERO);count++;
            }catch(NumberFormatException error){return null;}
        }
        return count==0?null:LayerDefinition.create(mesh,16,16).bakeRoot();
    }
    public static void submit(String data,PoseStack poses,SubmitNodeCollector nodes){
        ModelPart model=models.get(data);if(model==null){model=mesh(data);if(model==null)return;models.put(data,model);}
        nodes.submitModelPart(model,poses,RenderTypes.entityTranslucent(BLACK),0xF000F0,OverlayTexture.NO_OVERLAY,null);
    }
    public static void validate(){ModelPart model=mesh("cb_dark_ground|0,-1,0;1,-.5,0;");if(model==null)throw new IllegalStateException("Ground shadow mesh missing");model.getChild("tile_1");if(mesh("cb_dark_ground|0,NaN,0;")!=null)throw new IllegalStateException("Invalid shadow height accepted");}
}
