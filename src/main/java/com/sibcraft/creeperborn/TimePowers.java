package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.effect.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.*;

/** Server-authoritative temporal combat. No global tick-rate or permanent world changes. */
public final class TimePowers {
    private static final class State {
        Player p;ServerLevel level;int held=-1,charges;long press,heartbeat,until,nextDodge,counterUntil,recent=-10000,homeUntil;
        Vec3 anchor,homeStart;long anchorUntil;float anchorYaw,anchorPitch,anchorHealth;Display.BlockDisplay clock,marker;
        DamageSource evaded;long evadedAt=-1;
    }
    private record Echo(Player p,ServerLevel level,LivingEntity target,Vec3 at,long due,float damage){}
    private record Slow(Player p,LivingEntity target,ServerLevel level,long until){}
    private static final class Visual {Player p;Display.BlockDisplay entity;long until;Vec3 drift;Visual(Player p,Display.BlockDisplay e,long until,Vec3 drift){this.p=p;entity=e;this.until=until;this.drift=drift;}}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Visual> visuals=new HashMap<>();
    private static final List<Echo> echoes=new ArrayList<>();
    private static final Map<UUID,Slow> slows=new HashMap<>();
    private static long now(Entity e){return e.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_time")&&p.getUUID().equals(FruitClaims.owner("time"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"time");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(Mastery.get(p,"cb_time_uses_"),Mastery.get(p,"cb_time_time_")):0;}
    public static void grant(Player p,int t){Mastery.set(p,"cb_time_uses_",t>=3?40:t==2?10:0);Mastery.set(p,"cb_time_time_",t>=3?3600:t==2?300:0);}
    public static boolean consume(Player p,ItemStack item){if(item.isEmpty()||!Relics.kind(item).equals("fruit:time")||!FruitClaims.claimItem(p,item,"time"))return false;p.addTag("cb_fruit_time");grant(p,Relics.captureTier(item));if(Dark.unlocked(p))Dark.selectConsumed(p,"time");p.addTag("cb_mode_fruit");item.shrink(1);return true;}
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.p=p;if(s.level==null)s.level=(ServerLevel)p.level();return s;}
    private static boolean valid(State s){return s.p.isAlive()&&!s.p.isRemoved()&&!s.p.isSpectator()&&s.p.level()==s.level&&unlocked(s.p)&&Dark.matches(s.p,"time")&&!Dark.suppressed(s.p)&&!WaterRules.suppressed(s.p);}
    private static boolean restrained(Player p){return Shackles.rooted(p)||CelestialPowers.rooted(p)||NaturePowers.rooted(p)||ClimatePowers.rooted(p)||PairPowers.rooted(p)||Dark.rooted(p)||StringFruit.locked(p)||StringFruit.controlling(p);}
    private static long saved(Player p,String key){for(String tag:p.entityTags())if(tag.startsWith(key))try{return Long.parseLong(tag.substring(key.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void saved(Player p,String key,long v){for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith(key))p.removeTag(tag);p.addTag(key+v);}
    private static void credit(State s){if(now(s.p)-s.recent>=20)Mastery.set(s.p,"cb_time_uses_",Mastery.get(s.p,"cb_time_uses_")+1);s.recent=now(s.p);}
    public static void press(Player p,int move){if(!selected(p)||move<0||move>2||restrained(p))return;State s=state(p);if(!valid(s)||s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;}
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null)s.held=-1;}
    public static void release(Player p,int move){State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;s.held=-1;Creeperborn.say(p,cast(p,move,held));}
    public static int charges(Player p){State s=states.get(p.getUUID());return s!=null&&now(p)<s.until?s.charges:0;}
    public static int remaining(Player p){State s=states.get(p.getUUID());return s==null?0:(int)Math.max(0,s.until-now(p));}
    public static boolean awakened(Player p){return tier(p)>=3&&remaining(p)>0;}
    public static int visualCount(Player p){return (int)visuals.values().stream().filter(v->v.p==p).count();}
    public static int echoCount(){return echoes.size();}
    public static boolean channeling(Player p){State s=states.get(p.getUUID());return s!=null&&s.homeUntil>0;}
    public static boolean slowed(LivingEntity e){Slow q=slows.get(e.getUUID());return q!=null&&q.target.isAlive()&&q.p.isAlive()&&q.p.level()==q.level&&e.level()==q.level&&now(q.p)<q.until&&!Dark.suppressed(q.p)&&!WaterRules.suppressed(q.p);}
    private static void slowAttribute(LivingEntity e,boolean on){var a=e.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);if(a==null)return;var id=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","time_slow");a.removeModifier(id);if(on)a.addTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(id,-.65,net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));}
    public static String cast(Player p,int move,long held){
        if(!selected(p)||move<0||move>2||held<0)return "Select Time-Time with G.";State s=state(p);if(!valid(s)||restrained(p))return "Time powers suppressed or restrained.";if(p.getFoodData().getFoodLevel()<=6)return "Eat before bending time.";
        boolean hold=held>=20;int t=tier(p);long time=now(p);String key="cb_time_ready_"+move+"_"+(hold?"hold_":"tap_");if(time<saved(p,key))return "Time move recovering: "+((saved(p,key)-time+19)/20)+"s.";int cd;String text;
        if(move==0&&!hold){if(s.until>time)return "Time Dodge is already active.";int duration=t==3?900:t==2?500:300;s.until=time+duration;s.charges=t==3?9:t==2?6:3;s.nextDodge=time;s.counterUntil=0;cd=duration+1200;text=(t==3?"Awakened Time — 45 seconds":t==2?"Fractured Time — 25 seconds":"Time Skip — 15 seconds")+" | "+s.charges+" dodges";if(t==3)s.clock=visual(p,p.position(),2,duration,Vec3.ZERO);}
        else if(move==0){LivingEntity target=target(p,7+t*2);if(target==null)return "Aim at an enemy for Borrowed Second.";float damage=(t==3?24:t==2?16:10)*(s.counterUntil>time?1.5f:1);s.counterUntil=0;boolean hit=hit(p,target,damage);if(hit){echoes.add(new Echo(p,s.level,target,target.position(),time+12,damage*(t==3?1:t==2?.7f:.45f)));visual(p,p.position(),1,24,Vec3.ZERO);visual(p,target.position(),4,24,Vec3.ZERO);}cd=65;text="Borrowed Second — delayed echo!";}
        else if(move==1&&!hold){double radius=4+t*2;int count=0;for(var e:s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius)))if(enemy(p,e)&&p.distanceToSqr(e)<=radius*radius&&p.hasLineOfSight(e)){int duration=e instanceof Player||e.getMaxHealth()>100?30+t*10:50+t*20;slows.put(e.getUUID(),new Slow(p,e,s.level,time+duration));slowAttribute(e,true);count++;}visual(p,p.position(),3,35,Vec3.ZERO);cd=240;text="Stolen Moment — "+count+" enemies slowed!";}
        else if(move==1){if(s.anchor!=null&&time<s.anchorUntil){if(!safe(s.level,p,s.anchor)||!teleport(p,s.level,s.anchor,s.anchorYaw,s.anchorPitch))return "Rewind destination is obstructed.";visual(p,s.anchor,1,24,Vec3.ZERO);if(awakened(p))p.setHealth(Math.max(p.getHealth(),Math.min(s.anchorHealth,Math.min(p.getMaxHealth(),p.getHealth()+6))));clearAnchor(s);cd=240;text="Rewind — returned to your anchor!";}else{if(!safe(s.level,p,p.position()))return "Stand on safe ground to mark an anchor.";s.anchor=p.position();s.anchorYaw=p.getYRot();s.anchorPitch=p.getXRot();s.anchorHealth=p.getHealth();s.anchorUntil=time+600;s.marker=visual(p,s.anchor,3,600,Vec3.ZERO);cd=10;text="Rewind Anchor — hold X again within 30 seconds.";}}
        else if(!hold)return status(p);
        else{if(s.homeUntil>0)return "Homeward Hour is already channeling.";s.homeUntil=time+100;s.homeStart=p.position();cd=200;text="Homeward Hour — stay still for five seconds. Damage interrupts.";}
        saved(p,key,time+cd);p.causeFoodExhaustion(.25f);credit(s);return text;
    }
    /** Explicit origin scopes take priority over vanilla's playerAttack disguise. Unknown powers fail closed. */
    public static boolean eligible(Player victim,DamageSource source){
        if(source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)||source.is(DamageTypeTags.IS_EXPLOSION))return false;
        String power=ZoanPowers.damagePower(source.getEntity());if(!power.isEmpty()){
            if(tier(victim)<3)return false;Element e=Element.parse(power);if(e!=null){if(source.getEntity() instanceof Player attacker)return Mastery.tier(attacker,e)>0&&Mastery.tier(attacker,e)<3;return true;}
            if(!(source.getEntity() instanceof Player attacker))return false;int rank=Dark.fruitTier(attacker,power);return rank>0&&rank<=2;
        }
        return source.getDirectEntity() instanceof Projectile||source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
    }
    public static boolean intercept(Player p,DamageSource source,float amount){
        State s=states.get(p.getUUID());if(s==null||amount<=0||!Float.isFinite(amount)||!valid(s)||restrained(p)||p.isPassenger()||p.isSleeping()||s.until<=now(p)||s.charges<=0||s.nextDodge>now(p)||source.getEntity()==p||!eligible(p,source))return false;
        Vec3 old=p.position(),dest=dodgeDestination(p,source);if(dest==null||!teleport(p,s.level,dest,p.getYRot(),p.getXRot()))return false;
        s.evaded=source;s.evadedAt=now(p);s.charges--;s.nextDodge=now(p)+(tier(p)==1?40:20);s.counterUntil=now(p)+60;visual(p,old,1,24,p.getDeltaMovement().scale(.15));spark(p,old,20);spark(p,dest,15);if(source.getDirectEntity() instanceof Projectile projectile)projectile.discard();if(s.charges==0)endDodge(s);credit(s);return true;
    }
    public static boolean evaded(LivingEntity e,DamageSource source){State s=states.get(e.getUUID());return s!=null&&s.evaded==source&&s.evadedAt==now(e);}
    private static Vec3 dodgeDestination(Player p,DamageSource source){Vec3 motion=p.getDeltaMovement().multiply(1,0,1);Vec3 away=source.getEntity()!=null?p.position().subtract(source.getEntity().position()).multiply(1,0,1):p.getLookAngle().multiply(1,0,1).scale(-1);Vec3 dir=motion.lengthSqr()>.004?motion.normalize():away.lengthSqr()>.01?away.normalize():new Vec3(1,0,0);double distance=tier(p)>=2?4:3;for(double length:new double[]{distance,2,1})for(double angle:new double[]{0,Math.PI/2,-Math.PI/2,Math.PI})for(int dy:new int[]{0,1,-1}){Vec3 at=p.position().add(dir.yRot((float)angle).scale(length)).add(0,dy,0);if(safe((ServerLevel)p.level(),p,at)&&p.level().clip(new ClipContext(p.getEyePosition(),at.add(0,p.getEyeHeight(),0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS)return at;}return null;}
    private static boolean safe(ServerLevel l,Player p,Vec3 at){BlockPos feet=BlockPos.containing(at);if(!l.hasChunkAt(feet)||!l.getWorldBorder().isWithinBounds(feet)||!DoorSpace.fits(l,p,at)||!l.getBlockState(feet).getFluidState().isEmpty()||!l.getBlockState(feet.above()).getFluidState().isEmpty()||l.getBlockState(feet.below()).getCollisionShape(l,feet.below()).isEmpty()||StringFruit.blocked(p,at))return false;return !Set.of(Blocks.MAGMA_BLOCK,Blocks.CAMPFIRE,Blocks.SOUL_CAMPFIRE,Blocks.CACTUS).contains(l.getBlockState(feet.below()).getBlock());}
    private static boolean teleport(Player p,ServerLevel l,Vec3 at,float yaw,float pitch){Vec3 old=p.position();boolean ok;if(p instanceof ServerPlayer sp)ok=sp.teleportTo(l,at.x,at.y,at.z,Set.of(),yaw,pitch,false);else{p.snapTo(at.x,at.y,at.z);p.setYRot(yaw);p.setXRot(pitch);ok=true;}if(ok){p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();}return ok;}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&!Azure.ownedProxy(e,p)&&(e instanceof Enemy||e instanceof Player);}
    private static LivingEntity target(Player p,double range){Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(range));var wall=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(wall.getType()!=HitResult.Type.MISS)to=wall.getLocation();LivingEntity best=null;double near=from.distanceToSqr(to);for(var e:((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(.6)))if(enemy(p,e)){var at=e.getBoundingBox().inflate(.4).clip(from,to);if(at.isPresent()&&from.distanceToSqr(at.get())<=near){best=e;near=from.distanceToSqr(at.get());}}return best;}
    private static boolean hit(Player p,LivingEntity e,float amount){if(!enemy(p,e))return false;e.damageCooldownTime=0;return ZoanPowers.poweredHit(p,e,"time",p.damageSources().playerAttack(p),amount*(float)Mastery.multiplier(p));}
    public static void damaged(Player p){State s=states.get(p.getUUID());if(s!=null&&s.homeUntil>0){s.homeUntil=0;s.homeStart=null;Creeperborn.say(p,"Homeward Hour interrupted!");}}
    private static void endDodge(State s){s.until=0;s.charges=0;discard(s.clock);s.clock=null;}
    private static void clearAnchor(State s){s.anchor=null;s.anchorUntil=0;discard(s.marker);s.marker=null;}
    public static void tickPlayer(Player p){State s=states.get(p.getUUID());if(s==null)return;if(!valid(s)){forget(p);return;}long time=now(p);if(s.held>=0&&(!selected(p)||time>s.heartbeat))cancelInput(p);if(time>=s.until)endDodge(s);if(s.anchor!=null&&time>=s.anchorUntil)clearAnchor(s);
        if(s.clock!=null)s.clock.snapTo(p.getX(),p.getY()+1.2,p.getZ());if(s.until>time&&tier(p)>=3&&time%6==0&&p.getDeltaMovement().lengthSqr()>.002)visual(p,p.position(),1,24,Vec3.ZERO);
        if(s.homeUntil>0){if(restrained(p)||p.position().distanceToSqr(s.homeStart)>.64||!selected(p)){damaged(p);}else if(time>=s.homeUntil){s.homeUntil=0;s.homeStart=null;if(p instanceof ServerPlayer sp){var transition=sp.findRespawnPositionAndUseSpawnBlock(false,TeleportTransition.DO_NOTHING);if(sp.teleport(transition)!=null){endDodge(s);clearAnchor(s);saved(p,"cb_time_ready_2_hold_",time+2400);Creeperborn.say(p,"Homeward Hour — returned to your respawn point!");}else Creeperborn.say(p,"No safe respawn destination.");}}else if(time%5==0){visual(p,p.position(),4,12,Vec3.ZERO);spark(p,p.position().add(0,1,0),6);}}
        if(time%20==0){if(s.until>time||s.homeUntil>0||s.recent+200>time)Mastery.set(p,"cb_time_time_",Mastery.get(p,"cb_time_time_")+1);if(selected(p)&&s.until>time)Creeperborn.say(p,(tier(p)>=3?"Awakened Time":"Time Dodge")+" | "+((s.until-time+19)/20)+"s | "+s.charges+" dodges");}
    }
    public static void tick(MinecraftServer server){for(State s:List.copyOf(states.values()))tickPlayer(s.p);effects();}
    public static void effects(){for(Echo q:List.copyOf(echoes))if(!q.p.isAlive()||q.p.level()!=q.level||!unlocked(q.p)||Dark.suppressed(q.p)||WaterRules.suppressed(q.p)){echoes.remove(q);}else if(now(q.p)>=q.due){if(q.target.isAlive()&&q.target.level()==q.level&&q.target.position().distanceToSqr(q.at)<=64&&q.p.distanceToSqr(q.target)<=2304&&q.p.hasLineOfSight(q.target)){hit(q.p,q.target,q.damage);spark(q.p,q.target.getBoundingBox().getCenter(),20);}echoes.remove(q);}
        for(Slow q:List.copyOf(slows.values()))if(!slowed(q.target)){slowAttribute(q.target,false);slows.remove(q.target.getUUID());}for(Visual v:List.copyOf(visuals.values())){if(v.entity.isRemoved()||!v.p.isAlive()||v.p.level()!=v.entity.level()||now(v.p)>=v.until){discard(v.entity);continue;}if(v.entity.tickCount<6&&v.drift.lengthSqr()>0){Vec3 at=v.entity.position().add(v.drift);if(v.entity.level().noCollision(v.entity,v.entity.getBoundingBox().move(v.drift)))v.entity.snapTo(at.x,at.y,at.z);}}
    }
    private static Display.BlockDisplay visual(Player p,Vec3 at,int kind,int ticks,Vec3 drift){if(visualCount(p)>=18)return null;var d=EntityTypes.BLOCK_DISPLAY.create((ServerLevel)p.level(),EntitySpawnReason.TRIGGERED);if(d==null)return null;d.addTag("cb_temporary_rock");d.setCustomName(Component.literal("cb_time_"+kind+"|"+p.getUUID()));d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());d.snapTo(at.x,at.y,at.z);var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;a.creeperborn$duration(0);a.creeperborn$positionDuration(2);a.creeperborn$width(8);a.creeperborn$height(8);a.creeperborn$view(3);d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf().rotationY((float)Math.toRadians(-p.getYRot())),new org.joml.Vector3f(1,1,1),new org.joml.Quaternionf()));if(!p.level().addFreshEntity(d))return null;visuals.put(d.getUUID(),new Visual(p,d,now(p)+ticks,drift));return d;}
    private static void discard(Display.BlockDisplay e){if(e!=null){visuals.remove(e.getUUID());e.discard();}}
    public static void tickVisual(Entity e){if(!(e.level() instanceof ServerLevel)||e.getCustomName()==null||!e.getCustomName().getString().startsWith("cb_time_"))return;Visual v=visuals.get(e.getUUID());if(v==null&&e.tickCount>2||v!=null&&now(e)>=v.until){e.discard();visuals.remove(e.getUUID());}}
    private static void spark(Player p,Vec3 at,int count){((ServerLevel)p.level()).sendParticles(new DustParticleOptions(0x75F5F0,.7f),at.x,at.y,at.z,count,.35,.65,.35,.01);}
    public static void forget(Player p){State s=states.remove(p.getUUID());if(s!=null){endDodge(s);clearAnchor(s);}for(Visual v:List.copyOf(visuals.values()))if(v.p==p)discard(v.entity);echoes.removeIf(q->q.p==p);for(Slow q:List.copyOf(slows.values()))if(q.p==p||q.target==p){slowAttribute(q.target,false);slows.remove(q.target.getUUID());}}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.p);states.clear();visuals.clear();echoes.clear();slows.clear();}
    public static String status(Player p){return !unlocked(p)?"Time-Time: locked":"Time-Time T"+tier(p)+" | Z Dodge / hold Borrowed Second | X Stolen Moment / hold Rewind Anchor | C status / hold Homeward Hour";}
}
