package com.sibcraft.creeperborn.client;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
import com.sibcraft.creeperborn.Azure;
/** Observer appearance is independent of selected attacks. Camera reveal uses render-time interpolation. */
public final class AzureVisuals {
    private record View(int flags,long received){}
    private static final Map<Integer,View> views=new HashMap<>();private static CameraType previous;private static long entered;private static net.minecraft.world.phys.Vec3 cameraPosition;
    public static void update(int id,int flags){if(flags==0)views.remove(id);else views.put(id,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==id)Azure.clientState(c.player.getUUID(),flags);}
    public static int flags(int id){View v=views.get(id);return v==null||System.nanoTime()-v.received()>3_000_000_000L?0:v.flags();}
    public static void tick(Minecraft c){if(c.player==null)return;int flags=flags(c.player.getId());Azure.clientState(c.player.getUUID(),flags);if((flags&3)==2){if(entered==0)entered=System.nanoTime();if(previous==null){previous=c.options.getCameraType();c.options.setCameraType(CameraType.THIRD_PERSON_BACK);}}else{entered=0;restore();}}
    public static void cameraPosition(net.minecraft.world.phys.Vec3 position){cameraPosition=position;}
    public static net.minecraft.world.phys.Vec3 cameraPosition(){return cameraPosition;}
    public static float revealDistance(double seconds){double t=Math.max(0,Math.min(1,(seconds-.35)/1.65));t=t*t*(3-2*t);return (float)(30+(4.5-30)*t);}
    public static float cameraDistance(){return revealDistance(entered==0?0:(System.nanoTime()-entered)/1_000_000_000.0);}
    public static float cameraLift(){double t=Math.max(0,Math.min(1,(entered==0?0:(System.nanoTime()-entered)/1_000_000_000.0)/2));return (float)(6-t);}
    private static void restore(){if(previous!=null){var c=Minecraft.getInstance();if(c.options.getCameraType()==CameraType.THIRD_PERSON_BACK)c.options.setCameraType(previous);previous=null;}}
    public static void clear(){views.clear();Azure.clearClient();entered=0;cameraPosition=null;restore();}
}
