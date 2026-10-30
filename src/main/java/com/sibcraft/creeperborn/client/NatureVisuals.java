package com.sibcraft.creeperborn.client;
import java.util.*;
import com.sibcraft.creeperborn.NaturePowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
public final class NatureVisuals {
    private record View(int flags,long time){}
    private static final Map<Integer,View> views=new HashMap<>();private static CameraType previous;private static long giantSince;private static boolean wasGiant,aiming;
    public static void update(int entity,int flags){if(flags==0)views.remove(entity);else views.put(entity,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==entity)NaturePowers.clientState(c.player.getUUID(),flags);}
    public static int flags(int entity){View v=views.get(entity);return v==null||System.nanoTime()-v.time>3_000_000_000L?0:v.flags;}
    public static double revealZoom(double seconds){double t=Math.max(0,Math.min(1,(seconds-.7)/1.3));return 1-t*t*(3-2*t);}
    public static double revealZoom(){return wasGiant&&!aiming?revealZoom((System.nanoTime()-giantSince)/1_000_000_000.0):1;}
    public static void tick(Minecraft c){if(c.player==null)return;int f=flags(c.player.getId());NaturePowers.clientState(c.player.getUUID(),f);boolean giant=(f&4)!=0;if(giant&&!wasGiant){aiming=false;previous=c.options.getCameraType();giantSince=System.nanoTime();c.options.setCameraType(CameraType.THIRD_PERSON_BACK);}else if(giant&&System.nanoTime()-giantSince>=2_000_000_000L&&previous!=null&&!aiming){aiming=true;if(c.options.getCameraType()==CameraType.THIRD_PERSON_BACK)c.options.setCameraType(CameraType.FIRST_PERSON);}else if(!giant)restore();wasGiant=giant;}
    private static void restore(){if(previous!=null){var c=Minecraft.getInstance();if(c.options.getCameraType()==CameraType.THIRD_PERSON_BACK)c.options.setCameraType(previous);previous=null;}}
    public static void clear(){views.clear();NaturePowers.clearClient();restore();wasGiant=false;giantSince=0;}
}
