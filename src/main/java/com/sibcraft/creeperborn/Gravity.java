package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Native motion and temporary displays; no terrain edits or changes to vanilla flight permissions. */
public final class Gravity{
    private static final class State{
        Player owner;ServerLevel level;int held=-1;long press,heartbeat,recent=-1000,guard,rideEnds,landing;
        boolean riding;Vec3 last;Display.BlockDisplay platform;final List<Display.BlockDisplay> rocks=new ArrayList<>();
    }
    private record Pin(Player owner,LivingEntity target,ServerLevel level,long ends){}
    private record Blast(Player owner,ServerLevel level,Vec3 center,long begins,int radius){}
    private static final Map<UUID,Pin> pins=new HashMap<>();
    private static final List<Blast> blasts=new ArrayList<>();
    private record Lift(Player owner,LivingEntity target,ServerLevel level,double height,long ends){}
    private record Meteor(Player owner,ServerLevel level,Vec3 start,Vec3 end,long begins,long lands,Display.BlockDisplay visual,int radius,float damage){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Lift> lifts=new HashMap<>();
    private static final List<Meteor> meteors=new ArrayList<>();
    private static final DustParticleOptions PURPLE=new DustParticleOptions(0x9F55FF,1),WHITE=new DustParticleOptions(0xDAD0FF,.6f);
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;return s;}
    private static long now(Player p){return p.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_gravity")&&p.getUUID().equals(FruitClaims.owner("gravity"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"gravity");}
    public static int uses(Player p){return Mastery.get(p,"cb_gravity_uses_");}
    public static int seconds(Player p){return Mastery.get(p,"cb_gravity_time_");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(uses(p),seconds(p)):0;}
    public static void grant(Player p,int tier){Mastery.set(p,"cb_gravity_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_gravity_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,ItemStack item){
        if(item.isEmpty()||!Relics.kind(item).equals("fruit:gravity")||!FruitClaims.claimItem(p,item,"gravity"))return false;
        p.addTag("cb_fruit_gravity");grant(p,Relics.captureTier(item));p.addTag("cb_mode_fruit");
        if(Dark.unlocked(p))Dark.selectConsumed(p,"gravity");item.shrink(1);return true;
    }
    private static long value(Player p,String prefix){for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void value(Player p,String prefix,long n){for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);p.addTag(prefix+n);}
    private static void credit(Player p){if(!unlocked(p)||!p.isAlive())return;State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_gravity_uses_",uses(p)+1);s.recent=now(p);}
    private static boolean fuel(Player p,int cost){int food=p.getFoodData().getFoodLevel();if(food<=6||food<cost)return false;p.getFoodData().setFoodLevel(food-cost);return true;}
    private static boolean usable(Player p){return selected(p)&&p.isAlive()&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p);}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player);}
    private static int scale(Player p,int first,int second,int third){return tier(p)>=3?third:tier(p)==2?second:first;}
    public static int range(Player p){return (int)Math.round(scale(p,32,48,64)*(Mastery.harmony(p)?1.25:1));}
    public static int pinTicks(Player p){return scale(p,60,100,160);}
    public static int waveRange(Player p){return (int)Math.round(scale(p,12,18,24)*(Mastery.harmony(p)?1.25:1));}
    public static int waveDamage(Player p){return scale(p,8,10,12);}
    public static int liftHeight(Player p){return scale(p,8,16,24);}
    public static int liftTicks(Player p){return scale(p,60,80,120)+(Mastery.harmony(p)?40:0);}
    public static int rideTicks(Player p){return Mastery.harmony(p)?3600:scale(p,200,600,2400);}
    public static int guardTicks(Player p){return scale(p,160,240,320)+(Mastery.harmony(p)?80:0);}
    public static float meleeProtection(Player p){return Mastery.harmony(p)?.8f:scale(p,50,60,70)/100f;}
    public static float projectileProtection(Player p){return Mastery.harmony(p)?.95f:scale(p,80,85,90)/100f;}
    public static int guardRocks(Player p){return scale(p,4,6,8)+(Mastery.harmony(p)?2:0);}
    public static double guardRadius(Player p){return scale(p,15,18,21)/10.0+(Mastery.harmony(p)?.4:0);}
    public static int meteorRadius(Player p){return Mastery.harmony(p)?14:10;}
    public static boolean rooted(LivingEntity target){
        Pin pin=pins.get(target.getUUID());return pin!=null&&target.isAlive()&&target.level()==pin.level()
            &&validOwner(pin.owner(),pin.level())&&now(pin.owner())<pin.ends();
    }
    private static void impulse(LivingEntity e,Vec3 motion){if(e instanceof Player p)Rubber.impulse(p,motion);else e.setDeltaMovement(motion);}
    private static boolean hit(Player p,LivingEntity e,float amount){
        boolean result=e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),(float)(amount*Mastery.multiplier(p)));
        if(result)credit(p);return result;
    }
    private static Vec3 aim(Player p){
        Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(range(p)));
        var wall=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return wall.getType()==HitResult.Type.MISS?end:wall.getLocation();
    }
    static LivingEntity target(Player p){
        Vec3 from=p.getEyePosition(),end=aim(p);double nearest=from.distanceTo(end)+.001;LivingEntity best=null;
        for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.7))){
            if(!enemy(p,e))continue;var hit=e.getBoundingBox().inflate(.3).clip(from,end);
            if(hit.isPresent()&&hit.get().distanceTo(from)<nearest){best=e;nearest=hit.get().distanceTo(from);}
        }return best;
    }
    private static Vec3 location(Player p){LivingEntity t=target(p);return t==null?aim(p):t.position();}
    public static boolean guarding(Player p){State s=states.get(p.getUUID());return s!=null&&usable(p)&&now(p)<s.guard;}
    public static boolean riding(Player p){State s=states.get(p.getUUID());return s!=null&&s.riding&&usable(p);}
    public static boolean immune(Player p,DamageSource source){State s=states.get(p.getUUID());return unlocked(p)&&s!=null&&source.is(DamageTypeTags.IS_FALL)&&(s.riding||now(p)<s.landing);}
    public static float damage(Player p,DamageSource source,float amount){
        if(!guarding(p)||Phoenix.bypass(source))return amount;
        if(source.getDirectEntity() instanceof Projectile)return amount*(1-projectileProtection(p));
        return source.getEntity() instanceof LivingEntity?amount*(1-meleeProtection(p)):amount;
    }
    public static void press(Player p,int move){
        if(!usable(p)||move<0||move>2)return;State s=state(p);if(s.held>=0)return;
        s.held=move;s.press=now(p);s.heartbeat=now(p)+15;s.level=(ServerLevel)p.level();
    }
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null&&s.held>=0)s.heartbeat=now(p)+15;}
    private static void endRide(Player p,State s){
        if(!s.riding)return;s.riding=false;s.landing=now(p)+100;value(p,"cb_gravity_ready_1_true_",now(p)+120);
        if(s.platform!=null)s.platform.discard();s.platform=null;Rubber.impulse(p,Vec3.ZERO);
    }
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null){s.held=-1;endRide(p,s);}}
    public static void release(Player p,int move){
        State s=states.get(p.getUUID());if(s==null||s.held!=move)return;
        long held=now(p)-s.press;boolean wasRide=s.riding;cancelInput(p);
        if(!wasRide)Creeperborn.say(p,cast(p,move,held));
    }
    private static Display.BlockDisplay display(ServerLevel level,Vec3 at,String name,float size){
        var d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(d==null)return null;
        d.addTag("cb_temporary_rock");d.setCustomName(Component.literal(name));d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());
        var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;a.creeperborn$duration(0);a.creeperborn$positionDuration(0);a.creeperborn$width(16);a.creeperborn$height(16);a.creeperborn$view(2);
        d.snapTo(at.x,at.y,at.z);d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(size),new org.joml.Quaternionf()));
        return level.addFreshEntity(d)?d:null;
    }
    private static void clearGuard(State s){s.guard=0;s.rocks.forEach(Entity::discard);s.rocks.clear();}
    public static String cast(Player p,int move,long held){
        if(!usable(p))return "Select Gravity with G; powers need air and freedom from darkness.";
        if(move<0||move>2)return "";boolean hold=held>=7;
        if(tier(p)<move+1)return "Gravity move locked: X tier 2, C tier 3.";
        if(move==2&&!hold&&guarding(p)){clearGuard(state(p));return "Orbit Guard ended.";}
        if(move==2&&hold&&held<60)return "Meteor cancelled: charge for three seconds.";
        if(now(p)<value(p,"cb_gravity_ready_"+move+"_"+hold+"_"))return "Gravity move recovering.";
        ServerLevel level=(ServerLevel)p.level();State s=state(p);s.level=level;
        if(move==0){
            if(hold){
                if(!fuel(p,1))return "Eat before using Gravity.";
                for(var e:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(waveRange(p)))){
                    Vec3 direction=e.position().subtract(p.position());double distance=direction.length();
                    if(!enemy(p,e)||distance>waveRange(p)||!p.hasLineOfSight(e)||distance>.1&&direction.normalize().dot(p.getLookAngle())<.35)continue;
                    pins.remove(e.getUUID());e.stopRiding();hit(p,e,waveDamage(p));impulse(e,direction.normalize().scale(scale(p,14,17,20)/10.0).add(0,.35,0));
                }
                ring(level,p.position().add(0,1,0),waveRange(p));credit(p);
            }else{
                List<LivingEntity> targets=level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range(p))).stream()
                    .filter(t->enemy(p,t)&&p.distanceToSqr(t)<=range(p)*range(p)&&p.hasLineOfSight(t)).toList();
                if(targets.isEmpty())return "No visible enemies within "+range(p)+" blocks.";
                if(!fuel(p,1))return "Eat before using Gravity.";
                for(LivingEntity t:targets){
                    hit(p,t,scale(p,6,7,8));t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,pinTicks(p),2,false,false,true));
                    t.stopRiding();impulse(t,new Vec3(0,-1,0));pins.put(t.getUUID(),new Pin(p,t,level,now(p)+pinTicks(p)));column(level,t.position(),4);
                }credit(p);
            }
            value(p,"cb_gravity_ready_0_"+hold+"_",now(p)+(hold?120:20));return hold?"Repulsion Wave!":"Gravity Press!";
        }
        if(move==1){
            if(hold)return "Keep holding X to ride the rock; aim where you want to fly.";
            LivingEntity t=target(p);if(t==null)return "Aim at an enemy to levitate.";
            if(!fuel(p,1))return "Eat before using Gravity.";
            pins.remove(t.getUUID());t.stopRiding();lifts.entrySet().removeIf(v->v.getValue().owner()==p);
            lifts.put(t.getUUID(),new Lift(p,t,level,Math.min(level.getMaxY()-3,t.getY()+liftHeight(p)),now(p)+liftTicks(p)));hit(p,t,2);column(level,t.position(),Math.min(8,liftHeight(p)));
            value(p,"cb_gravity_ready_1_false_",now(p)+120);credit(p);return "Levitate!";
        }
        if(!hold){
            if(!fuel(p,1))return "Eat before using Orbit Guard.";
            clearGuard(s);s.guard=now(p)+guardTicks(p);
            for(int i=0;i<guardRocks(p);i++){var d=display(level,p.position().add(0,1,0),"cb_gravity_rock",.55f);if(d!=null)s.rocks.add(d);}
            value(p,"cb_gravity_ready_2_false_",now(p)+guardTicks(p)+80);credit(p);return "Orbit Guard!";
        }
        Vec3 end=location(p);end=new Vec3(end.x,Math.max(level.getMinY()+1,Math.min(level.getMaxY()-3,end.y)),end.z);BlockPos block=BlockPos.containing(end);
        if(!level.hasChunkAt(block)||!level.getWorldBorder().isWithinBounds(block))return "Aim inside loaded terrain and the world border.";
        if(!fuel(p,3))return "Meteor needs food above three icons.";
        Vec3 start=end.add(0,Math.min(24,level.getMaxY()-1-end.y),0);
        var ceiling=level.clip(new ClipContext(start,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(ceiling.getType()!=HitResult.Type.MISS)end=ceiling.getLocation();
        var visual=display(level,start,"cb_gravity_meteor",Mastery.harmony(p)?4:3);
        meteors.add(new Meteor(p,level,start,end,now(p),now(p)+30,visual,meteorRadius(p),24));
        value(p,"cb_gravity_ready_2_true_",now(p)+400);credit(p);return "Meteor descending!";
    }
    public static void tickPlayer(Player p){
        State s=state(p);long now=now(p);
        if(!usable(p)||s.level!=null&&s.level!=p.level()){cancelInput(p);clearGuard(s);return;}
        if(s.held>=0&&now>s.heartbeat){cancelInput(p);return;}
        if(s.held==1&&now-s.press>=7&&tier(p)>=2){
            if(!s.riding){
                if(now<value(p,"cb_gravity_ready_1_true_")||!fuel(p,1)){cancelInput(p);return;}
                p.stopRiding();s.riding=true;s.rideEnds=now+rideTicks(p);s.platform=display((ServerLevel)p.level(),p.position().add(0,-.5,0),"cb_gravity_rock",1.6f);credit(p);
            }
            Vec3 velocity=p.getLookAngle().scale(.45).add(0,.04,0),next=p.position().add(velocity);
            BlockPos block=BlockPos.containing(next);
            if(now>=s.rideEnds||p.getFoodData().getFoodLevel()<=6||!p.level().hasChunkAt(block)||!p.level().getWorldBorder().isWithinBounds(block)
                ||next.y<p.level().getMinY()||next.y+p.getBbHeight()>=p.level().getMaxY()
                ||!p.level().noCollision(p,p.getBoundingBox().move(velocity))||now%200==0&&!fuel(p,1)){cancelInput(p);return;}
            Rubber.impulse(p,velocity);if(s.platform!=null)s.platform.snapTo(p.getX(),p.getY()-.5,p.getZ());
            if(now%5==0)ring((ServerLevel)p.level(),p.position().add(0,-.3,0),1);
        }
        if(s.held==2&&now-s.press>=7&&now%5==0)Creeperborn.say(p,"Meteor "+Math.min(100,(now-s.press)*100/60)+"% — release after three seconds");
        if(s.guard>0){
            if(now>=s.guard)clearGuard(s);
            else{
                for(int i=0;i<s.rocks.size();i++){double a=now*.12+i*Math.PI*2/s.rocks.size();s.rocks.get(i).snapTo(p.getX()+Math.cos(a)*guardRadius(p),p.getY()+1+Math.sin(a*2)*.3,p.getZ()+Math.sin(a)*guardRadius(p));}
                for(var projectile:p.level().getEntitiesOfClass(Projectile.class,p.getBoundingBox().inflate(guardRadius(p)))){
                    Entity owner=projectile.getOwner();if(owner==p||owner!=null&&p.isAlliedTo(owner))continue;
                    projectile.discard();credit(p);ring((ServerLevel)p.level(),p.position().add(0,1,0),1.5);
                }
            }
        }
        if(now%20==0){
            boolean active=s.last!=null&&s.last.distanceToSqr(p.position())>.04||now-s.recent<200;s.last=p.position();
            if(active){int before=tier(p);Mastery.set(p,"cb_gravity_time_",seconds(p)+1);if(tier(p)>before)Creeperborn.say(p,"Gravity mastery increased!");}
        }
    }
    private static boolean validOwner(Player p,ServerLevel level){return p.isAlive()&&p.level()==level&&unlocked(p)&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p);}
    public static void tickEffects(){
        for(var entry:List.copyOf(pins.entrySet())){
            Pin pin=entry.getValue();if(!rooted(pin.target())){pins.remove(entry.getKey());continue;}
            pin.target().setDeltaMovement(Vec3.ZERO);if(pin.target() instanceof Mob mob)mob.getNavigation().stop();
            if(now(pin.owner())%10==0)ring(pin.level(),pin.target().position().add(0,.1,0),.9);
        }
        for(var entry:List.copyOf(lifts.entrySet())){
            Lift l=entry.getValue();LivingEntity t=l.target();
            if(!validOwner(l.owner(),l.level())||!t.isAlive()||t.level()!=l.level()||now(l.owner())>=l.ends()){lifts.remove(entry.getKey());continue;}
            Vec3 delta=t.getDeltaMovement();impulse(t,new Vec3(delta.x*.5,Math.max(-.4,Math.min(.6,(l.height()-t.getY())*.25+.08)),delta.z*.5));
            if(t instanceof Mob mob)mob.getNavigation().stop();if(now(l.owner())%5==0)ring(l.level(),t.position(),.8);
        }
        for(Meteor m:List.copyOf(meteors)){
            if(!meteors.contains(m))continue;
            if(!validOwner(m.owner(),m.level())){remove(m);continue;}
            double t=Math.max(0,Math.min(1,(now(m.owner())-m.begins())/(double)(m.lands()-m.begins())));
            Vec3 at=m.start().lerp(m.end(),t*t);
            if(m.visual()!=null)m.visual().snapTo(at.x,at.y,at.z);
            if(now(m.owner())%3==0){
                m.level().sendParticles(PURPLE,at.x,at.y,at.z,12,1,1,1,.02);
                m.level().sendParticles(ParticleTypes.FLAME,at.x,at.y-1.4,at.z,35,1.4,.35,1.4,.03);
                m.level().sendParticles(ParticleTypes.SMOKE,at.x,at.y+1,at.z,12,.8,.8,.8,.03);
            }
            if(t>=1){
                ring(m.level(),m.end(),m.radius());column(m.level(),m.end(),5);
                m.level().sendParticles(ParticleTypes.EXPLOSION_EMITTER,m.end().x,m.end().y+1,m.end().z,1,0,0,0,0);
                m.level().playSound(null,m.end().x,m.end().y,m.end().z,net.minecraft.sounds.SoundEvents.GENERIC_EXPLODE.value(),net.minecraft.sounds.SoundSource.BLOCKS,2f,.7f);
                m.level().sendParticles(ParticleTypes.FLAME,m.end().x,m.end().y+.5,m.end().z,100,3,.7,3,.12);
                blasts.add(new Blast(m.owner(),m.level(),m.end(),now(m.owner()),m.radius()));
                for(var target:m.level().getEntitiesOfClass(LivingEntity.class,new AABB(m.end(),m.end()).inflate(m.radius()))){
                    if(!meteors.contains(m)||!m.owner().isAlive())break;
                    if(!enemy(m.owner(),target)||target.position().distanceToSqr(m.end())>m.radius()*m.radius())continue;
                    Vec3 origin=m.end().add(0,.3,0);
                    if(m.level().clip(new ClipContext(origin,target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,m.owner())).getType()!=HitResult.Type.MISS)continue;
                    pins.remove(target.getUUID());hit(m.owner(),target,m.damage());target.stopRiding();impulse(target,target.position().subtract(m.end()).normalize().scale(1.2).add(0,.7,0));
                }remove(m);
            }
        }
        for(Blast blast:List.copyOf(blasts)){
            long age=now(blast.owner())-blast.begins();
            if(!validOwner(blast.owner(),blast.level())||age>=12){blasts.remove(blast);continue;}
            if(age%2==0)ring(blast.level(),blast.center().add(0,.2,0),blast.radius()*Math.max(.1,age/11.0));
        }
    }
    private static void remove(Meteor m){if(m.visual()!=null)m.visual().discard();meteors.remove(m);}
    private static void column(ServerLevel level,Vec3 at,int height){for(int i=0;i<height;i++)ring(level,at.add(0,i,0),.8);}
    private static void ring(ServerLevel level,Vec3 center,double radius){
        for(int i=0;i<32;i++){double a=i*Math.PI/16;Vec3 at=center.add(Math.cos(a)*radius,0,Math.sin(a)*radius);level.sendParticles(PURPLE,at.x,at.y,at.z,1,0,0,0,0);if(i%4==0)level.sendParticles(WHITE,at.x,at.y,at.z,1,0,0,0,0);}
    }
    public static int meteorCount(){return meteors.size();}
    public static String status(Player p){return "Gravity "+(unlocked(p)?"tier "+tier(p)+"/3 | "+uses(p)+" uses | "+seconds(p)+"s | Range "+range(p)+" | Guard "+guarding(p)+" | Rock ride "+riding(p):"locked");}
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())tickPlayer(p);tickEffects();}
    public static void forget(Player p){
        State s=states.get(p.getUUID());if(s!=null){cancelInput(p);clearGuard(s);}states.remove(p.getUUID());
        pins.entrySet().removeIf(v->v.getValue().owner()==p||v.getValue().target()==p);blasts.removeIf(v->v.owner()==p);
        lifts.entrySet().removeIf(v->v.getValue().owner()==p||v.getValue().target()==p);
        for(var m:List.copyOf(meteors))if(m.owner()==p)remove(m);
    }
    public static void clear(){for(var s:List.copyOf(states.values()))forget(s.owner);states.clear();pins.clear();blasts.clear();lifts.clear();for(var m:List.copyOf(meteors))remove(m);}
}
