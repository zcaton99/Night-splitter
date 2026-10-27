package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import net.minecraft.core.particles.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

public final class Phoenix {
    public static final int WING_TICKS=600,FORM_TICKS=1800,REVIVAL_RECOVERY=300;
    private static final class State {
        Player owner;ServerLevel level;long wingUntil,formUntil,attackReady,heldAt=-1,heartbeat,flareUntil,recent=-10000,credit=-10000,shieldUntil;
        int held=-1,diveTicks,healFuel;Vec3 diveDirection,lastPosition;
        boolean grantedFlight,oldFlying,flight,healing,blessAttempted;float oldSpeed;
    }
    private static final class Blessing{
        Player owner;LivingEntity target;long expires,next;float heal;
        Blessing(Player owner,LivingEntity target,float heal){this.owner=owner;this.target=target;this.heal=heal;this.expires=owner.level().getGameTime()+200;this.next=owner.level().getGameTime()+40;}
    }
    private static final Map<UUID,Blessing> blessings=new HashMap<>();
    private record View(int entity,int flags){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Map<UUID,View>> sent=new HashMap<>();
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;return s;}
    private static long now(Player p){return p.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_phoenix")&&FruitClaims.ownsPhoenix(p);}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit");}
    public static int uses(Player p){return Mastery.get(p,"cb_phoenix_uses_");}
    public static int seconds(Player p){return Mastery.get(p,"cb_phoenix_time_");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(uses(p),seconds(p)):0;}
    public static void grant(Player p,int tier){
        Mastery.set(p,"cb_phoenix_uses_",tier==3?40:tier==2?10:0);
        Mastery.set(p,"cb_phoenix_time_",tier==3?3600:tier==2?300:0);
    }
    public static boolean consume(Player p,ItemStack item){
        if(item.isEmpty()||!Relics.kind(item).equals("fruit:phoenix")||!FruitClaims.claimPhoenix(p))return false;
        p.addTag("cb_fruit_phoenix");p.addTag("cb_mode_fruit");grant(p,1);item.shrink(1);return true;
    }
    public static boolean form(Player p){State s=states.get(p.getUUID());return s!=null&&p.isAlive()&&s.formUntil>now(p);}
    public static boolean wing(Player p){State s=states.get(p.getUUID());return s!=null&&p.isAlive()&&s.wingUntil>now(p);}
    public static boolean healing(Player p){State s=states.get(p.getUUID());return s!=null&&s.healing;}
    public static boolean spent(Player p){return p.entityTags().contains("cb_phoenix_revived");}
    public static int recovery(Player p){return Mastery.get(p,"cb_phoenix_recovery_");}
    public static double strength(Player p){return recovery(p)>0?.5:1;}
    public static long ready(Player p,boolean full){
        String prefix=full?"cb_phoenix_form_ready_":"cb_phoenix_wing_ready_";
        for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    private static void cooldown(Player p,boolean full){
        String prefix=full?"cb_phoenix_form_ready_":"cb_phoenix_wing_ready_";
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);
        p.addTag(prefix+(now(p)+(full?600:100)));
    }
    public static void credit(Player p){
        State s=state(p);long now=now(p);if(!unlocked(p)||now-s.credit<20)return;
        s.credit=s.recent=now;Mastery.set(p,"cb_phoenix_uses_",uses(p)+1);
    }
    public static void press(Player p,int move){
        if(!selected(p)||!p.isAlive()||p.isSpectator()||WaterRules.suppressed(p)||move<0||move>2)return;
        State s=state(p);if(s.held>=0)return;s.level=(ServerLevel)p.level();s.held=move;s.blessAttempted=false;s.heldAt=now(p);s.heartbeat=now(p)+15;
    }
    public static void heartbeat(Player p){State s=state(p);if(s.held>=0)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=state(p);s.held=-1;s.heldAt=-1;s.healing=false;}
    public static void release(Player p,int move){
        State s=state(p);if(s.held!=move)return;long held=now(p)-s.heldAt;boolean healing=s.healing,blessed=s.blessAttempted;cancelInput(p);s.healing=healing;
        if(move==2&&held>=7&&blessed)return;
        if(!selected(p)||!p.isAlive()||p.isSpectator())return;
        String text=cast(p,move,held);if(!text.isEmpty())Creeperborn.say(p,text);
    }
    public static String cast(Player p,int move,long held){
        if(!selected(p)||!p.isAlive()||p.isSpectator())return "Select Phoenix with G first.";
        if(WaterRules.suppressed(p))return "Fruit weakness: Phoenix powers are disabled while submerged.";
        if(move<0||move>2)return "";
        State s=state(p);boolean charged=held>=7;
        if(move==0){
            if(s.healing)return "Tap C to stop Healing Flames before attacking.";
            if(now(p)<s.attackReady)return "Phoenix attack recovering.";
            s.attackReady=now(p)+(charged?100:20);s.recent=now(p);s.flareUntil=now(p)+10;
            if(charged){s.level=(ServerLevel)p.level();s.diveTicks=9;s.diveDirection=p.getLookAngle().normalize();return "Phoenix Dive!";}
            talon(p,s);return "Phoenix Talon!";
        }
        if(move==1){
            if(tier(p)<(charged?3:2))return charged?"Phoenix Form needs 40 uses + one active hour.":"Wing Flight needs 10 uses + five active minutes.";
            boolean active=charged?form(p):wing(p)||form(p);
            if(active){end(p);return "Phoenix flight ended.";}
            long wait=ready(p,charged)-now(p);if(wait>0)return "Phoenix recovering: "+((wait+19)/20)+"s.";
            if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to fly.";
            if(wing(p)||form(p))end(p);
            s.level=(ServerLevel)p.level();
            if(charged)s.formUntil=Mastery.harmony(p)?Long.MAX_VALUE:now(p)+FORM_TICKS;
            else s.wingUntil=now(p)+(Mastery.harmony(p)?900:WING_TICKS);
            grantFlight(p,s);s.flareUntil=now(p)+20;particles(p,30);
            return charged?"Phoenix Form! Tap C to heal nearby players and villagers.":"Wing Flight! Tap X again to land.";
        }
        if(charged)return bless(p);
        if(tier(p)<3)return "Healing Flames needs 40 uses + one active hour.";
        if(s.healing){s.healing=false;return "Healing Flames stopped.";}
        if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to heal.";
        s.healing=true;s.level=(ServerLevel)p.level();s.recent=now(p);
        return "Healing Flames! Tap C again to stop.";
    }
    private static boolean friendly(Player p,LivingEntity target){
        return target instanceof Villager||target instanceof Player&&(p.getTeam()==null||p.isAlliedTo(target));
    }
    private static String bless(Player p){
        if(tier(p)<3)return "Phoenix Blessing needs 40 uses + one active hour.";
        if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to bless teammates.";
        long ready=0;String prefix="cb_phoenix_bless_ready_";
        for(String t:p.entityTags())if(t.startsWith(prefix))try{ready=Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}
        if(now(p)<ready)return "Phoenix Blessing recovering: "+((ready-now(p)+19)/20)+"s.";
        int count=0;float heal=(float)(1.5*strength(p)*(Mastery.harmony(p)?1.25:1));
        for(LivingEntity target:((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10))){
            if(target==p||!friendly(p,target)||!target.isAlive()||target.isSpectator()||target.distanceToSqr(p)>100||!p.hasLineOfSight(target))continue;
            blessings.put(target.getUUID(),new Blessing(p,target,heal));count++;
        }
        if(count==0)return "No nearby teammates or villagers to bless.";
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);
        p.addTag(prefix+(now(p)+200));p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);credit(p);particles(p,20);
        return "Phoenix Blessing! "+count+" allies heal for 10s, even outside your circle.";
    }
    public static int blessings(){return blessings.size();}
    public static void tickBlessings(){
        blessings.values().removeIf(b->{
            long now=b.target.level().getGameTime();
            if(!b.target.isAlive()||b.target.isRemoved()||now>b.expires)return true;
            if(now<b.next)return false;b.next=now+40;
            State owner=states.get(b.owner.getUUID());
            boolean inside=owner!=null&&owner.healing&&b.owner.isAlive()&&b.owner.level()==b.target.level()
                &&b.target.distanceToSqr(b.owner)<=100&&b.owner.hasLineOfSight(b.target);
            if(!inside)b.target.heal(b.heal);
            if(b.target.level() instanceof ServerLevel level){
                Vec3 at=b.target.getEyePosition();level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,4,.2,.3,.2,.01);
            }
            return now>=b.expires;
        });
    }
    private static void grantFlight(Player p,State s){
        if(s.flight)return;s.flight=true;s.grantedFlight=!p.getAbilities().mayfly;
        s.oldSpeed=p.getAbilities().getFlyingSpeed();s.oldFlying=p.getAbilities().flying;
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith("cb_phoenix_flight_"))p.removeTag(t);
        p.addTag("cb_phoenix_flight_"+s.grantedFlight+"_"+s.oldSpeed+"_"+s.oldFlying);
        p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.getAbilities().setFlyingSpeed(form(p)?.075f:.05f);p.onUpdateAbilities();
    }
    public static void end(Player p){
        State s=states.get(p.getUUID());if(s==null)return;
        if(s.formUntil>0)cooldown(p,true);if(s.wingUntil>0)cooldown(p,false);
        s.formUntil=s.wingUntil=0;s.healing=false;s.diveTicks=0;
        s.level=null;
        if(s.flight){
            if(s.grantedFlight&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;}
            else if(!p.isSpectator())p.getAbilities().flying=s.oldFlying;
            p.getAbilities().setFlyingSpeed(s.oldSpeed);p.onUpdateAbilities();s.flight=false;
        }
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith("cb_phoenix_flight_"))p.removeTag(t);
    }
    public static void recoverFlight(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("cb_phoenix_flight_")){
            String[] v=tag.substring("cb_phoenix_flight_".length()).split("_");
            try{
                if(Boolean.parseBoolean(v[0])&&!p.isCreative()&&!p.isSpectator()){p.getAbilities().mayfly=false;p.getAbilities().flying=false;}
                else if(!p.isSpectator())p.getAbilities().flying=Boolean.parseBoolean(v[2]);
                p.getAbilities().setFlyingSpeed(Float.parseFloat(v[1]));p.onUpdateAbilities();
            }catch(RuntimeException ignored){}
            p.removeTag(tag);
        }
    }
    public static boolean bypass(DamageSource source){return source.is(DamageTypes.GENERIC_KILL)||source.is(DamageTypes.FELL_OUT_OF_WORLD);}
    public static boolean immune(Player p,DamageSource source){
        if(!unlocked(p)||bypass(source))return false;
        if(source.is(DamageTypeTags.IS_FIRE)||source.is(DamageTypeTags.IS_FALL))return true;
        State s=states.get(p.getUUID());
        if(source.is(DamageTypes.DROWN)&&!Mastery.harmony(p))return false;
        return s!=null&&(now(p)<s.shieldUntil||form(p)&&Mastery.harmony(p));
    }
    public static float damage(Player p,DamageSource source,float amount){
        if(!unlocked(p)||bypass(source))return amount;
        if(immune(p,source))return 0;
        return form(p)?amount*.5f:amount;
    }
    public static boolean revive(Player p,DamageSource source){
        State s=states.get(p.getUUID());
        if(s==null||!unlocked(p)||s.formUntil<=now(p)||spent(p)||bypass(source)||source.is(DamageTypes.DROWN)
            ||!Mastery.harmony(p)&&WaterRules.submerged(p))return false;
        p.addTag("cb_phoenix_revived");Mastery.set(p,"cb_phoenix_recovery_",REVIVAL_RECOVERY);
        p.setHealth(p.getMaxHealth()/2);p.clearFire();end(p);cancelInput(p);
        s.shieldUntil=now(p)+60;s.recent=now(p);particles(p,70);Creeperborn.say(p,"Phoenix Rebirth! Half strength for five active minutes. Revival spent.");return true;
    }
    private static void talon(Player p,State s){
        ServerLevel level=(ServerLevel)p.level();Vec3 eye=p.getEyePosition(),look=p.getLookAngle();
        LivingEntity best=null;double distance=6;
        for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(6))){
            if(!(e instanceof Enemy)||!e.isAlive()||!p.hasLineOfSight(e))continue;
            Vec3 delta=e.getEyePosition().subtract(eye);double d=delta.length();
            if(d<=5&&d<distance&&delta.normalize().dot(look)>.55){best=e;distance=d;}
        }
        if(best!=null)hit(p,best,(float)(6*strength(p)));particles(p,12);
    }
    private static void hit(Player p,LivingEntity e,float damage){
        if(e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),damage))credit(p);
        if(e.isAlive()){Vec3 push=e.position().subtract(p.position()).normalize();e.setDeltaMovement(e.getDeltaMovement().add(push.x*.35,.15,push.z*.35));}
    }
    private static void dive(Player p,State s){
        ServerLevel level=(ServerLevel)p.level();Vec3 from=p.getEyePosition(),end=from.add(s.diveDirection.scale(1.8));
        var wall=level.clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        Vec3 limit=wall.getType()==HitResult.Type.MISS?end:wall.getLocation();
        LivingEntity first=null;double nearest=from.distanceTo(limit);
        for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,new AABB(from,limit).inflate(.65))){
            if(!(e instanceof Enemy)||!e.isAlive()||!p.hasLineOfSight(e))continue;
            var cross=e.getBoundingBox().inflate(.4).clip(from,limit);
            if(cross.isPresent()&&cross.get().distanceTo(from)<=nearest){nearest=cross.get().distanceTo(from);first=e;}
        }
        if(first!=null){
            hit(p,first,(float)(10*strength(p)));
            for(LivingEntity e:level.getEntitiesOfClass(LivingEntity.class,first.getBoundingBox().inflate(2)))
                if(e!=first&&e instanceof Enemy&&e.isAlive()&&p.hasLineOfSight(e))hit(p,e,(float)(4*strength(p)));
            s.diveTicks=0;p.setDeltaMovement(Vec3.ZERO);particles(p,25);
        }else if(wall.getType()!=HitResult.Type.MISS||--s.diveTicks<=0){s.diveTicks=0;p.setDeltaMovement(Vec3.ZERO);}
        else Rubber.impulse(p,s.diveDirection.scale(1.8));
    }
    private static void ring(Player p){
        ServerLevel level=(ServerLevel)p.level();Vec3 at=p.position();
        for(int i=0;i<32;i++){double angle=i*Math.PI/16+now(p)*.04;
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x+Math.cos(angle)*10,at.y+.25,at.z+Math.sin(angle)*10,1,0,.1,0,.005);
        }
    }
    private static void particles(Player p,int count){
        if(p.level() instanceof ServerLevel level){
            Vec3 at=p.position().add(0,1,0);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,at.x,at.y,at.z,count,.45,.5,.45,.025);
            if(count>=25)level.sendParticles(new DustParticleOptions(0xFFD34D,1.3f),at.x,at.y,at.z,count/2,.6,.7,.6,.04);
        }
    }
    public static int visualFlags(Player p){
        State s=states.get(p.getUUID());if(s==null)return 0;
        return (wing(p)?1:0)|(form(p)?2:0)|(s.healing?4:0)|(now(p)<s.flareUntil?8:0);
    }
    public static String status(Player p){
        return "Phoenix "+(unlocked(p)?"tier "+tier(p)+"/3 | "+uses(p)+" uses | "+seconds(p)+"s":"locked")
            +" | Form: "+(form(p)?Mastery.harmony(p)?"unlimited":((state(p).formUntil-now(p)+19)/20)+"s":"off")
            +" | Revival: "+(spent(p)?"spent":"unused")+" | Recovery: "+recovery(p)+" active seconds";
    }
    public static void tickPlayer(Player p){
        State s=state(p);long now=now(p);
        if(!unlocked(p)||!p.isAlive()||p.isSpectator()||s.level!=null&&s.level!=p.level()||WaterRules.suppressed(p)){
            end(p);cancelInput(p);return;
        }
        p.clearFire();
        if(s.held>=0&&(!selected(p)||now>s.heartbeat))cancelInput(p);
        if(!selected(p)||tier(p)<3||p.getFoodData().getFoodLevel()<=6)s.healing=false;
        if(s.held==2&&now-s.heldAt>=7&&!s.blessAttempted){
            s.blessAttempted=true;Creeperborn.say(p,bless(p));
        }
        if(form(p)&&Mastery.harmony(p))s.formUntil=Long.MAX_VALUE;
        if(s.formUntil>0&&s.formUntil!=Long.MAX_VALUE&&now>=s.formUntil||s.wingUntil>0&&now>=s.wingUntil||p.getFoodData().getFoodLevel()<=6)end(p);
        if(s.formUntil==Long.MAX_VALUE&&!Mastery.harmony(p))s.formUntil=now+FORM_TICKS;
        if(s.diveTicks>0)dive(p,s);
        if(now%40==0){
            double heal=strength(p)*(Mastery.harmony(p)?1.25:1);
            if(s.healing){
                boolean changed=false;
                for(LivingEntity e:((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(10))){
                    if(e!=p&&(e instanceof Player||e instanceof Villager)&&e.isAlive()&&!e.isSpectator()&&e.distanceToSqr(p)<=100&&p.hasLineOfSight(e)){
                        float before=e.getHealth();e.heal((float)(2*heal));changed|=e.getHealth()>before;
                    }
                }
                // Detached regression players need the same self-heal as connected world players.
                float before=p.getHealth();p.heal((float)(2*heal));changed|=p.getHealth()>before;
                if(changed)credit(p);
            }else if(form(p)){
                float before=p.getHealth();p.heal((float)heal);if(p.getHealth()>before)credit(p);
            }
        }
        if(now%20==0){
            boolean moving=s.lastPosition!=null&&s.lastPosition.distanceToSqr(p.position())>.04;
            s.lastPosition=p.position();boolean active=moving||now-s.recent<200;
            if(active&&selected(p)){int before=tier(p);Mastery.set(p,"cb_phoenix_time_",seconds(p)+1);if(tier(p)>before)Creeperborn.say(p,"Phoenix mastery increased!");}
            if(active&&recovery(p)>0)Mastery.set(p,"cb_phoenix_recovery_",recovery(p)-1);
            if(form(p)||wing(p)){
                int fuel=Mastery.get(p,"cb_phoenix_fuel_")+1;
                if(fuel>=60){fuel=0;p.getFoodData().setFoodLevel(Math.max(6,p.getFoodData().getFoodLevel()-1));}
                Mastery.set(p,"cb_phoenix_fuel_",fuel);
            }
            if(s.healing&&++s.healFuel>=10){s.healFuel=0;p.getFoodData().setFoodLevel(Math.max(6,p.getFoodData().getFoodLevel()-1));}
        }
        if((form(p)||wing(p)||s.healing)&&now%5==0)particles(p,s.healing?10:4);
        if(s.healing&&now%10==0)ring(p);
        if((form(p)||wing(p))&&now%5==0){
            Vec3 tail=p.position().subtract(p.getLookAngle().scale(.8)).add(0,.7,0);
            ((ServerLevel)p.level()).sendParticles(ParticleTypes.SOUL_FIRE_FLAME,tail.x,tail.y,tail.z,3,.15,.15,.15,.01);
        }
        if((form(p)||wing(p)||s.healing)&&now%20==0){
            String mode=form(p)?"Phoenix Form "+(Mastery.harmony(p)?"unlimited":((s.formUntil-now+19)/20)+"s"):wing(p)?"Wing Flight "+((s.wingUntil-now+19)/20)+"s":"Healing Flames";
            Creeperborn.say(p,mode+" | Food "+p.getFoodData().getFoodLevel()+"/20 | Rebirth "+(tier(p)<3?"locked":spent(p)?"spent":"ready"));
        }
        if(p.getFoodData().getFoodLevel()<=6){end(p);cancelInput(p);}
    }
    public static void tick(MinecraftServer server){
        for(Player p:server.getPlayerList().getPlayers())tickPlayer(p);
        tickBlessings();
        for(var viewer:server.getPlayerList().getPlayers()){
            if(!ServerPlayNetworking.canSend(viewer,PhoenixVisualPayload.TYPE))continue;
            Map<UUID,View> cache=sent.computeIfAbsent(viewer.getUUID(),id->new HashMap<>());
            cache.entrySet().removeIf(e->{if(server.getPlayerList().getPlayer(e.getKey())!=null)return false;
                ServerPlayNetworking.send(viewer,new PhoenixVisualPayload(e.getValue().entity(),0));return true;});
            for(var subject:server.getPlayerList().getPlayers()){
                View next=new View(subject.getId(),subject.level()==viewer.level()?visualFlags(subject):0),old=cache.get(subject.getUUID());
                if(!next.equals(old)){
                    if(old!=null&&old.entity()!=next.entity())ServerPlayNetworking.send(viewer,new PhoenixVisualPayload(old.entity(),0));
                    ServerPlayNetworking.send(viewer,new PhoenixVisualPayload(next.entity(),next.flags()));cache.put(subject.getUUID(),next);
                }
            }
        }
    }
    public static void forget(Player p){end(p);states.remove(p.getUUID());sent.remove(p.getUUID());}
    public static void clear(){for(State s:List.copyOf(states.values()))end(s.owner);states.clear();sent.clear();blessings.clear();
}
