package com.sibcraft.creeperborn;

import java.util.*;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

public final class Punches {
    public record Visual(UUID owner,boolean left,boolean fist){}
    private static final class Punch {
        Player owner; ServerLevel level; Vec3 aim,tip; boolean left,charged,gear,struck;float damage;int age;
        Display.BlockDisplay arm,fist;
    }
    private static final List<Punch> active=new ArrayList<>();
    private static final Map<UUID,Boolean> lastHand=new HashMap<>();
    private Punches(){}
    public static Visual visual(String name){
        if(!name.startsWith("cb_punch|"))return null;
        String[] parts=name.split("\\|");
        if(parts.length!=4||!Set.of("L","R").contains(parts[2])||!Set.of("A","F").contains(parts[3]))return null;
        try{return new Visual(UUID.fromString(parts[1]),parts[2].equals("L"),parts[3].equals("F"));}catch(IllegalArgumentException ignored){return null;}
    }
    public static boolean lastLeft(Player p){return lastHand.getOrDefault(p.getUUID(),false);}
    public static int activeCount(){return active.size();}
    public static void launch(Player owner,double range,float damage,boolean charged,boolean gear,boolean left){
        if(!(owner.level() instanceof ServerLevel level))return;
        Punch p=new Punch();p.owner=owner;p.level=level;p.damage=damage;p.charged=charged;p.gear=gear;p.left=left;
        Vec3 eye=owner.getEyePosition();p.aim=eye.add(owner.getLookAngle().scale(range));
        var wall=level.clip(new ClipContext(eye,p.aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
        if(wall.getType()!=HitResult.Type.MISS)p.aim=wall.getLocation();
        p.tip=origin(p);p.arm=display(p,false);p.fist=display(p,true);
        lastHand.put(owner.getUUID(),left);active.add(p);advance(p);
    }
    private static Display.BlockDisplay display(Punch p,boolean fist){
        Display.BlockDisplay d=EntityTypes.BLOCK_DISPLAY.create(p.level,EntitySpawnReason.EVENT);
        if(d==null)throw new IllegalStateException("Punch display unavailable");
        d.addTag("cb_temporary_rock");
        d.setCustomName(Component.literal("cb_punch|"+p.owner.getUUID()+"|"+(p.left?"L":"R")+"|"+(fist?"F":"A")));
        d.setCustomNameVisible(false);d.setBlockState(Blocks.WHITE_TERRACOTTA.defaultBlockState());
        d.setTransformationInterpolationDuration(1);d.setPosRotInterpolationDuration(1);d.setViewRange(1);d.setWidth(48);d.setHeight(48);
        Vec3 from=origin(p);d.snapTo(from.x,from.y,from.z);p.level.addFreshEntity(d);return d;
    }
    private static Vec3 origin(Punch p){
        Vec3 look=p.owner.getLookAngle();Vec3 right=new Vec3(-look.z,0,look.x).normalize();
        return p.owner.getEyePosition().add(0,-.32,0).add(right.scale(p.left?-.28:.28)).add(look.scale(.15));
    }
    private static void transform(Display.BlockDisplay d,Vec3 at,Vec3 direction,float width,float length){
        d.snapTo(at.x,at.y,at.z);
        Quaternionf rotation=new Quaternionf().rotationTo(0,1,0,(float)direction.x,(float)direction.y,(float)direction.z);
        d.setTransformationInterpolationDelay(0);
        d.setTransformation(new Transformation(new Vector3f(),rotation,new Vector3f(width,length,width),new Quaternionf()));
    }
    private static void advance(Punch p){
        p.age++;Vec3 from=origin(p);double progress=p.age<=2?p.age/2.0:Math.max(0,(6-p.age)/4.0);
        Vec3 next=from.lerp(p.aim,progress);
        if(!p.struck&&p.age<=2){
            var wall=p.level.clip(new ClipContext(p.tip,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p.owner));
            Vec3 end=wall.getType()==HitResult.Type.MISS?next:wall.getLocation();
            LivingEntity victim=null;Vec3 hit=null;double nearest=p.tip.distanceTo(end);double radius=p.charged?.22:.12;
            for(LivingEntity target:p.level.getEntitiesOfClass(LivingEntity.class,new AABB(p.tip,end).inflate(radius+.5))){
                if(!(target instanceof Enemy)||!target.isAlive()||!p.owner.hasLineOfSight(target))continue;
                var intersection=target.getBoundingBox().inflate(radius).clip(p.tip,end);
                if(intersection.isPresent()&&p.tip.distanceTo(intersection.get())<=nearest){
                    victim=target;hit=intersection.get();nearest=p.tip.distanceTo(hit);
                }
            }
            if(victim!=null){
                p.struck=true;p.aim=hit;next=hit;
                if(p.gear)victim.damageCooldownTime=0;
                if(victim.hurtServer(p.level,p.owner.damageSources().playerAttack(p.owner),p.damage))Rubber.credit(p.owner);
                Vec3 push=victim.position().subtract(p.owner.position()).normalize().scale(p.charged?1:.4);
                if(victim.isAlive())victim.setDeltaMovement(victim.getDeltaMovement().add(push.x,.15,push.z));
                Abilities.particles(p.level,Element.AIR,hit,8);
            }else if(wall.getType()!=HitResult.Type.MISS){p.struck=true;p.aim=end;next=end;}
        }
        p.tip=next;Vec3 delta=next.subtract(from);double length=Math.max(.01,delta.length());
        Vec3 direction=delta.lengthSqr()<.0001?p.owner.getLookAngle():delta.normalize();
        transform(p.arm,from,direction,p.charged?1.45f:1,(float)length);
        transform(p.fist,next,direction,p.charged?1.8f:1.2f,p.charged?1.8f:1.2f);
    }
    private static void remove(Punch p){p.arm.discard();p.fist.discard();}
    public static void tick(){
        Iterator<Punch> it=active.iterator();
        while(it.hasNext()){
            Punch p=it.next();
            if(!p.owner.isAlive()||p.owner.isRemoved()||p.owner.isSpectator()||p.owner.level()!=p.level||WaterRules.suppressed(p.owner)||p.age>=6){
                remove(p);it.remove();continue;
            }
            advance(p);
        }
    }
    public static void cancel(Player owner){active.removeIf(p->{if(p.owner.getUUID().equals(owner.getUUID())){remove(p);return true;}return false;});lastHand.remove(owner.getUUID());}
    public static void clear(){active.forEach(Punches::remove);active.clear();lastHand.clear();}
}
