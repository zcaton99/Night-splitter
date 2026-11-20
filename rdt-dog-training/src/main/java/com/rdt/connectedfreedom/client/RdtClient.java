package com.rdt.connectedfreedom.client;
import com.google.gson.Gson;
import com.mojang.blaze3d.platform.InputConstants;
import com.rdt.connectedfreedom.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.*;
import net.minecraft.resources.Identifier;
public final class RdtClient implements ClientModInitializer {
    public static CoachPayload.View view;
    private static final Gson JSON=new Gson();
    private static final KeyMapping.Category CATEGORY=KeyMapping.Category.register(Identifier.fromNamespaceAndPath("rdt","training"));
    private static KeyMapping key(String name,int code){return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.rdt."+name,InputConstants.Type.KEYBOARD,code,CATEGORY));}
    public static void send(String action,String value){if(ClientPlayNetworking.canSend(JournalPayload.TYPE))ClientPlayNetworking.send(new JournalPayload(action,value));}
    @Override public void onInitializeClient(){
        ClientPlayNetworking.registerGlobalReceiver(CoachPayload.TYPE,(packet,ctx)->{var next=JSON.fromJson(packet.json(),CoachPayload.View.class);if(view==null||!view.notice().equals(next.notice()))CoachOverlay.show(next.notice());view=next;if(packet.open())ctx.client().gui.setScreen(new JournalScreen());});
        ClientPlayConnectionEvents.DISCONNECT.register((handler,client)->{view=null;CoachOverlay.clear();});
        KeyMapping journal=key("journal",InputConstants.KEY_J),name=key("name",InputConstants.KEY_N),yes=key("yes",InputConstants.KEY_Y),help=key("help",InputConstants.KEY_H),celebrate=key("celebrate",InputConstants.KEY_K),ready=key("ready",InputConstants.KEY_U),done=key("done",InputConstants.KEY_O);
        ClientTickEvents.END_CLIENT_TICK.register(c->{if(c.player==null||c.gui.screen()!=null||c.isPaused())return;
            while(journal.consumeClick())send("open","");while(name.consumeClick())send("name","");while(yes.consumeClick())send("yes","");while(help.consumeClick())send("help","");while(celebrate.consumeClick())send("celebrate","");while(ready.consumeClick())send("ready","");while(done.consumeClick())send("done","");
        });
    }
}
