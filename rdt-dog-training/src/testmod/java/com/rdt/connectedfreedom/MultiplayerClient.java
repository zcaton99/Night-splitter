package com.rdt.connectedfreedom;
import com.rdt.connectedfreedom.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.*;
import net.minecraft.client.gui.screens.*;
import net.minecraft.client.multiplayer.*;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
public final class MultiplayerClient implements ClientModInitializer {
    private int stage,ticks,total,exit;private boolean connecting;
    public void onInitializeClient(){if(!Boolean.getBoolean("rdt.multiplayer"))return;
        ClientPlayNetworking.registerGlobalReceiver(Probe.TYPE,(p,c)->{stage=p.stage();ticks=0;});
        ClientTickEvents.END_CLIENT_TICK.register(c->{try{total++;if(total>4500)throw new AssertionError("RDT client timeout at stage "+stage);
            if(stage==9){if(exit++==0)ClientPlayNetworking.send(new Probe(9,0));if(exit>=10){System.out.println("RDT_NATIVE_MULTIPLAYER_CLIENT_PASSED");c.stop();System.exit(0);}return;}
            if(c.player==null||c.level==null||c.getConnection()==null){if(!connecting&&total>40&&c.gui.overlay()==null){connecting=true;ConnectScreen.startConnecting(new TitleScreen(),c,ServerAddress.parseString("127.0.0.1:25575"),new ServerData("RDT test","127.0.0.1:25575",ServerData.Type.OTHER),false,null);}return;}
            ticks++;boolean alpha=c.player.getName().getString().equals("LanAlpha");c.options.keyUp.setDown(false);c.options.keyJump.setDown(false);
            if(c.gui.screen() instanceof JournalScreen&&stage!=8)c.gui.setScreen(null);
            if(stage==1&&ticks==10)RdtClient.send("adopt",alpha?"Sprout":"River");
            if(stage==2){if(ticks%150==20)RdtClient.send("name","");if(ticks%150==50)RdtClient.send("help","");if(ticks==5)RdtClient.send("yes","");if(ticks%20==0&&RdtClient.view!=null)ClientPlayNetworking.send(new Probe(2,RdtClient.view.connections()));}
            if(stage==3&&ticks==10&&alpha)RdtClient.send("line","");if(stage==3&&ticks==20)RdtClient.send("adopt","Overwrite");
            if(stage==4&&ticks==45)RdtClient.send("help","");
            if(stage==5){if(ticks==20||ticks==210||ticks==400)RdtClient.send("celebrate","");if(ticks>25){c.player.setYRot((ticks-25)*4);c.player.setXRot(0);c.options.keyUp.setDown(true);}}
            if(stage==6&&ticks==8)for(int i=0;i<9;i++)if(c.player.getInventory().getItem(i).is(DogTraining.TOY)){c.player.getInventory().setSelectedSlot(i);c.getConnection().send(new net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket(i));break;}
            if(stage==6&&ticks==20)RdtClient.send("ready","");if(stage==6&&ticks==45)RdtClient.send("yes","");
            if(stage==7&&ticks==15)RdtClient.send("done","");
            if(stage==8&&ticks==10)RdtClient.send("open","");if(stage==8&&ticks==55){if(!(c.gui.screen() instanceof JournalScreen)||RdtClient.view==null||!RdtClient.view.complete())throw new AssertionError("Completed journal did not synchronize");net.minecraft.client.Screenshot.grab(c.gameDirectory,"rdt-native-journal.png",c.gameRenderer.mainRenderTarget(),1,m->{});ClientPlayNetworking.send(new Probe(8,1));}
        }catch(Throwable e){e.printStackTrace();System.exit(1);}});
    }
}
