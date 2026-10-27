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
                PhoenixRenderer.validate();
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
                if(parts[0]!=24)throw new AssertionError("Expected 24 model submissions, got "+parts[0]);
                if(Arrays.stream(Class.forName("net.minecraft.client.renderer.entity.LivingEntityRenderer").getDeclaredMethods()).noneMatch(v->v.getName().contains("creeperborn$bird")))throw new AssertionError("Phoenix avatar mixin did not apply");
                if(Arrays.stream(Class.forName("net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer").getDeclaredMethods()).noneMatch(v->v.getName().contains("creeperborn$birdHands")))throw new AssertionError("Phoenix hands mixin did not apply");
                System.out.println("CREEPERBORN_CLIENT_SMOKE_PASSED");
                client.stop();
            }catch(Throwable error){error.printStackTrace();System.exit(1);}
        });
    }
}
