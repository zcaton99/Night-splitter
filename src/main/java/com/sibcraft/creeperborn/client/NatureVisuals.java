package com.sibcraft.creeperborn.client;
import java.util.*;
import com.sibcraft.creeperborn.NaturePowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
public final class NatureVisuals {
    private record View(int flags,long time){}
    private static final Map<Integer,View> views=new HashMap<>();private static CameraType previous;
    public static void update(int entity,int flags){if(flags==0)views.remove(entity);else views.put(entity,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==entity)NaturePowers.clientState(c.player.getUUID(),flags);}
    public static int flags(int entity){View v=views.get(entity);return v==null||System.nanoTime()-v.time>3_000_000_000L?0:v.flags;}
    public static void tick(Minecraft c){if(c.player==null)return;int f=flags(c.player.getId());NaturePowers.clientState(c.player.getUUID(),f);if((f&4)!=0){if(previous==null){previous=c.options.getCameraType();c.options.setCameraType(CameraType.THIRD_PERSON_BACK);}}else restore();}
    private static void restore(){if(previous!=null){var c=Minecraft.getInstance();if(c.options.getCameraType()==CameraType.THIRD_PERSON_BACK)c.options.setCameraType(previous);previous=null;}}
    public static void clear(){views.clear();NaturePowers.clearClient();restore();}
}
