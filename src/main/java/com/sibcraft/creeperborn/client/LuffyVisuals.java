package com.sibcraft.creeperborn.client;
import java.util.*;
import com.sibcraft.creeperborn.LuffyPowers;
import net.minecraft.client.Minecraft;
public final class LuffyVisuals {
 private record View(int flags,long at){}
 private static final Map<Integer,View> views=new HashMap<>();
 public static int flags(int id){View v=views.get(id);return v==null||System.nanoTime()-v.at>3_000_000_000L?0:v.flags;}
 public static float spin(int id){View v=views.get(id);if(v==null||(v.flags&64)==0)return 0;return LuffyRenderer.spin(((v.flags>>8)&31)+(System.nanoTime()-v.at)/50_000_000f);}
 public static void update(int id,int flags){int old=flags(id);views.put(id,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.level!=null&&c.level.getEntity(id) instanceof net.minecraft.world.entity.player.Player p){LuffyPowers.clientState(p.getUUID(),flags);if((old&2)!=(flags&2))p.refreshDimensions();}}
 public static void clear(){views.clear();LuffyPowers.clearClient();}
}
