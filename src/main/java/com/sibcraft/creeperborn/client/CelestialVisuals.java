package com.sibcraft.creeperborn.client;
import java.util.*;
import com.sibcraft.creeperborn.CelestialPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
public final class CelestialVisuals {
 private record View(int flags,long time){}
 private static final Map<Integer,View> views=new HashMap<>();private static boolean giant;private static long began;private static CameraType previous;
 public static void update(int entity,int flags){if(flags==0)views.remove(entity);else views.put(entity,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==entity)CelestialPowers.clientState(c.player.getUUID(),flags);}
 public static int flags(int entity){var v=views.get(entity);return v==null||System.nanoTime()-v.time>3_000_000_000L?0:v.flags;}
 public static void tick(Minecraft c){if(c.player==null)return;int f=flags(c.player.getId());CelestialPowers.clientState(c.player.getUUID(),f);boolean now=(f&7)==6;if(now&&!giant){previous=c.options.getCameraType();began=System.nanoTime();c.options.setCameraType(CameraType.THIRD_PERSON_BACK);}else if(now&&previous!=null&&System.nanoTime()-began>2_000_000_000L){if(c.options.getCameraType()==CameraType.THIRD_PERSON_BACK)c.options.setCameraType(CameraType.FIRST_PERSON);previous=null;}else if(!now&&previous!=null){c.options.setCameraType(previous);previous=null;}giant=now;}
 public static double zoom(){return giant?NatureVisuals.revealZoom((System.nanoTime()-began)/1_000_000_000.0):1;}
 public static void clear(){views.clear();CelestialPowers.clearClient();giant=false;previous=null;}
}
