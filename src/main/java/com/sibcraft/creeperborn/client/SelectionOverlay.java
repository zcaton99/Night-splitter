package com.sibcraft.creeperborn.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

/** Brief, background-free help anchored above the lower-left HUD. Reflows with GUI scale. */
public final class SelectionOverlay {
    private static final long HELP_NANOS=3_000_000_000L,TOAST_NANOS=2_000_000_000L,FADE_NANOS=400_000_000L;
    private static String help="",toast="";private static long helpUntil,toastUntil;
    private SelectionOverlay(){}
    public static void show(String text,boolean selection){long now=System.nanoTime();if(selection){help=text;helpUntil=now+HELP_NANOS;toast="";}else{toast=text;toastUntil=now+TOAST_NANOS;}}
    public static void clear(){help=toast="";helpUntil=toastUntil=0;}
    public static String help(){return help;}
    public static List<FormattedCharSequence> lines(String text,int width){return Minecraft.getInstance().font.split(Component.literal(text),Math.max(1,Math.min(360,width-24)));}
    public static void render(GuiGraphicsExtractor gui){
        var c=Minecraft.getInstance();if(c.player==null||c.gui.screen()!=null)return;long now=System.nanoTime();
        int bottom=gui.guiHeight()-58;
        if(now<helpUntil&&!help.isEmpty())bottom=panel(gui,help,bottom,helpUntil-now)-8;
        if(now<toastUntil&&!toast.isEmpty())panel(gui,toast,bottom,toastUntil-now);
    }
    private static int panel(GuiGraphicsExtractor gui,String text,int bottom,long remaining){
        var font=Minecraft.getInstance().font;var lines=lines(text,gui.guiWidth());int height=lines.size()*(font.lineHeight+1);
        int x=8,y=Math.max(8,bottom-height);
        int alpha=(int)(255*Math.min(1,remaining/(double)FADE_NANOS));
        // Minecraft treats almost-zero alpha as opaque; skip it instead of flashing at expiry.
        if(alpha>8)for(int i=0;i<lines.size();i++)gui.text(font,lines.get(i),x,y+i*(font.lineHeight+1),(alpha<<24)|0xFFFFFF,true);
        return y;
    }
}
