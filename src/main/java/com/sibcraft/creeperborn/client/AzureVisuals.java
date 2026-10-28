package com.sibcraft.creeperborn.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import com.sibcraft.creeperborn.Azure;
/** Ephemeral observer state; restore the player's chosen camera when transformation ends. */
public final class AzureVisuals {
    private record View(int flags,long received){}
    private static final Map<Integer,View> views=new HashMap<>();private static CameraType previous;
    public static void update(int id,int flags){if(flags==0)views.remove(id);else views.put(id,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==id)Azure.clientState(c.player.getUUID(),flags);}
    public static int flags(int id){View v=views.get(id);return v==null||System.nanoTime()-v.received()>3_000_000_000L?0:v.flags();}
    public static void tick(Minecraft c){if(c.player==null)return;int flags=flags(c.player.getId());Azure.clientState(c.player.getUUID(),flags);if((flags&3)==2){if(previous==null){previous=c.options.getCameraType();c.options.setCameraType(CameraType.THIRD_PERSON_BACK);}}else restore();}
    private static void restore(){if(previous!=null){var c=Minecraft.getInstance();if(c.options.getCameraType()==CameraType.THIRD_PERSON_BACK)c.options.setCameraType(previous);previous=null;}}
    public static void clear(){views.clear();Azure.clearClient();restore();}
}
