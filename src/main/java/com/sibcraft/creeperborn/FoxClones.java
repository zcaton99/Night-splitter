package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** A bounded squad with independent targets, melee fighters and ranged mini Beast Bombs. */
public final class FoxClones {
    private static final class Squad {
        final List<Display.BlockDisplay> bodies;final long until;
        final Map<UUID,UUID> targets=new HashMap<>();final Map<UUID,Long> meleeReady=new HashMap<>(),bombReady=new HashMap<>();
        Squad(List<Display.BlockDisplay> bodies,long until,long now){this.bodies=bodies;this.until=until;for(int i=0;i<bodies.size();i++)bombReady.put(bodies.get(i).getUUID(),now+20+i*8);}
    }
    private static final Map<UUID,Squad> squads=new HashMap<>();
    public static String cast(Player p){
        Squad squad=squads.get(p.getUUID());
        if(squad!=null&&p.level().getGameTime()<squad.until){
            for(var clone:squad.bodies){if(clone.isRemoved())continue;Vec3 at=clone.position(),old=p.position();if(!DoorSpace.fits((ServerLevel)p.level(),p,at)||StringFruit.blocked(p,at))continue;
                if(p instanceof net.minecraft.server.level.ServerPlayer sp){if(!sp.teleportTo((ServerLevel)p.level(),at.x,at.y,at.z,Set.of(),p.getYRot(),p.getXRot(),false))continue;}else p.snapTo(at.x,at.y,at.z);
                clone.snapTo(old.x,old.y,old.z);Collections.rotate(squad.bodies,-1);p.resetFallDistance();return "Shadow Swap!";
            }return "No safe clone position.";
        }
        clear(p);int count=ZoanPowers.tier(p,"fox")>=3?6:ZoanPowers.tier(p,"fox")==2?4:2;var bodies=new ArrayList<Display.BlockDisplay>();
        for(int i=0;i<count;i++){double a=i*Math.PI*2/count;Vec3 at=p.position().add(Math.cos(a)*3,0,Math.sin(a)*3);if(!DoorSpace.fits((ServerLevel)p.level(),p,at))continue;var d=TimePowers.visual(p,at,5,400,Vec3.ZERO);if(d!=null)bodies.add(d);}
        if(bodies.isEmpty())return "More space is needed for shadow clones.";long time=p.level().getGameTime();squads.put(p.getUUID(),new Squad(bodies,time+400,time));return "Multi Shadow Clones! Tap Z again to swap.";
    }
    private static boolean enemy(Player p,LivingEntity e){if(e==p||!e.isAlive()||e.isSpectator()||p.isAlliedTo(e)||e.level()!=p.level()||p.distanceToSqr(e)>576||NaturePowers.proxy(e)||Azure.ownedProxy(e,p)||CelestialPowers.ownedProxy(e,p))return false;if(e instanceof Enemy)return true;return e instanceof Player&&p.hasLineOfSight(e)&&e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(p.getLookAngle())>.65;}
    private static boolean sees(ServerLevel level,Player p,Display.BlockDisplay clone,LivingEntity target){return level.clip(new ClipContext(clone.position().add(0,1.4,0),target.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;}
    public static void tick(Player p){Squad s=squads.get(p.getUUID());if(s==null)return;long time=p.level().getGameTime();if(!p.isAlive()||p.isRemoved()||!ZoanPowers.unlocked(p,"fox")||ZoanPowers.giantFox(p)||Dark.suppressed(p)||WaterRules.suppressed(p)||time>=s.until||s.bodies.stream().allMatch(Entity::isRemoved)){clear(p);return;}
        var level=(ServerLevel)p.level();var candidates=level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(24)).stream().filter(e->enemy(p,e)).toList();Set<UUID> assigned=new HashSet<>();
        int i=0;for(var clone:s.bodies){if(clone.isRemoved())continue;int slot=i++;UUID id=clone.getUUID();LivingEntity target=null;UUID previous=s.targets.get(id);
            for(var e:candidates)if(e.getUUID().equals(previous)&&!assigned.contains(previous)&&sees(level,p,clone,e)){target=e;break;}
            if(target==null)target=candidates.stream().filter(e->!assigned.contains(e.getUUID())&&sees(level,p,clone,e)).min(Comparator.comparingDouble(e->e.distanceToSqr(clone))).orElse(null);
            if(target==null)target=candidates.stream().filter(e->sees(level,p,clone,e)).min(Comparator.comparingDouble(e->e.distanceToSqr(clone))).orElse(null);
            if(target!=null){assigned.add(target.getUUID());s.targets.put(id,target.getUUID());}else s.targets.remove(id);
            double a=slot*Math.PI*2/s.bodies.size();boolean ranged=slot%2==0;double distance=ranged?7:1.6;Vec3 dest=target==null?p.position().add(Math.cos(a)*3,0,Math.sin(a)*3):target.position().add(Math.cos(a)*distance,0,Math.sin(a)*distance);Vec3 delta=dest.subtract(clone.position());
            if(delta.lengthSqr()>.04){Vec3 at=clone.position().add(delta.normalize().scale(Math.min(.35,delta.length())));if(level.hasChunkAt(net.minecraft.core.BlockPos.containing(at))&&level.getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(at))&&level.noCollision(p,EntityDimensions.scalable(.6f,1.8f).makeBoundingBox(at)))clone.snapTo(at.x,at.y,at.z);}
            if(target==null)continue;Vec3 facing=target.position().subtract(clone.position());float yaw=(float)Math.toDegrees(Math.atan2(-facing.x,facing.z));clone.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf().rotationY((float)Math.toRadians(180-yaw)),new org.joml.Vector3f(1),new org.joml.Quaternionf()));
            if(time>=s.meleeReady.getOrDefault(id,0L)&&clone.distanceToSqr(target)<9&&sees(level,p,clone,target)){target.damageCooldownTime=0;ZoanPowers.poweredHit(p,target,"fox",p.damageSources().playerAttack(p),ZoanPowers.tier(p,"fox")*2+2);s.meleeReady.put(id,time+20);}
            if(time>=s.bombReady.getOrDefault(id,0L)&&clone.distanceToSqr(target)>=16&&ZoanPowers.cloneBomb(p,clone,target))s.bombReady.put(id,time+(ZoanPowers.tier(p,"fox")>=3?60:ZoanPowers.tier(p,"fox")==2?80:100));
        }
    }
    public static int targetCount(Player p){Squad s=squads.get(p.getUUID());return s==null?0:new HashSet<>(s.targets.values()).size();}
    public static int count(Player p){Squad s=squads.get(p.getUUID());return s==null?0:(int)s.bodies.stream().filter(e->!e.isRemoved()).count();}
    static boolean owns(Player p,UUID id){Squad s=squads.get(p.getUUID());return s!=null&&p.level().getGameTime()<s.until&&s.bodies.stream().anyMatch(e->!e.isRemoved()&&e.level()==p.level()&&e.getUUID().equals(id));}
    public static void clear(Player p){Squad s=squads.remove(p.getUUID());if(s!=null)for(var d:s.bodies)d.discard();ZoanPowers.clearCloneShots(p);}
}
