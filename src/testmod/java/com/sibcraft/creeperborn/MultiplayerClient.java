package com.sibcraft.creeperborn;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import com.sibcraft.creeperborn.client.*;
public final class MultiplayerClient implements ClientModInitializer {
 private int stage,peer,tick,total,retry;private boolean connecting,wasConnected;
 public void onInitializeClient(){if(!Boolean.getBoolean("creeperborn.multiplayer"))return;ClientPlayNetworking.registerGlobalReceiver(MultiplayerProbe.TYPE,(p,ctx)->{if(p.stage()!=stage){stage=p.stage();tick=0;}peer=p.peer();});
  ClientTickEvents.END_CLIENT_TICK.register(c->{try{total++;if(total>9000)throw new AssertionError("Native multiplayer client timed out");if(c.player==null||c.level==null||c.getConnection()==null){if(wasConnected){wasConnected=false;connecting=false;retry=60;}if(retry>0){retry--;return;}if(!connecting&&total>40&&c.gui.overlay()==null){connecting=true;ConnectScreen.startConnecting(new TitleScreen(),c,ServerAddress.parseString("127.0.0.1:25575"),new ServerData("Creeperborn CI","127.0.0.1:25575",ServerData.Type.OTHER),false,null);}return;}wasConnected=true;tick++;
   c.options.keyUp.setDown(false);c.options.keyRight.setDown(false);c.options.keyJump.setDown(false);c.options.keyAttack.setDown(false);c.options.keyUse.setDown(false);
   if(stage==1||stage==5){if(tick<70){c.player.setYRot(0);c.player.setXRot(0);c.options.keyUp.setDown(tick>15);c.options.keyJump.setDown(tick>15&&tick<35);}else if(tick<100){c.player.setYRot(35);c.options.keyUp.setDown(true);}else{c.player.setYRot(0);c.player.setXRot(0);}}
   if(tick%5==0&&ClientPlayNetworking.canSend(MultiplayerProbe.TYPE)){int displays=0;for(var e:c.level.entitiesForRendering())if(e.getCustomName()!=null&&e.getCustomName().getString().startsWith("cb_"))displays++;ClientPlayNetworking.send(new MultiplayerProbe(1,stage,peer,tick,CelestialVisuals.flags(c.player.getId()),CelestialVisuals.flags(peer),AzureVisuals.flags(c.player.getId()),AzureVisuals.flags(peer),NatureVisuals.flags(c.player.getId()),NatureVisuals.flags(peer),displays,c.player.getX(),c.player.getY(),c.player.getZ()));}
   if(stage==9&&tick>20){System.out.println("CREEPERBORN_NATIVE_MULTIPLAYER_CLIENT_PASSED "+c.player.getName().getString());c.stop();}
  }catch(Throwable error){error.printStackTrace();System.exit(1);}});
 }
}
