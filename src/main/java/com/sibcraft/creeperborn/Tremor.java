package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Quakes never edit blocks, fluids or explosions. All geometry is temporary display data. */
public final class Tremor {
    private static final class State {Player owner;ServerLevel level;int held=-1;long press,heartbeat,recent=-1000;Vec3 last;Display.BlockDisplay charge;}
    private static final class Pulse {Player owner;ServerLevel level;Vec3 from,to,normal;int age,duration,kind;double radius,width,height;float damage;Set<UUID> hit=new HashSet<>();Display.BlockDisplay visual;}
    private static final class Slam {Player owner;ServerLevel level;int age;long ends;}
    private static final class Carry {Player owner;LivingEntity target;ServerLevel level;Vec3 center;long ends,arrived=-1;}
    private record Ground(Display.BlockDisplay display,Vec3 base,long starts,int duration,float size){}
    private static final Map<UUID,Carry> carries=new HashMap<>();
    private static final List<Ground> groundPieces=new ArrayList<>();
    private record Visual(Display.BlockDisplay display,long expires){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Visual> visuals=new HashMap<>();
    private static final Map<UUID,UUID> visualOwners=new HashMap<>();
    private static final List<Pulse> pulses=new ArrayList<>();
    private static final Map<UUID,Slam> slams=new HashMap<>();
    private static final Map<UUID,Long> landing=new HashMap<>();
    private static final Set<UUID> moving=new HashSet<>();
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;if(s.level==null)s.level=(ServerLevel)p.level();return s;}
    private static long now(Entity e){return e.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_tremor")&&p.getUUID().equals(FruitClaims.owner("tremor"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"tremor");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(Mastery.get(p,"cb_tremor_uses_"),Mastery.get(p,"cb_tremor_time_")):0;}
    public static void grant(Player p,int tier){Mastery.set(p,"cb_tremor_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_tremor_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,ItemStack item){if(item.isEmpty()||!Relics.kind(item).equals("fruit:tremor")||!FruitClaims.claimItem(p,item,"tremor"))return false;p.addTag("cb_fruit_tremor");grant(p,Relics.captureTier(item));p.addTag("cb_mode_fruit");if(Dark.unlocked(p))Dark.selectConsumed(p,"tremor");item.shrink(1);state(p);return true;}
    private static boolean usable(Player p){return selected(p)&&p.isAlive()&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)&&!StringFruit.controlled(p);}
    private static boolean valid(Player p,ServerLevel level){return usable(p)&&p.level()==level&&!p.isRemoved();}
    private static int scale(Player p,int a,int b,int c){return tier(p)>=3?c:tier(p)==2?b:a;}
    private static float bonus(Player p){return Mastery.harmony(p)?1.15f:1;}
    private static long saved(Player p,String prefix){for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void saved(Player p,String prefix,long value){for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);p.addTag(prefix+value);}
    private static void credit(Player p){State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_tremor_uses_",Mastery.get(p,"cb_tremor_uses_")+1);s.recent=now(p);}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&!StringFruit.isKnight(e)&&(e instanceof Enemy||e instanceof Player);}
    private static boolean hit(Player p,LivingEntity e,float damage,Vec3 push){if(!enemy(p,e)||!p.hasLineOfSight(e)||!e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),damage*bonus(p)))return false;e.setDeltaMovement(push);if(e instanceof ServerPlayer sp)sp.connection.send(new net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket(e));return true;}
    private static boolean weapon(Player p){String name=net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(p.getMainHandItem().getItem()).getPath();return name.endsWith("_sword")||name.endsWith("_axe")||name.equals("trident")||name.equals("mace");}
    private static Vec3 heading(Player p){Vec3 v=p.getLookAngle().multiply(1,0,1).normalize();return v.lengthSqr()<.01?new Vec3(0,0,1):v;}
    private static Vec3 aim(Player p,double range){Vec3 a=p.getEyePosition(),b=a.add(p.getLookAngle().scale(range));var h=p.level().clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return h.getType()==HitResult.Type.MISS?b:h.getLocation();}
    public static void press(Player p,int move){if(!usable(p)||move<0||move>2)return;State s=state(p);if(s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;}
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null){s.held=-1;if(s.charge!=null)s.charge.discard();s.charge=null;}}
    public static void release(Player p,int move){State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;cancelInput(p);Creeperborn.say(p,cast(p,move,held));}
    public static String cast(Player p,int move,long held){
        if(!usable(p)||move<0||move>2)return "Tremor powers unavailable.";boolean hold=held>=7;int slot=move*2+(hold?1:0);String key="cb_tremor_ready_"+slot+"_";
        if(now(p)<saved(p,key))return "Tremor move recovering.";
        if(hold&&held<(move==2?40:20))return "Charge "+(move==2?"Island Shaking for two seconds.":"the quake for one second.");
        if(p.getFoodData().getFoodLevel()<=6)return "Eat before using Tremor.";
        ServerLevel level=(ServerLevel)p.level();Vec3 forward=heading(p);String result;int cooldown;
        if(move==0){
            Vec3 end=aim(p,scale(p,5,7,9));
            if(!hold){LivingEntity target=null;double nearest=p.getEyePosition().distanceTo(end);for(var e:level.getEntitiesOfClass(LivingEntity.class,new AABB(p.getEyePosition(),end).inflate(.6))){if(!enemy(p,e))continue;var h=e.getBoundingBox().inflate(.4).clip(p.getEyePosition(),end);if(h.isPresent()&&p.getEyePosition().distanceTo(h.get())<nearest){target=e;end=h.get();nearest=p.getEyePosition().distanceTo(end);}}if(target!=null)hit(p,target,scale(p,7,10,14),p.getLookAngle().scale(1.2).add(0,.3,0));flash(p,end,p.getLookAngle(),scale(p,3,5,7),1,16);result=!weapon(p)?"Quake Punch — air shattered!":"Kabutowari — quake-coated strike!";cooldown=20;}
            else{Vec3 center=p.position().add(forward.scale(2)).add(0,1,0);burst(p,center,scale(p,4,7,10),scale(p,12,18,26));result="Gekishin!";cooldown=160;}
        }else if(move==1){
            if(!hold){pulse(p,p.getEyePosition().add(p.getLookAngle().scale(1)),p.getLookAngle(),scale(p,24,36,48),24,2,1.4,1.4,scale(p,7,10,14));result="Shock Ball!";cooldown=50;}
            else{flash(p,p.getEyePosition().add(forward.scale(2)),forward,8,1,20);Vec3 side=new Vec3(forward.z,0,-forward.x);Vec3 center=p.position().add(forward.scale(scale(p,6,9,12)));double offset=scale(p,10,15,20);wave(p,center.add(side.scale(offset)),side.scale(-1),offset,40,scale(p,10,16,24),4,scale(p,10,14,18));wave(p,center.add(side.scale(-offset)),side,offset,40,scale(p,10,16,24),4,scale(p,10,14,18));result="Kaishin — opposing ground tremors!";cooldown=500;}
        }else if(!hold){if(Paw.airborne(p)||airborne(p)||StringFruit.locked(p))return "Finish your current flight first.";Slam s=new Slam();s.owner=p;s.level=level;s.ends=now(p)+400;slams.put(p.getUUID(),s);landing.put(p.getUUID(),now(p)+200);result="Gurash — quake descent!";cooldown=120;}
        else{burst(p,p.position().add(0,1,0),scale(p,8,14,22),scale(p,10,16,24));flash(p,p.position().add(0,.2,0),new Vec3(0,1,0),scale(p,16,28,44),4,30);for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 center=p.position().add(Math.cos(a)*scale(p,4,7,11),0,Math.sin(a)*scale(p,4,7,11));for(int dx=-1;dx<=1;dx++)for(int dz=-1;dz<=1;dz++)groundPiece(p,center.add(dx,0,dz),1,30);}result="Shima Yurashi — Island Shaking!";cooldown=600;}
        saved(p,key,now(p)+Math.round(cooldown/bonus(p)));p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-(hold?3:1));credit(p);return result;
    }
    private static void burst(Player p,Vec3 center,double radius,float damage){for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius)))if(e.getBoundingBox().getCenter().distanceToSqr(center)<=radius*radius)hit(p,e,damage,e.position().subtract(center).multiply(1,0,1).normalize().scale(1.5).add(0,.8,0));flash(p,center,p.getLookAngle(),(float)radius,1,20);flash(p,center,new Vec3(0,1,0),(float)radius*2,4,20);}
    private static void wave(Player p,Vec3 from,Vec3 direction,double distance,int duration,double width,double height,float damage){pulse(p,from,direction,distance,duration,3,width,height,damage);}
    private static void pulse(Player p,Vec3 from,Vec3 direction,double distance,int duration,int kind,double width,double height,float damage){Pulse q=new Pulse();q.owner=p;q.level=(ServerLevel)p.level();q.from=from;q.to=from.add(direction.normalize().scale(distance));q.normal=direction.normalize();q.duration=duration;q.kind=kind;q.width=width;q.height=height;q.radius=kind==2?2:0;q.damage=damage;q.visual=display(p,from,direction,kind);transform(q.visual,from,direction,(float)width,(float)height,kind==3?2:1.4f);pulses.add(q);}
    private static Display.BlockDisplay display(Player p,Vec3 at,Vec3 facing,int kind){ServerLevel level=(ServerLevel)p.level();var d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(d==null)return null;d.addTag("cb_temporary_rock");d.setCustomName(Component.literal(kind==6?"cb_tremor_ground":"cb_tremor_"+kind));d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;a.creeperborn$duration(0);a.creeperborn$positionDuration(0);a.creeperborn$width(128);a.creeperborn$height(128);a.creeperborn$view(4);d.snapTo(at.x,at.y,at.z);if(!level.addFreshEntity(d))return null;visuals.put(d.getUUID(),new Visual(d,now(p)+40));visualOwners.put(d.getUUID(),p.getUUID());return d;}
    private static void transform(Display.BlockDisplay d,Vec3 at,Vec3 direction,float x,float y,float z){if(d==null)return;d.snapTo(at.x,at.y,at.z);d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),Paw.orientation(direction),new org.joml.Vector3f(x,y,z),new org.joml.Quaternionf()));}
    private static void flash(Player p,Vec3 at,Vec3 facing,float size,int kind,int duration){var d=display(p,at,facing,kind);if(d!=null){transform(d,at,facing,size,size,size);visuals.put(d.getUUID(),new Visual(d,now(p)+duration));}}
    private static void groundPiece(Player p,Vec3 center,float size,int duration){
        ServerLevel level=(ServerLevel)p.level();Vec3 above=center.add(0,4,0);if(!level.hasChunkAt(BlockPos.containing(above)))return;
        var hit=level.clip(new ClipContext(above,above.add(0,-16,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()==HitResult.Type.MISS)return;
        BlockPos block=BlockPos.containing(hit.getLocation().add(0,-.02,0));var state=level.getBlockState(block);if(state.isAir())return;
        Vec3 base=new Vec3(block.getX()+.5,hit.getLocation().y+.02,block.getZ()+.5);var d=display(p,base,new Vec3(0,0,1),6);if(d==null)return;d.setBlockState(state);visuals.put(d.getUUID(),new Visual(d,now(p)+duration));groundPieces.add(new Ground(d,base,now(p),duration,size));
    }
    public static java.util.List<net.minecraft.world.level.block.state.BlockState> groundStates(){return groundPieces.stream().filter(g->!g.display().isRemoved()).map(g->g.display().getBlockState()).toList();}
    private static void carry(Pulse q,LivingEntity e){Carry old=carries.get(e.getUUID());if(old!=null)return;Carry c=new Carry();c.owner=q.owner;c.target=e;c.level=q.level;c.center=q.to;c.ends=now(q.owner)+70;carries.put(e.getUUID(),c);}
    private static void endCarry(Carry c){carries.remove(c.target.getUUID());c.target.setDeltaMovement(Vec3.ZERO);c.target.resetFallDistance();landing.put(c.target.getUUID(),now(c.target)+40);}
    public static void tickVisual(Entity e){if(!(e.level() instanceof ServerLevel)||!(e instanceof Display.BlockDisplay)||e.getCustomName()==null||!e.getCustomName().getString().matches("cb_tremor_([1-6]|ground)"))return;Visual v=visuals.get(e.getUUID());if(v==null&&e.tickCount>2||v!=null&&now(e)>=v.expires()){e.discard();visuals.remove(e.getUUID());visualOwners.remove(e.getUUID());}}
    public static boolean airborne(LivingEntity p){return slams.containsKey(p.getUUID())||carries.containsKey(p.getUUID());}
    public static boolean locked(LivingEntity p){return airborne(p)&&!moving.contains(p.getUUID());}
    public static boolean immune(LivingEntity p,DamageSource source){return source.is(DamageTypeTags.IS_FALL)&&(airborne(p)||now(p)<landing.getOrDefault(p.getUUID(),0L));}
    public static int pulseCount(){return pulses.size();}public static int visualCount(){return visuals.size();}
    public static void tickPlayer(Player p){State s=state(p);if(!valid(p,s.level)){forget(p);return;}if(s.held>=0&&now(p)>s.heartbeat)cancelInput(p);
        if(s.held>=0&&now(p)-s.press>=7){Vec3 at=p.getEyePosition().add(p.getLookAngle().scale(1.3));if(s.charge==null)s.charge=display(p,at,p.getLookAngle(),5);if(s.charge!=null){float growth=.6f+Math.min(1,(now(p)-s.press)/40f)*1.4f;transform(s.charge,at,p.getLookAngle(),growth,growth,growth);visuals.put(s.charge.getUUID(),new Visual(s.charge,now(p)+40));}if(now(p)%10==0)Creeperborn.say(p,s.held==2?"Grab the air — Island Shaking "+Math.min(100,(now(p)-s.press)*100/40)+"%":"Quake charge "+Math.min(100,(now(p)-s.press)*100/20)+"%");}
        if(now(p)%20==0){if(s.recent+200>now(p)||s.last!=null&&s.last.distanceToSqr(p.position())>.04)Mastery.set(p,"cb_tremor_time_",Mastery.get(p,"cb_tremor_time_")+1);s.last=p.position();}}
    public static void tickEffects(){
        for(Pulse q:List.copyOf(pulses)){
            if(!valid(q.owner,q.level)||q.age>=q.duration||q.visual==null||q.visual.isRemoved()){if(q.visual!=null)q.visual.discard();pulses.remove(q);continue;}
            Vec3 from=q.from.lerp(q.to,q.age/(double)q.duration),next=q.from.lerp(q.to,++q.age/(double)q.duration);
            if(!q.level.hasChunkAt(BlockPos.containing(next))||!q.level.getWorldBorder().isWithinBounds(BlockPos.containing(next))){q.visual.discard();pulses.remove(q);continue;}
            boolean blocked=false;if(q.kind==2){var wall=q.level.clip(new ClipContext(from,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,q.owner));if(wall.getType()!=HitResult.Type.MISS){next=wall.getLocation();blocked=true;}}
            AABB box=new AABB(from,next).inflate(q.kind==3?q.width/2:1,q.kind==3?q.height:1,q.kind==3?q.width/2:1);
            Vec3 side=new Vec3(q.normal.z,0,-q.normal.x);for(var e:q.level.getEntitiesOfClass(LivingEntity.class,box)){if(!enemy(q.owner,e)||q.hit.contains(e.getUUID()))continue;Vec3 center=e.getBoundingBox().getCenter(),offset=center.subtract(next);boolean contact=q.kind==2?e.getBoundingBox().inflate(1).clip(from,next).isPresent()||e.getBoundingBox().inflate(1).contains(from):Math.abs(offset.dot(side))<=q.width/2&&Math.abs(offset.dot(q.normal))<=2&&center.y>=next.y-1&&center.y<=next.y+q.height;
                if(contact&&hit(q.owner,e,q.damage,q.normal.scale(q.kind==3?.6:1.4).add(0,q.kind==3?.05:.7,0))){if(q.kind==3)carry(q,e);q.hit.add(e.getUUID());flash(q.owner,center,q.normal,4,1,14);if(q.kind==2)blocked=true;}}
            if(q.kind==3&&q.age%5==0)for(int i=-2;i<=2;i++)groundPiece(q.owner,next.add(side.scale(i*q.width/5)),1.2f,10);
            transform(q.visual,next,q.normal,(float)q.width,(float)q.height,q.kind==3?2:1.4f);visuals.put(q.visual.getUUID(),new Visual(q.visual,now(q.owner)+40));if(blocked){q.visual.discard();pulses.remove(q);}
        }
        for(Carry c:List.copyOf(carries.values())){LivingEntity e=c.target;
            if(!valid(c.owner,c.level)||!e.isAlive()||e.isRemoved()||e.level()!=c.level||now(e)>=c.ends){endCarry(c);continue;}
            Vec3 toward=c.center.subtract(e.position()).multiply(1,0,1);double distance=toward.length();
            if(distance<=.6){if(c.arrived<0)c.arrived=now(e);if(now(e)-c.arrived>=10){hit(c.owner,e,scale(c.owner,6,8,10),Vec3.ZERO);flash(c.owner,e.getBoundingBox().getCenter(),new Vec3(0,0,1),4,1,12);endCarry(c);}continue;}
            Vec3 motion=toward.normalize().scale(Math.min(.75,distance));if(!c.level.getWorldBorder().isWithinBounds(BlockPos.containing(e.position().add(motion)))||StringFruit.blocked(e,e.position().add(motion))||!c.level.noCollision(e,e.getBoundingBox().expandTowards(motion))){endCarry(c);continue;}
            moving.add(e.getUUID());try{e.move(MoverType.SELF,motion);e.setDeltaMovement(Vec3.ZERO);e.resetFallDistance();if(e instanceof ServerPlayer sp)sp.teleportTo(c.level,e.getX(),e.getY(),e.getZ(),Set.of(),e.getYRot(),e.getXRot(),false);}finally{moving.remove(e.getUUID());}
        }
        for(Ground g:List.copyOf(groundPieces)){var d=g.display();if(d.isRemoved()||now(d)-g.starts()>=g.duration()){d.discard();groundPieces.remove(g);continue;}float lift=(float)Math.sin((now(d)-g.starts())/(double)g.duration()*Math.PI);d.snapTo(g.base().x,g.base().y+lift*.8,g.base().z);d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(-g.size()/2,0,-g.size()/2),new org.joml.Quaternionf().rotationXYZ(lift*.24f,0,lift*.17f),new org.joml.Vector3f(g.size(),.3f,g.size()),new org.joml.Quaternionf()));}
        for(Slam s:List.copyOf(slams.values())){Player p=s.owner;if(!valid(p,s.level)||now(p)>=s.ends){endSlam(s,false);continue;}Vec3 motion=new Vec3(0,s.age++<8?.55:-1.1,0);boolean blocked=!s.level.noCollision(p,p.getBoundingBox().expandTowards(motion))||StringFruit.blocked(p,p.position().add(motion))||p.getY()+motion.y<s.level.getMinY()||p.getY()+motion.y+p.getBbHeight()>=s.level.getMaxY();
            if(blocked){if(motion.y<0)endSlam(s,true);else s.age=8;continue;}moving.add(p.getUUID());try{p.move(MoverType.SELF,motion);p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();if(p instanceof ServerPlayer sp)sp.teleportTo(s.level,p.getX(),p.getY(),p.getZ(),Set.of(),p.getYRot(),p.getXRot(),false);}finally{moving.remove(p.getUUID());}
            if(s.age>=320)endSlam(s,false);
        }
        for(Visual v:List.copyOf(visuals.values()))if(v.display().isRemoved()||now(v.display())>=v.expires()){v.display().discard();visuals.remove(v.display().getUUID());visualOwners.remove(v.display().getUUID());}
    }
    private static void endSlam(Slam s,boolean impact){slams.remove(s.owner.getUUID());landing.put(s.owner.getUUID(),now(s.owner)+200);s.owner.setDeltaMovement(Vec3.ZERO);s.owner.resetFallDistance();if(impact)burst(s.owner,s.owner.position().add(0,1,0),scale(s.owner,5,8,12),scale(s.owner,8,12,18));}
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())if(unlocked(p)||states.containsKey(p.getUUID()))tickPlayer(p);tickEffects();landing.entrySet().removeIf(e->server.overworld().getGameTime()>=e.getValue());}
    public static void forget(Player p){for(Carry c:List.copyOf(carries.values()))if(c.owner==p||c.target==p)endCarry(c);for(var id:List.copyOf(visualOwners.keySet()))if(p.getUUID().equals(visualOwners.get(id))){Visual v=visuals.remove(id);if(v!=null)v.display().discard();visualOwners.remove(id);}
        cancelInput(p);states.remove(p.getUUID());Slam s=slams.get(p.getUUID());if(s!=null)endSlam(s,false);for(Pulse q:List.copyOf(pulses))if(q.owner==p){if(q.visual!=null)q.visual.discard();pulses.remove(q);}}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.owner);for(Pulse q:pulses)if(q.visual!=null)q.visual.discard();pulses.clear();slams.clear();for(Carry c:List.copyOf(carries.values()))endCarry(c);carries.clear();groundPieces.clear();for(Visual v:visuals.values())v.display().discard();visuals.clear();visualOwners.clear();landing.clear();moving.clear();}
    public static String status(Player p){return unlocked(p)?"Tremor tier "+tier(p)+"/3 | Z Quake Punch/Gekishin | X Shock Ball/Seaquake | C Gurash/Island Shaking":"Tremor locked";}
}
