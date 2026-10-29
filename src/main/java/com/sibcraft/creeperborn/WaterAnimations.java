package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** One temporary display per cast; the tidal impact happens at the outward sweep. */
public final class WaterAnimations {
    public static final int GUARD_WINDUP=6;
    private static final class Effect {Player owner;ServerLevel level;Display.BlockDisplay display;Vec3 end;int kind;long starts,ends;Runnable impact;}
    private static final Map<UUID,Effect> active=new HashMap<>();
    private static boolean valid(Effect e){return e.owner.isAlive()&&!e.owner.isRemoved()&&!e.owner.isSpectator()&&e.owner.level()==e.level&&!WaterRules.suppressed(e.owner)&&!Dark.suppressed(e.owner);}
    public static void whip(Player p,Vec3 end){start(p,1,end,null);}
    public static void guard(Player p,Runnable impact){start(p,2,null,impact);}
    private static void start(Player p,int kind,Vec3 end,Runnable impact){
        if(!(p.level() instanceof ServerLevel level))return;
        // A recast replaces only its own visual, never another player's cast.
        for(Effect old:List.copyOf(active.values()))if(old.owner==p&&old.kind==kind)remove(old);
        var display=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(display==null)return;
        Effect e=new Effect();e.owner=p;e.level=level;e.display=display;e.kind=kind;e.end=end;e.starts=level.getGameTime();e.ends=e.starts+(kind==1?20:34);e.impact=impact;
        display.addTag("cb_temporary_rock");display.setCustomNameVisible(false);display.setBlockState(Blocks.STONE.defaultBlockState());
        var settings=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)display;settings.creeperborn$duration(1);settings.creeperborn$positionDuration(1);settings.creeperborn$width(128);settings.creeperborn$height(16);settings.creeperborn$view(4);
        position(e);if(level.addFreshEntity(display))active.put(display.getUUID(),e);
    }
    private static void position(Effect e){
        Vec3 from=e.kind==1?LightningBolts.hand(e.owner):e.owner.position();Vec3 direction=e.kind==1?e.end.subtract(from):new Vec3(0,0,1);float length=e.kind==1?(float)Math.max(.25,Math.min(64,direction.length())):1;
        e.display.setCustomName(Component.literal("cb_water_"+e.kind+"|"+e.owner.getUUID()+"|"+length));e.display.snapTo(from.x,from.y,from.z);
        e.display.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),Paw.orientation(direction.normalize()),new org.joml.Vector3f(1),new org.joml.Quaternionf()));
    }
    public static void tick(){for(Effect e:List.copyOf(active.values())){
        long now=e.level.getGameTime();if(!valid(e)||e.display.isRemoved()||now>=e.ends){remove(e);continue;}
        position(e);if(e.impact!=null&&now-e.starts>=GUARD_WINDUP){Runnable impact=e.impact;e.impact=null;impact.run();}
    }}
    private static void remove(Effect e){e.display.discard();active.remove(e.display.getUUID());}
    public static void forget(Player p){for(Effect e:List.copyOf(active.values()))if(e.owner==p)remove(e);}
    public static void clear(){for(Effect e:List.copyOf(active.values()))remove(e);}
    public static int activeCount(){return active.size();}
    public static void tickVisual(Entity entity){if(!(entity.level() instanceof ServerLevel)||entity.getCustomName()==null||!entity.getCustomName().getString().startsWith("cb_water_"))return;Effect e=active.get(entity.getUUID());if(e==null&&entity.tickCount>2||e!=null&&entity.level().getGameTime()>=e.ends){entity.discard();active.remove(entity.getUUID());}}
}
