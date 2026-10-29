package com.sibcraft.creeperborn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sibcraft.creeperborn.Element;
import com.sibcraft.creeperborn.ControlPayload;
import com.sibcraft.creeperborn.WaterStatePayload;
import com.sibcraft.creeperborn.WaterRules;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CreeperbornClient implements ClientModInitializer {
    private boolean combat, fruit, checkedBindings;
    private Element selected;
    private int ticks, lastAirStep=-100;
    private final boolean[] held=new boolean[3];
    private boolean waitingForRelease;
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("creeperborn","combat"));
    private static KeyMapping key(String name,int code){return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.creeperborn."+name,InputConstants.Type.KEYBOARD,code,CATEGORY));}
    private void send(int action,int value){if(ClientPlayNetworking.canSend(ControlPayload.TYPE))ClientPlayNetworking.send(new ControlPayload(action,value));}
    private boolean holding(){return held[0]||held[1]||held[2];}
    private void cancel(){if(holding()||StringVisuals.controlling())send(8,0);java.util.Arrays.fill(held,false);waitingForRelease=true;}
    private void changeSelection(){boolean wasHolding=holding();cancel();waitingForRelease=wasHolding;}
    @Override public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(com.sibcraft.creeperborn.AzureVisualPayload.TYPE,(payload,context)->AzureVisuals.update(payload.entity(),payload.flags()));
        ClientPlayNetworking.registerGlobalReceiver(com.sibcraft.creeperborn.PhoenixVisualPayload.TYPE,(payload,context)->PhoenixVisuals.update(payload.entity(),payload.flags()));
        ClientPlayNetworking.registerGlobalReceiver(com.sibcraft.creeperborn.LightningVisualPayload.TYPE,(payload,context)->LightningVisuals.update(payload.entity(),payload.flags()));
        ClientPlayNetworking.registerGlobalReceiver(com.sibcraft.creeperborn.DarkVisualPayload.TYPE,(payload,context)->DarkVisuals.update(payload.entity(),payload.flags()));
        ClientPlayNetworking.registerGlobalReceiver(com.sibcraft.creeperborn.StringControlPayload.TYPE,(payload,context)->StringVisuals.update(payload));
        KeyMapping toggle=key("toggle",InputConstants.KEY_B),cycle=key("cycle",InputConstants.KEY_R),fruits=key("fruit",InputConstants.KEY_G);
        KeyMapping[] moves={key("move1",InputConstants.KEY_Z),key("move2",InputConstants.KEY_X),key("move3",InputConstants.KEY_C)};
        ClientPlayNetworking.registerGlobalReceiver(ControlPayload.TYPE,(payload,context)->{
            if(payload.action()==11&&net.minecraft.client.Minecraft.getInstance().player!=null)com.sibcraft.creeperborn.ClimatePowers.clientState(net.minecraft.client.Minecraft.getInstance().player.getUUID(),payload.value());
            if(payload.action()==3){fruit=payload.value()>=4;selected=payload.value()>=0&&payload.value()<4?Element.values()[payload.value()]:null;}
        });
        ClientPlayNetworking.registerGlobalReceiver(WaterStatePayload.TYPE,(payload,context)->{
            var player=net.minecraft.client.Minecraft.getInstance().player;
            if(player!=null)WaterRules.clientState(player.getUUID(),payload.flags());
        });
        ClientTickEvents.END_CLIENT_TICK.register(client->{
            ticks++;
            if(client.player==null||client.getConnection()==null){StringVisuals.clear();AzureVisuals.clear();WaterRules.clearClient();com.sibcraft.creeperborn.ClimatePowers.clearClient();PhoenixVisuals.clear();LightningVisuals.clear();DarkVisuals.clear();combat=false;fruit=false;selected=null;java.util.Arrays.fill(held,false);waitingForRelease=true;return;}
            StringVisuals.tick(client,ticks);AzureVisuals.tick(client);
            int azureKeys=client.gui.screen()!=null||client.isPaused()?0:(client.options.keyUp.isDown()?1:0)|(client.options.keyDown.isDown()?2:0)|(client.options.keyLeft.isDown()?4:0)|(client.options.keyRight.isDown()?8:0)|(client.options.keyJump.isDown()?16:0)|(client.options.keyShift.isDown()?32:0)|(client.options.keyAttack.isDown()?64:0);
            com.sibcraft.creeperborn.Azure.clientInput(azureKeys);
            if((AzureVisuals.flags(client.player.getId())&3)==2&&ClientPlayNetworking.canSend(com.sibcraft.creeperborn.AzureFlightPayload.TYPE))ClientPlayNetworking.send(new com.sibcraft.creeperborn.AzureFlightPayload(azureKeys,client.player.getYRot(),client.player.getXRot()));
            if(!checkedBindings){
                checkedBindings=true;
                boolean legacy=moves[0].matches(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_R))
                    &&moves[1].matches(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_G))
                    &&moves[2].matches(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_H));
                boolean changed=false;
                if(legacy){
                    int[] defaults={InputConstants.KEY_Z,InputConstants.KEY_X,InputConstants.KEY_C};
                    for(int i=0;i<3;i++)moves[i].setKey(InputConstants.Type.KEYBOARD.getOrCreate(defaults[i]));
                    changed=true;
                }
                if(fruits.matches(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_T))){
                    fruits.setKey(InputConstants.Type.KEYBOARD.getOrCreate(InputConstants.KEY_G));changed=true;
                }
                if(changed){
                    KeyMapping.resetMapping();client.options.save();
                    client.player.sendSystemMessage(Component.literal("Creeperborn updated old controls: R elements, G fruits, Z/X/C moves. Custom bindings remain available in Controls."));
                }
            }
            if(ticks%4==0){int flags=0;if(client.gui.screen()==null&&!client.isPaused()){flags=(client.options.keyUp.isDown()?1:0)|(client.options.keyDown.isDown()?2:0)|(client.options.keyLeft.isDown()?4:0)|(client.options.keyRight.isDown()?8:0)|(client.options.keyJump.isDown()?16:0)|(client.options.keyShift.isDown()?32:0)|(client.options.keyAttack.isDown()?64:0);}send(10,flags);}
            if(ticks%4==0)send(9,client.gui.screen()==null&&!client.isPaused()&&client.options.keyUse.isDown()&&client.options.keyShift.isDown()?1:0);
            if(WaterRules.suppressed(client.player))cancel();
            if(client.gui.screen()!=null||client.isPaused()){
                cancel();while(toggle.consumeClick()){}while(cycle.consumeClick()){}while(fruits.consumeClick()){}
                for(KeyMapping mapping:moves)while(mapping.consumeClick()){}return;
            }
            while(toggle.consumeClick()){
                combat=!combat;cancel();
                if(combat&&!ClientPlayNetworking.canSend(ControlPayload.TYPE)){
                    combat=false;
                    client.player.sendSystemMessage(Component.literal("Creeperborn controls are unavailable on this server. Install the matching client/server version."));
                    continue;
                }
                if(combat){
                    send(0,0);
                    client.player.sendSystemMessage(Component.literal("Creeperborn controls — elements: "+cycle.getTranslatedKeyMessage().getString()
                        +"; fruit: "+fruits.getTranslatedKeyMessage().getString()+"; moves: "
                        +moves[0].getTranslatedKeyMessage().getString()+"/"+moves[1].getTranslatedKeyMessage().getString()+"/"+moves[2].getTranslatedKeyMessage().getString()));
                }
                client.player.sendOverlayMessage(Component.literal("Creeperborn combat "+(combat?"ON — R elements; G fruits; Z/X/C moves":"OFF")));
            }
            while(cycle.consumeClick()){if(ClientPlayNetworking.canSend(ControlPayload.TYPE)){combat=true;changeSelection();send(1,client.options.keyShift.isDown()?-1:1);}}
            while(fruits.consumeClick()){if(ClientPlayNetworking.canSend(ControlPayload.TYPE)){combat=true;changeSelection();send(4,0);}}
            boolean down=false;for(KeyMapping mapping:moves)down|=mapping.isDown();
            if(waitingForRelease&&!down)waitingForRelease=false;
            for(int i=0;i<3;i++)while(moves[i].consumeClick()){
                if(!combat||waitingForRelease||held[i])continue;
                int direction=(client.options.keyUp.isDown()?1:0)|(client.options.keyDown.isDown()?2:0)
                    |(client.options.keyLeft.isDown()?4:0)|(client.options.keyRight.isDown()?8:0);
                held[i]=true;send(5,i|(direction<<2));
                if(selected==Element.AIR&&i==1)lastAirStep=ticks;
            }
            if(combat){
                for(int i=0;i<3;i++)if(held[i]&&!moves[i].isDown()){send(6,i);held[i]=false;}
                if(holding()&&ticks%4==0)send(7,0);
            }
            if(combat&&!fruit&&!waitingForRelease&&selected==Element.AIR&&moves[1].isDown()&&ticks-lastAirStep>=4){send(5,1);lastAirStep=ticks;}
        });
    }
}


