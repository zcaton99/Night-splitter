package com.sibcraft.creeperborn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sibcraft.creeperborn.Element;
import com.sibcraft.creeperborn.ControlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CreeperbornClient implements ClientModInitializer {
    private boolean combat, fruit;
    private Element selected;
    private int ticks, lastAirStep=-100, held=-1;
    private boolean waitingForRelease;
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("creeperborn","combat"));
    private static KeyMapping key(String name,int code){return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.creeperborn."+name,InputConstants.Type.KEYBOARD,code,CATEGORY));}
    private void send(int action,int value){if(ClientPlayNetworking.canSend(ControlPayload.TYPE))ClientPlayNetworking.send(new ControlPayload(action,value));}
    private void cancel(){if(held>=0)send(8,0);held=-1;waitingForRelease=true;}
    @Override public void onInitializeClient(){
        KeyMapping toggle=key("toggle",InputConstants.KEY_B),cycle=key("cycle",InputConstants.KEY_R),fruits=key("fruit",InputConstants.KEY_T);
        KeyMapping[] moves={key("move1",InputConstants.KEY_Z),key("move2",InputConstants.KEY_X),key("move3",InputConstants.KEY_C)};
        ClientPlayNetworking.registerGlobalReceiver(ControlPayload.TYPE,(payload,context)->{
            if(payload.action()==3){fruit=payload.value()==4;selected=payload.value()>=0&&payload.value()<4?Element.values()[payload.value()]:null;}
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            ticks++;
            if(client.player==null||client.getConnection()==null){combat=false;fruit=false;selected=null;held=-1;waitingForRelease=true;return;}
            if(client.gui.screen()!=null||client.isPaused()){
                cancel();while(toggle.consumeClick()){}while(cycle.consumeClick()){}while(fruits.consumeClick()){}
                for(KeyMapping mapping:moves)while(mapping.consumeClick()){}return;
            }
            while(toggle.consumeClick()){
                combat=!combat;cancel();if(combat)send(0,0);
                client.player.sendOverlayMessage(Component.literal("Creeperborn combat "+(combat?"ON — R elements; T fruits; Z/X/C moves":"OFF")));
            }
            while(cycle.consumeClick()){if(combat){cancel();send(1,client.options.keyShift.isDown()?-1:1);}}
            while(fruits.consumeClick()){if(combat){cancel();send(4,0);}}
            boolean down=false;for(KeyMapping mapping:moves)down|=mapping.isDown();
            if(waitingForRelease&&!down)waitingForRelease=false;
            for(int i=0;i<3;i++)while(moves[i].consumeClick()){
                if(!combat||waitingForRelease)continue;
                if(fruit){if(held<0){held=i;
                    int direction=(client.options.keyUp.isDown()?1:0)|(client.options.keyDown.isDown()?2:0)
                        |(client.options.keyLeft.isDown()?4:0)|(client.options.keyRight.isDown()?8:0);
                    send(5,i|(direction<<2));}}
                else {send(2,i);if(selected==Element.AIR&&i==1)lastAirStep=ticks;}
            }
            if(combat&&fruit&&held>=0){
                if(!moves[held].isDown()){send(6,held);held=-1;}
                else if(ticks%4==0)send(7,0);
            }
            if(combat&&!fruit&&!waitingForRelease&&selected==Element.AIR&&moves[1].isDown()&&ticks-lastAirStep>=4){send(2,1);lastAirStep=ticks;}
        });
    }
}
