package com.rdt.connectedfreedom.client;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
public final class CoachOverlay {
    private static String text="";private static long until;
    public static void show(String message){text=message;until=System.nanoTime()+6_000_000_000L;}
    public static void clear(){text="";until=0;}
    public static void render(GuiGraphicsExtractor gui){
        var c=Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null||System.nanoTime()>until||text.isEmpty())return;
        var lines=c.font.split(Component.literal("Coach: "+text),Math.max(1,Math.min(340,gui.guiWidth()-20)));
        int y=Math.max(8,gui.guiHeight()-64-lines.size()*10);
        for(var line:lines){gui.text(c.font,line,9,y,0xFFF3F4D4,true);y+=10;}
    }
}
