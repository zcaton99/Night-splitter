package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.phys.*;

/** Server-owned repulsion, bounded pain storage and collision-respecting flight. */
public final class Paw {
    private static final class State {
        Player owner;ServerLevel level;int held=-1;long press,heartbeat,recent=-1000,guard,bubbleEnds;
        long[] ready=new long[6];Vec3 last,bubblePos;float pain;boolean extracting,guiding;
        Display.BlockDisplay bubble,charge,shield;
    }
    private static final class Flight {
        Player owner;LivingEntity target;ServerLevel level;Vec3 direction;double remaining,cruise;int phase;long ends;boolean step;
    }
    private static final class Shot {
        Player owner;ServerLevel level;Vec3 position,direction;double remaining;long ends;float damage;boolean pain;Display.BlockDisplay display;
    }
    private record Visual(Display.BlockDisplay display,long expires){}
    private static final Map<UUID,Visual> visuals=new HashMap<>();
    private record Effect(Display.BlockDisplay display,long ends){}
    private static final TicketType FLIGHT_TICKET=new TicketType(100L,TicketType.FLAG_LOADING|TicketType.FLAG_SIMULATION);
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Flight> flights=new HashMap<>();
    private record Landing(LivingEntity target,long ends){}
    private static final Map<UUID,Landing> landing=new HashMap<>();
    private static final Set<UUID> moving=new HashSet<>();
    private static final List<Shot> shots=new ArrayList<>();
    private static final List<Effect> effects=new ArrayList<>();
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),id->new State());s.owner=p;if(s.level==null)s.level=(ServerLevel)p.level();return s;}
    private static long now(Entity e){return e.level().getGameTime();}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_paw")&&p.getUUID().equals(FruitClaims.owner("paw"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"paw");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(Mastery.get(p,"cb_paw_uses_"),Mastery.get(p,"cb_paw_time_")):0;}
    public static void grant(Player p,int tier){Mastery.set(p,"cb_paw_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_paw_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,ItemStack item){if(item.isEmpty()||!Relics.kind(item).equals("fruit:paw")||!FruitClaims.claimItem(p,item,"paw"))return false;p.addTag("cb_fruit_paw");grant(p,Relics.captureTier(item));p.addTag("cb_mode_fruit");if(Dark.unlocked(p))Dark.selectConsumed(p,"paw");item.shrink(1);state(p);return true;}
    private static boolean usable(Player p){return selected(p)&&p.isAlive()&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)&&!StringFruit.controlled(p);}
    private static boolean valid(Player p,ServerLevel level){return p.isAlive()&&!p.isRemoved()&&p.level()==level&&unlocked(p)&&!p.isSpectator()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p);}
    private static float bonus(Player p){return Mastery.harmony(p)?1.15f:1;}
    private static int scale(Player p,int a,int b,int c){return tier(p)>=3?c:tier(p)==2?b:a;}
    private static int cooldown(Player p,int ticks){return Math.round(ticks/bonus(p));}
    private static boolean fuel(Player p,int n){if(p.getFoodData().getFoodLevel()<=6)return false;p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-n);return true;}
    private static void credit(Player p){State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_paw_uses_",Mastery.get(p,"cb_paw_uses_")+1);s.recent=now(p);}
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&e.isAlive()&&!e.isSpectator()&&!p.isAlliedTo(e)&&!StringFruit.isKnight(e)&&(e instanceof Enemy||e instanceof Player);}
    private static boolean hit(Player p,LivingEntity e,float damage){return damage>0&&enemy(p,e)&&e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),damage*bonus(p));}
    private static Vec3 aim(Player p,double range){Vec3 a=p.getEyePosition(),b=a.add(p.getLookAngle().scale(range));var h=p.level().clip(new ClipContext(a,b,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,p));return h.getType()==HitResult.Type.MISS?b:h.getLocation();}
    private static LivingEntity target(Player p,double range){Vec3 a=p.getEyePosition(),b=aim(p,range);LivingEntity best=null;double distance=range;for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(a,b).inflate(.7))){if(e==p||!e.isAlive()||e.isSpectator()||StringFruit.isKnight(e))continue;var h=e.getBoundingBox().inflate(.35).clip(a,b);if(h.isPresent()&&a.distanceTo(h.get())<distance){distance=a.distanceTo(h.get());best=e;}}return best;}
    private static long saved(Player p,String prefix){for(String t:p.entityTags())if(t.startsWith(prefix))try{return Long.parseLong(t.substring(prefix.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void saved(Player p,String prefix,long value){for(String t:Set.copyOf(p.entityTags()))if(t.startsWith(prefix))p.removeTag(t);p.addTag(prefix+value);}
    public static int stepCapacity(Player p){return scale(p,3,4,5);}
    public static int stepDistance(Player p){return scale(p,8,14,50);}
    public static int stepCharges(Player p){
        int cap=stepCapacity(p);long oldCap=saved(p,"cb_paw_step_cap_");
        int n=oldCap==0?cap:(int)Math.max(0,Math.min(cap,saved(p,"cb_paw_step_charges_")+Math.max(0,cap-oldCap)));
        long refill=oldCap==0?now(p)+60:saved(p,"cb_paw_step_refill_");
        if(n>=cap){n=cap;refill=now(p)+60;}
        else if(now(p)>=refill){long gained=1+(now(p)-refill)/60;n=(int)Math.min(cap,n+gained);refill=n==cap?now(p)+60:refill+gained*60;}
        saved(p,"cb_paw_step_cap_",cap);saved(p,"cb_paw_step_charges_",n);saved(p,"cb_paw_step_refill_",refill);return n;
    }
    public static int guardTicks(Player p){return scale(p,20,40,100);}
    /** Runs from the native display tick, independently of owner/effect bookkeeping. */
    public static void tickVisual(Entity e){
        if(!(e.level() instanceof ServerLevel)||!(e instanceof Display.BlockDisplay)||e.getCustomName()==null||!e.getCustomName().getString().matches("cb_paw_[1-6]"))return;
        Visual v=visuals.get(e.getUUID());if((v==null&&e.tickCount>2)||(v!=null&&now(e)>=v.expires())){e.discard();visuals.remove(e.getUUID());}
    }
    public static int visualCount(){return visuals.size();}
    public static int launchDistance(Player p,int roll){return tier(p)>=3?(Math.floorMod(roll,100)==0?300:200+Math.floorMod(roll,21)):tier(p)==2?60+Math.floorMod(roll,21):20+Math.floorMod(roll,11);}
    public static boolean airborne(LivingEntity e){return flights.containsKey(e.getUUID());}
    public static boolean locked(LivingEntity e){return airborne(e)&&!moving.contains(e.getUUID());}
    public static boolean immune(LivingEntity e,DamageSource source){return source.is(DamageTypeTags.IS_FALL)&&(airborne(e)||landing.containsKey(e.getUUID())&&now(e)<landing.get(e.getUUID()).ends());}
    public static boolean guard(Player p,DamageSource source){State s=states.get(p.getUUID());if(s==null||!usable(p)||now(p)>=s.guard)return false;Entity attacker=source.getEntity();if(source.getDirectEntity() instanceof Projectile projectile){reflect(p,projectile);return true;}if(attacker instanceof LivingEntity&&p.distanceToSqr(attacker)<16){attacker.setDeltaMovement(attacker.position().subtract(p.position()).normalize().scale(.8).add(0,.25,0));return true;}return false;}
    public static void press(Player p,int move){if(!usable(p)||move<0||move>2)return;State s=state(p);if(s.held>=0)return;s.held=move;s.press=now(p);s.heartbeat=now(p)+15;s.extracting=false;}
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s==null)return;s.held=-1;s.guiding=false;s.extracting=false;if(s.charge!=null){s.charge.discard();s.charge=null;}}
    public static void release(Player p,int move){State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;s.held=-1;if(s.charge!=null){s.charge.discard();s.charge=null;}if(move==2&&s.extracting){s.guiding=false;fireBubble(s);s.extracting=false;return;}s.guiding=false;Creeperborn.say(p,cast(p,move,held));}
    public static String cast(Player p,int move,long held){
        if(!usable(p)||move<0||move>2)return "Paw powers unavailable.";State s=state(p);boolean hold=held>=7;int slot=move*2+(hold?1:0);
        if(move==2&&s.bubble!=null){fireBubble(s);return "Pain Bubble released!";}
        if(move==2&&hold){String result=extract(p);if(s.bubble!=null)fireBubble(s);return result;}
        if(!(move==1&&!hold)&&now(p)<s.ready[slot])return "Paw move recovering.";
        if(move==0){
            if(hold&&held<40)return "Ursus Shock needs a two-second charge.";if(!fuel(p,hold?3:1))return "Eat before using Paw.";
            if(hold){Vec3 center=p.position().add(0,p.getBbHeight()/2,0);double radius=scale(p,4,10,20);flash(s.level,center,new Vec3(0,0,1),(float)radius*2,6,12);
                for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(center,center).inflate(radius)))if(enemy(p,e)&&e.getBoundingBox().getCenter().distanceToSqr(center)<radius*radius&&p.hasLineOfSight(e)){hit(p,e,scale(p,8,12,18));e.setDeltaMovement(e.position().subtract(p.position()).normalize().scale(1.2).add(0,.45,0));}
            }else shoot(p,p.getEyePosition().add(p.getLookAngle().scale(.7)),p.getLookAngle(),scale(p,4,6,8),false,scale(p,24,32,40));
            s.ready[slot]=now(p)+cooldown(p,hold?200:12);credit(p);return hold?"Ursus Shock!":"Paw Cannon!";
        }
        if(move==1){
            if(!hold&&stepCharges(p)<=0)return "Paw Step charges recovering — one returns every 3 seconds.";if(p.getFoodData().getFoodLevel()<=6)return "Eat before repelling.";
            if(hold){LivingEntity e=target(p,6);if(e==null)return "Aim at a nearby enemy or player to launch.";if(flights.containsKey(e.getUUID()))return "That target is already airborne.";if(!startFlight(p,e,launchDistance(p,p.getRandom().nextInt(2100)),false))return "No clear launch space.";hit(p,e,scale(p,3,4,6));}
            else{if(!startFlight(p,p,stepDistance(p),true))return "Paw Step blocked.";saved(p,"cb_paw_step_charges_",stepCharges(p)-1);}
            fuel(p,hold?2:1);if(hold)s.ready[slot]=now(p)+cooldown(p,160);credit(p);return hold?"Paw Launch — up and away!":"Paw Step! "+stepCharges(p)+"/"+stepCapacity(p)+" charges";
        }
        s.guard=now(p)+guardTicks(p);s.ready[slot]=now(p)+cooldown(p,scale(p,100,120,180));s.shield=display(s.level,p.getEyePosition().add(p.getLookAngle().scale(1.5)),p.getLookAngle(),1.8f,3);credit(p);return "Repel Guard!";
    }
    public static String extract(Player p){
        if(!usable(p))return "Paw powers unavailable.";State s=state(p);if(s.bubble!=null)return "Pain Bubble already waiting.";
        if(now(p)<saved(p,"cb_paw_heal_ready_"))return "Pain extraction recovering.";
        float restored=Math.max(0,p.getMaxHealth()-p.getHealth());s.pain=Math.min(24,restored*1.2f);p.setHealth(p.getMaxHealth());p.getFoodData().setFoodLevel(20);p.getFoodData().setSaturation(20);
        if(tier(p)>=3){float old=p.getAbsorptionAmount();p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,600,1));p.setAbsorptionAmount(Math.max(old,6));}
        saved(p,"cb_paw_heal_ready_",now(p)+cooldown(p,1200));s.bubblePos=p.getEyePosition().add(p.getLookAngle().scale(2));s.bubble=display(s.level,s.bubblePos,p.getLookAngle(),1.3f,4);s.bubbleEnds=now(p)+240;s.guiding=true;s.extracting=true;credit(p);return "Pain extracted — full hearts and hunger! "+(s.pain>0?"Stored pain: "+Math.round(s.pain*bonus(p))+" damage.":"No missing health: this bubble is harmless.")+" Hold C to guide; release to launch.";
    }
    public static float bubbleDamage(Player p){State s=states.get(p.getUUID());return s==null?0:s.pain;}
    public static Vec3 bubblePosition(Player p){State s=states.get(p.getUUID());return s==null?null:s.bubblePos;}
    private static void clearBubble(State s){if(s.bubble!=null)s.bubble.discard();s.bubble=null;s.bubblePos=null;s.guiding=false;s.pain=0;}
    private static void fireBubble(State s){if(s.bubble==null)return;Vec3 direction=painAim(s.owner,48).subtract(s.bubblePos);if(direction.lengthSqr()<.0001)direction=s.owner.getLookAngle();shoot(s.owner,s.bubblePos,direction,s.pain,true,48);clearBubble(s);}
    private static void shoot(Player p,Vec3 at,Vec3 direction,float damage,boolean pain,double range){Shot s=new Shot();s.owner=p;s.level=(ServerLevel)p.level();s.position=at;s.direction=direction.normalize();s.damage=damage;s.pain=pain;s.remaining=range;s.ends=now(p)+(pain?120:60);s.display=display(s.level,at,direction,pain?1.3f:.7f,pain?4:1);shots.add(s);}
    private static LivingEntity intersects(Player p,Vec3 from,Vec3 to,double width){LivingEntity found=null;double nearest=Double.MAX_VALUE;for(var e:p.level().getEntitiesOfClass(LivingEntity.class,new AABB(from,to).inflate(width))){if(!enemy(p,e))continue;AABB box=e.getBoundingBox().inflate(width);if(box.contains(from))return e;var h=box.clip(from,to);if(h.isPresent()&&from.distanceTo(h.get())<nearest){found=e;nearest=from.distanceTo(h.get());}}return found;}
    private static Vec3 painAim(Player p,double range){Vec3 from=p.getEyePosition(),to=aim(p,range);LivingEntity e=intersects(p,from,to,.35);return e==null?to:e.getBoundingBox().getCenter();}
    private static void guide(State s){
        if(s.bubble==null)return;if(now(s.owner)>=s.bubbleEnds){clearBubble(s);return;}
        if(s.guiding){Vec3 next=s.bubblePos.lerp(painAim(s.owner,24),.16);var wall=s.level.clip(new ClipContext(s.bubblePos,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.owner));if(wall.getType()!=HitResult.Type.MISS)next=wall.getLocation().subtract(next.subtract(s.bubblePos).normalize().scale(.15));
            LivingEntity e=intersects(s.owner,s.bubblePos,next,.85);
            if(e!=null){
                if(s.pain<=0||hit(s.owner,e,s.pain)){flash(s.level,next,s.owner.getLookAngle(),2,4,6);clearBubble(s);return;}
                next=e.getBoundingBox().inflate(.85).clip(s.bubblePos,next).orElse(s.bubblePos).lerp(e.getBoundingBox().getCenter(),.02);
            }s.bubblePos=next;}
        position(s.bubble,s.bubblePos,s.owner.getLookAngle(),1.3f);
        if(s.guiding&&now(s.owner)%20==0)Creeperborn.say(s.owner,s.pain>0?"Pain Bubble: "+Math.round(s.pain*bonus(s.owner))+" damage stored — release C":"Pain Bubble: harmless — no health was restored");
    }
    public static boolean reflect(Player p,Projectile projectile){State s=states.get(p.getUUID());if(s==null||now(p)>=s.guard||!usable(p)||projectile.getOwner()==p)return false;Vec3 v=projectile.getDeltaMovement();if(v.dot(p.position().subtract(projectile.position()))<=0)return false;projectile.setOwner(p);projectile.setDeltaMovement(p.getLookAngle().scale(Math.max(1.4,v.length())));flash(s.level,projectile.position(),p.getLookAngle(),1,3,8);return true;}
    public static void tickPlayer(Player p){
        State s=state(p);long now=now(p);if(!valid(p,s.level)){forget(p);return;}
        if(!selected(p)){cancelInput(p);clearBubble(s);s.guard=0;}if(s.held>=0&&now>s.heartbeat)cancelInput(p);
        if(s.held==2&&now-s.press>=20&&s.bubble==null&&!s.extracting){String result=extract(p);if(s.bubble==null)s.extracting=true;Creeperborn.say(p,result);}
        if(s.held==2&&s.bubble!=null&&now-s.press>=7)s.guiding=true;guide(s);
        if(s.held==0&&now-s.press>=7&&now>=s.ready[1]){
            double charge=Math.min(1,(now-s.press)/40.0);float size=(float)(.7+2.5*(charge<.5?charge*2:(1-charge)*2));Vec3 at=p.getEyePosition().add(p.getLookAngle().scale(1.8));
            if(s.charge==null)s.charge=display(s.level,at,p.getLookAngle(),size,2);else position(s.charge,at,p.getLookAngle(),size);
            if(now%5==0)Creeperborn.say(p,"Ursus Shock "+Math.min(100,(now-s.press)*100/40)+"% — release Z");
        }
        if(s.shield!=null){if(now>=s.guard){s.shield.discard();s.shield=null;}else{position(s.shield,p.getEyePosition().add(p.getLookAngle().scale(1.5)),p.getLookAngle(),1.8f);for(var e:s.level.getEntitiesOfClass(Projectile.class,p.getBoundingBox().inflate(3)))reflect(p,e);}}
        if(now%20==0){if(selected(p)&&(s.recent+200>now||s.bubble!=null||s.last!=null&&s.last.distanceToSqr(p.position())>.04))Mastery.set(p,"cb_paw_time_",Mastery.get(p,"cb_paw_time_")+1);s.last=p.position();}
    }
    public static boolean startFlight(Player owner,LivingEntity target,double distance,boolean step){
        Flight previous=flights.get(target.getUUID());boolean replacing=previous!=null&&step&&previous.step&&previous.owner==owner&&target==owner;
        if(previous!=null&&!replacing||flights.size()>=32&&!replacing||flights.values().stream().filter(f->f.owner==owner&&f!=previous).count()>=2||StringFruit.controlled(target)||!target.isAlive())return false;
        ServerLevel level=(ServerLevel)target.level();Vec3 heading=step?owner.getLookAngle():new Vec3(owner.getLookAngle().x,0,owner.getLookAngle().z).normalize();if(heading.lengthSqr()<.01)heading=new Vec3(0,0,1);
        Vec3 initial=step?heading.scale(.8):new Vec3(0,1,0);if(!level.noCollision(target,target.getBoundingBox().expandTowards(initial))||StringFruit.blocked(target,target.position().add(initial)))return false;
        if(replacing)endFlight(previous);Flight f=new Flight();f.owner=owner;f.target=target;f.level=level;f.direction=heading;f.remaining=distance;f.step=step;f.phase=step?1:0;f.cruise=level.getMaxY()-8-target.getBbHeight();f.ends=now(owner)+600;target.stopRiding();target.ejectPassengers();target.resetFallDistance();flights.put(target.getUUID(),f);landing.put(target.getUUID(),new Landing(target,now(target)+800));flash(level,target.position().add(0,1,0),heading,2,1,10);return true;
    }
    public static double flightRemaining(LivingEntity e){Flight f=flights.get(e.getUUID());return f==null?0:f.remaining;}
    private static void endFlight(Flight f){flights.remove(f.target.getUUID());f.target.setDeltaMovement(Vec3.ZERO);f.target.resetFallDistance();landing.put(f.target.getUUID(),new Landing(f.target,now(f.target)+200));}
    private static void flight(Flight f){
        LivingEntity e=f.target;if(!valid(f.owner,f.level)||!e.isAlive()||e.isRemoved()||e.level()!=f.level||now(e)>=f.ends){endFlight(f);return;}
        Vec3 motion;
        if(f.phase==0){double rise=Math.min(2.5,f.cruise-e.getY());if(rise<=.1){f.phase=1;return;}motion=new Vec3(0,rise,0);}
        else if(f.phase==1){if(f.remaining<=.01){if(f.step){endFlight(f);return;}f.phase=2;return;}motion=f.direction.scale(Math.min(f.step?1.6:2.4,f.remaining));}
        else{if(e.onGround()){endFlight(f);return;}motion=new Vec3(0,-Math.min(2.5,e.getY()-f.level.getMinY()+1),0);}
        BlockPos block=BlockPos.containing(e.position().add(motion));if(!f.level.getWorldBorder().isWithinBounds(block)||block.getY()<f.level.getMinY()||block.getY()+e.getBbHeight()>=f.level.getMaxY()){endFlight(f);return;}
        // Short-lived native teleport tickets expire automatically, without persistent forced chunks.
        f.level.getChunkSource().addTicketWithRadius(FLIGHT_TICKET,new ChunkPos(block.getX()>>4,block.getZ()>>4),2);f.level.getChunk(block.getX()>>4,block.getZ()>>4);
        if(StringFruit.blocked(e,e.position().add(motion))||!f.level.noCollision(e,e.getBoundingBox().expandTowards(motion))){if(f.phase==2){motion=new Vec3(0,-.15,0);}else{f.phase=2;return;}}
        moving.add(e.getUUID());try{Vec3 before=e.position();e.move(MoverType.SELF,motion);e.setDeltaMovement(Vec3.ZERO);e.resetFallDistance();if(e instanceof ServerPlayer sp)sp.teleportTo(f.level,e.getX(),e.getY(),e.getZ(),Set.of(),e.getYRot(),e.getXRot(),false);if(f.phase==1)f.remaining=Math.max(0,f.remaining-e.position().distanceTo(before));if(f.phase==2&&e.position().distanceToSqr(before)<.0001)endFlight(f);}finally{moving.remove(e.getUUID());}
        if(now(e)%5==0)flash(f.level,e.position().add(0,1,0),f.direction,.8f,5,12);
    }
    public static void tickEffects(){
        for(Flight f:List.copyOf(flights.values()))flight(f);
        for(Shot s:List.copyOf(shots)){
            if(!valid(s.owner,s.level)||s.remaining<=0||now(s.owner)>=s.ends||s.display==null||s.display.isRemoved()){remove(s);continue;}Vec3 next=s.position.add(s.direction.scale(Math.min(s.pain?1.3:1.8,s.remaining)));BlockPos pos=BlockPos.containing(next);
            if(!s.level.hasChunkAt(pos)||!s.level.getWorldBorder().isWithinBounds(pos)){remove(s);continue;}
            var wall=s.level.clip(new ClipContext(s.position,next,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,s.owner));if(wall.getType()!=HitResult.Type.MISS)next=wall.getLocation();LivingEntity e=intersects(s.owner,s.position,next,s.pain?.85:.35);
            if(e!=null){
                boolean damaged=hit(s.owner,e,s.damage);
                if(s.pain&&s.damage>0&&!damaged){
                    Vec3 contact=e.getBoundingBox().inflate(.85).clip(s.position,next).orElse(s.position).lerp(e.getBoundingBox().getCenter(),.02);
                    s.remaining-=s.position.distanceTo(contact);s.position=contact;position(s.display,contact,s.direction,1.3f);continue;
                }
                if(damaged)e.setDeltaMovement(s.direction.scale(.65).add(0,.15,0));
                flash(s.level,next,s.direction,s.pain?2:1.3f,s.pain?4:1,s.pain?6:4);remove(s);continue;
            }
            if(wall.getType()!=HitResult.Type.MISS){remove(s);continue;}s.remaining-=s.position.distanceTo(next);s.position=next;position(s.display,next,s.direction,s.pain?1.3f:.7f);
        }
        for(Effect e:List.copyOf(effects))if(e.display()==null||e.display().isRemoved()||e.display().level().getGameTime()>=e.ends()){if(e.display()!=null)e.display().discard();effects.remove(e);}
        for(Visual v:List.copyOf(visuals.values()))if(v.display().isRemoved()||v.display().level().getGameTime()>=v.expires()){v.display().discard();visuals.remove(v.display().getUUID());}
        landing.entrySet().removeIf(e->e.getValue().target().isRemoved()||now(e.getValue().target())>=e.getValue().ends());
    }
    private static void remove(Shot s){if(s.display!=null)s.display.discard();shots.remove(s);}
    private static Display.BlockDisplay display(ServerLevel level,Vec3 at,Vec3 facing,float size,int kind){var d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(d==null)return null;d.addTag("cb_temporary_rock");d.addTag("cb_paw_visual");d.setCustomName(Component.literal("cb_paw_"+kind));d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;a.creeperborn$duration(0);a.creeperborn$positionDuration(0);a.creeperborn$width(64);a.creeperborn$height(64);a.creeperborn$view(3);position(d,at,facing,size);if(!level.addFreshEntity(d))return null;visuals.put(d.getUUID(),new Visual(d,level.getGameTime()+40));return d;}
    /** Explicit yaw/pitch preserves an upright local right axis, with no shortest-arc roll. */
    public static org.joml.Quaternionf orientation(Vec3 direction){Vec3 v=direction.lengthSqr()<.00001?new Vec3(0,0,1):direction.normalize();float yaw=(float)Math.atan2(v.x,v.z),pitch=(float)-Math.atan2(v.y,Math.sqrt(v.x*v.x+v.z*v.z));return new org.joml.Quaternionf().rotationYXZ(yaw,pitch,0);}
    private static void position(Display.BlockDisplay d,Vec3 at,Vec3 direction,float size){if(d==null)return;if(visuals.containsKey(d.getUUID()))visuals.put(d.getUUID(),new Visual(d,d.level().getGameTime()+40));d.snapTo(at.x,at.y,at.z);Vec3 v=direction.lengthSqr()<.001?new Vec3(0,0,1):direction.normalize();d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),orientation(v),new org.joml.Vector3f(size),new org.joml.Quaternionf()));}
    private static void flash(ServerLevel level,Vec3 at,Vec3 facing,float size,int kind,int ticks){var d=display(level,at,facing,size,kind);if(d!=null)effects.add(new Effect(d,level.getGameTime()+ticks));}
    public static String status(Player p){return unlocked(p)?"Paw tier "+tier(p)+"/3 | Step "+stepCharges(p)+"/"+stepCapacity(p)+" | Z Cannon/Shock | X Step/Launch | C Guard/Pain Bubble":"Paw locked";}
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())if(unlocked(p)||states.containsKey(p.getUUID()))tickPlayer(p);tickEffects();}
    public static void forget(Player p){State s=states.remove(p.getUUID());if(s!=null){clearBubble(s);if(s.charge!=null)s.charge.discard();if(s.shield!=null)s.shield.discard();}for(Flight f:List.copyOf(flights.values()))if(f.owner==p||f.target==p)endFlight(f);for(Shot s0:List.copyOf(shots))if(s0.owner==p)remove(s0);}
    public static void clear(){for(State s:List.copyOf(states.values()))forget(s.owner);for(Flight f:List.copyOf(flights.values()))endFlight(f);for(Shot s:List.copyOf(shots))remove(s);for(Effect e:effects)if(e.display()!=null)e.display().discard();effects.clear();for(Visual v:visuals.values())v.display().discard();visuals.clear();landing.clear();moving.clear();}
}
