package com.sibcraft.creeperborn.client;

import com.sibcraft.creeperborn.Rasengan;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.geom.*;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.*;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Display;
import org.joml.Quaternionf;

/** Rounded blue chakra core, counter-rotating filaments and orbiting elemental spheres. */
public final class RasenganRenderer {
    private static Identifier tex(String n){return Identifier.withDefaultNamespace("textures/block/"+n+".png");}
    private static final Identifier BLUE=tex("light_blue_concrete"),WHITE=tex("white_concrete"),CYAN=tex("cyan_concrete"),EARTH=tex("moss_block"),WATER=tex("blue_concrete"),FIRE=tex("orange_concrete"),AIR=tex("white_wool"),GOLD=tex("yellow_concrete");
    private static final ModelPart BALL=ball(),THREAD=thread(),SHARD=shard();
    private static ModelPart bake(MeshDefinition m){return LayerDefinition.create(m,16,16).bakeRoot();}
    private static void box(PartDefinition r,String n,float x,float y,float z,float w,float h,float d){r.addOrReplaceChild(n,CubeListBuilder.create().texOffs(0,0).addBox(x,y,z,w,h,d),PartPose.ZERO);}
    private static ModelPart ball(){var m=new MeshDefinition();for(int i=-7;i<8;i++){float radius=(float)Math.sqrt(64-(i+.5f)*(i+.5f));box(m.getRoot(),"slice"+i,-radius,i,-radius,radius*2,1,radius*2);}return bake(m);}
    private static ModelPart thread(){var m=new MeshDefinition();for(int i=0;i<48;i++){double a=i*Math.PI/24;var r=m.getRoot().addOrReplaceChild("strand"+i,CubeListBuilder.create(),PartPose.offsetAndRotation((float)Math.cos(a)*8.2f,(float)Math.sin(a)*8.2f,0,0,0,(float)a));box(r,"line",-.2f,-.7f,-.18f,.4f,1.6f,.36f);}return bake(m);}
    private static ModelPart shard(){var m=new MeshDefinition();box(m.getRoot(),"core",-1.2f,-2,-1.2f,2.4f,4,2.4f);return bake(m);}
    private static void draw(ModelPart mesh,Identifier texture,PoseStack p,SubmitNodeCollector n){n.submitModelPart(mesh,p,RenderTypes.eyes(texture),0xF000F0,OverlayTexture.NO_OVERLAY,null);}
    private static void sphere(PoseStack p,SubmitNodeCollector n,float age,Identifier inner){
        p.pushPose();p.scale(.7f,.7f,.7f);draw(BALL,inner,p,n);p.popPose();
        for(int i=0;i<7;i++){p.pushPose();p.rotate(new Quaternionf().rotationXYZ(i*.63f+age*(i%2==0?.09f:-.07f),i*.9f+age*.05f,i*.47f));float scale=.78f+i*.035f;p.scale(scale,scale,scale);draw(THREAD,i%3==0?WHITE:BLUE,p,n);p.popPose();}
    }
    public static void submit(int kind,float age,PoseStack p,SubmitNodeCollector n){int tier=Rasengan.tier(kind),mask=tier==3?kind-46:0;p.pushPose();sphere(p,n,age,CYAN);
        if(tier==3)for(int i=0;i<4;i++){p.pushPose();float a=age*.055f+i*(float)Math.PI/2;p.rotate(new Quaternionf().rotationY(a));p.translate(1.05,Math.sin(a*1.4)*.28,0);p.scale(.3f,.3f,.3f);Identifier element=(mask&(1<<i))==0?CYAN:switch(i){case 0->EARTH;case 1->WATER;case 2->FIRE;default->AIR;};sphere(p,n,-age,element);
            if((mask&(1<<i))!=0){for(int j=0;j<3;j++){p.pushPose();p.rotate(new Quaternionf().rotationXYZ(age*.11f+j,age*.08f+j*2,0));p.translate(.18,0,0);draw(SHARD,element,p,n);p.popPose();}}p.popPose();}
        if(mask==15){p.pushPose();p.rotate(new Quaternionf().rotationXYZ(.5f,age*.06f,.4f));p.scale(1.3f,1.3f,1.3f);draw(THREAD,GOLD,p,n);p.popPose();}p.popPose();
    }
    /** Match movement and camera rotation between network ticks, while the server owns the charge. */
    public static void follow(Display.BlockDisplay d,BlockDisplayEntityRenderState s,float partial){if(d.getCustomName()==null)return;String name=d.getCustomName().getString();if(!name.endsWith("|charge"))return;int kind=CelestialRenderer.kind(name);if(kind<44||kind>61)return;try{String[] data=name.split("\\|");var c=net.minecraft.client.Minecraft.getInstance();var owner=c.level==null?null:c.level.getPlayerByUUID(java.util.UUID.fromString(data[1]));if(owner==null)return;int tier=Rasengan.tier(kind);float fraction=Rasengan.chargeFraction(tier,(long)(d.tickCount+partial+8));var at=Rasengan.anchor(owner,tier,fraction,partial);s.x=at.x;s.y=at.y;s.z=at.z;}catch(IllegalArgumentException ignored){}}
    public static void pose(net.minecraft.client.model.player.PlayerModel model,AvatarRenderState state,int flags){if((flags&3)!=1||((flags>>2)&3)==3||(flags&768)==0)return;boolean left=state.mainArm==net.minecraft.world.entity.HumanoidArm.LEFT;var arm=left?model.leftArm:model.rightArm;boolean strike=(flags&512)!=0;int tier=(flags>>4)&3;arm.xRot=strike?-1.55f:tier>=2?-(float)Math.PI:-1.25f;arm.yRot=strike?0:left?-.25f:.25f;arm.zRot=tier>=2&&!strike?(left?-.12f:.12f):0;(left?model.leftSleeve:model.rightSleeve).loadPose(arm.storePose());}
}
