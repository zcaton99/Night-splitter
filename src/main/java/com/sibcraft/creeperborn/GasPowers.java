package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Bounded, server-owned gas fields. No explosions, block edits or forced chunks. */
public final class GasPowers {
    private static final class State {
        Player p; ServerLevel level; int held=-1; long press,heartbeat,recent=-1000,bodyEnds,veilEnds;
        boolean flight; float oldSpeed; boolean oldFlying,granted; Vec3 last;
    }
    private static final class Cloud {
        Player owner; ServerLevel level; Vec3 at; double radius; long ends,ignites; boolean dense;
        Display.BlockDisplay visual;
    }
    private static final class Shot {Player owner;ServerLevel level;Vec3 at,dir;int life;Display.BlockDisplay visual;}
    private record View(int entity,int flags){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final List<Cloud> clouds=new ArrayList<>();
    private static final List<Shot> shots=new ArrayList<>();
    private static final Map<UUID,Long> nauseaReady=new HashMap<>(),landing=new HashMap<>();
    private static final Map<UUID,Map<UUID,View>> sent=new HashMap<>();
    private GasPowers(){}
    private static long now(Entity e){return e.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_gas")&&p.getUUID().equals(FruitClaims.owner("gas"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"gas");}
    private static boolean usable(Player p){return unlocked(p)&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)&&!StringFruit.controlled(p)&&!PairPowers.rooted(p)&&!ClimatePowers.rooted(p);}
    private static boolean active(State s){return s!=null&&s.level==s.p.level()&&usable(s.p)&&(selected(s.p)||FormCarry.carries(s.p,"gas")||!s.p.entityTags().contains("cb_mode_fruit"));}
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.p=p;s.level=(ServerLevel)p.level();return s;}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(Mastery.get(p,"cb_gas_uses_"),Mastery.get(p,"cb_gas_time_")):0;}
    public static void grant(Player p,int tier){Mastery.set(p,"cb_gas_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_gas_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,ItemStack item){if(item.isEmpty()||!Relics.kind(item).equals("fruit:gas")||!FruitClaims.claimItem(p,item,"gas"))return false;p.addTag("cb_fruit_gas");grant(p,Relics.captureTier(item));p.addTag("cb_mode_fruit");if(Dark.unlocked(p))Dark.selectConsumed(p,"gas");item.shrink(1);return true;}
    private static int scale(Player p,int a,int b,int c){return tier(p)>=3?c:tier(p)==2?b:a;}
    private static long ready(Player p,int slot){String prefix="cb_gas_ready_"+slot+"_";for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void ready(Player p,int slot,long value){String prefix="cb_gas_ready_"+slot+"_";for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);p.addTag(prefix+value);}
    private static void credit(Player p){State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_gas_uses_",Mastery.get(p,"cb_gas_uses_")+1);s.recent=now(p);}
    public static boolean body(Player p){State s=states.get(p.getUUID());return active(s)&&s.bodyEnds>now(p);}
    public static boolean veil(Player p){State s=states.get(p.getUUID());return active(s)&&s.veilEnds>now(p);}
    public static boolean poisonImmune(Player p){return usable(p)&&(selected(p)||body(p)||FormCarry.carries(p,"gas"));}
    public static boolean immune(Player p,DamageSource source){return source.is(DamageTypeTags.IS_FALL)&&(body(p)||now(p)<landing.getOrDefault(p.getUUID(),0L))&&!Phoenix.bypass(source);}
    public static float damage(Player p,DamageSource source,float amount){
        if(Phoenix.bypass(source)||!usable(p)||source.is(DamageTypeTags.IS_FIRE)||source.getEntity() instanceof Player a&&Mastery.harmony(a))return amount;
        boolean physical=source.is(DamageTypeTags.IS_PROJECTILE)||source.is(DamageTypes.PLAYER_ATTACK)||source.is(DamageTypes.MOB_ATTACK)||source.is(DamageTypes.MOB_ATTACK_NO_AGGRO);
        return physical?amount*(body(p)?.25f:veil(p)?.8f:1):amount;
    }
    public static void press(Player p,int move){if(!selected(p)||!usable(p)||move<0||move>2)return;State s=state(p);if(s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;}
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null&&s.held>=0)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null)s.held=-1;}
    public static void release(Player p,int move){State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;s.held=-1;Creeperborn.say(p,cast(p,move,held));}
    public static String cast(Player p,int move,long held){
        if(!selected(p)||!usable(p)||move<0||move>2)return "Gas powers unavailable.";
        boolean hold=held>=7;State s=state(p);int slot=move*2+(hold?1:0);long time=now(p);
        if(move==2&&hold&&body(p)){endBody(s);return "Gas Body ended.";}
        if(move==1&&hold&&veil(p)){s.veilEnds=0;return "Poison Veil ended.";}
        if(tier(p)<move+1)return "Gas: X unlocks at tier 2; C and Gas Body at mastery.";
        if(hold&&held<20)return "Charge for one second.";
        if(time<ready(p,slot))return "Gas move recovering.";
        if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to use Gas.";
        int cooldown;String result;
        if(move==0&&!hold){if(shots.stream().filter(q->q.owner==p).count()>=4||shots.size()>=64)return "Too many Gas Shots active.";Shot q=new Shot();q.owner=p;q.level=(ServerLevel)p.level();q.at=p.getEyePosition().add(p.getLookAngle().scale(.7));q.dir=p.getLookAngle().normalize();q.life=scale(p,20,28,36);q.visual=display(p,q.at,2,.5);shots.add(q);cooldown=24;result="Gas Shot!";}
        else if(move==0){Vec3 from=p.getEyePosition(),end=aim(p,scale(p,12,18,24));double distance=from.distanceTo(end);int n=scale(p,3,4,6);for(int i=1;i<=n;i++)cloud(p,from.lerp(end,i/(double)n).add(0,-1,0),scale(p,2,3,4),scale(p,100,140,200),true);puff((ServerLevel)p.level(),from.lerp(end,.5),0xBDDF8A,10,distance*.15);cooldown=140;result="Choking Cloud — poison, slow and brief nausea pulses!";}
        else if(move==1&&!hold){Vec3 from=p.position(),end=from,dir=p.getLookAngle().normalize();double range=scale(p,6,10,14);for(double d=.25;d<=range;d+=.25){Vec3 at=from.add(dir.scale(d));if(!safe(p,at))break;end=at;}if(from.distanceTo(end)<.5)return "Gas Dash blocked.";p.stopRiding();if(p instanceof ServerPlayer sp)sp.teleportTo((ServerLevel)p.level(),end.x,end.y,end.z,Set.of(),p.getYRot(),p.getXRot(),false);else p.snapTo(end.x,end.y,end.z);p.resetFallDistance();landing.put(p.getUUID(),time+80);for(int i=0;i<3;i++)cloud(p,from.lerp(end,i/3.0),2,80,false);cooldown=70;result="Gas Dash!";}
        else if(move==1){s.veilEnds=time+scale(p,100,160,240);cooldown=300;result="Poison Veil — protective gas follows you!";}
        else if(!hold){int count=0;for(Cloud c:clouds)if(c.owner==p&&c.level==p.level()&&c.ignites==0&&c.at.distanceToSqr(p.position())<=48*48){c.ignites=time+20;c.ends=Math.max(c.ends,c.ignites+1);if(c.visual!=null)c.visual.setCustomName(Component.literal("cb_gas_3"));count++;}if(count==0)return "Create gas clouds before Combustion.";cooldown=180;result="Combustion — orange clouds detonate in one second!";}
        else{FormCarry.replacing(p,"gas");s.bodyEnds=time+2400;startFlight(s);cooldown=3000;result="Gas Body — lower body becomes gas! WASD flies, Space rises, Shift descends. Hold C to end.";}
        ready(p,slot,time+cooldown);p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-(hold?2:1));credit(p);return result;
    }
    private static boolean safe(Player p,Vec3 at){ServerLevel l=(ServerLevel)p.level();return at.y>=l.getMinY()&&at.y+p.getBbHeight()<l.getMaxY()&&l.hasChunkAt(BlockPos.containing(at))&&l.getWorldBorder().isWithinBounds(BlockPos.containing(at))&&!StringFruit.blocked(p,at)&&l.noCollision(p,p.getBoundingBox().expandTowards(at.subtract(p.position())));}
    private static Vec3 aim(Player p,double range){Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(range));var hit=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return hit.getType()==HitResult.Type.MISS?to:hit.getLocation();}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player)&&!(e instanceof Player other&&poisonImmune(other));}
    private static boolean hit(Player p,LivingEntity e,float damage){return enemy(p,e)&&ZoanPowers.poweredHit(p,e,"gas",p.damageSources().playerAttack(p),damage);}
    private static void expose(Player p,LivingEntity e,boolean dense){
        if(!enemy(p,e)||!p.hasLineOfSight(e)||ZoanPowers.evadePower(p,e,"gas"))return;
        if(hit(p,e,scale(p,1,1,2))){e.addEffect(new MobEffectInstance(MobEffects.POISON,60,0,false,true,true),p);if(dense){e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,30,0,false,true,true),p);if(e instanceof Player&&now(p)>=nauseaReady.getOrDefault(e.getUUID(),0L)){e.addEffect(new MobEffectInstance(MobEffects.NAUSEA,100,0,false,true,true),p);nauseaReady.put(e.getUUID(),now(p)+200);}}}
    }
    private static void cloud(Player p,Vec3 at,double radius,int ticks,boolean dense){
        if(!p.level().hasChunkAt(BlockPos.containing(at)))return;
        List<Cloud> owned=clouds.stream().filter(c->c.owner==p).toList();if(owned.size()>=12)remove(owned.getFirst());if(clouds.size()>=96)return;
        Cloud c=new Cloud();c.owner=p;c.level=(ServerLevel)p.level();c.at=at;c.radius=radius;c.ends=now(p)+ticks;c.dense=dense;c.visual=display(p,at,1,radius);clouds.add(c);
    }
    private static void remove(Cloud c){if(c.visual!=null)c.visual.discard();clouds.remove(c);}
    private static void remove(Shot q){if(q.visual!=null)q.visual.discard();shots.remove(q);}
    private static Display.BlockDisplay display(Player p,Vec3 at,int kind,double radius){ServerLevel l=(ServerLevel)p.level();var d=EntityTypes.BLOCK_DISPLAY.create(l,EntitySpawnReason.TRIGGERED);if(d==null)return null;d.addTag("cb_temporary_rock");d.setCustomName(Component.literal("cb_gas_"+kind));d.setCustomNameVisible(false);d.setBlockState(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());var access=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;access.creeperborn$interpolation(2);access.creeperborn$view(8);d.snapTo(at.x,at.y,at.z);d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f((float)radius,kind==2?.6f:1.2f,(float)radius),new org.joml.Quaternionf()));return l.addFreshEntity(d)?d:null;}
    private static void puff(ServerLevel l,Vec3 at,int color,int n,double spread){l.sendParticles(new DustParticleOptions(color,1.2f),at.x,at.y,at.z,n,spread,Math.min(.5,spread),spread,.01);}
    private static void startFlight(State s){Player p=s.p;s.flight=true;s.granted=!p.getAbilities().mayfly;s.oldFlying=p.getAbilities().flying;s.oldSpeed=p.getAbilities().getFlyingSpeed();p.addTag("cb_gas_flight_"+s.granted+"_"+s.oldSpeed+"_"+s.oldFlying);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.getAbilities().setFlyingSpeed(.065f);p.onUpdateAbilities();p.resetFallDistance();}
    public static void recover(Player p){for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("cb_gas_flight_")){try{String[] v=tag.substring("cb_gas_flight_".length()).split("_");if(Boolean.parseBoolean(v[0])&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;}else if(!p.isSpectator())p.getAbilities().flying=Boolean.parseBoolean(v[2]);p.getAbilities().setFlyingSpeed(Float.parseFloat(v[1]));p.onUpdateAbilities();}catch(RuntimeException ignored){}p.removeTag(tag);}}
    private static void endBody(State s){if(s.bodyEnds==0&&!s.flight)return;s.bodyEnds=0;s.flight=false;recover(s.p);s.p.resetFallDistance();landing.put(s.p.getUUID(),now(s.p)+80);}
    public static void tickPlayer(Player p){State s=states.get(p.getUUID());if(s==null){if(selected(p)&&usable(p)){p.removeEffect(MobEffects.POISON);}return;}if(!active(s)){forget(p);return;}long t=now(p);if(s.held>=0&&(t>s.heartbeat||!selected(p)))s.held=-1;if(poisonImmune(p))p.removeEffect(MobEffects.POISON);if(s.bodyEnds>0){if(t>=s.bodyEnds||p.getFoodData().getFoodLevel()<=6)endBody(s);else{p.resetFallDistance();if(t%40==0)p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);if(t%3==0){Vec3 at=p.position().add(0,.4,0);puff(s.level,at,0xB8DD88,3,.45);if(s.last!=null&&s.last.distanceToSqr(p.position())>.03)puff(s.level,s.last.add(0,.35,0),0xA978C6,2,.3);}}}
        if(s.veilEnds>t&&t%20==0)for(var e:s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(4)))if(e.distanceToSqr(p)<16)expose(p,e,false);
        if(s.veilEnds>t&&t%5==0)puff(s.level,p.position().add(0,.7,0),0xAE82CB,5,1.2);
        if(selected(p)&&t%20==0&&(s.last!=null&&s.last.distanceToSqr(p.position())>.04||t-s.recent<200))Mastery.set(p,"cb_gas_time_",Mastery.get(p,"cb_gas_time_")+1);s.last=p.position();
    }
    public static void effects(){
        for(Shot q:List.copyOf(shots)){if(!active(states.get(q.owner.getUUID()))||q.owner.level()!=q.level||q.life--<=0){remove(q);continue;}Vec3 next=q.at.add(q.dir.scale(1.25));if(!q.level.hasChunkAt(BlockPos.containing(next))){remove(q);continue;}var wall=q.level.clip(new ClipContext(q.at,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,q.owner));Vec3 end=wall.getType()==HitResult.Type.MISS?next:wall.getLocation();LivingEntity target=null;double near=q.at.distanceTo(end)+.01;for(var e:q.level.getEntitiesOfClass(LivingEntity.class,new AABB(q.at,end).inflate(.5)))if(enemy(q.owner,e)){var cross=e.getBoundingBox().inflate(.3).clip(q.at,end);if(cross.isPresent()&&cross.get().distanceTo(q.at)<near){near=cross.get().distanceTo(q.at);target=e;}}if(target!=null){end=q.at.add(q.dir.scale(near));if(hit(q.owner,target,scale(q.owner,6,9,12)))target.addEffect(new MobEffectInstance(MobEffects.POISON,80,0),q.owner);}if(target!=null||wall.getType()!=HitResult.Type.MISS){cloud(q.owner,end.add(0,-.5,0),1.6,60,false);remove(q);continue;}q.at=next;if(q.visual!=null)q.visual.snapTo(next.x,next.y,next.z);puff(q.level,next,0xB8DD88,1,.1);}
        Map<UUID,Set<UUID>> exposed=new HashMap<>();Map<UUID,Set<UUID>> blasted=new HashMap<>();
        for(Cloud c:List.copyOf(clouds)){if(!active(states.get(c.owner.getUUID()))||c.owner.level()!=c.level||now(c.owner)>=c.ends||!c.level.hasChunkAt(BlockPos.containing(c.at))){remove(c);continue;}long t=now(c.owner);if(c.ignites>0&&t>=c.ignites){Set<UUID> hit=blasted.computeIfAbsent(c.owner.getUUID(),id->new HashSet<>());for(var e:c.level.getEntitiesOfClass(LivingEntity.class,new AABB(c.at,c.at).inflate(c.radius+1,3,c.radius+1)))if(enemy(c.owner,e)&&e.position().distanceToSqr(c.at)<Math.pow(c.radius+1,2)&&c.level.clip(new ClipContext(c.at.add(0,1,0),e.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.owner)).getType()==HitResult.Type.MISS&&hit.add(e.getUUID()))hit(c.owner,e,scale(c.owner,10,16,24));puff(c.level,c.at.add(0,.6,0),0xFFB24C,20,c.radius*.5);remove(c);continue;}if(t%20==0){Set<UUID> hit=exposed.computeIfAbsent(c.owner.getUUID(),id->new HashSet<>());for(var e:c.level.getEntitiesOfClass(LivingEntity.class,new AABB(c.at,c.at).inflate(c.radius,2,c.radius)))if(e.position().multiply(1,0,1).distanceToSqr(c.at.multiply(1,0,1))<=c.radius*c.radius&&hit.add(e.getUUID()))expose(c.owner,e,c.dense);}if(t%8==0)puff(c.level,c.at.add(0,.6,0),c.ignites>0?0xFFB24C:0xAECA8B,4,c.radius*.5);}
    }
    private static int flags(Player p){return (body(p)?1:0)|(veil(p)?2:0)|(tier(p)<<2);}
    private static void sync(MinecraftServer server){for(var viewer:server.getPlayerList().getPlayers()){if(!ServerPlayNetworking.canSend(viewer,GasVisualPayload.TYPE))continue;Map<UUID,View> previous=sent.computeIfAbsent(viewer.getUUID(),id->new HashMap<>()),next=new HashMap<>();for(var p:server.getPlayerList().getPlayers()){int flags=flags(p);if((flags&3)==0||p.level()!=viewer.level()||p.distanceToSqr(viewer)>128*128)continue;View v=new View(p.getId(),flags);next.put(p.getUUID(),v);if(!v.equals(previous.get(p.getUUID()))||now(p)%40==0)ServerPlayNetworking.send(viewer,new GasVisualPayload(v.entity(),v.flags()));}for(var entry:previous.entrySet())if(!next.containsKey(entry.getKey()))ServerPlayNetworking.send(viewer,new GasVisualPayload(entry.getValue().entity(),0));sent.put(viewer.getUUID(),next);}}
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())tickPlayer(p);effects();sync(server);nauseaReady.entrySet().removeIf(e->e.getValue()<server.overworld().getGameTime()-200);landing.entrySet().removeIf(e->e.getValue()<server.overworld().getGameTime());}
    public static void tickVisual(Entity e){if(!(e.level() instanceof ServerLevel)||e.getCustomName()==null||!e.getCustomName().getString().startsWith("cb_gas_")||e.tickCount<=2)return;if(clouds.stream().noneMatch(c->c.visual==e)&&shots.stream().noneMatch(q->q.visual==e))e.discard();}
    public static int cloudCount(Player p){return (int)clouds.stream().filter(c->c.owner==p).count();}
    public static int shotCount(){return shots.size();}
    public static void forget(Player p){State s=states.remove(p.getUUID());if(s!=null){endBody(s);s.veilEnds=0;}else recover(p);for(Cloud c:List.copyOf(clouds))if(c.owner==p)remove(c);for(Shot q:List.copyOf(shots))if(q.owner==p)remove(q);sent.remove(p.getUUID());}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.p);for(Cloud c:List.copyOf(clouds))remove(c);for(Shot q:List.copyOf(shots))remove(q);states.clear();sent.clear();nauseaReady.clear();landing.clear();}
    public static String status(Player p){return unlocked(p)?"Gas Gas T"+tier(p)+" | Z Gas Shot / hold Choking Cloud | X Gas Dash / hold Poison Veil | C Combustion / hold Gas Body | poison immunity":"Gas Gas: locked";}
}
