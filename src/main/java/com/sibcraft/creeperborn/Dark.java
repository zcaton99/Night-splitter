package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

public final class Dark{
    public static final List<String> FRUITS=List.of("rubber","door","phoenix","lightning","dark","op","gravity","string","paw","tremor","love","sand");
    private static final class State{Player owner;ServerLevel level;int held=-1,surge;long press,heartbeat,recent=-1000,mantleUntil,flare;Vec3 last,surgeDirection;}
    private record Void(Player owner,ServerLevel level,Vec3 center,long ends,Display.BlockDisplay visual,Display.BlockDisplay ground,int radius,float size){}
    private record Coil(Player owner,LivingEntity target,ServerLevel level,long ends){}
    private static final List<Coil> coils=new ArrayList<>();
    private record Tether(Player owner,LivingEntity target,ServerLevel level,long ends){}
    public record Capture(String tag,UUID token,String fruit,int tier){}
    private record View(int entity,int flags){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final List<Void> voids=new ArrayList<>();
    private static final Map<UUID,Tether> tethers=new HashMap<>();
    private static final Map<UUID,Map<UUID,View>> sent=new HashMap<>();
    private static final ThreadLocal<Player> voidKiller=new ThreadLocal<>();
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;return s;}
    private static long now(Player p){return p.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_dark")&&FruitClaims.ownsDark(p);}
    public static boolean matches(Player p,String fruit){return !unlocked(p)||choice(p).equals(fruit);}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&choice(p).equals("dark");}
    public static String extra(Player p){for(String f:FRUITS)if(!f.equals("dark")&&p.entityTags().contains("cb_fruit_"+f)&&p.getUUID().equals(FruitClaims.owner(f)))return f;return null;}
    public static String choice(Player p){
        for(String tag:p.entityTags())if(tag.startsWith("cb_dark_choice_")){
            String f=tag.substring("cb_dark_choice_".length());if(FRUITS.contains(f)&&p.getUUID().equals(FruitClaims.owner(f)))return f;
        }
        return "dark";
    }
    public static void selectConsumed(Player p,String fruit){stopForms(p);choose(p,fruit);}
    public static void choose(Player p,String fruit){
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith("cb_dark_choice_"))p.removeTag(t);
        p.addTag("cb_dark_choice_"+fruit);p.addTag("cb_mode_fruit");
    }
    public static String cycle(Player p){
        String current=choice(p),other=extra(p),next=!p.entityTags().contains("cb_mode_fruit")?current:current.equals("dark")&&other!=null?other:"dark";
        if(!next.equals(current)){stopForms(p);cancelInput(p);}
        choose(p,next);return "Creeper Fruit: "+next+" | G switches Dark / second power";
    }
    public static int uses(Player p){return Mastery.get(p,"cb_dark_uses_");}
    public static int seconds(Player p){return Mastery.get(p,"cb_dark_time_");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(uses(p),seconds(p)):0;}
    public static int fruitTier(Player p,String f){return switch(f){case "rubber"->Rubber.tier(p);case "door"->DoorFruit.tier(p);case "phoenix"->Phoenix.tier(p);case "lightning"->Lightning.tier(p);case "love","sand"->PairPowers.tier(p,f);case "tremor"->Tremor.tier(p);case "paw"->Paw.tier(p);case "string"->StringFruit.tier(p);case "gravity"->Gravity.tier(p);case "op"->Op.tier(p);case "dark"->tier(p);default->0;};}
    public static void grantFruit(Player p,String f,int tier){switch(f){case "rubber"->Rubber.grant(p,tier);case "door"->DoorFruit.grant(p,tier);case "phoenix"->Phoenix.grant(p,tier);case "lightning"->Lightning.grant(p,tier);case "love","sand"->PairPowers.grant(p,f,tier);case "tremor"->Tremor.grant(p,tier);case "paw"->Paw.grant(p,tier);case "string"->StringFruit.grant(p,tier);case "gravity"->Gravity.grant(p,tier);case "op"->Op.grant(p,tier);case "dark"->grant(p,tier);}}
    public static void grant(Player p,int tier){
        Mastery.set(p,"cb_dark_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_dark_time_",tier==3?3600:tier==2?300:0);
    }
    public static boolean consume(Player p,ItemStack item){
        if(item.isEmpty()||!Relics.kind(item).equals("fruit:dark")||!FruitClaims.claimItem(p,item,"dark"))return false;
        p.addTag("cb_fruit_dark");grant(p,Relics.captureTier(item));stopForms(p);choose(p,"dark");item.shrink(1);return true;
    }
    private static void credit(Player p){if(!p.isAlive()||!unlocked(p))return;State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_dark_uses_",uses(p)+1);s.recent=now(p);}
    private static long value(Player p,String prefix){
        for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}
        return 0;
    }
    private static void value(Player p,String prefix,long n){for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);p.addTag(prefix+n);}
    public static boolean suppressed(Player p){return now(p)<value(p,"cb_fruit_suppressed_");}
    public static int graspTicks(Player p){return Mastery.harmony(p)?100:60;}
    public static void suppress(Player victim,int ticks,boolean strong){
        value(victim,"cb_fruit_suppressed_",Math.max(value(victim,"cb_fruit_suppressed_"),now(victim)+ticks));
        Rubber.cancelInput(victim);Phoenix.cancelInput(victim);Lightning.cancelInput(victim);Op.cancelInput(victim);Gravity.cancelInput(victim);Paw.cancelInput(victim);Tremor.cancelInput(victim);PairPowers.cancelInput(victim);cancelInput(victim);
        if(strong)stopForms(victim);
    }
    private static void stopForms(Player p){Paw.forget(p);Tremor.forget(p);PairPowers.stopOwned(p);StringFruit.strongSuppress(p);Gravity.forget(p);Op.forget(p);Rubber.forget(p);Phoenix.forget(p);Lightning.forget(p);DoorSpace.forget(p);endMantle(p);}
    public static boolean rooted(LivingEntity target){
        Tether t=tethers.get(target.getUUID());return t!=null&&target.isAlive()&&t.owner().isAlive()&&target.level()==t.level()&&t.owner().level()==t.level()&&now(t.owner())<t.ends();
    }
    public static boolean mantle(Player p){State s=states.get(p.getUUID());return s!=null&&p.isAlive()&&s.mantleUntil>now(p);}
    public static int energy(Player p){return Math.min(6,Mastery.get(p,"cb_dark_energy_"));}
    public static float damage(Player p,DamageSource source,float amount){
        if(!mantle(p)||amount<=0||Phoenix.bypass(source))return amount;
        boolean projectile=source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile||Lightning.electrical(source);
        if(!projectile)return source.getEntity() instanceof LivingEntity?amount*.5f:amount;
        Mastery.set(p,"cb_dark_energy_",Math.min(6,energy(p)+1));
        if(source.getEntity() instanceof Player attacker&&attacker!=p&&FruitClaims.hasFruit(attacker))suppress(attacker,20,false);
        if(source.getDirectEntity() instanceof net.minecraft.world.entity.projectile.Projectile projectileEntity)projectileEntity.discard();
        credit(p);return amount*.25f;
    }
    public static void press(Player p,int move){
        if(!selected(p)||suppressed(p)||!p.isAlive()||p.isSpectator()||WaterRules.suppressed(p)||move<0||move>2)return;
        State s=state(p);if(s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;s.level=(ServerLevel)p.level();
    }
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null&&s.held>=0)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null)s.held=-1;}
    public static void release(Player p,int move){
        State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;cancelInput(p);
        Creeperborn.say(p,cast(p,move,held));
    }
    static void blind(Player owner,LivingEntity target){
        if(enemy(owner,target)&&target instanceof Player victim)
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.DARKNESS,60,0,false,false,true));
    }
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player);}
    private static Vec3 aim(Player p,int range){
        Vec3 from=p.getEyePosition(),to=from.add(p.getLookAngle().scale(range));
        var wall=p.level().clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return wall.getType()==HitResult.Type.MISS?to:wall.getLocation();
    }
    private static LivingEntity target(Player p,int range){
        Vec3 from=p.getEyePosition(),end=aim(p,range);LivingEntity best=null;double nearest=from.distanceTo(end)+.01;
        for(LivingEntity e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.8))){
            if(!enemy(p,e))continue;var hit=e.getBoundingBox().inflate(.4).clip(from,end);
            if(hit.isPresent()&&hit.get().distanceTo(from)<nearest){best=e;nearest=hit.get().distanceTo(from);}
        }
        return best;
    }
    static boolean voidDamage(Player owner,LivingEntity target,float amount){return hurt(owner,target,amount,true);}
    private static boolean hurt(Player owner,LivingEntity target,float damage,boolean capture){
        Player previous=voidKiller.get();if(capture)voidKiller.set(owner);
        try{
            boolean hit=target.hurtServer((ServerLevel)owner.level(),owner.damageSources().playerAttack(owner),damage);
            if(hit){if(target.isAlive()&&target instanceof Player victim)suppress(victim,20,false);credit(owner);}return hit;
        }finally{if(capture){if(previous==null)voidKiller.remove();else voidKiller.set(previous);}}
    }
    public static long ready(Player p,int move,boolean held){return value(p,"cb_dark_ready_"+move+"_"+held+"_");}
    public static String cast(Player p,int move,long held){
        if(!selected(p)||!p.isAlive()||p.isSpectator())return "Select Dark with G first.";
        if(suppressed(p)||WaterRules.suppressed(p))return "Dark powers are suppressed.";
        if(move<0||move>2)return "";
        if(tier(p)<move+1)return "Dark move locked: tier 2 needs 10 uses + 5 minutes; tier 3 needs 40 uses + one hour.";
        boolean charged=held>=7;
        if(move==2&&charged&&held<60)return "Void Collapse cancelled: charge for three seconds.";
        if(move==1&&!charged&&mantle(p)){endMantle(p);return "Dark Mantle ended.";}
        if(now(p)<ready(p,move,charged))return "Dark move recovering.";
        State s=state(p);ServerLevel level=(ServerLevel)p.level();double power=Mastery.multiplier(p);
        if(move==0){
            LivingEntity target=target(p,charged?6:24);if(target==null)return "No enemy in your aim.";
            if(charged){
                if(target instanceof Player victim)suppress(victim,graspTicks(p),true);
                tethers.put(target.getUUID(),new Tether(p,target,level,now(p)+graspTicks(p)));
                target.setDeltaMovement(Vec3.ZERO);if(target instanceof Mob mob)mob.getNavigation().stop();
                chains(level,target);s.flare=now(p)+20;hurt(p,target,(float)(5*power),false);
            }else{
                blind(p,target);Vec3 pull=p.position().subtract(target.position()).normalize();
                if(target instanceof ServerPlayer sp)Rubber.impulse(sp,pull.scale(1.3).add(0,.15,0));
                else target.setDeltaMovement(pull.scale(1.3).add(0,.15,0));
                darkLine(level,LightningBolts.hand(p),target.getEyePosition(),24);
                coils.add(new Coil(p,target,level,now(p)+20));coil(level,target,now(p));
                hurt(p,target,(float)(5*power),false);
            }
            value(p,"cb_dark_ready_0_"+charged+"_",now(p)+(charged?160:40));return charged?"Nullifying Grasp!":"Black Vortex!";
        }
        if(move==1){
            if(charged){
                int energy=energy(p);if(energy==0)return "Absorb attacks with Dark Mantle before Liberation.";
                for(LivingEntity target:level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(8))){
                    if(enemy(p,target)&&target.distanceToSqr(p)<=64&&p.hasLineOfSight(target)){
                        hurt(p,target,(float)((4+energy*2)*power),false);
                        Vec3 away=target.position().subtract(p.position()).normalize().scale(.7+energy*.12);
                        if(target instanceof ServerPlayer sp)Rubber.impulse(sp,away.add(0,.25,0));else target.setDeltaMovement(away.add(0,.25,0));
                    }
                }
                Mastery.set(p,"cb_dark_energy_",0);s.flare=now(p)+20;wave(level,p.position().add(0,1,0),8);
            }else{
                if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons for Dark Mantle.";
                p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);s.mantleUntil=now(p)+160;s.level=level;credit(p);
            }
            value(p,"cb_dark_ready_1_"+charged+"_",now(p)+(charged?160:240));return charged?"Liberation!":"Dark Mantle!";
        }
        if(charged){
            Vec3 center=voidCenter(p);int radius=voidRadius(p);voids.add(new Void(p,level,center,now(p)+80,orb(level,center),ground(level,p,center,radius),radius,voidSize(p)));darkLine(level,LightningBolts.hand(p),center,28);credit(p);
            value(p,"cb_dark_ready_2_true_",now(p)+400);return "Void Collapse!";
        }
        if(p.isPassenger())return "Dismount before Shadow Surge.";
        double yaw=Math.toRadians(p.getYRot());s.surgeDirection=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        s.surge=12;s.level=level;credit(p);value(p,"cb_dark_ready_2_false_",now(p)+120);return "Shadow Surge!";
    }
    public static int voidRadius(Player p){return Mastery.harmony(p)?9:6;}
    public static float voidSize(Player p){return Mastery.harmony(p)?4:2.5f;}
    public static Vec3 voidCenter(Player p){
        double yaw=Math.toRadians(p.getYRot());Vec3 forward=new Vec3(-Math.sin(yaw),0,Math.cos(yaw));
        Vec3 desired=p.position().add(forward.scale(3)).add(0,1,0);
        var wall=p.level().clip(new ClipContext(p.getEyePosition(),desired,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return wall.getType()==HitResult.Type.MISS?desired:wall.getLocation().subtract(forward.scale(.35));
    }
    private static Display.BlockDisplay ground(ServerLevel level,Player owner,Vec3 center,int radius){
        Vec3 origin=new Vec3(Math.floor(center.x)+.5,center.y,Math.floor(center.z)+.5);
        StringBuilder name=new StringBuilder("cb_dark_ground|");
        for(int x=-radius;x<=radius;x++)for(int z=-radius;z<=radius;z++){
            if(x*x+z*z>radius*radius)continue;
            Vec3 top=origin.add(x,2.5,z),bottom=origin.add(x,-12,z);
            if(!level.hasChunkAt(net.minecraft.core.BlockPos.containing(top)))continue;
            var hit=level.clip(new ClipContext(top,bottom,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,owner));
            if(hit.getType()==HitResult.Type.MISS)continue;
            float y=(float)(hit.getLocation().y-origin.y+.015);
            name.append(x).append(',').append(y).append(',').append(z).append(';');
        }
        return display(level,origin,name.toString());
    }
    private static Display.BlockDisplay orb(ServerLevel level,Vec3 center){return display(level,center,"cb_dark_void");}
    private static Display.BlockDisplay display(ServerLevel level,Vec3 center,String name){
        Display.BlockDisplay display=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.EVENT);if(display==null)return null;
        display.addTag("cb_temporary_rock");display.setCustomName(Component.literal(name));display.setCustomNameVisible(false);
        display.setBlockState(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        var settings=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)display;settings.creeperborn$duration(0);settings.creeperborn$positionDuration(0);settings.creeperborn$width(32);settings.creeperborn$height(32);settings.creeperborn$view(1);
        display.snapTo(center.x,center.y,center.z);level.addFreshEntity(display);return display;
    }
    private static void removeVoid(Void v){if(v.visual()!=null)v.visual().discard();if(v.ground()!=null)v.ground().discard();}
    private static final DustParticleOptions BLACK=new DustParticleOptions(0x020204,1.8f),VIOLET=new DustParticleOptions(0x6E1CBB,.65f);
    private static void dust(ServerLevel level,Vec3 at,int count){level.sendParticles(BLACK,at.x,at.y,at.z,count,.4,.5,.4,.01);level.sendParticles(VIOLET,at.x,at.y,at.z,Math.max(1,count/3),.4,.5,.4,.01);}
    private static void darkLine(ServerLevel level,Vec3 from,Vec3 to,int count){
        for(int i=0;i<=count;i++){double t=i/(double)count;Vec3 at=from.lerp(to,t).add(Math.sin(t*24)*.1,Math.sin(t*12)*.1,0);level.sendParticles(BLACK,at.x,at.y,at.z,2,.04,.04,.04,0);level.sendParticles(VIOLET,at.x,at.y,at.z,1,0,0,0,0);}
    }
    private static void coil(ServerLevel level,LivingEntity target,long time){
        double radius=Math.max(.5,target.getBbWidth()/2+.2),height=target.getBbHeight();
        for(int i=0;i<36;i++){
            double t=i/35.0,a=t*Math.PI*6+time*.22;
            Vec3 at=target.position().add(Math.cos(a)*radius,.1+t*Math.max(.6,height-.15),Math.sin(a)*radius);
            level.sendParticles(BLACK,at.x,at.y,at.z,1,.025,.025,.025,0);
            level.sendParticles(VIOLET,at.x,at.y,at.z,1,0,0,0,0);
        }
    }
    private static void chains(ServerLevel level,LivingEntity target){
        for(int i=0;i<24;i++){double a=i*Math.PI/6;Vec3 at=target.position().add(Math.cos(a)*.6,.2+i*.06,Math.sin(a)*.6);level.sendParticles(BLACK,at.x,at.y,at.z,1,0,0,0,0);}
        // Two separated fruit lobes and a jagged gap suggest a cracked fruit above the victim.
        Vec3 head=target.getEyePosition().add(0,.5,0);
        for(int i=0;i<24;i++){double a=i*Math.PI/12;double x=Math.cos(a)*.25;if(Math.abs(x)<.06)continue;
            level.sendParticles(VIOLET,head.x+x,head.y+Math.sin(a)*.23,head.z,1,0,0,0,0);}
        darkLine(level,head.add(.03,.24,0),head.add(-.05,.05,0),4);
        darkLine(level,head.add(-.05,.05,0),head.add(.05,-.04,0),4);
        darkLine(level,head.add(.05,-.04,0),head.add(-.03,-.24,0),4);
    }
    private static void wave(ServerLevel level,Vec3 center,double radius){
        for(int i=0;i<64;i++){double a=i*Math.PI/32;Vec3 at=center.add(Math.cos(a)*radius,0,Math.sin(a)*radius);level.sendParticles(BLACK,at.x,at.y,at.z,1,.05,.1,.05,0);}
    }
    public static void endMantle(Player p){State s=states.get(p.getUUID());if(s!=null){s.mantleUntil=0;s.surge=0;}}
    public static void tickPlayer(Player p){
        State s=state(p);long now=now(p);
        if(!unlocked(p)||!p.isAlive()||p.isSpectator()||WaterRules.suppressed(p)||s.level!=null&&s.level!=p.level()){endMantle(p);cancelInput(p);return;}
        if(s.held>=0&&(!selected(p)||suppressed(p)||now>s.heartbeat))cancelInput(p);
        if(s.held==2&&now-s.press>=7&&now%5==0)Creeperborn.say(p,"Void Collapse "+Math.min(100,(now-s.press)*100/60)+"% — hold three seconds, release");
        if((s.held==0&&now-s.press>=7||now<s.flare)&&now%5==0){Vec3 hand=LightningBolts.hand(p);((ServerLevel)p.level()).sendParticles(VIOLET,hand.x,hand.y,hand.z,8,.12,.12,.12,.01);}
        if(s.surge>0){
            Vec3 next=p.position().add(s.surgeDirection.scale(1.3));
            if(!p.level().hasChunkAt(net.minecraft.core.BlockPos.containing(next))||!p.level().getWorldBorder().isWithinBounds(net.minecraft.core.BlockPos.containing(next))
                ||!p.level().noCollision(p,p.getBoundingBox().move(s.surgeDirection.scale(1.3))))s.surge=0;
            else{Rubber.impulse(p,s.surgeDirection.scale(1.3).add(0,Math.min(0,p.getDeltaMovement().y),0));dust((ServerLevel)p.level(),p.position().add(0,.1,0),8);s.surge--;}
            if(s.surge==0)Rubber.impulse(p,new Vec3(0,p.getDeltaMovement().y,0));
        }
        if(mantle(p)&&now%5==0){
            dust((ServerLevel)p.level(),p.position().add(0,1,0),8);
            for(int i=0;i<energy(p);i++){double a=now*.12+i*Math.PI/3;Vec3 at=p.position().add(Math.cos(a)*.8,1,Math.sin(a)*.8);((ServerLevel)p.level()).sendParticles(BLACK,at.x,at.y,at.z,2,0,.05,0,0);}
        }
        if(now%20==0){
            boolean active=s.last!=null&&s.last.distanceToSqr(p.position())>.04||now-s.recent<200;s.last=p.position();
            if(active&&selected(p)){int before=tier(p);Mastery.set(p,"cb_dark_time_",seconds(p)+1);if(tier(p)>before)Creeperborn.say(p,"Dark mastery increased!");}
            if(mantle(p))Creeperborn.say(p,"Dark Mantle | Darkness "+energy(p)+"/6 | hold X: Liberation");
        }
    }
    public static void tickEffects(){
        coils.removeIf(c->{if(!c.owner().isAlive()||!c.target().isAlive()||c.owner().level()!=c.level()||c.target().level()!=c.level()||now(c.owner())>=c.ends())return true;if(now(c.owner())%4==0)coil(c.level(),c.target(),now(c.owner()));return false;});
        tethers.entrySet().removeIf(entry->{Tether t=entry.getValue();if(!rooted(t.target()))return true;
            t.target().setDeltaMovement(Vec3.ZERO);if(t.target() instanceof Mob mob)mob.getNavigation().stop();if(now(t.owner())%5==0)chains(t.level(),t.target());return false;});
        for(Void v:List.copyOf(voids)){
            if(!voids.contains(v))continue;
            if(!v.owner().isAlive()||v.owner().level()!=v.level()||WaterRules.suppressed(v.owner())){removeVoid(v);voids.remove(v);continue;}
            long now=now(v.owner());boolean finish=now>=v.ends();
            if(v.visual()!=null){float scale=(float)Math.max(.05,Math.min(1,(v.ends()-now)/10.0))*v.size();v.visual().setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(scale),new org.joml.Quaternionf()));}
            if(v.ends()-now>=68&&now%2==0)darkLine(v.level(),LightningBolts.hand(v.owner()),v.center(),18);
            if(now%5==0){dust(v.level(),v.center(),20);wave(v.level(),v.center(),v.radius());}
            for(LivingEntity target:v.level().getEntitiesOfClass(LivingEntity.class,new AABB(v.center(),v.center()).inflate(v.radius()))){
                if(!v.owner().isAlive()||!voids.contains(v))break;
                if(!enemy(v.owner(),target)||target.position().distanceToSqr(v.center())>v.radius()*v.radius()
                    ||v.level().clip(new ClipContext(v.center(),target.getEyePosition(),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,v.owner())).getType()!=HitResult.Type.MISS)continue;
                blind(v.owner(),target);Vec3 pull=v.center().subtract(target.position()).normalize().scale(.28);
                if(target instanceof ServerPlayer sp)Rubber.impulse(sp,pull);else target.setDeltaMovement(target.getDeltaMovement().scale(.4).add(pull));
                if(finish||now%20==0)hurt(v.owner(),target,(float)((finish?12:4)*Mastery.multiplier(v.owner())),true);
            }
            if(finish){wave(v.level(),v.center(),v.radius());removeVoid(v);voids.remove(v);}
        }
    }
    public static List<Capture> captures(Player p){
        List<Capture> list=new ArrayList<>();
        for(String tag:p.entityTags())if(tag.startsWith("cb_dark_capture|"))try{
            String[] parts=tag.split("\\|");if(parts.length==4&&FRUITS.contains(parts[2]))list.add(new Capture(tag,UUID.fromString(parts[1]),parts[2],Math.min(2,Math.max(1,Integer.parseInt(parts[3])))));
        }catch(IllegalArgumentException ignored){}
        list.sort(Comparator.comparing(c->c.token().toString()));return list;
    }
    public static String ownedFruit(Player p){
        if(unlocked(p))return choice(p);
        for(String f:FRUITS)if(p.entityTags().contains("cb_fruit_"+f)&&p.getUUID().equals(FruitClaims.owner(f)))return f;
        return null;
    }
    public static void died(LivingEntity entity,DamageSource source){
        Player killer=voidKiller.get();
        if(killer==null||!unlocked(killer)||!(entity instanceof Player victim)||source.getEntity()!=killer)return;
        capture(killer,victim);
    }
    static Capture capture(Player killer,Player victim){
        String fruit=ownedFruit(victim);if(fruit==null||!unlocked(killer))return null;
        int tier=Math.min(2,fruitTier(victim,fruit));UUID token=UUID.randomUUID();
        FruitClaims.setOwner(fruit,token);
        Capture c=new Capture("cb_dark_capture|"+token+"|"+fruit+"|"+tier,token,fruit,tier);killer.addTag(c.tag());show(killer,c);return c;
    }
    public static void show(Player p,Capture c){
        boolean canEat=unlocked(p)&&extra(p)==null&&!c.fruit().equals("dark");
        p.sendSystemMessage(Component.literal("Captured "+c.fruit()+" Creeper Fruit — tier "+c.tier()+". "));
        if(canEat)p.sendSystemMessage(Component.literal("[Eat]").withStyle(style->style.withClickEvent(new ClickEvent.RunCommand("/creeperborn dark eat "+c.token()))));
        p.sendSystemMessage(Component.literal("[Materialize]").withStyle(style->style.withClickEvent(new ClickEvent.RunCommand("/creeperborn dark materialize "+c.token()))));
    }
    public static String decide(Player p,String token,boolean eat){
        Capture c=captures(p).stream().filter(v->token.isEmpty()||v.token().toString().equals(token)).findFirst().orElse(null);
        if(c==null)return "No pending capture.";
        if(!c.token().equals(FruitClaims.owner(c.fruit())))return "That capture is no longer reserved.";
        if(eat){
            if(!FruitClaims.eatCapture(p,c.fruit(),c.token()))return "Dark already has two powers; choose Materialize.";
            p.addTag("cb_fruit_"+c.fruit());grantFruit(p,c.fruit(),c.tier());stopForms(p);choose(p,c.fruit());
        }else{
            ItemStack item=Relics.captured(c.fruit(),c.token().toString(),c.tier());
            if(!p.addItem(item))return "Inventory full. Capture kept; make space and Materialize again.";
        }
        p.removeTag(c.tag());return eat?"Captured power eaten at tier "+c.tier()+"! G switches your two powers.":"Fruit materialized!";
    }
    public static void ownerDied(Player p){
        for(Capture c:captures(p))if(c.token().equals(FruitClaims.owner(c.fruit()))&&p.level() instanceof ServerLevel level){
            if(p.spawnAtLocation(level,Relics.captured(c.fruit(),c.token().toString(),c.tier()))!=null)p.removeTag(c.tag());
        }
    }
    public static int flags(Player p){State s=states.get(p.getUUID());return s==null?0:(mantle(p)?1:0)|(s.surge>0?2:0)|(s.held==0&&now(p)-s.press>=7?4:0)|(now(p)<s.flare?8:0);}
    public static String status(Player p){return "Dark "+(unlocked(p)?"tier "+tier(p)+"/3 | "+uses(p)+" uses | "+seconds(p)+"s | Selected: "+choice(p)+" | Second power: "+extra(p)+(extra(p)==null?"":" tier "+fruitTier(p,extra(p))+"/3"):"locked")+" | Pending captures: "+captures(p).size();}
    public static void tick(MinecraftServer server){
        for(Player p:server.getPlayerList().getPlayers())tickPlayer(p);tickEffects();
        for(var viewer:server.getPlayerList().getPlayers()){
            if(!ServerPlayNetworking.canSend(viewer,DarkVisualPayload.TYPE))continue;
            Map<UUID,View> cache=sent.computeIfAbsent(viewer.getUUID(),id->new HashMap<>());
            cache.entrySet().removeIf(entry->{if(server.getPlayerList().getPlayer(entry.getKey())!=null)return false;ServerPlayNetworking.send(viewer,new DarkVisualPayload(entry.getValue().entity(),0));return true;});
            for(var subject:server.getPlayerList().getPlayers()){
                View next=new View(subject.getId(),subject.level()==viewer.level()?flags(subject):0),old=cache.get(subject.getUUID());
                if(!next.equals(old)){if(old!=null&&old.entity()!=next.entity())ServerPlayNetworking.send(viewer,new DarkVisualPayload(old.entity(),0));
                    ServerPlayNetworking.send(viewer,new DarkVisualPayload(next.entity(),next.flags()));cache.put(subject.getUUID(),next);}
            }
        }
    }
    public static void forget(Player p){coils.removeIf(c->c.owner()==p||c.target()==p);endMantle(p);cancelInput(p);states.remove(p.getUUID());sent.remove(p.getUUID());tethers.entrySet().removeIf(e->e.getValue().owner()==p||e.getValue().target()==p);voids.removeIf(v->{if(v.owner()!=p)return false;removeVoid(v);return true;});}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.owner);states.clear();sent.clear();voids.forEach(Dark::removeVoid);voids.clear();tethers.clear();coils.clear();}
}
