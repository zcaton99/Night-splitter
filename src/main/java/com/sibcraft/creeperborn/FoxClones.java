package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/** A bounded squad of skin-matched chakra doubles. A second Z tap exchanges positions. */
public final class FoxClones {
    private record Squad(List<Display.BlockDisplay> bodies,long until){}
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
        if(bodies.isEmpty())return "More space is needed for shadow clones.";squads.put(p.getUUID(),new Squad(bodies,p.level().getGameTime()+400));return "Multi Shadow Clones! Tap Z again to swap.";
    }
    public static void tick(Player p){Squad s=squads.get(p.getUUID());if(s==null)return;long time=p.level().getGameTime();if(!p.isAlive()||!ZoanPowers.unlocked(p,"fox")||ZoanPowers.giantFox(p)||Dark.suppressed(p)||WaterRules.suppressed(p)||time>=s.until){clear(p);return;}
        var level=(ServerLevel)p.level();LivingEntity target=null;double best=-1;
        for(var e:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(24)))if(e!=p&&e.isAlive()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player)&&p.hasLineOfSight(e)){double score=e.getBoundingBox().getCenter().subtract(p.getEyePosition()).normalize().dot(p.getLookAngle());if(score>.65&&score>best){best=score;target=e;}}
        int i=0;for(var clone:s.bodies){if(clone.isRemoved())continue;double a=i++*Math.PI*2/s.bodies.size();Vec3 dest=target==null?p.position().add(Math.cos(a)*3,0,Math.sin(a)*3):target.position().add(Math.cos(a)*1.6,0,Math.sin(a)*1.6);Vec3 delta=dest.subtract(clone.position());if(delta.lengthSqr()>.04){Vec3 step=delta.normalize().scale(Math.min(.35,delta.length()));Vec3 at=clone.position().add(step);if(level.noCollision(p,EntityDimensions.scalable(.6f,1.8f).makeBoundingBox(at)))clone.snapTo(at.x,at.y,at.z);}
            if(target!=null&&time%20==0&&clone.position().distanceToSqr(target.position())<9&&p.hasLineOfSight(target)){target.damageCooldownTime=0;ZoanPowers.poweredHit(p,target,"fox",p.damageSources().playerAttack(p),ZoanPowers.tier(p,"fox")*2+2);}
        }
    }
    public static int count(Player p){Squad s=squads.get(p.getUUID());return s==null?0:s.bodies.size();}
    public static void clear(Player p){Squad s=squads.remove(p.getUUID());if(s!=null)for(var d:s.bodies)d.discard();}
}
