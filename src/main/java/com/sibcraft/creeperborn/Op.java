package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.*;

/** Server-authoritative operating room; the shell is visual and never changes terrain. */
public final class Op {
    private static final class State {
        Player owner; ServerLevel level; Vec3 center,last; Display.BlockDisplay shell;
        long ends,press,heartbeat,recent=-1000,charge; int held=-1; LivingEntity target; float health;
    }
    private static final Map<UUID,State> states=new HashMap<>();
    private static final DustParticleOptions BLUE=new DustParticleOptions(0x8DDDFF,.9f);
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;return s;}
    private static long now(Player p){return p.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_op")&&p.getUUID().equals(FruitClaims.owner("op"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"op");}
    public static int uses(Player p){return Mastery.get(p,"cb_op_uses_");}
    public static int seconds(Player p){return Mastery.get(p,"cb_op_time_");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(uses(p),seconds(p)):0;}
    public static void grant(Player p,int tier){Mastery.set(p,"cb_op_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_op_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,ItemStack item){
        if(item.isEmpty()||!Relics.kind(item).equals("fruit:op")||!FruitClaims.claimItem(p,item,"op"))return false;
        p.addTag("cb_fruit_op");grant(p,Relics.captureTier(item));p.addTag("cb_mode_fruit");
        if(Dark.unlocked(p))Dark.selectConsumed(p,"op");item.shrink(1);return true;
    }
    public static double diameter(Player p){return tier(p)>=3?40:tier(p)==2?32:25;}
    public static int duration(Player p){return tier(p)>=3?1800:tier(p)==2?1200:600;}
    public static int charges(Player p){return tier(p)>=3?5:3;}
    public static boolean room(Player p){State s=states.get(p.getUUID());return s!=null&&s.center!=null&&s.level==p.level()&&p.isAlive()&&now(p)<s.ends;}
    public static boolean inside(Player p,LivingEntity e){
        State s=states.get(p.getUUID());return room(p)&&e.level()==s.level&&e.getBoundingBox().getCenter().distanceToSqr(s.center)<=diameter(p)*diameter(p)/4;
    }
    private static long value(Player p,String prefix){for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void value(Player p,String prefix,long n){for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);p.addTag(prefix+n);}
    private static void credit(Player p){State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_op_uses_",uses(p)+1);s.recent=now(p);}
    private static boolean fuel(Player p){if(p.getFoodData().getFoodLevel()<=6)return false;p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);return true;}
    public static String toggle(Player p){
        if(room(p)){endRoom(p);return "ROOM dismissed.";}
        if(now(p)<value(p,"cb_op_room_ready_"))return "ROOM recovering.";
        if(!fuel(p))return "Eat above three food icons for ROOM.";
        State s=state(p);s.level=(ServerLevel)p.level();s.center=p.position().add(0,1,0);s.ends=now(p)+duration(p);
        s.shell=EntityTypes.BLOCK_DISPLAY.create(s.level,EntitySpawnReason.TRIGGERED);
        if(s.shell!=null){
            s.shell.addTag("cb_temporary_rock");s.shell.setCustomName(Component.literal("cb_op_room"));s.shell.setCustomNameVisible(false);
            s.shell.setBlockState(net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)s.shell;a.creeperborn$duration(0);a.creeperborn$positionDuration(0);a.creeperborn$width(48);a.creeperborn$height(48);a.creeperborn$view(2);
            s.shell.snapTo(s.center.x,s.center.y,s.center.z);s.level.addFreshEntity(s.shell);scale(s,diameter(p)/2);
        }
        credit(p);return "ROOM! "+(int)diameter(p)+" block diameter.";
    }
    private static void scale(State s,double radius){if(s.shell!=null)s.shell.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f((float)radius),new org.joml.Quaternionf()));}
    public static void endRoom(Player p){
        State s=states.get(p.getUUID());if(s==null)return;
        if(s.center!=null)value(p,"cb_op_room_ready_",now(p)+100);
        if(s.shell!=null)s.shell.discard();s.shell=null;s.center=null;cancelInput(p);
    }
    public static void press(Player p,int move){
        if(!selected(p)||Dark.suppressed(p)||WaterRules.suppressed(p)||p.isSpectator()||!p.isAlive()||move<0||move>2)return;
        State s=state(p);if(s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;s.health=p.getHealth();
    }
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null&&s.held>=0)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null){if(s.target!=null)s.target.setDeltaMovement(Vec3.ZERO);s.held=-1;s.target=null;s.charge=0;}}
    public static void release(Player p,int move){
        State s=states.get(p.getUUID());if(s==null||s.held!=move)return;
        long held=now(p)-s.press;LivingEntity lifted=s.target;
        if(move==2&&held>=7&&lifted!=null&&valid(p)&&inside(p,lifted)){
            impulse(lifted,p.getLookAngle().scale(1.6).add(0,.25,0));s.target=null;
            value(p,"cb_op_ready_2_true_",now(p)+120);credit(p);cancelInput(p);Creeperborn.say(p,"Takt!");return;
        }
        cancelInput(p);Creeperborn.say(p,cast(p,move,held));
    }
    private static boolean valid(Player p){return selected(p)&&p.isAlive()&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)&&inside(p,p);}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&(e instanceof Enemy||e instanceof Player);}
    static LivingEntity target(Player p,double range){
        Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(range));
        var wall=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)end=wall.getLocation();
        LivingEntity best=null;double nearest=from.distanceTo(end)+.001;
        for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.6))){
            if(!enemy(p,e)||!inside(p,e))continue;var hit=e.getBoundingBox().inflate(.25).clip(from,end);
            if(hit.isPresent()&&hit.get().distanceTo(from)<nearest){best=e;nearest=hit.get().distanceTo(from);}
        }return best;
    }
    private static void impulse(LivingEntity e,Vec3 motion){if(e instanceof ServerPlayer sp)Rubber.impulse(sp,motion);else e.setDeltaMovement(motion);}
    private static boolean hurt(Player p,LivingEntity e,float amount){boolean hit=e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),(float)(amount*Mastery.multiplier(p)));if(hit)credit(p);return hit;}
    public static int remaining(Player p){
        if(now(p)>=value(p,"cb_op_swap_refill_"))return charges(p);
        return Math.max(0,charges(p)-(int)value(p,"cb_op_swap_spent_"));
    }
    private static boolean clearAt(LivingEntity e,Vec3 at){
        var level=e.level();BlockPos pos=BlockPos.containing(at);
        return level.hasChunkAt(pos)&&level.getWorldBorder().isWithinBounds(pos)&&at.y>=level.getMinY()&&at.y+e.getBbHeight()<level.getMaxY()
            &&level.noCollision(e,e.getBoundingBox().move(at.subtract(e.position())));
    }
    private static void teleport(LivingEntity e,Vec3 at){
        if(e instanceof ServerPlayer sp)sp.teleportTo((ServerLevel)e.level(),at.x,at.y,at.z,Set.of(),e.getYRot(),e.getXRot(),false);
        else e.snapTo(at.x,at.y,at.z);e.setDeltaMovement(Vec3.ZERO);
    }
    public static String swap(Player p,LivingEntity e){
        if(!valid(p)||!enemy(p,e)||!inside(p,e)||p.isPassenger()||e.isPassenger()||p.isVehicle()||e.isVehicle())return "Both targets must be free inside ROOM.";
        if(remaining(p)==0)return "Shambles recharging.";
        Vec3 a=p.position(),b=e.position();if(!clearAt(p,b)||!clearAt(e,a))return "Not enough space to swap.";
        if(!fuel(p))return "Eat before using Shambles.";
        int spent=now(p)>=value(p,"cb_op_swap_refill_")?0:(int)value(p,"cb_op_swap_spent_");
        teleport(p,b);teleport(e,a);value(p,"cb_op_swap_spent_",spent+1);value(p,"cb_op_swap_refill_",now(p)+100);
        line((ServerLevel)p.level(),a.add(0,1,0),b.add(0,1,0));credit(p);return "Shambles! "+remaining(p)+" swaps left.";
    }
    public static String cast(Player p,int move,long held){
        if(!selected(p)||!p.isAlive()||p.isSpectator())return "Select Op-Op with G.";
        if(Dark.suppressed(p)||WaterRules.suppressed(p))return "Op-Op powers suppressed.";
        boolean hold=held>=7;if(move<0||move>2)return "";
        if(move==0&&!hold)return toggle(p);
        if(tier(p)<move+1)return "Move locked: X needs tier 2; C needs tier 3.";
        if(!valid(p))return "You and your target must be inside ROOM.";
        if(now(p)<value(p,"cb_op_ready_"+move+"_"+hold+"_"))return "Move recovering.";
        if(move==0){
            LivingEntity t=target(p,diameter(p));if(t==null)return "No enemy in your aim inside ROOM.";
            if(!fuel(p))return "Eat before attacking.";
            line((ServerLevel)p.level(),LightningBolts.hand(p),t.getEyePosition());
            // Two attacks split the strike between ordinary damage and armor-bypassing magic.
            hurt(p,t,5);t.damageCooldownTime=0;t.hurtServer((ServerLevel)p.level(),p.damageSources().magic(),(float)(5*Mastery.multiplier(p)));
            credit(p);value(p,"cb_op_ready_0_true_",now(p)+100);return "Gamma Knife!";
        }
        if(move==1&&hold)return held>=60?"Hold X steadily on an enemy for three seconds.":"Heart Extraction needs a three-second channel.";
        if(move==1){
            if(!fuel(p))return "Eat before Counter Shock.";
            for(var t:p.level().getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(5)))
                if(enemy(p,t)&&inside(p,t)&&p.distanceToSqr(t)<=25&&p.hasLineOfSight(t)){hurt(p,t,6);stuns.put(t.getUUID(),new Stun(p,t,now(p)+20));impulse(t,Vec3.ZERO);line((ServerLevel)p.level(),p.getEyePosition(),t.getEyePosition());}
            value(p,"cb_op_ready_1_false_",now(p)+120);credit(p);return "Counter Shock!";
        }
        if(!hold){LivingEntity t=target(p,diameter(p));return t==null?"Aim at an enemy inside ROOM.":swap(p,t);}
        return "Hold C to lift an enemy, then release to throw.";
    }
    private record Stun(Player owner,LivingEntity target,long ends){}
    private static final Map<UUID,Stun> stuns=new HashMap<>();
    public static boolean rooted(LivingEntity e){Stun s=stuns.get(e.getUUID());return s!=null&&e.isAlive()&&s.owner().isAlive()&&s.owner().level()==e.level()&&now(s.owner())<s.ends();}
    static void line(ServerLevel level,Vec3 from,Vec3 to){for(int i=0;i<=20;i++){Vec3 at=from.lerp(to,i/20.0);level.sendParticles(BLUE,at.x,at.y,at.z,1,0,0,0,0);}}
    public static void tickPlayer(Player p){
        State s=state(p);long now=now(p);
        if(!unlocked(p)||!p.isAlive()||p.isSpectator()||s.level!=null&&s.level!=p.level()||WaterRules.suppressed(p)||Dark.suppressed(p)){endRoom(p);return;}
        if(s.center!=null){
            if(now>=s.ends){endRoom(p);return;}
            scale(s,diameter(p)/2);
            if(now%100==0&&!fuel(p)){endRoom(p);return;}
            if(now%20==0)Creeperborn.say(p,"ROOM "+((s.ends-now+19)/20)+"s | "+(int)diameter(p)+" blocks | Shambles "+remaining(p)+"/"+charges(p));
        }
        if(s.held>=0&&(!selected(p)||now>s.heartbeat)){cancelInput(p);return;}
        if(s.held>=0&&now-s.press>=7){
            if(!valid(p)){cancelInput(p);return;}
            if(s.held==1){
                if(tier(p)<2){cancelInput(p);return;}
                LivingEntity t=target(p,8);
                if(t==null||Hearts.hasHeart(p)||now<value(p,"cb_op_ready_1_true_")||p.getHealth()<s.health||p.hurtTime>0){cancelInput(p);return;}
                if(s.target!=t){s.target=t;s.charge=now;}
                line((ServerLevel)p.level(),LightningBolts.hand(p),t.getEyePosition());
                if(now%10==0)Creeperborn.say(p,"Heart Extraction "+Math.min(100,(now-s.charge)*100/60)+"%");
                if(now-s.charge>=60){
                    if(fuel(p)&&Hearts.extract(p,t)){credit(p);value(p,"cb_op_ready_1_true_",now+200);Creeperborn.say(p,"Heart extracted! Right-click cube to squeeze.");}
                    cancelInput(p);
                }
            }else if(s.held==2&&tier(p)>=3){
                if(now<value(p,"cb_op_ready_2_true_")){cancelInput(p);return;}
                if(s.target==null){s.target=target(p,diameter(p));if(s.target==null||!fuel(p)){cancelInput(p);return;}}
                LivingEntity t=s.target;
                if(!t.isAlive()||!inside(p,t)||!p.hasLineOfSight(t)||now-s.press>100){cancelInput(p);return;}
                Vec3 anchor=p.getEyePosition().add(p.getLookAngle().scale(5)).add(0,2,0);
                Vec3 delta=anchor.subtract(t.position());impulse(t,delta.scale(.25).add(0,.08,0));
                if(t instanceof Mob mob)mob.getNavigation().stop();
                if(now%4==0)line((ServerLevel)p.level(),LightningBolts.hand(p),t.getEyePosition());
            }
        }
        if(now%20==0&&selected(p)){
            boolean active=s.last!=null&&s.last.distanceToSqr(p.position())>.04||now-s.recent<200;s.last=p.position();
            if(active){int before=tier(p);Mastery.set(p,"cb_op_time_",seconds(p)+1);if(tier(p)>before)Creeperborn.say(p,"Op-Op mastery increased!");}
        }
    }
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())tickPlayer(p);stuns.entrySet().removeIf(e->!rooted(e.getValue().target()));}
    public static String status(Player p){return "Op-Op "+(unlocked(p)?"tier "+tier(p)+"/3 | "+uses(p)+" uses | "+seconds(p)+"s | ROOM "+(int)diameter(p)+" blocks / "+duration(p)/20+"s | Shambles "+remaining(p)+"/"+charges(p):"locked");}
    public static void forget(Player p){endRoom(p);states.remove(p.getUUID());stuns.entrySet().removeIf(e->e.getValue().owner()==p||e.getValue().target()==p);}
    public static void clear(){for(var s:List.copyOf(states.values()))forget(s.owner);states.clear();stuns.clear();}
}
