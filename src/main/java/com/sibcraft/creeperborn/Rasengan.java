package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** One attached charge and one bounded, swept palm strike per owner. No terrain damage. */
public final class Rasengan {
    private record Charge(Display.BlockDisplay display,int tier,int mask){}
    private record Strike(Player owner,ServerLevel level,Display.BlockDisplay display,Vec3 from,Vec3 end,long start,int tier,int mask,float damage,double radius){}
    private static final Map<UUID,Charge> charges=new HashMap<>();
    private static final Map<UUID,Strike> strikes=new HashMap<>();
    public static int elements(Player p){int mask=0;for(Element e:Element.values())if(Mastery.tier(p,e)>=3)mask|=1<<e.ordinal();return mask;}
    public static int kind(int tier,int mask){return tier==1?44:tier==2?45:46+(mask&15);}
    public static int tier(int kind){return kind==44?1:kind==45?2:3;}
    public static float chargeFraction(int tier,long ticks){return Math.max(0,Math.min(1,ticks/(tier==1?20f:tier==2?40f:60f)));}
    public static float size(int tier,float charge){return (tier==1?.55f:tier==2?2.4f:3.6f)*(.2f+.8f*charge);}
    public static float damage(int tier,int mask,float charge){float base=tier==1?22:tier==2?44:70;return base*(.55f+.45f*charge)*(tier==3?(1+.12f*Integer.bitCount(mask&15))*(mask==15?1.6f:1):1);}
    public static double radius(int tier,int mask){return tier==1?1:tier==2?3:6+(mask==15?2:0);}
    public static Vec3 hand(Player p,float partial){Vec3 look=p.getViewVector(partial);double yaw=Math.toRadians(p.getViewYRot(partial));double side=p.getMainArm()==HumanoidArm.LEFT?-1:1;return p.getEyePosition(partial).add(look.scale(.8)).add(Math.cos(yaw)*.38*side,-.28,-Math.sin(yaw)*.38*side);}
    public static Vec3 anchor(Player p,int tier,float charge,float partial){if(tier==1)return hand(p,partial);double yaw=Math.toRadians(p.getViewYRot(partial)),side=p.getMainArm()==HumanoidArm.LEFT?-1:1;return p.getPosition(partial).add(Math.cos(yaw)*.4*side,2.65+size(tier,charge)*.5,-Math.sin(yaw)*.4*side);}
    public static boolean charging(Player p){return charges.containsKey(p.getUUID());}
    public static int chargeCount(){return charges.size();}
    public static int strikeCount(){return strikes.size();}
    public static void charge(Player p,int tier,long ticks){
        int mask=tier==3?elements(p):0;Charge old=charges.get(p.getUUID());if(old!=null&&(old.display.isRemoved()||old.tier!=tier||old.mask!=mask)){cancel(p);old=null;}
        float fraction=chargeFraction(tier,ticks);Vec3 at=anchor(p,tier,fraction,1);
        if(old==null){var d=ZoanPowers.effect(p,at,kind(tier,mask),1200,size(tier,fraction),0);if(d==null)return;d.setCustomName(Component.literal("cb_celestial_"+kind(tier,mask)+"|"+p.getUUID()+"|charge"));old=new Charge(d,tier,mask);charges.put(p.getUUID(),old);}
        old.display.snapTo(at.x,at.y,at.z);transform(old.display,size(tier,fraction));
    }
    private static void transform(Display.BlockDisplay d,float size){d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(size),new org.joml.Quaternionf()));}
    public static void cancel(Player p){Charge c=charges.remove(p.getUUID());if(c!=null)c.display.discard();}
    public static String release(Player p,int tier,long held){
        cancel(p);Strike previous=strikes.remove(p.getUUID());if(previous!=null)previous.display.discard();
        int mask=tier==3?elements(p):0;float fraction=chargeFraction(tier,held);double range=tier==1?6:tier==2?10:16;
        Vec3 eye=p.getEyePosition(),end=eye.add(p.getLookAngle().scale(range));var level=(ServerLevel)p.level();
        var wall=level.clip(new ClipContext(eye,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation().subtract(p.getLookAngle().scale(.15));
        double nearest=eye.distanceToSqr(end);for(var e:level.getEntitiesOfClass(LivingEntity.class,new AABB(eye,end).inflate(.6)))if(enemy(p,e)){var cross=e.getBoundingBox().inflate(.35).clip(eye,end);if(cross.isPresent()&&eye.distanceToSqr(cross.get())<nearest){end=cross.get();nearest=eye.distanceToSqr(end);}}
        Vec3 from=anchor(p,tier,fraction,1);var d=ZoanPowers.effect(p,from,kind(tier,mask),16,size(tier,fraction),0);if(d!=null)strikes.put(p.getUUID(),new Strike(p,level,d,from,end,level.getGameTime(),tier,mask,damage(tier,mask,fraction),radius(tier,mask)));
        return tier==1?"Rasengan!":tier==2?"Giant Rasengan!":mask==15?"Harmony Planetary Rasengan!":"Planetary Rasengan!";
    }
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player)&&!NaturePowers.proxy(e)&&!Azure.ownedProxy(e,p)&&!CelestialPowers.ownedProxy(e,p);}
    private static boolean valid(Strike s){return s.owner.isAlive()&&!s.owner.isRemoved()&&s.owner.level()==s.level&&ZoanPowers.unlocked(s.owner,"fox")&&!Dark.suppressed(s.owner)&&!WaterRules.suppressed(s.owner);}
    public static void tick(){for(Strike s:List.copyOf(strikes.values())){if(!valid(s)){forget(s.owner);continue;}float t=Math.min(1,(s.level.getGameTime()-s.start)/8f);Vec3 at=s.from.lerp(s.end,t*t*(3-2*t));s.display.snapTo(at.x,at.y,at.z);if(t<1)continue;
        for(var e:s.level.getEntitiesOfClass(LivingEntity.class,new AABB(s.end,s.end).inflate(s.radius+1)))if(enemy(s.owner,e)&&e.getBoundingBox().getCenter().distanceTo(s.end)<=s.radius+e.getBbWidth()*.5&&s.owner.hasLineOfSight(e)&&s.level.clip(new ClipContext(s.end,e.getBoundingBox().getCenter(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.owner)).getType()==HitResult.Type.MISS){
            if(ZoanPowers.poweredHit(s.owner,e,"fox",s.owner.damageSources().playerAttack(s.owner),s.damage)){Vec3 push=e.position().subtract(s.owner.position()).multiply(1,0,1).normalize().scale(s.tier==3?2.4:1.5).add(0,.45,0);e.setDeltaMovement(e.getDeltaMovement().add(push));e.hurtMarked=true;}
        }
        ZoanPowers.effect(s.owner,s.end,kind(s.tier,s.mask),16,size(s.tier,1)*1.15f,0);s.display.discard();strikes.remove(s.owner.getUUID());
    }}
    public static void forget(Player p){cancel(p);Strike s=strikes.remove(p.getUUID());if(s!=null)s.display.discard();}
    public static void clear(){for(var c:charges.values())c.display.discard();for(var s:strikes.values())s.display.discard();charges.clear();strikes.clear();}
}
