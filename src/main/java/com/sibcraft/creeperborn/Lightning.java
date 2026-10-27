package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class Lightning {
    private static final class State {
        Player owner;ServerLevel level;int held=-1;long press,heartbeat,recent=-1000,attackReady,flareUntil,stormUntil,rushStart;
        boolean flight,granted,oldFlying;float oldSpeed;Vec3 last;
    }
    private record View(int entity,int flags){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Map<UUID,View>> sent=new HashMap<>();
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;return s;}
    private static long now(Player p){return p.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_lightning")&&FruitClaims.ownsLightning(p);}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"lightning");}
    public static int uses(Player p){return Mastery.get(p,"cb_lightning_uses_");}
    public static int seconds(Player p){return Mastery.get(p,"cb_lightning_time_");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(uses(p),seconds(p)):0;}
    public static void grant(Player p,int tier){
        Mastery.set(p,"cb_lightning_uses_",tier==3?40:tier==2?10:0);
        Mastery.set(p,"cb_lightning_time_",tier==3?3600:tier==2?300:0);
    }
    public static boolean consume(Player p,ItemStack item){
        if(item.isEmpty()||!Relics.kind(item).equals("fruit:lightning")||!FruitClaims.claimItem(p,item,"lightning"))return false;
        p.addTag("cb_fruit_lightning");p.addTag("cb_mode_fruit");grant(p,Relics.captureTier(item));if(Dark.unlocked(p))Dark.choose(p,"lightning");item.shrink(1);return true;
    }
    public static boolean storm(Player p){State s=states.get(p.getUUID());return s!=null&&p.isAlive()&&s.stormUntil>now(p);}
    public static int duration(Player p){return Mastery.harmony(p)?1800:1200;}
    public static int range(Player p,boolean charged){return charged?storm(p)?64:50:storm(p)?50:30;}
    public static int flashCapacity(Player p){return Mastery.harmony(p)?5:3;}
    public static int flashCharges(Player p){
        if(now(p)>=ready(p,"flash"))Mastery.set(p,"cb_lightning_flash_spent_",0);
        return Math.max(0,flashCapacity(p)-Mastery.get(p,"cb_lightning_flash_spent_"));
    }
    public static long ready(Player p,String action){
        String prefix="cb_lightning_"+action+"_ready_";
        for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    private static void cooldown(Player p,String action,int ticks){
        String prefix="cb_lightning_"+action+"_ready_";
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);
        p.addTag(prefix+(now(p)+ticks));
    }
    private static void credit(Player p){
        State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_lightning_uses_",uses(p)+1);s.recent=now(p);
    }
    public static void press(Player p,int move){
        if(!selected(p)||!p.isAlive()||p.isSpectator()||WaterRules.suppressed(p)||move<0||move>2)return;
        State s=state(p);if(s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;s.level=(ServerLevel)p.level();
    }
    public static int chargeTicks(Player p){
        State s=states.get(p.getUUID());return s!=null&&s.held==0?(int)Math.min(160,Math.max(0,now(p)-s.press)):0;
    }
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null&&s.held>=0)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s==null)return;
        if(s.rushStart>0){cooldown(p,"rush",160);Rubber.impulse(p,Vec3.ZERO);}s.rushStart=0;s.held=-1;
    }
    public static void release(Player p,int move){
        State s=states.get(p.getUUID());if(s==null||s.held!=move)return;
        long held=now(p)-s.press;boolean rushed=s.rushStart>0;cancelInput(p);
        if(!selected(p)||!p.isAlive()||p.isSpectator())return;
        if(move==1&&held>=7){if(!rushed)Creeperborn.say(p,tier(p)<2?"Lightning movement needs tier 2.":"Lightning Rush recovering.");return;}
        Creeperborn.say(p,cast(p,move,held));
    }
    public static String cast(Player p,int move,long held){
        if(Dark.suppressed(p))return "Fruit powers suppressed by darkness.";
        if(!selected(p)||!p.isAlive()||p.isSpectator())return "Select Lightning with G first.";
        if(WaterRules.suppressed(p))return "Fruit weakness: Lightning disabled while submerged.";
        State s=state(p);
        if(move==0){
            boolean charged=held>=160;
            if(held>=7&&!charged)return "Thunder Judgment cancelled: charge for eight seconds.";
            if(now(p)<s.attackReady)return "Lightning attack recovering.";
            s.attackReady=now(p)+(charged?300:10);s.flareUntil=now(p)+12;
            strike(p,charged);return charged?"THUNDER JUDGMENT!":storm(p)?"Directed Lightning!":"Lightning Bolt!";
        }
        if(move==1){
            if(tier(p)<2)return "Electric movement needs 10 uses + five active minutes.";
            if(p.isPassenger())return "Dismount before using electric movement.";
            if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to use electric movement.";
            if(flashCharges(p)<=0)return "Flash Step recovering: "+((ready(p,"flash")-now(p)+19)/20)+"s.";
            Vec3 from=p.position(),to=flashDestination(p,20);
            if(to.distanceToSqr(from)<.25)return "No clear space to Flash Step.";
            beam((ServerLevel)p.level(),p.getEyePosition(),to.add(0,p.getEyeHeight(),0),30);
            if(p instanceof ServerPlayer sp)sp.teleportTo((ServerLevel)p.level(),to.x,to.y,to.z,Set.of(),p.getYRot(),p.getXRot(),false);
            else p.snapTo(to.x,to.y,to.z);
            p.setDeltaMovement(Vec3.ZERO);Mastery.set(p,"cb_lightning_flash_spent_",Mastery.get(p,"cb_lightning_flash_spent_")+1);cooldown(p,"flash",60);credit(p);s.flareUntil=now(p)+10;
            return "Flash Step! "+flashCharges(p)+"/"+flashCapacity(p)+" charges";
        }
        if(move==2){
            if(storm(p)){end(p);return "Storm Form ended.";}
            if(tier(p)<3)return "Storm Form needs 40 uses + one active hour.";
            if(now(p)<ready(p,"storm"))return "Storm Form recovering.";
            if(p.isPassenger())return "Dismount before transforming.";
            if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to transform.";
            s.level=(ServerLevel)p.level();s.stormUntil=now(p)+duration(p);s.flight=true;
            s.granted=!p.getAbilities().mayfly;s.oldFlying=p.getAbilities().flying;s.oldSpeed=p.getAbilities().getFlyingSpeed();
            p.addTag("cb_lightning_flight_"+s.granted+"_"+s.oldSpeed+"_"+s.oldFlying);
            p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.getAbilities().setFlyingSpeed(.065f);p.onUpdateAbilities();
            credit(p);s.flareUntil=now(p)+20;
            return "Storm Form! Aim and tap Z to direct lightning; hold Z eight seconds for Judgment.";
        }
        return "";
    }
    public static Vec3 flashDestination(Player p,int blocks){
        ServerLevel level=(ServerLevel)p.level();Vec3 from=p.position(),last=from,look=p.getLookAngle();
        for(int i=1;i<=blocks*4;i++){
            Vec3 candidate=from.add(look.scale(i*.25));
            if(candidate.y<level.getMinY()||candidate.y+p.getBbHeight()>level.getMaxY()
                ||!level.hasChunkAt(BlockPos.containing(candidate))||!level.getWorldBorder().isWithinBounds(BlockPos.containing(candidate))
                ||!level.noCollision(p,p.getBoundingBox().move(candidate.subtract(from))))break;
            last=candidate;
        }
        return last;
    }
    private static boolean enemy(Player p,LivingEntity e){
        return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player);
    }
    private static LivingEntity aimed(Player p,Vec3 from,Vec3 end){
        LivingEntity best=null;double distance=from.distanceTo(end)+.01;
        for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.7))){
            if(!enemy(p,e))continue;var hit=e.getBoundingBox().inflate(.45).clip(from,end);
            if(hit.isPresent()&&hit.get().distanceTo(from)<distance){best=e;distance=hit.get().distanceTo(from);}
        }
        return best;
    }
    private static final ThreadLocal<DamageSource> electricSource=new ThreadLocal<>();
    public static boolean electrical(DamageSource source){return source==electricSource.get();}
    static boolean hit(Player p,LivingEntity e,float amount){
        if(e instanceof Player target&&Rubber.unlocked(target)&&Rubber.gear(target))return false;
        DamageSource source=p.damageSources().playerAttack(p),previous=electricSource.get();
        electricSource.set(source);
        try{return e.hurtServer((ServerLevel)p.level(),source,amount);}
        finally{if(previous==null)electricSource.remove();else electricSource.set(previous);}
    }
    private static void strike(Player p,boolean charged){
        ServerLevel level=(ServerLevel)p.level();Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(range(p,charged)));
        BlockHitResult wall=level.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
        LivingEntity target=aimed(p,from,end);
        if(target!=null)end=target.position().add(0,target.getBbHeight()*.5,0);
        if(!level.hasChunkAt(BlockPos.containing(end)))return;
        double strength=storm(p)&&Mastery.harmony(p)?1.25:1;
        float damage=(float)((charged?(storm(p)?40:24):(storm(p)?14:8))*strength);
        if(!charged&&!storm(p)){
            LightningBolts.launch(p,LightningBolts.hand(p),end,true);
        }else beam(level,from,end,charged?80:40);
        boolean changed=false;
        if(charged||storm(p)){
            LightningBolt visual=EntityTypes.LIGHTNING_BOLT.create(level,EntitySpawnReason.EVENT);
            if(visual!=null){visual.setVisualOnly(true);visual.snapTo(end.x,end.y,end.z);level.addFreshEntity(visual);}
            int radius=charged?5:2;
            for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AABB(end,end).inflate(radius))){
                if(enemy(p,e)&&e.position().distanceToSqr(end)<=radius*radius&&visible(level,p,end,e.getEyePosition()))changed|=hit(p,e,damage);
            }
        }else if(target!=null){
            changed=hit(p,target,damage);
            final LivingEntity first=target;final Vec3 center=target.position();
            List<LivingEntity> nearby=new ArrayList<>(level.getEntitiesOfClass(LivingEntity.class,target.getBoundingBox().inflate(5)));
            nearby.sort(Comparator.comparingDouble(e->e.distanceToSqr(center)));
            int count=0;
            for(LivingEntity e:nearby)if(e!=first&&enemy(p,e)&&e.distanceToSqr(first)<=25&&first.hasLineOfSight(e)){
                LightningBolts.launch(p,first.getEyePosition(),e.getEyePosition(),false);changed|=hit(p,e,damage*.5f);if(++count>=2)break;
            }
        }
        if(changed)credit(p);
    }
    private static boolean visible(ServerLevel level,Player p,Vec3 from,Vec3 end){
        return level.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p)).getType()==HitResult.Type.MISS;
    }
    private static void beam(ServerLevel level,Vec3 from,Vec3 to,int count){
        Vec3 delta=to.subtract(from);
        for(int i=0;i<=count;i++){
            Vec3 at=from.add(delta.scale(i/(double)count));
            level.sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,1,.05,.05,.05,.005);
        }
    }
    public static boolean immune(Player p,DamageSource source){
        if(!unlocked(p)||Phoenix.bypass(source))return false;
        if(source.is(DamageTypes.LIGHTNING_BOLT))return true;
        if(!storm(p))return false;
        if(source.is(net.minecraft.tags.DamageTypeTags.IS_FALL))return true;
        return Mastery.harmony(p)&&!Phoenix.masteredAttacker(source);
    }
    public static float damage(Player p,DamageSource source,float amount){
        if(immune(p,source))return 0;
        return storm(p)&&!Phoenix.bypass(source)?amount*.5f:amount;
    }
    public static void end(Player p){
        State s=states.get(p.getUUID());if(s==null)return;
        if(s.stormUntil>0)cooldown(p,"storm",900);
        s.stormUntil=0;
        if(s.flight){
            if(s.granted&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;}
            else if(!p.isSpectator())p.getAbilities().flying=s.oldFlying;
            p.getAbilities().setFlyingSpeed(s.oldSpeed);p.onUpdateAbilities();s.flight=false;
        }
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith("cb_lightning_flight_"))p.removeTag(t);
    }
    public static void recoverFlight(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("cb_lightning_flight_")){
            String[] v=tag.substring("cb_lightning_flight_".length()).split("_");
            try{
                if(Boolean.parseBoolean(v[0])&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;}
                else if(!p.isSpectator())p.getAbilities().flying=Boolean.parseBoolean(v[2]);
                p.getAbilities().setFlyingSpeed(Float.parseFloat(v[1]));p.onUpdateAbilities();
            }catch(RuntimeException ignored){}
            p.removeTag(tag);
        }
    }
    public static int visualFlags(Player p){
        State s=states.get(p.getUUID());return s==null?0:(storm(p)?1:0)|(s.held==0&&now(p)-s.press>=7?2:0)|(now(p)<s.flareUntil?4:0);
    }
    public static String status(Player p){
        return "Lightning "+(unlocked(p)?"tier "+tier(p)+"/3 | "+uses(p)+" uses | "+seconds(p)+"s":"locked")
            +" | Storm: "+(storm(p)?((state(p).stormUntil-now(p)+19)/20)+"s":"off");
    }
    public static void tickPlayer(Player p){
        State s=state(p);long now=now(p);
        if(!unlocked(p)||!p.isAlive()||p.isSpectator()||WaterRules.suppressed(p)||s.level!=null&&s.level!=p.level()){end(p);cancelInput(p);return;}
        if(s.held>=0&&(!selected(p)||now>s.heartbeat))cancelInput(p);
        if(s.stormUntil>0&&(now>=s.stormUntil||tier(p)<3||p.getFoodData().getFoodLevel()<=6))end(p);
        if(s.held==0&&now-s.press>=7&&now%5==0){
            double progress=Math.min(1,(now-s.press)/160.0);
            Vec3 at=p.getEyePosition();((ServerLevel)p.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y,at.z,(int)(4+progress*12),.7,.5,.7,.015);
            Creeperborn.say(p,"Thunder Judgment "+Math.min(100,(now-s.press)*100/160)+"% — "+(progress>=1?"RELEASE Z!":"hold for eight seconds"));
        }
        if(s.held==1&&now-s.press>=7){
            if(tier(p)<2||p.isPassenger()||p.getFoodData().getFoodLevel()<=6||!selected(p)){cancelInput(p);}
            else if(s.rushStart==0&&now>=ready(p,"rush"))s.rushStart=now;
            if(s.rushStart>0){
                if(now-s.rushStart>=60){cancelInput(p);Creeperborn.say(p,"Lightning Rush ended.");}
                else{
                    Vec3 next=flashDestination(p,2),delta=next.subtract(p.position());
                    if(delta.lengthSqr()<.01)cancelInput(p);
                    else{if(now==s.rushStart)credit(p);Rubber.impulse(p,delta.normalize().scale(Math.min(1.6,delta.length())));if(now%5==0)beam((ServerLevel)p.level(),p.getEyePosition(),p.getEyePosition().subtract(p.getLookAngle()),5);}
                }
            }
        }
        if(now%20==0){
            boolean active=s.last!=null&&s.last.distanceToSqr(p.position())>.04||now-s.recent<200;s.last=p.position();
            if(active&&selected(p)){int before=tier(p);Mastery.set(p,"cb_lightning_time_",seconds(p)+1);if(tier(p)>before)Creeperborn.say(p,"Lightning mastery increased!");}
            if(storm(p)||s.rushStart>0){
                int fuel=Mastery.get(p,"cb_lightning_fuel_")+1,limit=s.rushStart>0?2:4;
                if(fuel>=limit){fuel=0;p.getFoodData().setFoodLevel(Math.max(6,p.getFoodData().getFoodLevel()-1));}
                Mastery.set(p,"cb_lightning_fuel_",fuel);
            }
            if(storm(p))Creeperborn.say(p,"Storm Form "+((s.stormUntil-now+19)/20)+"s | Food "+p.getFoodData().getFoodLevel()+"/20 | Z aims lightning");
        }
        if(storm(p)&&now%5==0){
            Vec3 at=p.position().add(0,1.5,0);
            ((ServerLevel)p.level()).sendParticles(ParticleTypes.CLOUD,at.x,at.y,at.z,16,4.8,.6,4.8,.01);
            ((ServerLevel)p.level()).sendParticles(ParticleTypes.ELECTRIC_SPARK,at.x,at.y-.5,at.z,12,4.2,.3,4.2,.02);
        }
        if(p.getFoodData().getFoodLevel()<=6){end(p);if(s.rushStart>0||s.held==1)cancelInput(p);}
    }
    public static void tick(MinecraftServer server){
        LightningBolts.tick();
        for(Player p:server.getPlayerList().getPlayers())tickPlayer(p);
        for(var viewer:server.getPlayerList().getPlayers()){
            if(!ServerPlayNetworking.canSend(viewer,LightningVisualPayload.TYPE))continue;
            Map<UUID,View> cache=sent.computeIfAbsent(viewer.getUUID(),id->new HashMap<>());
            cache.entrySet().removeIf(e->{if(server.getPlayerList().getPlayer(e.getKey())!=null)return false;
                ServerPlayNetworking.send(viewer,new LightningVisualPayload(e.getValue().entity(),0));return true;});
            for(var subject:server.getPlayerList().getPlayers()){
                View next=new View(subject.getId(),subject.level()==viewer.level()?visualFlags(subject):0),old=cache.get(subject.getUUID());
                if(!next.equals(old)){
                    if(old!=null&&old.entity()!=next.entity())ServerPlayNetworking.send(viewer,new LightningVisualPayload(old.entity(),0));
                    ServerPlayNetworking.send(viewer,new LightningVisualPayload(next.entity(),next.flags()));cache.put(subject.getUUID(),next);
                }
            }
        }
    }
    public static void forget(Player p){LightningBolts.cancel(p);end(p);cancelInput(p);states.remove(p.getUUID());sent.remove(p.getUUID());}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.owner);states.clear();sent.clear();LightningBolts.clear();}
}
