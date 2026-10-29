package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** A short-lived fireball and flame tail; targeting and damage remain owned by Abilities. */
public final class FireAnimations {
    private record Effect(Player owner,ServerLevel level,Display.BlockDisplay display,long ends){}
    private static final Map<UUID,Effect> active=new HashMap<>();
    public static void fireball(Player p,Vec3 end){start(p,end,1,1);}
    public static void ring(Player p,double radius){start(p,null,2,(float)radius);}
    private static void start(Player p,Vec3 end,int kind,float radius){
        if(!(p.level() instanceof ServerLevel level))return;for(Effect e:List.copyOf(active.values()))if(e.owner==p&&e.display.getCustomName()!=null&&e.display.getCustomName().getString().startsWith(kind==1?"cb_fireball|":"cb_fire_ring|"))remove(e);
        Vec3 from=kind==1?LightningBolts.hand(p):p.position().add(0,.05,0),direction=kind==1?end.subtract(from):new Vec3(0,0,1);float length=kind==1?(float)Math.max(.25,Math.min(64,direction.length())):radius;
        var display=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(display==null)return;
        display.addTag("cb_temporary_rock");display.setCustomName(Component.literal((kind==1?"cb_fireball|":"cb_fire_ring|")+length));display.setCustomNameVisible(false);display.setBlockState(Blocks.STONE.defaultBlockState());
        var settings=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)display;settings.creeperborn$duration(0);settings.creeperborn$positionDuration(0);settings.creeperborn$width(128);settings.creeperborn$height(16);settings.creeperborn$view(4);
        display.snapTo(from.x,from.y,from.z);display.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),Paw.orientation(direction),new org.joml.Vector3f(1),new org.joml.Quaternionf()));
        if(level.addFreshEntity(display))active.put(display.getUUID(),new Effect(p,level,display,level.getGameTime()+(kind==1?12:28)));
    }
    private static boolean valid(Effect e){return e.owner.isAlive()&&!e.owner.isRemoved()&&!e.owner.isSpectator()&&e.owner.level()==e.level&&!WaterRules.suppressed(e.owner)&&!Dark.suppressed(e.owner);}
    private static void remove(Effect e){e.display.discard();active.remove(e.display.getUUID());}
    public static void tick(){for(Effect e:List.copyOf(active.values()))if(!valid(e)||e.display.isRemoved()||e.level.getGameTime()>=e.ends)remove(e);}
    public static void forget(Player p){for(Effect e:List.copyOf(active.values()))if(e.owner==p)remove(e);}
    public static void clear(){for(Effect e:List.copyOf(active.values()))remove(e);}
    public static int activeCount(){return active.size();}
    public static void tickVisual(Entity entity){if(!(entity.level() instanceof ServerLevel)||entity.getCustomName()==null||!(entity.getCustomName().getString().startsWith("cb_fireball|")||entity.getCustomName().getString().startsWith("cb_fire_ring|")))return;Effect e=active.get(entity.getUUID());if(e==null&&entity.tickCount>2||e!=null&&entity.level().getGameTime()>=e.ends){entity.discard();active.remove(entity.getUUID());}}
}
