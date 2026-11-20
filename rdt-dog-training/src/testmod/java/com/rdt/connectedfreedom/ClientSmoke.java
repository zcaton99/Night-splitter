package com.rdt.connectedfreedom;
import com.rdt.connectedfreedom.client.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.gui.components.AbstractWidget;
public final class ClientSmoke implements ClientModInitializer {
    private int ticks,step;private boolean started;
    public void onInitializeClient(){if(!Boolean.getBoolean("rdt.visualSmoke"))return;ClientTickEvents.END_CLIENT_TICK.register(c->{try{
        ticks++;if(!started){if(ticks<60||c.gui.screen()==null||c.gui.overlay()!=null)return;started=true;ticks=0;RdtClient.view=null;c.gui.setScreen(new JournalScreen());}
        if(ticks==15)net.minecraft.client.Screenshot.grab(c.gameDirectory,"rdt-welcome.png",c.gameRenderer.mainRenderTarget(),1,m->{});
        if(ticks==20){RdtClient.view=new CoachPayload.View("Sprout",1,2,3,0,0,18,1,40,30,5,true,false,"explore","You reconnected and the line is loose. Continue walking together.","After a success, celebrate together.");c.gui.setScreen(new JournalScreen());}
        if(ticks==35)net.minecraft.client.Screenshot.grab(c.gameDirectory,"rdt-walk-menu.png",c.gameRenderer.mainRenderTarget(),1,m->{});
        if(ticks==40||ticks==65){var screen=new JournalScreen();var field=JournalScreen.class.getDeclaredField("page");field.setAccessible(true);field.setInt(screen,ticks==40?1:2);c.gui.setScreen(screen);}
        if(ticks==55||ticks==80)net.minecraft.client.Screenshot.grab(c.gameDirectory,ticks==55?"rdt-learning.png":"rdt-progress.png",c.gameRenderer.mainRenderTarget(),1,m->{});
        if(ticks==90){for(int page=0;page<4;page++){var screen=new JournalScreen();var f=JournalScreen.class.getDeclaredField("page");f.setAccessible(true);f.setInt(screen,page);screen.init(320,240);for(var child:screen.children())if(child instanceof AbstractWidget w){if(w.getX()<0||w.getY()<0||w.getX()+w.getWidth()>320||w.getY()+w.getHeight()>240)throw new AssertionError("Journal control clips at minimum GUI size");}}System.out.println("RDT_CLIENT_SMOKE_PASSED");c.stop();}
    }catch(Throwable e){e.printStackTrace();System.exit(1);}});}
}
