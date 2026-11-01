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
 private int stage,peer,tick,total,retry,blocked,exitTicks;private boolean connecting,wasConnected;
 public void onInitializeClient(){if(!Boolean.getBoolean("creeperborn.multiplayer"))return;ClientPlayNetworking.registerGlobalReceiver(MultiplayerProbe.TYPE,(p,ctx)->{if(p.stage()!=stage){stage=p.stage();tick=0;}peer=p.peer();});
  ClientTickEvents.END_CLIENT_TICK.register(c->{try{if(stage==9){exitTicks++;if(exitTicks==1&&ClientPlayNetworking.canSend(MultiplayerProbe.TYPE))ClientPlayNetworking.send(new MultiplayerProbe(2,9,peer,0,0,0,0,0,0,0,0,0,0,0,0,0));if(exitTicks>=10){System.out.println("CREEPERBORN_NATIVE_MULTIPLAYER_CLIENT_PASSED");c.stop();System.exit(0);}return;}total++;if(connecting&&!wasConnected&&stage==0&&c.gui.screen()!=null&&c.gui.screen().getClass().getSimpleName().equals("DisconnectedScreen"))throw new AssertionError("Native multiplayer login rejected; see dedicated server log");if(total>9000)throw new AssertionError("Native multiplayer client timed out");if(c.player==null||c.level==null||c.getConnection()==null){if(wasConnected){wasConnected=false;connecting=false;retry=60;}if(retry>0){retry--;return;}if(!connecting&&total>40&&c.gui.overlay()==null){connecting=true;ConnectScreen.startConnecting(new TitleScreen(),c,ServerAddress.parseString("127.0.0.1:25575"),new ServerData("Creeperborn CI","127.0.0.1:25575",ServerData.Type.OTHER),false,null);}return;}wasConnected=true;if(c.gui.screen()!=null||c.gui.overlay()!=null){blocked++;if(blocked%100==0)System.out.println("MULTIPLAYER_CLIENT_WAIT stage="+stage+" screen="+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName())+" overlay="+(c.gui.overlay()==null?"none":c.gui.overlay().getClass().getSimpleName()));if(blocked>1200)throw new AssertionError("Native client gameplay screen remained blocked: "+(c.gui.screen()==null?"none":c.gui.screen().getClass().getSimpleName()));return;}blocked=0;tick++;
   c.options.keyUp.setDown(false);c.options.keyRight.setDown(false);c.options.keyJump.setDown(false);c.options.keyAttack.setDown(false);c.options.keyUse.setDown(false);c.options.keyShift.setDown(false);
   if(stage==1||stage==5||stage==8){if(tick<70){c.player.setYRot(0);c.player.setXRot(0);c.options.keyUp.setDown(tick>15);c.options.keyJump.setDown(tick>15&&tick<35);}else if(tick<100){c.player.setYRot(35);c.options.keyUp.setDown(true);}else{c.player.setYRot(0);c.player.setXRot(0);}}
   if(stage==8&&(ZoanVisuals.flags(c.player.getId())&3)==1)c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
   if(stage==12&&(LuffyVisuals.flags(c.player.getId())&1)!=0)c.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
   if(stage==8&&tick==130||stage==10&&tick==70||stage==12&&tick==90)net.minecraft.client.Screenshot.grab(c.gameDirectory,"fruit-stage-"+stage+".png",c.mainRenderTarget(),1,message->{});
   if(stage==19){c.options.setCameraType(tick%100<50?net.minecraft.client.CameraType.FIRST_PERSON:net.minecraft.client.CameraType.THIRD_PERSON_BACK);c.player.setYRot(tick*.8f);c.player.setXRot((float)Math.sin(tick*.04)*20);}
   if(stage==10&&tick>15&&(ZoanVisuals.flags(c.player.getId())&15)==13&&c.options.getCameraType()!=net.minecraft.client.CameraType.FIRST_PERSON)throw new AssertionError("Native full Fox did not enter first-person head aiming");
   if(stage==10&&(ZoanVisuals.flags(c.player.getId())&3)==2){c.player.setYRot(0);c.player.setXRot(0);c.options.keyUp.setDown(tick>15&&tick<55);}
   if(stage>=12&&stage<=14&&(LuffyVisuals.flags(c.player.getId())&1)!=0)c.options.keyShift.setDown(true);
   if(stage==12&&(LuffyVisuals.flags(c.player.getId())&1)!=0){c.player.setYRot(0);c.player.setXRot(0);c.options.keyUse.setDown(tick>=15&&tick<65);}
   if(stage==13&&(LuffyVisuals.flags(c.player.getId())&1)==0)c.options.keyUp.setDown(tick<80);
   if(stage==14&&(LuffyVisuals.flags(c.player.getId())&1)!=0){c.player.setYRot(0);c.player.setXRot(0);c.options.keyUp.setDown(tick>15&&tick<55);}
   if(tick%5==0&&ClientPlayNetworking.canSend(MultiplayerProbe.TYPE)){int timeOwn=0,timeOther=0;for(var entity:c.level.entitiesForRendering())if(entity instanceof net.minecraft.world.entity.Display.BlockDisplay d&&d.getCustomName()!=null&&d.getCustomName().getString().startsWith("cb_time_1|")){var data=TimeRenderer.extract(d);if(data!=null&&data.skin()!=null){if(d.getCustomName().getString().endsWith("|"+c.player.getUUID()))timeOwn++;else timeOther++;}}int displays=0;for(var e:c.level.entitiesForRendering())if(e.getCustomName()!=null&&e.getCustomName().getString().startsWith("cb_"))displays++;if(!c.level.hasChunkAt(c.player.blockPosition()))displays=-1;ClientPlayNetworking.send(new MultiplayerProbe(1,stage,c.level.getEntity(peer)==null?-1:peer,tick,CelestialVisuals.flags(c.player.getId()),CelestialVisuals.flags(peer),AzureVisuals.flags(c.player.getId()),AzureVisuals.flags(peer),NatureVisuals.flags(c.player.getId()),NatureVisuals.flags(peer),stage>=16?timeOwn:stage>=12?LuffyVisuals.flags(c.player.getId()):ZoanVisuals.flags(c.player.getId()),stage>=16?timeOther:stage>=12?LuffyVisuals.flags(peer):ZoanVisuals.flags(peer),displays,c.player.getX(),c.player.getY(),c.player.getZ()));}
   if(stage==9&&tick>20){System.out.println("CREEPERBORN_NATIVE_MULTIPLAYER_CLIENT_PASSED "+c.player.getName().getString());c.stop();}
  }catch(Throwable error){error.printStackTrace();System.exit(1);}});
 }
}
