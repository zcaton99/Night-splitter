package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.*;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Server-authoritative Azure forms, bounded attacks and temporary articulated dragon body. */
public final class Azure {
    public static final int DRAGON_TICKS=3000;
    private static final class State {
        Player owner;ServerLevel level;int form=1,held=-1,keys,blade,attackTicks;long press,heartbeat,inputUntil,ends=Long.MAX_VALUE,flightInput=-1,correction=-100,transition,recent=-1000,basicReady,flare,torchUntil,breathUntil,breathNext,twisterUntil,twisterNext;Vec3 heading,last;boolean moving,breathCommitted;float swing;
        final ArrayList<Vec3> path=new ArrayList<>();final ArrayList<Display.BlockDisplay> body=new ArrayList<>();final ArrayList<Mob> proxies=new ArrayList<>();Display.BlockDisplay charge;final Set<UUID> rushHit=new HashSet<>();
    }
    private static final class Shot {Player owner;ServerLevel level;Vec3 at,dir;float damage;double remaining,width;int kind;long ends;Display.BlockDisplay visual;Set<UUID> hit=new HashSet<>();}
    private record Visual(Player owner,Display.BlockDisplay entity,long ends){}
    private static final Map<UUID,State> states=new HashMap<>();
    private static final Map<UUID,Visual> visuals=new HashMap<>();
    private static final Map<UUID,Player> proxies=new HashMap<>();
    private static final List<Shot> shots=new ArrayList<>();
    private static final Set<UUID> moving=new HashSet<>();
    private static final Map<UUID,Long> landing=new HashMap<>();
    private static final Map<UUID,Map<Integer,Integer>> sent=new HashMap<>();
    private static final ThreadLocal<Boolean> redirect=ThreadLocal.withInitial(()->false);
    private static UUID clientPlayer;private static int clientFlags,clientKeys;
    private Azure(){}
    private static long now(Entity e){return e.level().getGameTime();}
    private static State state(Player p){State s=states.computeIfAbsent(p.getUUID(),k->new State());s.owner=p;s.level=(ServerLevel)p.level();return s;}
    public static boolean unlocked(Player p){return p.entityTags().contains("cb_fruit_azure")&&p.getUUID().equals(FruitClaims.owner("azure"));}
    public static boolean selected(Player p){return unlocked(p)&&p.entityTags().contains("cb_mode_fruit")&&Dark.matches(p,"azure");}
    public static int tier(Player p){return unlocked(p)?Rubber.progressTier(Mastery.get(p,"cb_azure_uses_"),Mastery.get(p,"cb_azure_time_")):0;}
    public static void grant(Player p,int tier){tier=Math.max(1,Math.min(3,tier));Mastery.set(p,"cb_azure_uses_",tier==3?40:tier==2?10:0);Mastery.set(p,"cb_azure_time_",tier==3?3600:tier==2?300:0);}
    public static boolean consume(Player p,ItemStack item){if(item.isEmpty()||!Relics.kind(item).equals("fruit:azure")||!FruitClaims.claimItem(p,item,"azure"))return false;p.addTag("cb_fruit_azure");p.addTag("cb_mode_fruit");grant(p,Relics.captureTier(item));if(Dark.unlocked(p))Dark.selectConsumed(p,"azure");state(p);item.shrink(1);return true;}
    private static boolean usable(Player p){return p.level() instanceof ServerLevel&&unlocked(p)&&p.isAlive()&&!p.isRemoved()&&!p.isSpectator()&&!p.isPassenger()&&!Dark.suppressed(p)&&!WaterRules.suppressed(p)&&!StringFruit.controlled(p)&&!PairPowers.rooted(p)&&!ClimatePowers.rooted(p)&&!Shackles.rooted(p);}
    private static boolean valid(Player p,ServerLevel level){return p.level()==level&&usable(p);}
    public static int form(Player p){State s=states.get(p.getUUID());return s==null?0:s.form==2&&s.ends<=now(p)?1:s.form;}
    public static long remaining(Player p){State s=states.get(p.getUUID());return s==null||s.form!=2?0:Math.max(0,s.ends-now(p));}
    public static void clientState(UUID id,int flags){clientPlayer=id;clientFlags=flags;}
    public static void clearClient(){clientPlayer=null;clientFlags=0;clientKeys=0;}
    public static boolean locked(LivingEntity p){if(moving.contains(p.getUUID()))return false;if(!(p.level() instanceof ServerLevel))return p.getUUID().equals(clientPlayer)&&(clientFlags&3)==2;if(moving.contains(p.getUUID()))return false;return p instanceof Player player&&form(player)==2;}
    public static boolean ownedProxy(Entity e,Entity owner){return proxies.get(e.getUUID())==owner;}
    public static boolean proxy(Entity e){return proxies.containsKey(e.getUUID());}
    public static boolean redirect(LivingEntity e,DamageSource source,float amount){Player owner=proxies.get(e.getUUID());if(owner==null)return false;if(!redirect.get()&&source.getEntity()!=owner&&source.getEntity()!=null&&owner.isAlive()&&owner.level()==e.level()){redirect.set(true);try{owner.hurtServer((ServerLevel)owner.level(),source,amount);}finally{redirect.set(false);}}return true;}
    public static float damage(Player p,DamageSource source,float amount){if(Phoenix.bypass(source)||source.typeHolder().equals(p.damageSources().drown().typeHolder()))return amount;int form=form(p);if(form==0||!usable(p))return amount;float reduction=form==2?.7f:tier(p)==3?.65f:tier(p)==2?.5f:.35f;if(ClimatePowers.masteredElement(source)){reduction*=source.getEntity() instanceof Player a&&Mastery.harmony(a)?0:.5f;}return amount*(1-reduction);}
    public static boolean immune(Player p,DamageSource source){return source.is(DamageTypeTags.IS_FALL)&&(form(p)==2||now(p)<landing.getOrDefault(p.getUUID(),0L));}
    private static long ready(Player p,String key){String prefix="cb_azure_ready_"+key+"_";for(String tag:p.entityTags())if(tag.startsWith(prefix))try{return Long.parseLong(tag.substring(prefix.length()));}catch(NumberFormatException ignored){}return 0;}
    private static void ready(Player p,String key,long at){String prefix="cb_azure_ready_"+key+"_";for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith(prefix))p.removeTag(tag);p.addTag(prefix+at);}
    private static void credit(Player p){State s=state(p);if(now(p)-s.recent>=20)Mastery.set(p,"cb_azure_uses_",Mastery.get(p,"cb_azure_uses_")+1);s.recent=now(p);}
    public static void press(Player p,int move){if(move<0||move>2||!usable(p)||!selected(p))return;State s=state(p);if(s.held>=0)return;s.held=move;s.breathCommitted=false;s.press=now(p);s.heartbeat=now(p)+15;}
    public static void heartbeat(Player p){State s=states.get(p.getUUID());if(s!=null&&s.held>=0)s.heartbeat=now(p)+15;}
    public static void cancelInput(Player p){State s=states.get(p.getUUID());if(s==null)return;s.held=-1;s.breathUntil=0;if(s.charge!=null)s.charge.discard();s.charge=null;}
    public static void release(Player p,int move){State s=states.get(p.getUUID());if(s==null||s.held!=move)return;long held=now(p)-s.press;boolean breathed=s.breathCommitted;cancelInput(p);if(breathed)return;String text=cast(p,move,held);if(!text.isEmpty())Creeperborn.say(p,text);}
    public static void input(Player p,int keys){if(keys<0||keys>127||!usable(p))return;State s=state(p);boolean basic=(keys&64)!=0&&(s.keys&64)==0;s.keys=keys;s.inputUntil=now(p)+10;if(basic&&selected(p)&&s.form>0&&now(p)>=s.basicReady&&s.held<0){s.basicReady=now(p)+16;s.flare=now(p)+8;melee(p,s.form==2?22:8+tier(p)*4,s.form==2?5:3,0);}}
    public static String cast(Player p,int move,long held){if(!usable(p)||!selected(p)||move<0||move>2)return "Azure powers unavailable.";State s=state(p);int tier=tier(p);long now=now(p);boolean charged=held>=20;
        if(move==2){
            if(charged){
                if(now<ready(p,"form"))return "Transformation recovering.";
                if(s.form==2){toHybrid(s);ready(p,"form",now+40);return "Azure Hybrid!";}
                if(tier<3)return "Full Dragon needs mastery: 40 uses and one active hour.";
                if(p.getFoodData().getFoodLevel()<=6)return "Eat above three food icons to transform.";
                Vec3 lift=p.position();for(int i=1;i<=6;i++){Vec3 next=p.position().add(0,i*.5,0);if(!safe(p,next))break;lift=next;}if(lift.y-p.getY()<1.5)return "Need more overhead space to take flight.";moveTo(p,lift);endForm(s);s.form=2;s.ends=now+DRAGON_TICKS;s.transition=now+30;s.heading=p.getLookAngle().normalize();ready(p,"form",now+40);p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);credit(p);seedPath(s);createBody(s);particles(p,p.position().add(0,1,0),0xFFA82C,40);return "Azure Dragon — 2:30! C tap Torch / tap again Flame Bagua; hold C returns to Hybrid.";
            }
            if(now<s.transition)return "Finish transforming first.";
            if(s.form==2){
                if(s.torchUntil>now){s.torchUntil=0;s.attackTicks=12;s.rushHit.clear();credit(p);return "Rising Dragon: Flame Bagua!";}
                if(now<ready(p,"ultimate"))return "Flame Dragon Torch recovering.";
                if(p.getFoodData().getFoodLevel()<10)return "Eat at least five food icons for Flame Dragon Torch.";
                ready(p,"ultimate",now+2400);p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-4);s.torchUntil=now+200;credit(p);return "Flame Dragon Torch! Tap C again for Rising Dragon: Flame Bagua. Z still breathes fire.";
            }
            if(now<ready(p,"clap"))return "Thunderclap recovering.";
            if(p.getFoodData().getFoodLevel()<=3)return "Eat to use Azure attacks.";
            ready(p,"clap",now+160);p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);double radius=tier==3?9:tier==2?6:4;
            for(var e:s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius))){Vec3 d=e.position().subtract(p.position());if(d.length()<=radius)hit(p,e,tier==3?12:tier==2?9:6,d.normalize().scale(.8).add(0,.25,0),false);}
            s.flare=now+12;flash(p,p.position().add(0,.4,0),new Vec3(0,0,1),(float)radius*2,16,18);credit(p);return "Thunderclap!";
        }
        if(now<s.transition)return "Finish transforming first.";
        String key=move==0?"z":"x";if(now<ready(p,key))return "Azure attack recovering.";
        if(p.getFoodData().getFoodLevel()<=3)return "Eat to use Azure attacks.";
        p.getFoodData().setFoodLevel(p.getFoodData().getFoodLevel()-1);
        if(move==0){
            if(s.form==2){
                if(charged){s.breathUntil=now+80;s.breathCommitted=true;s.breathNext=now;ready(p,key,now+200);credit(p);return "Sustained Bolo Breath!";}
                shoot(p,mouth(p),s.heading,34,100,2.2,5);ready(p,key,now+70);credit(p);return "Bolo Breath!";
            }
            s.flare=now+12;ready(p,key,now+(charged?100:35));float damage=charged?(tier==3?44:tier==2?30:20):(tier==3?28:tier==2?20:12);if(charged){s.attackTicks=6;s.rushHit.clear();s.swing=damage;}else{rush(p,tier==3?12:tier==2?8:5,damage);}credit(p);return charged?s.form==1?"Destroyer Bagua!":"Thunder Bellow Bagua!":"Thunder Bagua!";
        }
        if(charged&&s.form==2){shoot(p,mouth(p),s.heading,28,90,4,10);ready(p,key,now+180);credit(p);return "Dragon Twister — storm projectile!";}
        if(charged){s.twisterUntil=now+(s.form==2?80:40);s.twisterNext=now;ready(p,key,now+180);credit(p);return s.form==2?"Dragon Twister!":"Twister Smash!";}
        ready(p,key,now+60);int count=s.form==2?7:s.form==1?5:3;Vec3 dir=s.form==2?s.heading:p.getLookAngle();for(int i=0;i<count;i++){double a=(i-(count-1)/2.0)*.19;Vec3 fan=new Vec3(dir.x*Math.cos(a)-dir.z*Math.sin(a),dir.y,dir.z*Math.cos(a)+dir.x*Math.sin(a)).normalize();shoot(p,s.form==2?mouth(p):p.getEyePosition().add(dir.scale(1.2)),fan,tier==3?20:tier==2?14:9,s.form==2?110:tier==3?75:50,1.2,(s.blade++%2==0)?3:4);}credit(p);return "Demolition Gust — intertwined lightning blades!";
    }

    private static void ensureKanabo(Player p){
        if(p.entityTags().contains("cb_azure_kanabo_received"))return;
        ItemStack club=new ItemStack(net.minecraft.world.item.Items.NETHERITE_SWORD);
        club.set(net.minecraft.core.component.DataComponents.CUSTOM_NAME,Component.literal("Hassaikai — Azure Thunder Kanabo"));
        club.set(net.minecraft.core.component.DataComponents.ITEM_MODEL,net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","azure_kanabo"));
        club.enchant(p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SHARPNESS),5);
        club.enchant(p.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(net.minecraft.world.item.enchantment.Enchantments.UNBREAKING),3);
        if(p.getInventory().getFreeSlot()<0)return;p.getInventory().add(club);p.addTag("cb_azure_kanabo_received");
    }
    private static boolean enemy(Player p,LivingEntity e){return e!=p&&!proxy(e)&&e.isAlive()&&!e.isRemoved()&&!e.isSpectator()&&(e instanceof Enemy||e instanceof Player)&&!p.isAlliedTo(e)&&!(e instanceof Player other&&p.getTeam()!=null&&p.isAlliedTo(other));}
    private static boolean hit(Player p,LivingEntity e,float damage,Vec3 force,boolean fire){if(!enemy(p,e)||!p.hasLineOfSight(e))return false;boolean accepted=e.hurtServer((ServerLevel)p.level(),p.damageSources().playerAttack(p),damage);if(accepted){if(fire)e.setRemainingFireTicks(80);if(force!=null){if(e instanceof Player target)Rubber.impulse(target,force);else e.setDeltaMovement(e.getDeltaMovement().add(force));}particles(p,e.getBoundingBox().getCenter(),fire?0xFF9B24:0xBEEFFF,8);}return accepted;}
    private static void melee(Player p,float damage,double range,double splash){Vec3 look=p.getLookAngle();for(var e:((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(range))){Vec3 d=e.getBoundingBox().getCenter().subtract(p.getEyePosition());if(d.length()<=range&&d.normalize().dot(look)>.4)hit(p,e,damage,look.scale(.6).add(0,.15,0),false);}}
    private static void rush(Player p,double range,float damage){Vec3 dir=p.getLookAngle().multiply(1,0,1).normalize();Vec3 from=p.position(),end=from;for(double d=.5;d<=range;d+=.5){Vec3 next=from.add(dir.scale(d));if(!safe(p,next))break;end=next;}Vec3 at=end;for(var e:((ServerLevel)p.level()).getEntitiesOfClass(LivingEntity.class,new AABB(from,at).inflate(1.7)))if(enemy(p,e)&&(e.getBoundingBox().inflate(1.2).contains(from.add(0,1,0))||e.getBoundingBox().inflate(1.2).clip(from.add(0,1,0),at.add(0,1,0)).isPresent()))hit(p,e,damage,dir.scale(.8).add(0,.2,0),false);moveTo(p,end);flash(p,end.add(0,1,0),dir,5,6,14);}
    private static void shoot(Player p,Vec3 at,Vec3 dir,float damage,double range,double width,int kind){Shot q=new Shot();q.owner=p;q.level=(ServerLevel)p.level();q.at=at;q.dir=dir.normalize();q.damage=damage;q.remaining=range;q.width=width;q.kind=kind;q.ends=now(p)+100;q.visual=display(p,at,kind,100);shots.add(q);}
    private static Vec3 mouth(Player p){State s=states.get(p.getUUID());return p.position().add(0,1,0).add((s!=null&&s.heading!=null?s.heading:p.getLookAngle()).scale(1.8));}
    private static boolean safe(Player p,Vec3 at){ServerLevel l=(ServerLevel)p.level();Vec3 d=at.subtract(p.position());return l.hasChunkAt(BlockPos.containing(at))&&l.getWorldBorder().isWithinBounds(BlockPos.containing(at))&&at.y>=l.getMinY()&&at.y+p.getBbHeight()<l.getMaxY()&&!StringFruit.blocked(p,at)&&l.noCollision(p,p.getBoundingBox().expandTowards(d));}
    private static boolean moveTo(Player p,Vec3 at){return moveTo(p,at,true);}
    private static boolean moveTo(Player p,Vec3 at,boolean sync){if(!safe(p,at))return false;moving.add(p.getUUID());try{p.move(MoverType.SELF,at.subtract(p.position()));p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();if(sync&&p instanceof ServerPlayer sp)sp.teleportTo((ServerLevel)p.level(),p.getX(),p.getY(),p.getZ(),Set.of(),p.getYRot(),p.getXRot(),false);return true;}finally{moving.remove(p.getUUID());}}
    public static Vec3 flightMotion(float yaw,float pitch,int keys){int f=((keys&1)!=0?1:0)-((keys&2)!=0?1:0),r=((keys&8)!=0?1:0)-((keys&4)!=0?1:0);Vec3 raw=StringFruit.controlDirection(yaw,f,r);Vec3 h=raw.lengthSqr()>.001?raw.normalize().scale(.65):Vec3.ZERO;double vy=(keys&16)!=0?.45:(keys&32)!=0?-.45:0;if(f!=0)vy+=-Math.sin(Math.toRadians(pitch))*f*.5;return h.add(0,vy,0);}
    public static void clientInput(int flags){clientKeys=flags&127;}
    public static boolean clientTravel(Player p){if(p.level() instanceof ServerLevel||!p.getUUID().equals(clientPlayer)||(clientFlags&3)!=2)return false;Vec3 motion=(clientFlags&64)!=0?Vec3.ZERO:flightMotion(p.getYRot(),p.getXRot(),clientKeys);if((clientFlags&256)!=0)motion=motion.add(p.getLookAngle().scale(1.1));moving.add(p.getUUID());try{p.move(MoverType.SELF,motion);p.setDeltaMovement(Vec3.ZERO);p.resetFallDistance();}finally{moving.remove(p.getUUID());}return true;}
    public static void flightInput(Player p,AzureFlightPayload payload){if(!payload.valid()||!usable(p)||form(p)!=2)return;State s=state(p);if(s.flightInput==now(p))return;s.flightInput=now(p);p.setYRot(net.minecraft.util.Mth.wrapDegrees(payload.yaw()));p.setXRot(net.minecraft.util.Mth.clamp(payload.pitch(),-90,90));input(p,payload.keys());}
    public static void connectionMove(ServerPlayer p,net.minecraft.network.protocol.game.ServerboundMovePlayerPacket packet){float yaw=packet.getYRot(p.getYRot()),pitch=packet.getXRot(p.getXRot());if(Float.isFinite(yaw)&&Float.isFinite(pitch)){p.setYRot(net.minecraft.util.Mth.wrapDegrees(yaw));p.setXRot(net.minecraft.util.Mth.clamp(pitch,-90,90));}Vec3 client=new Vec3(packet.getX(p.getX()),packet.getY(p.getY()),packet.getZ(p.getZ()));State s=states.get(p.getUUID());if(s!=null&&Double.isFinite(client.x)&&Double.isFinite(client.y)&&Double.isFinite(client.z)&&client.distanceToSqr(p.position())>9&&now(p)-s.correction>=10){s.correction=now(p);p.teleportTo((ServerLevel)p.level(),p.getX(),p.getY(),p.getZ(),Set.of(),p.getYRot(),p.getXRot(),false);}}
    private static void seedPath(State s){s.path.clear();Vec3 heading=s.heading.lengthSqr()<.01?new Vec3(0,0,1):s.heading;Vec3 right=new Vec3(heading.z,0,-heading.x).normalize();for(int i=0;i<24;i++){double wave=Math.sin(i*.4)*Math.sin(i*Math.PI/24)*1.8;s.path.add(s.owner.position().add(0,1,0).subtract(heading.scale(i*.9)).add(right.scale(wave)).add(0,Math.sin(i*.25)*.35,0));}}
    private static void createBody(State s){for(int i=0;i<24;i++){s.body.add(display(s.owner,s.path.get(i),i==0?1:i==23?7:2,40));if(i%2==0){var e=EntityTypes.SLIME.create(s.level,EntitySpawnReason.TRIGGERED);if(e!=null){e.setSize(i==0?4:3,true);e.setNoAi(true);e.setNoGravity(true);e.setInvisible(true);e.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY,Integer.MAX_VALUE,0,false,false,false));e.setCustomName(Component.literal("cb_azure_hitbox:"+s.owner.getUUID()));e.setCustomNameVisible(false);e.setSilent(true);e.noPhysics=true;e.addTag("cb_temporary_rock");e.addTag("cb_azure_proxy");e.snapTo(s.path.get(i).x,s.path.get(i).y-.8,s.path.get(i).z);if(s.level.addFreshEntity(e)){proxies.put(e.getUUID(),s.owner);s.proxies.add(e);}}}}
        for(int i=0;i<4;i++)s.body.add(display(s.owner,s.path.get(i<2?4:17),i%2==0?8:9,40));}
    public static void tickPlayer(Player p){State s=states.get(p.getUUID());if(!usable(p)||s!=null&&s.level!=p.level()){if(s!=null)stopOwned(p);return;}s=state(p);long now=now(p);if(s.held>=0&&(now>s.heartbeat||!selected(p)))cancelInput(p);int keys=s.inputUntil>=now?s.keys:0;
        if(s.form==2&&(now>=s.ends||tier(p)<3||p.getFoodData().getFoodLevel()<=3))toHybrid(s);
        if(s.form==2&&now%200==0&&!p.isCreative())p.getFoodData().setFoodLevel(Math.max(0,p.getFoodData().getFoodLevel()-1));
        if(s.form==2){if(p instanceof ServerPlayer sp)((com.sibcraft.creeperborn.mixin.AirStepConnectionAccessor)sp.connection).creeperborn$resetAirStepCounter(0);Vec3 desired=p.getLookAngle().normalize();s.heading=s.heading==null?desired:s.heading.lerp(desired,s.breathUntil>now?.035:.12).normalize();Vec3 step=flightMotion(p.getYRot(),p.getXRot(),keys);s.moving=step.lengthSqr()>.001;if(now>=s.transition)moveTo(p,p.position().add(step),false);else moveTo(p,p.position(),false);animateBody(s);}
        if(s.transition>now){double a=now*.4;for(int i=0;i<6;i++)particles(p,p.position().add(Math.cos(a+i)*1.5,(s.transition-now)/12.0,Math.sin(a+i)*1.5),s.form==2?0xFFA62A:0x287BD0,1);}
        if(s.held>=0&&now-s.press>=7){Vec3 at=s.form==2?mouth(p):p.getEyePosition().add(p.getLookAngle().scale(1.1));if(s.charge==null)s.charge=display(p,at,6,40);position(s.charge,at,p.getLookAngle(),.6f+Math.min(2,(now-s.press)/20f));touch(s.charge,p,40);}
        if(selected(p)&&s.form==2&&s.held==0&&now-s.press>=20&&!s.breathCommitted&&now>=ready(p,"z")&&now>=s.transition){s.breathUntil=now+80;s.breathCommitted=true;s.breathNext=now;ready(p,"z",now+200);credit(p);}
        if(s.breathUntil>now){if(now>=s.breathNext){s.breathNext=now+10;shoot(p,mouth(p),s.heading,10,100,2.2,5);}if(s.held!=0&&s.breathUntil-now>70)s.breathUntil=now+70;}
        if(s.twisterUntil>now){double radius=s.form==2?9:tier(p)==3?9:tier(p)==2?6:4;if(now>=s.twisterNext){s.twisterNext=now+10;for(var e:s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(radius))){Vec3 d=p.position().subtract(e.position());if(d.length()>radius||!enemy(p,e))continue;boolean last=s.twisterUntil-now<=10;hit(p,e,5,d.normalize().scale(last?-1:.25).add(0,last?.5:.12,0),false);}flash(p,p.position().add(0,1,0),new Vec3(0,0,1),(float)radius*2,10,12);}}
        if(s.attackTicks>0){s.attackTicks--;if(s.form==2){Vec3 end=p.position().add(s.heading.scale(1.1));moveTo(p,end,false);for(var e:s.level.getEntitiesOfClass(LivingEntity.class,p.getBoundingBox().inflate(5)))if(enemy(p,e)&&s.rushHit.add(e.getUUID()))hit(p,e,60,s.heading.scale(1.5).add(0,.5,0),true);flash(p,mouth(p),s.heading,6,6,12);}else if(s.attackTicks==0){melee(p,s.swing,5,0);flash(p,p.position().add(p.getLookAngle().scale(2)).add(0,1,0),p.getLookAngle(),6,6,16);}}
        if(s.torchUntil>now&&s.form==2&&now%4==0){for(int i=0;i<s.path.size();i+=3)particles(p,s.path.get(i),0xFFAA24,4);}
        if(now%20==0&&unlocked(p))ensureKanabo(p);
        if(now%20==0){if(s.form>0||now-s.recent<200||s.last!=null&&p.position().distanceToSqr(s.last)>.04)Mastery.set(p,"cb_azure_time_",Mastery.get(p,"cb_azure_time_")+1);s.last=p.position();}
    }
    private static void animateBody(State s){Player p=s.owner;Vec3 head=p.position().add(0,1,0);s.path.set(0,head);for(int i=1;i<s.path.size();i++){Vec3 previous=s.path.get(i-1),old=s.path.get(i),d=old.subtract(previous);if(d.lengthSqr()<.001)d=s.heading.scale(-1);Vec3 target=previous.add(d.normalize().scale(.9));Vec3 next=old.lerp(target,.65),delta=next.subtract(previous);if(delta.length()>1.05)next=previous.add(delta.normalize().scale(1.05));s.path.set(i,next);}
        for(int i=0;i<24&&i<s.body.size();i++){Vec3 at=s.path.get(i),dir=i==0?s.heading:s.path.get(i-1).subtract(at).normalize();float size=i==0?3.2f:i==23?1.35f:2.4f-i*.045f;s.body.get(i).setCustomName(Component.literal("cb_azure_"+(s.torchUntil>now(p)||s.attackTicks>0?(i==0?13:i==23?15:14):(i==0?1:i==23?7:2))));position(s.body.get(i),at,dir,size);touch(s.body.get(i),p,40);}
        for(int i=0;i<4&&s.body.size()>27;i++){int index=i<2?4:17;Vec3 at=s.path.get(index),dir=s.path.get(index-1).subtract(at).normalize();s.body.get(24+i).setCustomName(Component.literal("cb_azure_"+(s.moving?(i%2==0?11:12):(i%2==0?8:9))));position(s.body.get(24+i),at,dir,2.1f);touch(s.body.get(24+i),p,40);if(now(p)%6==0){double side=i%2==0?-1:1;Vec3 right=new Vec3(dir.z,0,-dir.x).normalize().scale(side*1.7);particles(p,at.add(right).add(0,-.8,0),0xFFA92E,s.moving?4:2);}}
        for(int i=0;i<s.proxies.size();i++){Vec3 at=s.path.get(i*2);Mob e=s.proxies.get(i);e.snapTo(at.x,at.y-.8,at.z);e.setDeltaMovement(Vec3.ZERO);}
    }
    public static void tickEffects(){for(Shot q:List.copyOf(shots)){if(!valid(q.owner,q.level)||q.remaining<=0||now(q.owner)>=q.ends){remove(q);continue;}Vec3 end=q.at.add(q.dir.scale(Math.min(2,q.remaining)));if(!q.level.hasChunkAt(BlockPos.containing(end))){remove(q);continue;}var wall=q.level.clip(new ClipContext(q.at,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,q.owner));boolean stop=wall.getType()!=HitResult.Type.MISS;if(stop)end=wall.getLocation();for(var e:q.level.getEntitiesOfClass(LivingEntity.class,new AABB(q.at,end).inflate(q.width))){if(!enemy(q.owner,e)||q.hit.contains(e.getUUID()))continue;var box=e.getBoundingBox().inflate(q.width);if(box.contains(q.at)||box.clip(q.at,end).isPresent()){hit(q.owner,e,q.damage,q.dir.scale(.4),q.kind==5);q.hit.add(e.getUUID());}}q.remaining-=q.at.distanceTo(end);q.at=end;position(q.visual,end,q.dir,q.kind==5?4:q.kind==10?8:3.5f);touch(q.visual,q.owner,40);if(q.kind==5)particles(q.owner,end,0xFF8A20,4);if(stop)remove(q);}
        for(Visual v:List.copyOf(visuals.values()))if(v.entity().isRemoved()||now(v.entity())>=v.ends()){v.entity().discard();visuals.remove(v.entity().getUUID());}}
    private static void remove(Shot q){if(q.visual!=null)q.visual.discard();shots.remove(q);}
    private static Display.BlockDisplay display(Player p,Vec3 at,int kind,long duration){var level=(ServerLevel)p.level();var d=EntityTypes.BLOCK_DISPLAY.create(level,EntitySpawnReason.TRIGGERED);if(d==null)return null;d.addTag("cb_temporary_rock");d.setCustomName(Component.literal("cb_azure_"+kind));d.setCustomNameVisible(false);d.setBlockState(Blocks.STONE.defaultBlockState());var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;a.creeperborn$duration(2);a.creeperborn$positionDuration(2);a.creeperborn$width(12);a.creeperborn$height(12);a.creeperborn$view(4);d.snapTo(at.x,at.y,at.z);if(!level.addFreshEntity(d))return null;visuals.put(d.getUUID(),new Visual(p,d,now(p)+duration));return d;}
    private static void position(Display.BlockDisplay d,Vec3 at,Vec3 dir,float scale){if(d==null)return;d.snapTo(at.x,at.y,at.z);d.setTransformation(new com.mojang.math.Transformation(new org.joml.Vector3f(),Paw.orientation(dir),new org.joml.Vector3f(scale,scale,scale),new org.joml.Quaternionf()));}
    private static void touch(Display.BlockDisplay d,Player p,long duration){if(d!=null&&visuals.containsKey(d.getUUID()))visuals.put(d.getUUID(),new Visual(p,d,now(p)+duration));}
    private static void flash(Player p,Vec3 at,Vec3 dir,float size,int kind,int ticks){var d=display(p,at,kind,ticks);position(d,at,dir,size);}
    private static void particles(Player p,Vec3 at,int color,int count){if(p.level() instanceof ServerLevel level){level.sendParticles(new DustParticleOptions(color,1.2f),at.x,at.y,at.z,count,.22,.22,.22,.02);if(color==0xFFA92E){level.sendParticles(ParticleTypes.FLAME,at.x,at.y,at.z,2,.18,.08,.18,.005);level.sendParticles(ParticleTypes.SMOKE,at.x,at.y+.15,at.z,1,.12,.1,.12,.005);}}}
    public static void tickVisual(Entity e){if(!(e.level() instanceof ServerLevel))return;if(e.entityTags().contains("cb_azure_proxy")){if(!proxies.containsKey(e.getUUID())&&e.tickCount>2)e.discard();return;}if(e.getCustomName()==null||!e.getCustomName().getString().matches("cb_azure_([1-9]|1[0-6])"))return;Visual v=visuals.get(e.getUUID());if(v==null&&e.tickCount>2||v!=null&&now(e)>=v.ends()){e.discard();visuals.remove(e.getUUID());}}
    private static void toHybrid(State s){endForm(s);s.form=1;s.ends=Long.MAX_VALUE;}
    private static void endForm(State s){if(s.form>0)landing.put(s.owner.getUUID(),now(s.owner)+100);s.form=0;s.ends=0;s.attackTicks=0;s.flare=0;s.torchUntil=0;s.breathUntil=0;s.twisterUntil=0;for(var d:s.body)if(d!=null)d.discard();s.body.clear();s.path.clear();for(var e:s.proxies){proxies.remove(e.getUUID());e.discard();}s.proxies.clear();}
    public static void stopOwned(Player p){State s=states.remove(p.getUUID());if(s!=null){cancelState(s);endForm(s);}for(var q:List.copyOf(shots))if(q.owner==p)remove(q);for(var v:List.copyOf(visuals.values()))if(v.owner()==p){v.entity().discard();visuals.remove(v.entity().getUUID());}}
    private static void cancelState(State s){s.held=-1;if(s.charge!=null)s.charge.discard();s.charge=null;}
    public static void forget(Player p){stopOwned(p);sent.remove(p.getUUID());}
    public static int flags(Player p){State s=states.get(p.getUUID());if(!usable(p)||s==null)return 0;return s.form|(net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","azure_kanabo").equals(p.getMainHandItem().get(net.minecraft.core.component.DataComponents.ITEM_MODEL))?512:0)|(tier(p)<<2)|(s.moving||p.getDeltaMovement().horizontalDistanceSqr()>.005?16:0)|(s.flare>now(p)||s.torchUntil>now(p)?32:0)|(s.transition>now(p)?64:0)|(selected(p)?128:0)|(s.form==2&&s.attackTicks>0?256:0);}
    public static void tick(MinecraftServer server){for(var p:server.getPlayerList().getPlayers())if(unlocked(p)||states.containsKey(p.getUUID()))tickPlayer(p);tickEffects();landing.entrySet().removeIf(e->server.overworld().getGameTime()>=e.getValue());for(var viewer:server.getPlayerList().getPlayers()){Map<Integer,Integer> cache=sent.computeIfAbsent(viewer.getUUID(),k->new HashMap<>());Set<Integer> present=new HashSet<>();for(var p:server.getPlayerList().getPlayers()){if(p.level()!=viewer.level()||p.distanceToSqr(viewer)>256*256)continue;int flags=flags(p);present.add(p.getId());if((!Objects.equals(cache.get(p.getId()),flags)||server.overworld().getGameTime()%20==0)&&ServerPlayNetworking.canSend(viewer,AzureVisualPayload.TYPE)){ServerPlayNetworking.send(viewer,new AzureVisualPayload(p.getId(),flags));cache.put(p.getId(),flags);}}for(int id:Set.copyOf(cache.keySet()))if(!present.contains(id)){if(ServerPlayNetworking.canSend(viewer,AzureVisualPayload.TYPE))ServerPlayNetworking.send(viewer,new AzureVisualPayload(id,0));cache.remove(id);}}}
    public static double maxBodyGap(Player p){State s=states.get(p.getUUID());double max=0;if(s!=null)for(int i=1;i<s.path.size();i++)max=Math.max(max,s.path.get(i).distanceTo(s.path.get(i-1)));return max;}
    public static int visualCount(){return visuals.size();}public static int proxyCount(){return proxies.size();}public static int shotCount(){return shots.size();}
    public static void clear(){for(State s:List.copyOf(states.values()))stopOwned(s.owner);for(var v:visuals.values())v.entity().discard();states.clear();visuals.clear();proxies.clear();shots.clear();sent.clear();landing.clear();moving.clear();clearClient();}
    public static String status(Player p){return unlocked(p)?"Azure Dragon tier "+tier(p)+"/3 | Z Bagua/Breath | X Lightning Gust/Twister | C Thunderclap/Torch/Finisher; hold C Dragon/Hybrid | Form "+form(p)+(form(p)==2?" "+((remaining(p)+19)/20)+"s":" unlimited"):"Azure Dragon locked";}
}
