package com.sibcraft.creeperborn.client;
import java.util.*;
import com.sibcraft.creeperborn.ZoanPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.client.CameraType;
public final class ZoanVisuals {
    private record View(int flags,long time){}
    private static final Map<Integer,View> views=new HashMap<>();
    private static CameraType previous;private static int cameraForm;
    public static int flags(int id){View v=views.get(id);return v==null||System.nanoTime()-v.time>3_000_000_000L?0:v.flags;}
    public static void update(int id,int flags){int old=flags(id);if(flags==0)views.remove(id);else views.put(id,new View(flags,System.nanoTime()));var c=Minecraft.getInstance();if(c.player!=null&&c.player.getId()==id)ZoanPowers.clientState(c.player.getUUID(),flags);if(c.level!=null&&c.level.getEntity(id) instanceof net.minecraft.world.entity.player.Player p){ZoanPowers.clientState(p.getUUID(),flags);if(old!=flags)p.refreshDimensions();}}
    public static CameraType cameraType(int flags){return (flags&15)==13?CameraType.FIRST_PERSON:ZoanRenderer.full(flags)?CameraType.THIRD_PERSON_BACK:null;}
    public static void tick(Minecraft c){if(c.player==null)return;int flags=flags(c.player.getId());ZoanPowers.clientState(c.player.getUUID(),flags);CameraType target=cameraType(flags);int form=target==CameraType.FIRST_PERSON?1:target==null?0:2;if(form!=cameraForm){if(cameraForm==0&&form!=0)previous=c.options.getCameraType();if(target!=null)c.options.setCameraType(target);else if(previous!=null){c.options.setCameraType(previous);previous=null;}cameraForm=form;}}
    public static void clear(){views.clear();ZoanPowers.clearClient();previous=null;cameraForm=0;}
}
