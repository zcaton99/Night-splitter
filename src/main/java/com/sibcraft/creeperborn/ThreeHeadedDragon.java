package com.sibcraft.creeperborn;

import java.nio.file.Path;
import java.util.*;
import com.mojang.math.Transformation;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** Multipart encounter: shared body health and three independently damageable, severable heads.
 * Vanilla synced display entities form one animated dragon; invisible living proxies provide hitboxes.
 * No vanilla Ender Dragon fight state or exit portal is replaced by this encounter.
 */
public final class ThreeHeadedDragon {
    public static final class Fight {
        public String id,dimension,body;
        public String[] heads=new String[3];
        public float health,max,headMax;
        public float[] headHealth=new float[3];
        public double x,y,z;
        public float yaw;
        public long age,exposed;
        public boolean active,rewarded;
        public int participants;
        public Vec3 at(){return new Vec3(x,y,z);}
    }
    private record Part(Display entity,Vec3 offset,Vec3 size,int head,int wing,boolean skull){}
    private static Fight fight=new Fight();
    private static Path path;
    private static final List<Part> parts=new ArrayList<>();
    private static final Map<UUID,Entity> visualIds=new HashMap<>();
    private static Vec3 renderOrigin=Vec3.ZERO,breathAim;
    private static final ThreadLocal<Boolean> scriptedDamage=ThreadLocal.withInitial(()->false);
    private static final Map<UUID,Long> hurtAt=new HashMap<>();
    public static final int AWAKENING=100;
    private static final Map<String,Long> hits=new HashMap<>();
    private static ServerBossEvent bar;
    private static long wait,awakeningUntil=AWAKENING;
    public static void load(MinecraftServer s){
        path=s.getWorldPath(LevelResource.ROOT).resolve("creeperborn-dragon.json");fight=WorldJson.read(path,Fight.class,new Fight());
        if(fight.active&&(fight.id==null||fight.dimension==null||fight.heads==null||fight.heads.length!=3||fight.headHealth==null||fight.headHealth.length!=3||!Float.isFinite(fight.health)||fight.max<=0))throw new IllegalStateException("Invalid dragon encounter ledger");
        wait=0;awakeningUntil=fight.age+AWAKENING;
    }
    public static boolean active(){return fight.active;}
    public static Fight state(){return fight;}
    private static void save(){WorldJson.write(path,fight);}
    private static ServerLevel level(MinecraftServer s){return fight.dimension==null?null:s.getLevel(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION,net.minecraft.resources.Identifier.parse(fight.dimension)));}
    public static String summon(Player p,ItemStack item){
        if(!Nations.unlocked())return "The four nations must complete their trials and offerings first.";
        if(!p.isAlive()||p.isSpectator()||p.level().dimension()!=Level.END)return "Use the Dragon Covenant at the End exit portal.";
        ServerLevel l=(ServerLevel)p.level();
        if(active())return "A Three-Headed Dragon is already summoned.";
        if(Math.hypot(p.getX(),p.getZ())>16||l.getDragonFight()==null||!l.getDragonFight().hasPreviouslyKilledDragon())return "Defeat the normal Ender Dragon, then stand at the central exit portal.";
        if(l.getAllEntities().iterator().hasNext())for(Entity e:l.getAllEntities())if(e instanceof net.minecraft.world.entity.boss.enderdragon.EnderDragon&&e.isAlive())return "Defeat the normal Ender Dragon before summoning the Three-Headed Dragon.";
        int n=(int)l.players().stream().filter(v->v.isAlive()&&!v.isSpectator()&&!v.isCreative()&&v.position().distanceToSqr(p.position())<128*128).count();
        begin(l,p.position().add(0,8,18),Math.max(1,n));item.shrink(1);
        return "The Three-Headed Dragon awakens! Sever its heads to expose its body.";
    }
    public static void begin(ServerLevel l,Vec3 at,int participants){
        if(active())throw new IllegalStateException("Dragon already active");
        purgeLoaded(l.getServer());fight=new Fight();fight.id=UUID.randomUUID().toString();fight.dimension=l.dimension().identifier().toString();fight.participants=Math.max(1,Math.min(6,participants));
        fight.max=2700+900*(fight.participants-1);fight.health=fight.max;fight.headMax=180+60*(fight.participants-1);
        Arrays.fill(fight.headHealth,fight.headMax);fight.x=at.x;fight.y=at.y;fight.z=at.z;fight.active=true;
        fight.body=proxy(l,at,12).getUUID().toString();for(int i=0;i<3;i++)fight.heads[i]=proxy(l,head(i),6).getUUID().toString();
        build(l);wait=0;awakeningUntil=AWAKENING;save();
    }
    private static Mob proxy(ServerLevel l,Vec3 at,int size){
        var m=EntityTypes.SLIME.create(l,EntitySpawnReason.EVENT);if(m==null)throw new IllegalStateException("Dragon hitbox unavailable");
        m.setSize(size,true);m.setNoAi(true);m.setNoGravity(true);m.setInvisible(true);m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,Integer.MAX_VALUE,0,false,false,false));m.setSilent(true);m.setCustomName(Component.literal("Three-Headed Dragon"));m.setCustomNameVisible(false);m.noPhysics=true;m.setPersistenceRequired();
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);m.setHealth(1000);m.addTag("cb_dragon_part_"+fight.id);m.snapTo(at.x,at.y,at.z);
        if(!l.addFreshEntity(m))throw new IllegalStateException("Dragon hitbox could not spawn");return m;
    }
    private static Vec3 rotated(Vec3 local){return local.yRot((float)Math.toRadians(fight.yaw));}
    private static Vec3 head(int i){return fight.at().add(rotated(new Vec3((i==0?0:i==1?-4:4),5.5,-6-Math.abs(i-1))));}
    private static void box(ServerLevel l,Block block,Vec3 offset,Vec3 size,int head,int wing){
        Display d;
        if(block==Blocks.DRAGON_HEAD){
            var skull=EntityTypes.ITEM_DISPLAY.create(l,EntitySpawnReason.EVENT);if(skull==null)throw new IllegalStateException("Dragon head model unavailable");
            var accessor=(com.sibcraft.creeperborn.mixin.DragonHeadDisplayAccessor)skull;accessor.creeperborn$item(new ItemStack(net.minecraft.world.item.Items.DRAGON_HEAD));accessor.creeperborn$context(net.minecraft.world.item.ItemDisplayContext.FIXED);
            skull.setCustomName(Component.literal("cb_dragon_head|"+fight.id));skull.setCustomNameVisible(false);d=skull;
        }else{var blockDisplay=EntityTypes.BLOCK_DISPLAY.create(l,EntitySpawnReason.EVENT);if(blockDisplay==null)throw new IllegalStateException("Dragon model unavailable");blockDisplay.setBlockState(block.defaultBlockState());blockDisplay.setCustomName(Component.literal("cb_dragon_visual|"+fight.id));blockDisplay.setCustomNameVisible(false);d=blockDisplay;}
        d.addTag("cb_dragon_visual_"+fight.id);d.addTag("cb_temporary_rock");
        var access=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;access.creeperborn$duration(3);access.creeperborn$positionDuration(3);access.creeperborn$width(64);access.creeperborn$height(64);access.creeperborn$view(2);
        d.snapTo(renderOrigin.x,renderOrigin.y,renderOrigin.z);
        Part part=new Part(d,offset,size,head,wing,block==Blocks.DRAGON_HEAD);transform(part);
        visualIds.put(d.getUUID(),d);l.addFreshEntity(d);parts.add(part);
    }
    private static void transform(Part p){
        double flap=p.wing==0?0:Math.sin(fight.age*.09)*Math.abs(p.offset.x)*.18;
        p.entity.snapTo(fight.x,fight.y,fight.z);
        Vec3 offset=rotated(p.offset.add(0,flap,0));
        var a=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)p.entity;a.creeperborn$delay(0);
        Quaternionf rotation=new Quaternionf().rotationY((float)Math.toRadians(fight.yaw)+(p.skull?(float)Math.PI:0));
        p.entity.setTransformation(new Transformation(new Vector3f((float)offset.x,(float)offset.y,(float)offset.z),rotation,new Vector3f((float)p.size.x,(float)p.size.y,(float)p.size.z),new Quaternionf()));
    }

    private static void build(ServerLevel l){
        for(Part part:parts)part.entity.discard();parts.clear();visualIds.clear();renderOrigin=fight.at();
        for(Entity e:List.copyOf(loadedEntities(l)))if(e.entityTags().stream().anyMatch(t->t.startsWith("cb_dragon_visual_")))e.discard();
        box(l,Blocks.OBSIDIAN,new Vec3(-3,-1,-3),new Vec3(6,4,9),-1,0);
        box(l,Blocks.CRYING_OBSIDIAN,new Vec3(-2,0,-3.08),new Vec3(4,2,.18),-1,0);
        // Segmented tail, spine, legs and claws establish a dragon silhouette from every angle.
        for(int i=0;i<6;i++){
            double w=2.4-i*.3;box(l,Blocks.OBSIDIAN,new Vec3(-w/2,.5-i*.13,6+i*1.8),new Vec3(w,w,2.2),-1,0);
            if(i<5)box(l,Blocks.AMETHYST_BLOCK,new Vec3(-.3,3.1,-1+i*1.6),new Vec3(.6,1,.8),-1,0);
        }
        for(int side:new int[]{-1,1}){
            for(int leg=0;leg<2;leg++){
                box(l,Blocks.OBSIDIAN,new Vec3(side<0?-4:2.5,-3,leg*5-1),new Vec3(1.5,3,2),-1,0);
                for(int claw=0;claw<3;claw++)box(l,Blocks.END_STONE,new Vec3((side<0?-4:2.5)+claw*.5,-3.1,leg*5-2),new Vec3(.3,.4,1.5),-1,0);
            }
            for(int span=0;span<5;span++){
                double x=side<0?-4-span*2:3+span*2;
                box(l,Blocks.PURPUR_BLOCK,new Vec3(x,2,span*.7-1),new Vec3(2.2,.18,6-span*.8),-1,side);
                box(l,Blocks.OBSIDIAN,new Vec3(x,2.15,span*.7-1),new Vec3(2.2,.3,.35),-1,side);
            }
        }
        for(int i=0;i<3;i++)if(fight.headHealth[i]>0){
            double x=(i==0?0:i==1?-4:4);
            box(l,Blocks.OBSIDIAN,new Vec3(x-.8,1,-8-Math.abs(i-1)),new Vec3(1.6,4.5,7),i,0);
            box(l,Blocks.DRAGON_HEAD,new Vec3(x,4,-6.5-Math.abs(i-1)),new Vec3(3,3,3),i,0);
        }
    }
    private static List<Entity> loadedEntities(ServerLevel l){List<Entity> result=new ArrayList<>();l.getAllEntities().forEach(result::add);return result;}
    private static boolean part(Entity e){return e!=null&&e.entityTags().stream().anyMatch(t->t.startsWith("cb_dragon_part_"));}
    private static boolean visual(Entity e){return e.entityTags().stream().anyMatch(t->t.startsWith("cb_dragon_visual_"));}
    private static boolean attack(Entity e){return e.entityTags().stream().anyMatch(t->t.startsWith("cb_dragon_attack_"));}
    public static void tickEntity(Entity e){
        if(!(e.level() instanceof ServerLevel))return;
        if(e instanceof AreaEffectCloud cloud&&part(cloud.getOwner()))e.addTag("cb_dragon_attack_"+fight.id);
        if(visual(e)&&(visualIds.get(e.getUUID())!=e||!fight.active))e.discard();
        if(attack(e)&&!fight.active)e.discard();
        if(part(e)&&fight.active&&e.entityTags().contains("cb_dragon_part_"+fight.id)){e.noPhysics=true;if(e.getCustomName()==null||!e.getCustomName().getString().equals("Three-Headed Dragon")){e.setCustomName(Component.literal("Three-Headed Dragon"));e.setCustomNameVisible(false);}}
        if(part(e)&&(!fight.active||!e.entityTags().contains("cb_dragon_part_"+fight.id)))e.discard();
    }
    public static void loaded(Entity e){tickEntity(e);}
    public static boolean cancelContact(DamageSource source){return part(source.getEntity())&&source.is(net.minecraft.world.damagesource.DamageTypes.MOB_ATTACK)&&!scriptedDamage.get();}
    private static void purgeLoaded(MinecraftServer server){
        for(ServerLevel l:server.getAllLevels()){
            List<Entity> entities=loadedEntities(l);
            for(Entity e:entities)if(e instanceof AreaEffectCloud cloud&&part(cloud.getOwner()))e.addTag("cb_dragon_attack_"+fight.id);
            for(Entity e:entities)if(visual(e)||part(e)||attack(e))e.discard();
        }
        parts.clear();visualIds.clear();hits.clear();hurtAt.clear();breathAim=null;
    }
    /** Cancel vanilla hitbox damage, including slime splitting; apply validated damage to the ledger. */
    public static boolean damage(LivingEntity target,DamageSource source,float amount){
        if(!target.entityTags().stream().anyMatch(t->t.startsWith("cb_dragon_part_")))return false;
        if(!fight.active||!Float.isFinite(amount)||amount<=0||!(source.getEntity() instanceof Player p)||p.isSpectator())return true;
        int part=target.getUUID().toString().equals(fight.body)?-1:Arrays.asList(fight.heads).indexOf(target.getUUID().toString());
        if(part<0&&!target.getUUID().toString().equals(fight.body))return true;
        String key=p.getUUID()+":"+target.getUUID();long now=target.level().getGameTime();
        if(now-hits.getOrDefault(key,-100L)<5)return true;hits.put(key,now);
        float dealt=Math.min(100,amount);
        if(part>=0){
            if(fight.headHealth[part]<=0)return true;
            fight.headHealth[part]=Math.max(0,fight.headHealth[part]-dealt);fight.health-=dealt*.25f;
            if(fight.headHealth[part]==0){target.discard();fight.exposed=fight.age+200;build((ServerLevel)target.level());((ServerLevel)target.level()).sendParticles(ParticleTypes.EXPLOSION,target.getX(),target.getY()+1,target.getZ(),10,1,1,1,0);}
        }else fight.health-=dealt*((fight.age<fight.exposed||Arrays.stream(new double[]{fight.headHealth[0],fight.headHealth[1],fight.headHealth[2]}).allMatch(v->v<=0))?2:1);
        if(fight.health<=0)finish((ServerLevel)target.level());else save();return true;
    }
    private static void finish(ServerLevel l){
        if(!fight.active||fight.rewarded)return;
        // Commit the one-time reward flag before spawning the single physical bag.
        fight.active=false;fight.rewarded=true;fight.health=0;save();
        // Keep the single reward reachable even if the fight finishes over the void.
        double rewardX=l.dimension()==Level.END?.5:fight.x,rewardZ=l.dimension()==Level.END?.5:fight.z;
        int rewardY=l.getHeightmapPos(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,BlockPos.containing(rewardX,64,rewardZ)).getY()+1;
        var item=new net.minecraft.world.entity.item.ItemEntity(l,rewardX,rewardY,rewardZ,Relics.holyBag());
        item.setNoGravity(true);item.setExtendedLifetime();l.addFreshEntity(item);
        cleanup(l);if(bar!=null)bar.removeAllPlayers();
    }
    private static void cleanup(ServerLevel l){purgeLoaded(l.getServer());}
    private static List<ServerPlayer> players(ServerLevel l){return l.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&p.position().distanceToSqr(fight.at())<128*128).toList();}
    private static void strike(ServerLevel l,LivingEntity owner,Player p,float amount){
        long now=l.getGameTime();if(now-hurtAt.getOrDefault(p.getUUID(),-100L)<20)return;
        scriptedDamage.set(true);try{if(p.hurtServer(l,l.damageSources().mobAttack(owner),amount))hurtAt.put(p.getUUID(),now);}finally{scriptedDamage.remove();}
    }
    private static boolean inLane(Vec3 from,Vec3 to,Vec3 at,double radius){Vec3 delta=to.subtract(from);double t=delta.lengthSqr()<.001?0:Math.clamp(at.subtract(from).dot(delta)/delta.lengthSqr(),0,1);return from.add(delta.scale(t)).distanceToSqr(at)<=radius*radius;}
    public static void moveToward(Vec3 target){
        Vec3 desired=target.add(Math.sin(fight.age*.006)*18,5+Math.sin(fight.age*.018),Math.cos(fight.age*.006)*18);
        Vec3 delta=desired.subtract(fight.at());if(delta.length()>.09)delta=delta.normalize().scale(.09);
        fight.x+=delta.x;fight.y+=Math.clamp(delta.y,-.035,.035);fight.z+=delta.z;
        Vec3 look=target.subtract(fight.at());float yaw=(float)Math.toDegrees(Math.atan2(-look.x,-look.z));
        fight.yaw+=Math.clamp(net.minecraft.util.Mth.wrapDegrees(yaw-fight.yaw),-1.5f,1.5f);
    }
    public static void tick(MinecraftServer s){
        if(!fight.active)return;ServerLevel l=level(s);if(l==null)return;
        if(wait>0){wait--;return;}
        var players=players(l);if(players.isEmpty()){if(bar!=null)bar.removeAllPlayers();return;}
        Entity raw=l.getEntity(UUID.fromString(fight.body));if(!(raw instanceof Mob body))return; // Unloaded actors never count as a victory.
        if(parts.isEmpty()||parts.stream().anyMatch(p->p.entity.isRemoved()))build(l);fight.age++;
        moveToward(players.get(0).position());
        if(bar==null)bar=new ServerBossEvent(UUID.randomUUID(),Component.literal("Three-Headed Dragon"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        for(ServerPlayer p:List.copyOf(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);
        players.forEach(bar::addPlayer);bar.setProgress(Math.max(0,fight.health/fight.max));
        String heads="";for(int i=0;i<3;i++)heads+=(i==0?"":" | ")+(fight.headHealth[i]<=0?"Severed":Math.round(fight.headHealth[i])+" HP");
        boolean enraged=fight.health<fight.max*.25f;
        bar.setName(Component.literal((fight.age<awakeningUntil?"Awakening — ":"")+(enraged?"ENRAGED — ":"")+"Three-Headed Dragon — "+heads+(fight.age<fight.exposed?" — BODY EXPOSED":"")));
        for(int i=0;i<3;i++){
            Entity e=l.getEntity(UUID.fromString(fight.heads[i]));if(e!=null){Vec3 at=head(i);e.snapTo(at.x,at.y,at.z);e.setDeltaMovement(Vec3.ZERO);}
        }
        body.snapTo(fight.x,fight.y-1,fight.z);body.setDeltaMovement(Vec3.ZERO);
        if(fight.age%3==0){
            for(Part p:parts)transform(p);
            for(Entity e:loadedEntities(l))if(part(e)||visual(e)){
                var packet=net.minecraft.network.protocol.game.ClientboundEntityPositionSyncPacket.of(e);
                for(ServerPlayer viewer:net.fabricmc.fabric.api.networking.v1.PlayerLookup.tracking(e))viewer.connection.send(packet);
            }
        }
        if(fight.age<awakeningUntil)return;
        int period=enraged?80:160;long phase=(fight.age-awakeningUntil)%period;
        // Center head: a visible breath telegraph, followed by a damaging lane and close bites.
        if(fight.headHealth[0]>0){
            Player target=players.get(0);if(phase==0||breathAim==null)breathAim=target.getEyePosition();
            Vec3 from=head(0).add(0,1.2,0),to=breathAim,dir=to.subtract(from).normalize();
            if(phase<40&&phase%5==0)for(int k=0;k<35;k++)l.sendParticles(ParticleTypes.REVERSE_PORTAL,from.x+dir.x*k,from.y+dir.y*k,from.z+dir.z*k,1,.1,.1,.1,0);
            if(phase==40)for(Player p:players)if(inLane(from,to,p.getEyePosition(),1.6))strike(l,body,p,12);
            if(fight.age%40==0)for(Player p:players)if(p.position().distanceToSqr(from)<36)strike(l,body,p,8);
        }
        // Left head: real dragon fireballs pressure distant and airborne players.
        if(fight.headHealth[1]>0&&phase==60){
            Player target=players.get((int)(fight.age/period)%players.size());Vec3 mouth=head(1).add(0,1.2,0),dir=target.getEyePosition().subtract(mouth).normalize(),at=mouth.add(dir.scale(3.5));
            var bolt=EntityTypes.DRAGON_FIREBALL.create(l,EntitySpawnReason.EVENT);if(bolt!=null){bolt.setOwner(body);bolt.addTag("cb_dragon_attack_"+fight.id);bolt.snapTo(at.x,at.y,at.z);bolt.setDeltaMovement(dir.scale(1.2));l.addFreshEntity(bolt);}
        }
        // Right head: telegraphed wing sweep and knockback discourage grouping under the boss.
        if(fight.headHealth[2]>0){
            if(phase>=80&&phase<100&&phase%5==0)l.sendParticles(ParticleTypes.SWEEP_ATTACK,fight.x,fight.y-2,fight.z-7,25,10,1,6,0);
            if(phase==100||enraged&&phase==70)for(Player p:players)if(p.position().distanceToSqr(fight.at())<24*24){strike(l,body,p,6);Vec3 away=p.position().subtract(fight.at()).normalize();p.push(away.x*1.8,.7,away.z*1.8);}
        }
        if(fight.age%100==0)save();
    }
    public static void dismiss(MinecraftServer s){fight.active=false;save();purgeLoaded(s);if(bar!=null)bar.removeAllPlayers();}
    public static void stop(MinecraftServer s){if(fight.active)save();ServerLevel l=level(s);if(l!=null){for(Part p:parts)p.entity.discard();parts.clear();}if(bar!=null)bar.removeAllPlayers();}
    public static void clear(){fight=new Fight();path=null;parts.clear();visualIds.clear();hits.clear();bar=null;wait=0;renderOrigin=Vec3.ZERO;breathAim=null;hurtAt.clear();scriptedDamage.remove();}
}
