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
        public long age,exposed;
        public boolean active,rewarded;
        public int participants;
        public Vec3 at(){return new Vec3(x,y,z);}
    }
    private record Part(Display entity,Vec3 offset,Vec3 size,int head,int wing){}
    private static Fight fight=new Fight();
    private static Path path;
    private static final List<Part> parts=new ArrayList<>();
    private static final Set<UUID> visualIds=new HashSet<>();
    private static final Map<String,Long> hits=new HashMap<>();
    private static ServerBossEvent bar;
    private static long wait;
    public static void load(MinecraftServer s){
        path=s.getWorldPath(LevelResource.ROOT).resolve("creeperborn-dragon.json");fight=WorldJson.read(path,Fight.class,new Fight());
        if(fight.active&&(fight.id==null||fight.dimension==null||fight.heads==null||fight.heads.length!=3||fight.headHealth==null||fight.headHealth.length!=3||!Float.isFinite(fight.health)||fight.max<=0))throw new IllegalStateException("Invalid dragon encounter ledger");
        wait=100;
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
        fight=new Fight();fight.id=UUID.randomUUID().toString();fight.dimension=l.dimension().identifier().toString();fight.participants=Math.max(1,Math.min(6,participants));
        fight.max=2700+900*(fight.participants-1);fight.health=fight.max;fight.headMax=180+60*(fight.participants-1);
        Arrays.fill(fight.headHealth,fight.headMax);fight.x=at.x;fight.y=at.y;fight.z=at.z;fight.active=true;
        fight.body=proxy(l,at,12).getUUID().toString();for(int i=0;i<3;i++)fight.heads[i]=proxy(l,head(i),4).getUUID().toString();
        build(l);wait=0;save();
    }
    private static Mob proxy(ServerLevel l,Vec3 at,int size){
        var m=EntityTypes.SLIME.create(l,EntitySpawnReason.EVENT);if(m==null)throw new IllegalStateException("Dragon hitbox unavailable");
        m.setSize(size,true);m.setNoAi(true);m.setNoGravity(true);m.setInvisible(true);m.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.INVISIBILITY,Integer.MAX_VALUE,0,false,false,false));m.setSilent(true);m.noPhysics=true;m.setPersistenceRequired();
        m.getAttribute(Attributes.MAX_HEALTH).setBaseValue(1000);m.setHealth(1000);m.addTag("cb_dragon_part_"+fight.id);m.snapTo(at.x,at.y,at.z);
        if(!l.addFreshEntity(m))throw new IllegalStateException("Dragon hitbox could not spawn");return m;
    }
    private static Vec3 head(int i){return fight.at().add((i==0?0:i==1?-4:4),3,-6-Math.abs(i-1));}
    private static void box(ServerLevel l,Block block,Vec3 offset,Vec3 size,int head,int wing){
        Display d;
        if(block==Blocks.DRAGON_HEAD){
            var skull=EntityTypes.ITEM_DISPLAY.create(l,EntitySpawnReason.EVENT);if(skull==null)throw new IllegalStateException("Dragon head model unavailable");
            var accessor=(com.sibcraft.creeperborn.mixin.DragonHeadDisplayAccessor)skull;accessor.creeperborn$item(new ItemStack(net.minecraft.world.item.Items.DRAGON_HEAD));accessor.creeperborn$context(net.minecraft.world.item.ItemDisplayContext.FIXED);
            skull.setCustomName(Component.literal("cb_dragon_head|"+fight.id));skull.setCustomNameVisible(false);d=skull;
        }else{var blockDisplay=EntityTypes.BLOCK_DISPLAY.create(l,EntitySpawnReason.EVENT);if(blockDisplay==null)throw new IllegalStateException("Dragon model unavailable");blockDisplay.setBlockState(block.defaultBlockState());d=blockDisplay;}
        d.addTag("cb_dragon_visual_"+fight.id);
        var access=(com.sibcraft.creeperborn.mixin.PunchDisplayAccessor)d;access.creeperborn$duration(3);access.creeperborn$positionDuration(3);access.creeperborn$width(64);access.creeperborn$height(64);access.creeperborn$view(2);
        Vec3 at=fight.at().add(offset);d.snapTo(at.x,at.y,at.z);d.setTransformation(new Transformation(new Vector3f(),new Quaternionf(),new Vector3f((float)size.x,(float)size.y,(float)size.z),new Quaternionf()));
        visualIds.add(d.getUUID());l.addFreshEntity(d);parts.add(new Part(d,offset,size,head,wing));
    }
    private static void build(ServerLevel l){
        for(Part part:parts)part.entity.discard();parts.clear();
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
            box(l,Blocks.OBSIDIAN,new Vec3(x-.8,1,-5),new Vec3(1.6,2,4),i,0);
            box(l,Blocks.DRAGON_HEAD,new Vec3(x,4,-6.5-Math.abs(i-1)),new Vec3(3,3,3),i,0);
        }
    }
    public static void loaded(Entity e){
        for(String t:e.entityTags())if(t.startsWith("cb_dragon_visual_")&&!visualIds.contains(e.getUUID()))e.discard(); // Rebuild displays from encounter state after loading.
        for(String t:e.entityTags())if(t.startsWith("cb_dragon_part_")&&(!fight.active||!t.equals("cb_dragon_part_"+fight.id)))e.discard();
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
    private static void cleanup(ServerLevel l){
        for(Part part:parts)part.entity.discard();parts.clear();
        List<String> ids=new ArrayList<>(Arrays.asList(fight.heads));ids.add(fight.body);
        for(String id:ids)if(id!=null){Entity e=l.getEntity(UUID.fromString(id));if(e!=null)e.discard();}
        hits.clear();
    }
    private static List<ServerPlayer> players(ServerLevel l){return l.players().stream().filter(p->p.isAlive()&&!p.isSpectator()&&!p.isCreative()&&p.position().distanceToSqr(fight.at())<128*128).toList();}
    private static void strike(ServerLevel l,LivingEntity owner,Player p,float amount){p.hurtServer(l,l.damageSources().mobAttack(owner),amount);}
    public static void tick(MinecraftServer s){
        if(!fight.active)return;ServerLevel l=level(s);if(l==null)return;
        if(wait>0){wait--;return;}
        var players=players(l);if(players.isEmpty()){if(bar!=null)bar.removeAllPlayers();return;}
        Entity raw=l.getEntity(UUID.fromString(fight.body));if(!(raw instanceof Mob body))return; // Unloaded actors never count as a victory.
        if(parts.isEmpty())build(l);fight.age++;
        if(bar==null)bar=new ServerBossEvent(UUID.randomUUID(),Component.literal("Three-Headed Dragon"),BossEvent.BossBarColor.PURPLE,BossEvent.BossBarOverlay.PROGRESS);
        for(ServerPlayer p:List.copyOf(bar.getPlayers()))if(!players.contains(p))bar.removePlayer(p);
        players.forEach(bar::addPlayer);bar.setProgress(Math.max(0,fight.health/fight.max));
        String heads="";for(int i=0;i<3;i++)heads+=(i==0?"":" | ")+(fight.headHealth[i]<=0?"Severed":Math.round(fight.headHealth[i])+" HP");
        boolean enraged=fight.health<fight.max*.25f;
        bar.setName(Component.literal((enraged?"ENRAGED — ":"")+"Three-Headed Dragon — "+heads+(fight.age<fight.exposed?" — BODY EXPOSED":"")));
        for(int i=0;i<3;i++){
            Entity e=l.getEntity(UUID.fromString(fight.heads[i]));if(e!=null){Vec3 at=head(i);e.snapTo(at.x,at.y,at.z);e.setDeltaMovement(Vec3.ZERO);}
        }
        body.snapTo(fight.x,fight.y-1,fight.z);body.setDeltaMovement(Vec3.ZERO);
        if(fight.age%3==0)for(Part part:parts){
            double flap=part.wing==0?0:Math.sin(fight.age*.10)*Math.abs(part.offset.x)*.22;
            Vec3 at=fight.at().add(part.offset).add(0,flap,0);part.entity.snapTo(at.x,at.y,at.z);
        }
        int period=enraged?80:160;long phase=fight.age%period;
        // Center head: a visible breath telegraph, followed by a damaging lane and close bites.
        if(fight.headHealth[0]>0){
            Player target=players.get(0);Vec3 from=head(0),to=target.getEyePosition(),dir=to.subtract(from).normalize();
            if(phase<40&&phase%5==0)for(int k=0;k<35;k++)l.sendParticles(ParticleTypes.REVERSE_PORTAL,from.x+dir.x*k,from.y+dir.y*k,from.z+dir.z*k,1,.1,.1,.1,0);
            if(phase==40)for(Player p:players)if(new AABB(from,to).inflate(2).contains(p.position()))strike(l,body,p,20);
            if(fight.age%40==0)for(Player p:players)if(p.position().distanceToSqr(from)<36)strike(l,body,p,14);
        }
        // Left head: real dragon fireballs pressure distant and airborne players.
        if(fight.headHealth[1]>0&&phase==60){
            Player target=players.get((int)(fight.age/period)%players.size());Vec3 at=head(1),dir=target.getEyePosition().subtract(at).normalize();
            var bolt=EntityTypes.DRAGON_FIREBALL.create(l,EntitySpawnReason.EVENT);if(bolt!=null){bolt.setOwner(body);bolt.snapTo(at.x,at.y,at.z);bolt.setDeltaMovement(dir.scale(1.2));l.addFreshEntity(bolt);}
        }
        // Right head: telegraphed wing sweep and knockback discourage grouping under the boss.
        if(fight.headHealth[2]>0){
            if(phase>=80&&phase<100&&phase%5==0)l.sendParticles(ParticleTypes.SWEEP_ATTACK,fight.x,fight.y-2,fight.z-7,25,10,1,6,0);
            if(phase==100||enraged&&phase==70)for(Player p:players)if(p.position().distanceToSqr(fight.at())<24*24){strike(l,body,p,12);Vec3 away=p.position().subtract(fight.at()).normalize();p.push(away.x*1.8,.7,away.z*1.8);}
        }
        // Low pass makes melee possible; slow movement keeps all hitboxes and visuals together.
        if(fight.age%200==0){Player p=players.get(0);Vec3 to=p.position().add(0,3,10),step=to.subtract(fight.at());if(step.length()>4)step=step.normalize().scale(4);fight.x+=step.x;fight.y+=Math.clamp(step.y,-2,2);fight.z+=step.z;}
        if(fight.age%100==0)save();
    }
    public static void dismiss(MinecraftServer s){if(!fight.active)return;ServerLevel l=level(s);fight.active=false;save();if(l!=null)cleanup(l);if(bar!=null)bar.removeAllPlayers();}
    public static void stop(MinecraftServer s){if(fight.active)save();ServerLevel l=level(s);if(l!=null){for(Part p:parts)p.entity.discard();parts.clear();}if(bar!=null)bar.removeAllPlayers();}
    public static void clear(){fight=new Fight();path=null;parts.clear();visualIds.clear();hits.clear();bar=null;wait=0;}
}
