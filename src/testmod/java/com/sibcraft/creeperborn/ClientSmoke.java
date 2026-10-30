package com.sibcraft.creeperborn;
import com.sibcraft.creeperborn.client.PhoenixRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
public final class ClientSmoke implements ClientModInitializer{
    private boolean done;private int ticks;
    @Override public void onInitializeClient(){
        System.out.println("CREEPERBORN_CLIENT_SMOKE_INITIALIZED "+Boolean.getBoolean("creeperborn.visualSmoke"));
        if(!Boolean.getBoolean("creeperborn.visualSmoke"))return;
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            if(done)return;
            ticks++;
            if(ticks%100==0)System.out.println("Client smoke waiting: "+(client.gui.screen()==null?"no screen":client.gui.screen().getClass().getSimpleName()));
            if(ticks<60||client.gui.screen()==null||client.gui.overlay()!=null)return;
            done=true;
            try{
                var modelManager=client.getModelManager();
                var itemGetter=Arrays.stream(modelManager.getClass().getMethods()).filter(method->method.getName().equals("getItemModel")
                    &&method.getParameterCount()==1&&method.getParameterTypes()[0]==net.minecraft.resources.Identifier.class).findFirst()
                    .orElseThrow(()->new AssertionError("Native item-model lookup unavailable"));
                for(String fruit:Dark.FRUITS){
                    var id=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","fruit_"+fruit);
                    var texture=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","textures/item/fruit_"+fruit+".png");
                    if(!fruit.equals("tree")&&!fruit.equals("petal")&&client.getResourceManager().getResource(texture).isEmpty())throw new AssertionError("Missing fruit texture: "+fruit);
                    Object model=itemGetter.invoke(modelManager,id);
                    if(model==null||model.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("missing"))throw new AssertionError("Fruit item model did not bake: "+fruit);
                }
                for(var e:com.sibcraft.creeperborn.Element.values()){Object model=itemGetter.invoke(modelManager,net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","element_"+e.key()));if(model==null||model.getClass().getSimpleName().toLowerCase(java.util.Locale.ROOT).contains("missing"))throw new AssertionError("Element symbol did not bake: "+e);}
                com.sibcraft.creeperborn.client.BoltRenderer.validate();
                com.sibcraft.creeperborn.client.DarkGroundRenderer.validate();
                com.sibcraft.creeperborn.client.ShadowRenderer.validate();com.sibcraft.creeperborn.client.VoidRenderer.validate();
                com.sibcraft.creeperborn.client.RoomRenderer.validate();
                com.sibcraft.creeperborn.client.GravityRenderer.validate();
                com.sibcraft.creeperborn.client.StringRenderer.validate();com.sibcraft.creeperborn.client.PawRenderer.validate();com.sibcraft.creeperborn.client.TremorRenderer.validate();com.sibcraft.creeperborn.client.PairRenderer.validate();com.sibcraft.creeperborn.client.ClimateRenderer.validate();
                PhoenixRenderer.validate();com.sibcraft.creeperborn.client.StormRenderer.validate();
                var identity=new AvatarRenderState();
                identity.nameTag=net.minecraft.network.chat.Component.literal("Visible");
                identity.scoreText=net.minecraft.network.chat.Component.literal("Score");
                com.sibcraft.creeperborn.client.PhoenixVisuals.hideIdentity(identity,1);
                if(identity.nameTag==null||identity.scoreText==null)throw new AssertionError("Wing Flight must preserve identity");
                com.sibcraft.creeperborn.client.PhoenixVisuals.hideIdentity(identity,2);
                if(identity.nameTag!=null||identity.scoreText!=null)throw new AssertionError("Full Phoenix must hide name and score");
                if(Arrays.stream(Class.forName("net.minecraft.client.renderer.entity.EntityRenderer").getDeclaredMethods()).noneMatch(v->v.getName().contains("creeperborn$hidePhoenixName")))throw new AssertionError("Phoenix name mixin did not apply");
                int[] parts={0};
                SubmitNodeCollector nodes=(SubmitNodeCollector)Proxy.newProxyInstance(SubmitNodeCollector.class.getClassLoader(),new Class<?>[]{SubmitNodeCollector.class},(proxy,method,args)->{
                    if(method.getName().equals("submitModelPart"))parts[0]++;
                    return null;
                });
                AvatarRenderState state=new AvatarRenderState();state.bodyRot=45;state.xRot=20;
                PhoenixRenderer.submit(state,new PoseStack(),nodes,2,true);
                state.ageInTicks=15;PhoenixRenderer.submit(state,new PoseStack(),nodes,1,false);
                state.ageInTicks=30;PhoenixRenderer.submit(state,new PoseStack(),nodes,6,true);
                state.ageInTicks=50;PhoenixRenderer.submit(state,new PoseStack(),nodes,10,true);
                com.sibcraft.creeperborn.client.StormRenderer.submit(state,new PoseStack(),nodes,7);
                state.ageInTicks=12;com.sibcraft.creeperborn.client.StormRenderer.submit(state,new PoseStack(),nodes,1);
                for(int variant=0;variant<3;variant++)com.sibcraft.creeperborn.client.BoltRenderer.submit(variant,variant==0?1:variant==1?30:50,new PoseStack(),nodes);
                var boltState=new net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState();
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$bolt(2);
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$boltLength(30);
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$bolt()!=2)throw new AssertionError("Bolt render-state mixin did not apply");
                com.sibcraft.creeperborn.client.ShadowRenderer.submit(state,new PoseStack(),nodes,1);
                com.sibcraft.creeperborn.client.ShadowRenderer.submit(state,new PoseStack(),nodes,2);
                com.sibcraft.creeperborn.client.ShadowRenderer.submitHand(new PoseStack(),nodes);
                com.sibcraft.creeperborn.client.VoidRenderer.submit(new PoseStack(),nodes,30);
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$darkVoid(true);
                if(!((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$darkVoid())throw new AssertionError("Void display state mixin did not apply");
                com.sibcraft.creeperborn.client.DarkGroundRenderer.submit("cb_dark_ground|0,-1,0;1,-.5,0;",new PoseStack(),nodes);
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$darkGround("cb_dark_ground|0,-1,0;");
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$darkGround().isEmpty())throw new AssertionError("Ground shadow state mixin missing");
                com.sibcraft.creeperborn.client.RoomRenderer.submit(new PoseStack(),nodes);
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$opRoom(true);
                if(!((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$opRoom())throw new AssertionError("ROOM render state missing");
                com.sibcraft.creeperborn.client.GravityRenderer.submit(1,new PoseStack(),nodes,30);
                com.sibcraft.creeperborn.client.GravityRenderer.submit(2,new PoseStack(),nodes,15);
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$gravity(1);
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$gravity()!=1)throw new AssertionError("Gravity render state missing");
                for(int kind=1;kind<=13;kind++)com.sibcraft.creeperborn.client.StringRenderer.submit(kind,new PoseStack(),nodes,10);
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$stringKind(4);
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$stringKind()!=4)throw new AssertionError("String state mixin");
                PoseStack pawPoses=new PoseStack();org.joml.Matrix4f pawBefore=new org.joml.Matrix4f(pawPoses.last().pose());
                for(int kind:new int[]{2,1,3,4,5,6}){com.sibcraft.creeperborn.client.PawRenderer.submit(kind,pawPoses,nodes,20);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Paw renderer leaked rotation/scale from kind "+kind);}
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$pawKind(4);
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$pawKind()!=4)throw new AssertionError("Paw state mixin");
                for(int kind=1;kind<=8;kind++){com.sibcraft.creeperborn.client.TremorRenderer.submit(kind,pawPoses,nodes,12);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Tremor pose leak");}
                for(int kind=1;kind<=12;kind++){com.sibcraft.creeperborn.client.PairRenderer.submit(kind,pawPoses,nodes,12);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Love/Sand pose leak");}
                for(int kind=1;kind<=18;kind++){com.sibcraft.creeperborn.client.ClimateRenderer.submit(kind,pawPoses,nodes,12);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Ice/Magma pose leak");}
                if(parts[0]!=225)throw new AssertionError("Legacy model submissions changed: "+parts[0]);
                com.sibcraft.creeperborn.client.FireRenderer.validate();
                for(int frame:new int[]{0,3,6,9,11}){com.sibcraft.creeperborn.client.FireRenderer.submit(16,frame,pawPoses,nodes);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Fireball leaked a pose");}
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$fireKind(1);((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$fireballLength(16);
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$fireKind()!=1||((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$fireballLength()!=16)throw new AssertionError("Fireball render state missing");
                for(int frame:new int[]{0,4,12,21,27}){com.sibcraft.creeperborn.client.FireRenderer.submitRing(8,frame,pawPoses,nodes);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Flame Ring leaked a pose");}
                com.sibcraft.creeperborn.client.WaterRenderer.validate();
                for(int kind:new int[]{1,2})for(int frame:new int[]{0,6,12,20,30}){com.sibcraft.creeperborn.client.WaterRenderer.submit(kind,16,frame,pawPoses,nodes);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Water animation leaked a pose");}
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$waterKind(2);((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$waterLength(16);
                if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$waterKind()!=2||((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$waterLength()!=16)throw new AssertionError("Water render state missing");
                var visualOwner=java.util.UUID.randomUUID();
                if(!visualOwner.equals(com.sibcraft.creeperborn.client.ClimateRenderer.attachedOwner("cb_climate_17|"+visualOwner))||com.sibcraft.creeperborn.client.ClimateRenderer.kind("cb_climate_11|"+visualOwner)!=11)throw new AssertionError("Attached climate visual must preserve its owner and model");
                var ownerAt=new net.minecraft.world.phys.Vec3(8,20,12);var staleAt=new net.minecraft.world.phys.Vec3(3,2,4);
                for(int kind:new int[]{11,17})if(!com.sibcraft.creeperborn.client.ClimateRenderer.attachmentPosition(kind,ownerAt,staleAt).equals(ownerAt))throw new AssertionError("Armor and cloak must follow predicted owner position");
                if(!com.sibcraft.creeperborn.client.ClimateRenderer.attachmentPosition(16,ownerAt,staleAt).equals(new net.minecraft.world.phys.Vec3(8,2,12)))throw new AssertionError("Magma column must follow owner horizontally while keeping ground height");
                com.sibcraft.creeperborn.client.AzureRenderer.validate();
                if(com.sibcraft.creeperborn.client.AzureVisuals.revealDistance(0)!=30||com.sibcraft.creeperborn.client.AzureVisuals.revealDistance(2)!=12f)throw new AssertionError("Dragon camera must reveal wide then settle close");
                for(int pitch=-90;pitch<=90;pitch++){var offset=com.sibcraft.creeperborn.client.AzureVisuals.cameraOffset(45,pitch,3);if(!Double.isFinite(offset.lengthSqr())||offset.length()<5)throw new AssertionError("Dragon camera orbit must remain outside skull through climbs and dives");}
                if(com.sibcraft.creeperborn.client.AzureVisuals.aimBlend(0)!=0||com.sibcraft.creeperborn.client.AzureVisuals.aimBlend(60)!=1)throw new AssertionError("Dragon camera must blend cruise into downward aim");
                float prior=30;for(int frame=0;frame<=120;frame++){float distance=com.sibcraft.creeperborn.client.AzureVisuals.revealDistance(frame/60.0);if(distance>prior||distance<12f||prior-distance>1)throw new AssertionError("Camera reveal is discontinuous");prior=distance;}
                var hidden=(com.sibcraft.creeperborn.client.AzureHiddenState)new net.minecraft.client.renderer.entity.state.LivingEntityRenderState();hidden.creeperborn$azureHidden(true);if(!hidden.creeperborn$azureHidden())throw new AssertionError("Invisible helper render state not mixed in");
                if(Arrays.stream(Class.forName("net.minecraft.client.Camera").getDeclaredMethods()).noneMatch(v->v.getName().contains("creeperborn$dragonZoom")))throw new AssertionError("Azure camera mixin did not apply");
                int azureStart=parts[0];
                for(int tier=1;tier<=3;tier++)for(int flags:new int[]{1|(tier<<2)|128,1|(tier<<2)|16|32|128}){com.sibcraft.creeperborn.client.AzureRenderer.avatar(state,pawPoses,nodes,flags);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Hybrid pose leak");}
                for(int kind=1;kind<=16;kind++){com.sibcraft.creeperborn.client.AzureRenderer.submit(kind,pawPoses,nodes,12);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Azure effect pose leak");}
                com.sibcraft.creeperborn.client.AzureRenderer.hand(pawPoses,nodes);
                if(parts[0]-azureStart<60)throw new AssertionError("Azure forms did not submit complete meshes");
                ((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$azureKind(12);if(((com.sibcraft.creeperborn.client.BoltVisualState)boltState).creeperborn$azureKind()!=12)throw new AssertionError("Azure render state missing");
                if(Arrays.stream(Class.forName("net.minecraft.client.renderer.entity.LivingEntityRenderer").getDeclaredMethods()).noneMatch(v->v.getName().contains("creeperborn$bird")))throw new AssertionError("Phoenix avatar mixin did not apply");
                if(Arrays.stream(Class.forName("net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer").getDeclaredMethods()).noneMatch(v->v.getName().contains("creeperborn$birdHands")))throw new AssertionError("Phoenix hands mixin did not apply");
                if(com.sibcraft.creeperborn.client.NatureVisuals.revealZoom(0)!=1||com.sibcraft.creeperborn.client.NatureVisuals.revealZoom(2)!=0||com.sibcraft.creeperborn.client.NatureVisuals.revealZoom(1)<=0)throw new AssertionError("Nature camera must reveal then return to aim");
                if(Math.abs(com.sibcraft.creeperborn.NaturePowers.headHeight(1|4|1024)-4.9)>.001||Math.abs(com.sibcraft.creeperborn.NaturePowers.headHeight(2|4)-3.25)>.001)throw new AssertionError("Giant camera eye heights must match rendered heads");
                int chainStart=parts[0];com.sibcraft.creeperborn.client.NatureRenderer.submit(13,1,10,pawPoses,nodes);int singleChain=parts[0]-chainStart;chainStart=parts[0];com.sibcraft.creeperborn.client.NatureRenderer.submit(13,5,10,pawPoses,nodes);if(parts[0]-chainStart!=singleChain*5)throw new AssertionError("Mastered grapple must render five linked arm chains");
                com.sibcraft.creeperborn.client.NatureRenderer.validate();for(var texture:com.sibcraft.creeperborn.client.NatureRenderer.textures())if(client.getResourceManager().getResource(texture).isEmpty())throw new AssertionError("Missing native nature texture: "+texture);
                for(int biome=0;biome<12;biome++)for(int kind:new int[]{1,2,3,4,5,11,12,13,14})for(int frame:new int[]{0,10,21}){com.sibcraft.creeperborn.client.NatureRenderer.submit(kind,biome,frame,pawPoses,nodes);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Nature effect pose leak");}
                for(int biome=0;biome<12;biome++)for(int flags:new int[]{1|4|1024,1|8,2|4|8,2|32}){com.sibcraft.creeperborn.client.NatureRenderer.avatar(state,pawPoses,nodes,flags|(biome<<6),(flags&4)==0);if(!pawBefore.equals(pawPoses.last().pose()))throw new AssertionError("Nature form pose leak");}
                System.out.println("CREEPERBORN_CLIENT_SMOKE_PASSED");
                client.stop();
            }catch(Throwable error){error.printStackTrace();System.exit(1);}
        });
    }
}
