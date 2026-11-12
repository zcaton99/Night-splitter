package com.sibcraft.creeperborn.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Reflow on every frame so window size and GUI scale never clip selection help. */
public final class SelectionOverlay {
    private static String help="",toast="";private static long helpUntil,toastUntil;
    private SelectionOverlay(){}
    public static void show(String text,boolean selection){long now=System.nanoTime();if(selection){help=text;helpUntil=now+8_000_000_000L;toast="";}else{toast=text;toastUntil=now+3_000_000_000L;}}
    public static void clear(){help=toast="";helpUntil=toastUntil=0;}
    public static String help(){return help;}
    public static List<FormattedCharSequence> lines(String text,int width){return Minecraft.getInstance().font.split(Component.literal(text),Math.max(1,Math.min(520,width-24)));}
    public static void render(GuiGraphicsExtractor gui){
        var c=Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null)return;long now=System.nanoTime();
        if(now<helpUntil&&!help.isEmpty())panel(gui,help,true);
        if(now<toastUntil&&!toast.isEmpty())panel(gui,toast,false);
    }
    private static void panel(GuiGraphicsExtractor gui,String text,boolean selection){
        var font=Minecraft.getInstance().font;var lines=lines(text,gui.guiWidth());int height=lines.size()*(font.lineHeight+1)+8;
        int width=Math.min(520,gui.guiWidth()-24),x=(gui.guiWidth()-width)/2;
        int y=selection?Math.min(34,Math.max(4,gui.guiHeight()-height-8)):Math.max(4,gui.guiHeight()-height-66);
        gui.fill(x-4,y-4,x+width+4,y+height-4,0xB0000000);
        for(int i=0;i<lines.size();i++)gui.text(font,lines.get(i),x,y+i*(font.lineHeight+1),0xFFFFFFFF,true);
    }
}
