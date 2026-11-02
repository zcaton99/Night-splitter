package com.sibcraft.creeperborn.client;
import java.util.*;
import com.sibcraft.creeperborn.ZoanPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
public final class ZoanVisuals {
    private record View(int flags,long time){}
    private static final Map<Integer,View> views=new HashMap<>();
    private static CameraType previous;private static int cameraForm;private static long foxEntered=-1;private static boolean wasFox;
    public static int flags(int id){View v=views.get(id);return v==null||System.nanoTime()-v.time>3_000_000_000L?0:v.flags;}
    public static void update(int id,int flags){int old=flags(id);if(flags==0)views.remove(id);else views.put(id,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==id)ZoanPowers.clientState(c.player.getUUID(),flags);if(c.level!=null&&c.level.getEntity(id) instanceof net.minecraft.world.entity.player.Player p){ZoanPowers.clientState(p.getUUID(),flags);if(old!=flags)p.refreshDimensions();}}
    public static CameraType cameraType(int flags){return (flags&15)==13?CameraType.FIRST_PERSON:ZoanRenderer.full(flags)?CameraType.THIRD_PERSON_BACK:null;}
    public static final int FOX_REVEAL_HOLD=18,FOX_REVEAL_END=44;
    public static float revealBlend(float elapsed){float t=Math.max(0,Math.min(1,(elapsed-FOX_REVEAL_HOLD)/(FOX_REVEAL_END-FOX_REVEAL_HOLD)));return t*t*(3-2*t);}
    public static float foxRevealBlend(float partial){var c=Minecraft.getInstance();return foxEntered<0||c.level==null?1:revealBlend(c.level.getGameTime()-foxEntered+partial);}
    public static boolean revealingFox(){return wasFox&&foxRevealBlend(0)<1;}
    public static boolean hideLocalFoxBody(){return revealingFox()&&foxRevealBlend(0)>.82f;}
    public static void tick(Minecraft c){if(c.player==null)return;int flags=flags(c.player.getId());ZoanPowers.clientState(c.player.getUUID(),flags);boolean fox=(flags&15)==13;if(fox&&!wasFox)foxEntered=c.level.getGameTime();else if(!fox)foxEntered=-1;wasFox=fox;CameraType target=fox&&revealingFox()?CameraType.THIRD_PERSON_BACK:cameraType(flags);int form=target==CameraType.FIRST_PERSON?1:target==null?0:2;if(form!=cameraForm){if(cameraForm==0&&form!=0)previous=c.options.getCameraType();if(target!=null)c.options.setCameraType(target);else if(previous!=null){c.options.setCameraType(previous);previous=null;}cameraForm=form;}}
    public static void clear(){views.clear();ZoanPowers.clearClient();previous=null;cameraForm=0;foxEntered=-1;wasFox=false;}
}
