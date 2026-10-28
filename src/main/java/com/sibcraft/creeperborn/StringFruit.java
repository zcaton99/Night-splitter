package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;

/** Server-owned String state. Temporary geometry never changes blocks or flight permissions. */
public final class StringFruit {
    private static final class State {
        Player owner; ServerLevel level; int held=-1;long press,heartbeat,recent=-1000,lastDamage,heal,guard,cloneEnds,glideEnds,landing,controlEnds,inputUntil,attackReady,inputAt=-1;
        final long[] ready=new long[6]; boolean gliding; Vec3 last,anchor,route; Zombie knight; Display.BlockDisplay knightVisual,web; LivingEntity puppet; boolean oldNoAi,attackQueued;
        int flags;float yaw,pitch; final List<Display.BlockDisplay> strings=new ArrayList<>();
    }
    private static final class Cage {Player owner;ServerLevel level;Vec3 center;double radius;long closing;Display.BlockDisplay visual;}
    private record Effect(Display.BlockDisplay display,long ends){}
    private record Spear(Player owner,ServerLevel level,Vec3 origin,Vec3 end,long starts,boolean god,int index,Set<UUID> hit,Display.BlockDisplay display){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Cage> cages=new HashMap<>();
    private static final List<Effect> effects=new ArrayList<>();
    private static final List<Spear> spears=new ArrayList<>();
    private static final Set<UUID> moving=new HashSet<>();
    private static final DustParticleOptions WHITE=new DustParticleOptions(0xFFFFFF,.7f),PURPLE=new DustParticleOptions(0xA354FF,.85f);
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;return s;}
    private static long now(Player p){return p.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_string")&&p.getUUID().equals(FruitClaims.owner("string"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"string");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(Mastery.get(p,"cb_string_uses_"),Mastery.get(p,"cb_string_time_")):0;}
    public static void grant(Player p,int tier){Mastery.set(p,"cb_string_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_string_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,net.minecraft.world.item.ItemStack item){
        if(item.isEmpty()||!Relics.kind(item).equals("fruit:string")||!FruitClaims.claimItem(p,item,"string"))return false;
        p.addTag("cb_fruit_string");grant(p,Relics.captureTier(item));p.addTag("cb_mode_fruit");if(Dark.unlocked(p))Dark.selectConsumed(p,"string");item.shrink(1);state(p).lastDamage=now(p);return true;
    }
    private static boolean usable(Player p){return selected(p)&&p.isAlive()&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)&&!controlled(p);}
    private static boolean valid(Player p,ServerLevel level){return unlocked(p)&&p.isAlive()&&!p.isRemoved()&&p.level()==level&&!p.isSpectator()&&!WaterRules.suppressed(p);}
    private static double bonus(Player p){return Mastery.harmony(p)?1.15:1;}
    public static int parasiteTicks(Player p){return (int)Math.round((tier(p)>=3?400:100)*bonus(p));}
    public static int knightTicks(Player p){return (int)Math.round((tier(p)>=3?1200:600)*bonus(p));}
    public static double cageRadius(Player p){return (tier(p)>=3?100:50)*bonus(p);}
    public static double range(Player p){return (tier(p)>=3?40:tier(p)==2?30:20)*bonus(p);}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&!isKnight(e)&&(e instanceof Enemy||e instanceof Player);}
    private static boolean fuel(Player p,int n){if(p.getFoodData().getFoodLevel()<=6||p.getFoodData().getFoodLevel()<n)return false;p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-n);return true;}
    private static void credit(Player p){State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_string_uses_",Mastery.get(p,"cb_string_uses_")+1);s.recent=now(p);}
    private static void hit(Player p,LivingEntity e,float n){if(enemy(p,e)&&e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),(float)(n*bonus(p))))credit(p);}
    private static Vec3 aim(Player p,double range){Vec3 from=p.getEyePosition(),end=from.add(p.getLookAngle().scale(range));var wall=p.level().clip(new ClipContext(from,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return wall.getType()==HitResult.Type.MISS?end:wall.getLocation();}
    private static LivingEntity target(Player p,double range){Vec3 from=p.getEyePosition(),end=aim(p,range);LivingEntity best=null;double distance=from.distanceTo(end)+.01;for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.6))){if(!enemy(p,e))continue;var clip=e.getBoundingBox().inflate(.3).clip(from,end);if(clip.isPresent()&&clip.get().distanceTo(from)<distance){distance=clip.get().distanceTo(from);best=e;}}return best;}
    private static long cooldown(Player p,int n){return Math.round(n/(Mastery.harmony(p)?1.15:1));}
    public static void press(Player p,int move){if(!usable(p)||move<0||move>2)return;State s=state(p);if(s.held>=0)return;
        if(s.puppet!=null){if(move==1||move==2)releaseControl(s);return;}
        s.held=move;s.press=now(p);s.heartbeat=now(p)+15;s.level=(ServerLevel)p.level();}
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s!=null){s.held=-1;releaseControl(s);}}
    public static void release(Player p,int move){State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;s.held=-1;Creeperborn.say(p,cast(p,move,held));}
    public static String cast(Player p,int move,long held){
        if(!usable(p)||move<0||move>2)return "String powers unavailable.";State s=state(p);s.level=(ServerLevel)p.level();boolean hold=held>=7;
        if(tier(p)<move+1)return "String: X unlocks at tier 2; C and Awakening at tier 3.";
        if(move==2&&hold&&cages.containsKey(p.getUUID())){Cage c=cages.get(p.getUUID());if(c.closing==0)c.closing=now(p);return "Birdcage closing — dissolves in 10 seconds.";}
        if(move==1&&!hold&&s.gliding){endGlide(s);return "String Glide ended.";}
        if(move==2&&!hold&&s.knight!=null&&s.knight.isAlive()){if(s.puppet!=null)releaseControl(s);else possess(p,s.knight,Math.max(1,s.cloneEnds-now(p)));return "Black Knight control — C or X to return.";}
        int slot=move*2+(hold?1:0);if(now(p)<s.ready[slot])return "String move recovering.";
        if(move==0){
            boolean awakened=awakened(p);
            if(hold&&awakened&&held<40)return "God Thread needs a 2-second charge.";
            if(!fuel(p,hold?2:1))return "Eat before using String.";
            LivingEntity aimed=target(p,range(p));Vec3 from=p.getEyePosition(),end=aimed==null?aim(p,range(p)):aimed.getEyePosition();boolean god=hold&&awakened;
            if(hold){
                Set<UUID> struck=new HashSet<>();int count=god?6:1;Cage cage=cages.get(p.getUUID());
                for(int i=0;i<count;i++){
                    Vec3 origin=from.add((i-count/2.0)*.55,1+(i%2)*.4,0).subtract(p.getLookAngle().scale(1.5));
                    if(god){origin=terrainOrigin(p,end,i,count);if(origin==null)continue;}
                    spears.add(new Spear(p,(ServerLevel)p.level(),origin,end,now(p)+i*2,god,i,struck,beam((ServerLevel)p.level(),origin,origin.add(0,.01,0),god?.25f:.12f,0)));
                }
            }else{
                Set<UUID> struck=new HashSet<>();
                Vec3 right=new Vec3(p.getLookAngle().z,0,-p.getLookAngle().x).normalize();
                for(int i=0;i<5;i++){
                    Vec3 offset=right.scale((i-2)*.55).add(0,(i-2)*.16,0);
                    Vec3 a=awakened?terrainOrigin(p,end,i,5):from.add(offset);
                    if(a==null)continue;Vec3 b=end.add(offset.scale(.5));
                    flash((ServerLevel)p.level(),a,b,.065f,awakened?0:3+i,12);
                    for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(.6)))
                        if(enemy(p,e)&&e.getBoundingBox().inflate(.6).clip(a,b).isPresent()&&struck.add(e.getUUID()))hit(p,e,6);
                }
            }
            s.ready[slot]=now(p)+cooldown(p,hold?160:15);credit(p);return god?"God Thread!":hold?"Overheat!":"Five-Color Slash!";
        }
        if(move==1){
            if(!hold){if(!fuel(p,1))return "Eat before gliding.";s.gliding=true;s.flags=0;s.inputUntil=now(p)+15;s.glideEnds=now(p)+(long)((tier(p)>=3?2400:600)*bonus(p));s.route=p.position();s.anchor=p.position().add(p.getLookAngle().scale(8));s.landing=now(p)+100;sync(s);credit(p);return "String Glide — W to move, release W to hover; aim to steer; Space transfers; X ends.";}
            if(held<20)return "Hold X for one second to attach Parasite.";LivingEntity t=target(p,range(p));if(t==null)return "Aim at a mob or player for Parasite.";
            if(!fuel(p,1))return "Eat before using Parasite.";
            if(!possess(p,t,parasiteTicks(p)))return "That target is already controlled.";
            s.ready[slot]=now(p)+parasiteTicks(p)+cooldown(p,200);credit(p);return "Parasite attached — WASD, mouse, attack; X releases. Your body is vulnerable!";
        }
        if(hold){
            if(!fuel(p,3))return "Birdcage needs food.";Cage c=new Cage();c.owner=p;c.level=(ServerLevel)p.level();c.center=p.position();c.radius=cageRadius(p);
            c.visual=display(c.level,c.center,"cb_string_cage",(float)c.radius);cages.put(p.getUUID(),c);s.ready[slot]=now(p)+cooldown(p,600);credit(p);return "Birdcage! Everyone is trapped. Hold C to close it.";
        }
        if(!fuel(p,2))return "Eat before summoning Black Knight.";
        Zombie knight=EntityTypes.HUSK.create((ServerLevel)p.level(),EntitySpawnReason.TRIGGERED);if(knight==null)return "Cannot summon knight.";
        Vec3 at=p.position().add(p.getLookAngle().multiply(2,0,2));knight.snapTo(at.x,at.y,at.z);knight.setNoAi(true);knight.setInvisible(true);knight.addTag("cb_string_knight");knight.addTag("cb_temporary_rock");
        if(!p.level().noCollision(knight,knight.getBoundingBox())||!((ServerLevel)p.level()).addFreshEntity(knight))return "Need clear space ahead for Black Knight.";
        knight.setHealth(knight.getMaxHealth());s.knight=knight;s.cloneEnds=now(p)+knightTicks(p);s.knightVisual=display((ServerLevel)p.level(),knight.position(),"cb_string_knight",1);
        s.guard=now(p)+(long)(80*bonus(p));s.web=display((ServerLevel)p.level(),p.position().add(0,1,0),"cb_string_web",2);
        s.ready[slot]=now(p)+knightTicks(p)+cooldown(p,200);credit(p);return "Black Knight summoned — web protection for four seconds; tap C to pilot.";
    }
    public static boolean awakened(Player p){Cage c=cages.get(p.getUUID());return tier(p)>=3&&c!=null&&c.level==p.level()&&p.position().distanceTo(c.center)<c.radius;}
    /** Select real exposed building/ground surfaces around the aimed point; never alter terrain. */
    public static Vec3 terrainOrigin(Player p,Vec3 end,int index,int count){
        ServerLevel level=(ServerLevel)p.level();double angle=index*Math.PI*2/count;
        Vec3 outward=new Vec3(Math.cos(angle),0,Math.sin(angle));
        Vec3 start=end.add(0,2,0),side=end.add(outward.scale(7)).add(0,2,0);
        var wall=level.clip(new ClipContext(start,side,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        if(wall.getType()!=HitResult.Type.MISS)return wall.getLocation().subtract(outward.scale(.08));
        Vec3 above=end.add(outward.scale(4)).add(0,10,0);
        var floor=level.clip(new ClipContext(above,above.add(0,-32,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));
        return floor.getType()==HitResult.Type.MISS?null:floor.getLocation().add(0,.08,0);
    }
    public static boolean isKnight(Entity e){return e.entityTags().contains("cb_string_knight");}
    public static boolean controlling(Player p){State s=states.get(p.getUUID());return s!=null&&s.puppet!=null;}
    public static boolean controlled(LivingEntity target){return states.values().stream().anyMatch(s->s.puppet==target);}
    public static boolean locked(LivingEntity e){return !moving.contains(e.getUUID())&&(controlled(e)||e instanceof Player p&&controlling(p));}
    public static boolean possess(Player p,LivingEntity target,long ticks){
        if(controlled(target)||target==p||target.isSpectator()||!target.isAlive()||target.level()!=p.level())return false;
        State s=state(p);releaseControl(s);endGlide(s);target.stopRiding();target.ejectPassengers();s.puppet=target;s.controlEnds=now(p)+ticks;s.inputUntil=now(p)+20;s.yaw=target.getYRot();s.pitch=target.getXRot();s.level=(ServerLevel)p.level();
        if(target instanceof Mob mob){s.oldNoAi=mob.isNoAi();mob.setNoAi(true);mob.getNavigation().stop();}
        for(int i=0;i<3;i++)s.strings.add(beam(s.level,p.getEyePosition(),target.getEyePosition().add((i-1)*.25,0,0),.035f,isKnight(target)?2:1));
        if(target instanceof Player victim)victim.sendSystemMessage(Component.literal("Parasite: your movement is being controlled for a limited time."));
        sync(s);return true;
    }
    private static void sync(State s){if(s.owner instanceof ServerPlayer p&&ServerPlayNetworking.canSend(p,StringControlPayload.TYPE))ServerPlayNetworking.send(p,new StringControlPayload(s.puppet==null?-1:s.puppet.getId(),(s.gliding?128:0)|(s.gliding&&tier(s.owner)>=3?64:0),s.yaw,s.pitch));}
    private static void releaseControl(State s){if(s.puppet==null)return;LivingEntity target=s.puppet;s.puppet=null;
        if(target instanceof Mob mob)mob.setNoAi(s.oldNoAi);target.setDeltaMovement(Vec3.ZERO);s.flags=0;s.attackQueued=false;s.strings.forEach(Entity::discard);s.strings.clear();sync(s);
        if(s.owner instanceof ServerPlayer sp&&sp.connection!=null&&sp.isAlive())sp.teleportTo((ServerLevel)sp.level(),sp.getX(),sp.getY(),sp.getZ(),Set.of(),sp.getYRot(),sp.getXRot(),false);
        if(target instanceof Player victim)victim.sendSystemMessage(Component.literal("Parasite control ended."));}
    public static void input(Player p,StringControlPayload packet){State s=states.get(p.getUUID());if(s==null)return;if(s.puppet==null){if(packet.entity()==-1&&s.gliding&&packet.valid()){s.flags=packet.flags();s.inputUntil=now(p)+15;}return;}if(packet.entity()!=s.puppet.getId()||!packet.valid())return;if((packet.flags()&32)!=0)s.attackQueued=true;if(s.inputAt==now(p))return;s.inputAt=now(p);s.flags=packet.flags();s.yaw=packet.yaw();s.pitch=Math.max(-90,Math.min(90,packet.pitch()));s.inputUntil=now(p)+15;}
    public static Vec3 controlDirection(float yaw,float forward,float right){double a=Math.toRadians(yaw);return new Vec3(-Math.sin(a)*forward-Math.cos(a)*right,0,Math.cos(a)*forward-Math.sin(a)*right);}
    private static void drive(State s){
        LivingEntity t=s.puppet;Player p=s.owner;if(t==null)return;
        if(!t.isAlive()||t.isRemoved()||t.level()!=p.level()||now(p)>=s.controlEnds||now(p)>s.inputUntil||p.distanceToSqr(t)>Math.pow(range(p)*1.5,2)||t instanceof Player tp&&(Dark.suppressed(tp)||WaterRules.suppressed(tp))){releaseControl(s);return;}
        t.setYRot(s.yaw);t.setYHeadRot(s.yaw);t.setYBodyRot(s.yaw);t.setXRot(s.pitch);float forward=((s.flags&1)!=0?1:0)-((s.flags&2)!=0?1:0),side=((s.flags&8)!=0?1:0)-((s.flags&4)!=0?1:0);
        Vec3 velocity=controlDirection(s.yaw,forward,side);
        if(velocity.lengthSqr()>0)velocity=velocity.normalize().scale(.24);
        double vy=t.onGround()?((s.flags&16)!=0?.42:0):Math.max(-.8,t.getDeltaMovement().y-.08);
        moving.add(t.getUUID());try{t.setDeltaMovement(velocity.add(0,vy,0));t.move(MoverType.SELF,t.getDeltaMovement());if(t instanceof ServerPlayer sp)sp.teleportTo((ServerLevel)t.level(),t.getX(),t.getY(),t.getZ(),Set.of(),s.yaw,s.pitch,false);}finally{moving.remove(t.getUUID());}
        if((s.attackQueued||(s.flags&32)!=0)&&now(p)>=s.attackReady){s.attackQueued=false;s.attackReady=now(p)+15;Vec3 from=t.getEyePosition(),end=from.add(t.getLookAngle().scale(t==s.knight?12:3));LivingEntity victim=null;double nearest=Double.MAX_VALUE;
            for(var e:t.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,end).inflate(.7))){if(!enemy(p,e)||e==t||!t.hasLineOfSight(e))continue;var h=e.getBoundingBox().inflate(.4).clip(from,end);if(h.isPresent()&&h.get().distanceTo(from)<nearest){victim=e;nearest=h.get().distanceTo(from);}}
            if(victim!=null){t.swing(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.component.SwingAnimation.DEFAULT,true);hit(p,victim,t==s.knight?4:3);flash(s.level,from,victim.getEyePosition(),.035f,t==s.knight?2:1,5);}}
        for(int i=0;i<s.strings.size();i++)positionBeam(s.strings.get(i),p.getEyePosition(),t.getEyePosition().add((i-1)*.25,0,0),.035f);
        if(now(p)%10==0)Creeperborn.say(p,(t==s.knight?"Black Knight":"Parasite")+" control "+(s.controlEnds-now(p)+19)/20+"s | WASD + attack | X/C: return");
    }
    public static boolean insideCage(Entity e){for(Cage c:cages.values())if(c.level==e.level()&&e.position().distanceTo(c.center)<c.radius)return true;return false;}
    public static boolean crosses(Vec3 a,Vec3 b,Vec3 center,double radius){
        double da=a.distanceTo(center),db=b.distanceTo(center);boolean ia=da<radius,ib=db<radius;if(ia!=ib)return true;
        if(!ia){Vec3 delta=b.subtract(a);double n=delta.lengthSqr();if(n>1e-8){double t=Math.max(0,Math.min(1,center.subtract(a).dot(delta)/n));return a.add(delta.scale(t)).distanceTo(center)<radius;}}return false;
    }
    /** Position gate catches vanilla movement and same-dimension teleports, including other fruit abilities. */
    public static boolean blocked(Entity e,Vec3 destination){
        if(!(e.level() instanceof ServerLevel)||e.isRemoved()||!(e instanceof LivingEntity)&&!e.isVehicle())return false;
        if(e instanceof LivingEntity l&&locked(l)&&e.position().distanceToSqr(destination)>.0001)return true;
        for(Cage c:cages.values())if(e.level()==c.level&&crosses(e.position(),destination,c.center,c.radius))return true;return false;
    }
    public static boolean canTeleport(Entity e,ServerLevel level,Vec3 to){if(e.level()!=level)return !insideCage(e)&&cages.values().stream().noneMatch(c->c.level==level&&to.distanceTo(c.center)<c.radius);return !blocked(e,to);}
    private static void moveImpulse(Player p,Vec3 v){Rubber.impulse(p,v);}
    private static void endGlide(State s){if(!s.gliding)return;s.gliding=false;s.flags=0;s.landing=now(s.owner)+100;moveImpulse(s.owner,Vec3.ZERO);sync(s);}
    public static boolean immune(Player p,DamageSource source){State s=states.get(p.getUUID());return s!=null&&unlocked(p)&&source.is(DamageTypeTags.IS_FALL)&&(s.gliding||now(p)<s.landing);}
    public static float damage(Player p,DamageSource source,float amount){State s=states.get(p.getUUID());if(s==null||!unlocked(p)||now(p)>=s.guard||Phoenix.bypass(source))return amount;return source.getEntity() instanceof LivingEntity?amount*(Mastery.harmony(p)?.15f:.25f):amount;}
    public static void damaged(Player p){if(unlocked(p))state(p).lastDamage=now(p);}
    private static void awaken(State s){
        Player p=s.owner;ServerLevel level=(ServerLevel)p.level();
        for(int i=0;i<8;i++){double a=i*Math.PI/4;Vec3 at=p.position().add(Math.cos(a)*3,3,Math.sin(a)*3);var hit=level.clip(new ClipContext(at,at.add(0,-10,0),ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));if(hit.getType()==HitResult.Type.MISS)continue;Vec3 ground=hit.getLocation().add(0,.03,0);flash(level,ground,ground.add(Math.cos(a)*2,0,Math.sin(a)*2),.025f,0,30);flash(level,ground,p.position().add(0,1,0),.025f,0,20);}
    }
    public static Vec3 glideVelocity(Player p,Vec3 anchor,int flags,boolean fresh){return fresh&&(flags&1)!=0?anchor.subtract(p.position()).normalize().scale(.65*bonus(p)):Vec3.ZERO;}
    private static void glide(State s){Player p=s.owner;if(!s.gliding)return;long now=now(p);
        if(now>=s.glideEnds||p.getFoodData().getFoodLevel()<=6||now%200==0&&!fuel(p,1)){endGlide(s);return;}
        Vec3 look=p.getLookAngle(),next=p.position().add(look.scale(8));Cage c=cages.get(p.getUUID());
        if(c!=null){Vec3 d=next.subtract(c.center);if(d.length()>c.radius-2)next=c.center.add(d.normalize().scale(c.radius-2));}
        if(s.anchor==null||p.position().distanceTo(s.anchor)<1||look.dot(s.anchor.subtract(p.position()).normalize())<.7||(s.flags&16)!=0){s.route=p.position();s.anchor=next;}
        Vec3 velocity=glideVelocity(p,s.anchor,s.flags,now<=s.inputUntil);BlockPos block=BlockPos.containing(p.position().add(velocity));
        if(!p.level().hasChunkAt(block)||!p.level().getWorldBorder().isWithinBounds(block)||!p.level().noCollision(p,p.getBoundingBox().move(velocity))||blocked(p,p.position().add(velocity))){moveImpulse(p,Vec3.ZERO);return;}
        moveImpulse(p,velocity);s.landing=now+100;
        if(tier(p)>=3&&now%40==0)awaken(s);
        if(now%8==0){Vec3 a=s.route.add(0,-.1,0),b=s.anchor.add(look.scale(5));flash(s.level,a,b,.018f,0,12);}
    }
    public static void tickPlayer(Player p){State s=state(p);long now=now(p);
        if(!valid(p,s.level==null?(ServerLevel)p.level():s.level)){forget(p);return;}
        if(!selected(p)||Dark.suppressed(p)){cancelInput(p);endGlide(s);}if(s.held>=0&&now>s.heartbeat)s.held=-1;
        if(s.held==1&&now-s.press>=7&&now%4==0){LivingEntity t=target(p,range(p));if(t!=null)flash((ServerLevel)p.level(),p.getEyePosition(),t.getEyePosition(),.025f,1,5);}
        if(s.held==0&&now-s.press>=7&&now%4==0){Vec3 at=p.getEyePosition();for(int i=0;i<(awakened(p)?6:1);i++)flash((ServerLevel)p.level(),at.add((i-2)*.4,.8,-1),at.add((i-2)*.4,1.4,-1),.07f,0,5);Creeperborn.say(p,(awakened(p)?"God Thread "+Math.min(100,(now-s.press)*100/40)+"%":"Overheat ready")+" — release Z");}
        drive(s);glide(s);if(now%10==0)sync(s);
        if(s.web!=null){if(now>=s.guard){s.web.discard();s.web=null;}else s.web.snapTo(p.getX(),p.getY()+1,p.getZ());}
        if(s.knight!=null){if(!s.knight.isAlive()||now>=s.cloneEnds||s.knight.level()!=p.level()){clearKnight(s);}else{
            if(s.knightVisual!=null)s.knightVisual.snapTo(s.knight.getX(),s.knight.getY(),s.knight.getZ());
            if(s.puppet!=s.knight){LivingEntity t=s.puppet;
                if(t==null)t=s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(24)).stream().filter(e->enemy(p,e)).min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
                if(t!=null&&s.knight.hasLineOfSight(t)){
                    double dist=s.knight.distanceToSqr(t);if(dist>64){Vec3 dir=t.position().subtract(s.knight.position()).normalize().scale(.18);s.knight.setDeltaMovement(dir.add(0,s.knight.onGround()?0:s.knight.getDeltaMovement().y,0));}
                    if(dist<144&&now%20==0){flash(s.level,s.knight.getEyePosition(),t.getEyePosition(),.025f,2,6);hit(p,t,4);}}
            }
        }}
        {
            if(now-s.lastDamage>=140&&now>=s.heal&&p.getHealth()<p.getMaxHealth()&&p.getFoodData().getFoodLevel()>6&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)){
                p.heal(1);p.getFoodData().addExhaustion(1);s.heal=now+Math.round(60/bonus(p));((ServerLevel)p.level()).sendParticles(WHITE,p.getX(),p.getY()+1,p.getZ(),12,.3,.6,.3,0);}
            if(now%20==0){boolean active=s.last!=null&&s.last.distanceToSqr(p.position())>.04||now-s.recent<200||s.gliding||s.puppet!=null;s.last=p.position();
            if(active&&selected(p)){int before=tier(p);Mastery.set(p,"cb_string_time_",Mastery.get(p,"cb_string_time_")+1);if(tier(p)>before)Creeperborn.say(p,"String mastery increased!");}}
        }
    }
    private static void clearKnight(State s){if(s.puppet==s.knight)releaseControl(s);if(s.knight!=null)s.knight.discard();s.knight=null;if(s.knightVisual!=null)s.knightVisual.discard();s.knightVisual=null;}
    private static void removeCage(Cage c){cages.remove(c.owner.getUUID());if(c.visual!=null)c.visual.discard();}
    public static void strongSuppress(Player p){forget(p);}
    public static void tickEffects(){
        for(Cage c:List.copyOf(cages.values())){if(!valid(c.owner,c.level)){removeCage(c);continue;}if(c.closing>0){double t=(now(c.owner)-c.closing)/200.0;if(t>=1){removeCage(c);continue;}scale(c.visual,(float)(c.radius+(10-c.radius)*t));}}
        for(Effect e:List.copyOf(effects))if(e.display().isRemoved()||e.display().level().getGameTime()>=e.ends()){e.display().discard();effects.remove(e);}
        for(Spear s:List.copyOf(spears)){
            if(!valid(s.owner(),s.level())||Dark.suppressed(s.owner())){removeSpear(s);continue;}long age=now(s.owner())-s.starts();if(age<0)continue;
            if(age>12){removeSpear(s);continue;}Vec3 a=s.origin().lerp(s.end(),Math.max(0,(age-2)/12.0)),b=s.origin().lerp(s.end(),Math.min(1,(age+1)/12.0));positionBeam(s.display(),s.origin(),b,s.god()?.22f:.1f);
            for(var e:s.level().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(.65))){if(!enemy(s.owner(),e)||!e.getBoundingBox().inflate(.65).clip(a,b).isPresent())continue;
                if(s.god()){
                    // Each spear may hit once: six direct hits total 24 health before mastery bonus.
                    UUID key=UUID.nameUUIDFromBytes((e.getUUID()+":"+s.index()).getBytes(java.nio.charset.StandardCharsets.UTF_8));if(s.hit().add(key)){e.damageCooldownTime=0;hit(s.owner(),e,4);}
                }else if(s.hit().add(e.getUUID()))hit(s.owner(),e,12);
            }
        }
    }
    private static void removeSpear(Spear s){if(s.display()!=null)s.display().discard();spears.remove(s);}
    private static Display.BlockDisplay display(ServerLevel level,Vec3 at,String name,float size){var d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(d==null)return null;d.addTag("cb_temporary_rock");d.setCustomName(Component.literal(name));d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());
        var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;a.creeperborn$duration(0);a.creeperborn$positionDuration(0);a.creeperborn$width(256);a.creeperborn$height(256);a.creeperborn$view(4);d.snapTo(at.x,at.y,at.z);scale(d,size);return level.addFreshEntity(d)?d:null;}
    private static void scale(Display.BlockDisplay d,float n){if(d!=null)d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),new org.joml.Quaternionf(),new org.joml.Vector3f(n),new org.joml.Quaternionf()));}
    private static Display.BlockDisplay beam(ServerLevel level,Vec3 a,Vec3 b,float width,int kind){var d=display(level,a,"cb_string_beam_"+kind,1);positionBeam(d,a,b,width);return d;}
    private static void positionBeam(Display.BlockDisplay d,Vec3 a,Vec3 b,float width){if(d==null)return;Vec3 v=b.subtract(a);float length=(float)v.length();d.snapTo(a.x,a.y,a.z);org.joml.Quaternionf q=new org.joml.Quaternionf().rotationTo(new org.joml.Vector3f(0,1,0),new org.joml.Vector3f((float)v.x,(float)v.y,(float)v.z).normalize());d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),q,new org.joml.Vector3f(width,Math.max(.001f,length),width),new org.joml.Quaternionf()));}
    private static void flash(ServerLevel level,Vec3 a,Vec3 b,float width,int kind,int ticks){var d=beam(level,a,b,width,kind);if(d!=null)effects.add(new Effect(d,level.getGameTime()+ticks));}
    public static int cageCount(){return cages.size();}
    public static String status(Player p){State s=states.get(p.getUUID());return "String "+(unlocked(p)?"tier "+tier(p)+"/3 | Parasite "+parasiteTicks(p)/20+"s | Knight "+knightTicks(p)/20+"s | Awakening "+(tier(p)>=3)+" | Cage "+cages.containsKey(p.getUUID())+(s!=null&&s.puppet!=null?" | controlling "+s.puppet.getName().getString():""):"locked");}
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())if(unlocked(p)||states.containsKey(p.getUUID()))tickPlayer(p);tickEffects();}
    public static void forget(Player p){State s=states.remove(p.getUUID());if(s!=null){s.held=-1;releaseControl(s);endGlide(s);clearKnight(s);if(s.web!=null)s.web.discard();}Cage c=cages.get(p.getUUID());if(c!=null)removeCage(c);for(State other:List.copyOf(states.values()))if(other.puppet==p)releaseControl(other);for(Spear spear:List.copyOf(spears))if(spear.owner()==p)removeSpear(spear);}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.owner);for(Cage c:List.copyOf(cages.values()))removeCage(c);for(Effect e:effects)e.display().discard();effects.clear();moving.clear();}
}
