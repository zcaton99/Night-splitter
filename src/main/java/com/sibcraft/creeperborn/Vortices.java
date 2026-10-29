package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;

public final class Vortices {
    private static final class Swirl {
        Player owner; ServerLevel level; Vec3 center; double radius,power; boolean mastered; int ticks;
        Set<UUID> hit=new HashSet<>();
    }
    private final List<Swirl> active=new ArrayList<>();
    public int activeCount(){return active.size();}
    public void clear(){active.clear();}
    public void start(Player owner,double power){
        if(!(owner.level() instanceof ServerLevel level))return;
        active.removeIf(s->s.owner.getUUID().equals(owner.getUUID()));
        Swirl s=new Swirl();s.owner=owner;s.level=level;s.center=owner.position();
        s.mastered=Mastery.harmony(owner);s.radius=s.mastered?12:10;s.power=power;active.add(s);
    }
    public void tick(){
        Iterator<Swirl> it=active.iterator();
        while(it.hasNext()){
            Swirl s=it.next();
            if(!s.owner.isAlive()||s.owner.isRemoved()||s.owner.isSpectator()||s.owner.level()!=s.level||WaterRules.suppressed(s.owner)){
                it.remove();continue;
            }
            boolean finish=++s.ticks>=100;
            for(LivingEntity target:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(s.center,s.center).inflate(s.radius))){
                if(Azure.ownedProxy(target,s.owner)||!(target instanceof Enemy)||!target.isAlive()||target.position().distanceToSqr(s.center)>s.radius*s.radius)continue;
                Vec3 sight=s.center.add(0,1,0),end=target.position().add(0,target.getBbHeight()/2,0);
                if(s.level.clip(new ClipContext(sight,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.owner)).getType()!=HitResult.Type.MISS)continue;
                Vec3 delta=target.position().subtract(s.center);
                double distance=Math.sqrt(delta.x*delta.x+delta.z*delta.z);
                Vec3 outward=distance<.01?new Vec3(1,0,0):new Vec3(delta.x/distance,0,delta.z/distance);
                if(s.hit.add(target.getUUID()))Abilities.strike(s.level,s.owner,target,Element.AIR,(float)(2*s.power),0);
                if(!target.isAlive())continue;
                if(finish){
                    double force=(s.mastered?1.5:1.1)*s.power;
                    target.setDeltaMovement(outward.x*force,(s.mastered?1.1:.8)*s.power,outward.z*force);
                }else{
                    double inward=Math.min(.55,distance*.12)*s.power;
                    double tangent=.28*s.power;
                    target.setDeltaMovement(-outward.x*inward-outward.z*tangent,
                        Math.max(-.15,Math.min(.12,target.getDeltaMovement().y)),
                        -outward.z*inward+outward.x*tangent);
                }
            }
            if(s.ticks%5==0||finish){
                for(int i=0;i<24;i++){
                    double angle=i*Math.PI/12+s.ticks*.2;
                    double radius=finish?s.radius: s.radius*(.6+.25*Math.sin(s.ticks*.1));
                    Abilities.particles(s.level,Element.AIR,s.center.add(Math.cos(angle)*radius,.5+(i%4)*.55,Math.sin(angle)*radius),1);
                }
            }
            if(finish)it.remove();
        }
    }
}
