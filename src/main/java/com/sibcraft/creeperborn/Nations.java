package com.sibcraft.creeperborn;

import java.nio.file.Path;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.*;

/** World-wide cooperative progression. A villager belongs to exactly one nation. */
public final class Nations {
    public static final int REQUIRED=9, RADIUS=64;
    public static final class Nation {
        public String dimension,element;
        public int x,y,z,wave;
        public boolean passed,offered,raiding;
        public long remaining,nextWave;
        public Set<String> enemies=new HashSet<>();
        public Set<String> members=new HashSet<>();
        public BlockPos center(){return new BlockPos(x,y,z);}
    }
    public static final class Data {public Map<String,Nation> nations=new HashMap<>();}
    private static Data data=new Data();
    private static Path path;
    public static void load(MinecraftServer s){
        path=s.getWorldPath(LevelResource.ROOT).resolve("creeperborn-nations.json");
        data=WorldJson.read(path,Data.class,new Data());
        if(data.nations==null||data.nations.size()>4)throw new IllegalStateException("Invalid nations ledger");
        for(var entry:data.nations.entrySet()){
            Nation n=entry.getValue();
            if(Element.parse(entry.getKey())==null||n==null||n.dimension==null||n.enemies==null||n.members==null)throw new IllegalStateException("Invalid nation");
        }
    }
    public static Nation get(Element e){return data.nations.get(e.key());}
    public static ServerLevel level(MinecraftServer s,Nation n){return s.getLevel(ResourceKey.create(Registries.DIMENSION,Identifier.parse(n.dimension)));}
    public static boolean trialsPassed(){return data.nations.size()==4&&data.nations.values().stream().allMatch(n->n.passed);}
    public static boolean unlocked(){return trialsPassed()&&data.nations.values().stream().allMatch(n->n.offered);}
    private static void save(){WorldJson.write(path,data);}
    public static String found(Player p,Element e){
        if(!Progress.all(p)||p.isSpectator()||!p.isAlive())return "Collect all four elements and the Elemental Trident first.";
        if(p.level().dimension()!=Level.OVERWORLD)return "Found nations in the Overworld.";
        if(get(e)!=null)return e.title+" Nation already exists. Use /creeperborn nations to find it.";
        for(Nation n:data.nations.values())if(p.blockPosition().distSqr(n.center())<192*192)return "Nation centers must be at least 192 blocks apart.";
        if(p.level().getEntitiesOfClass(Villager.class,p.getBoundingBox().inflate(16)).stream().noneMatch(v->!v.isBaby()&&v.isAlive()))return "Stand within 16 blocks of an adult villager to found a nation.";
        Nation n=new Nation();n.element=e.key();n.dimension=p.level().dimension().identifier().toString();n.x=p.blockPosition().getX();n.y=p.blockPosition().getY();n.z=p.blockPosition().getZ();
        data.nations.put(e.key(),n);enroll((ServerLevel)p.level(),n);save();
        return e.title+" Nation founded here. Empower 9 villagers within 64 blocks, then /creeperborn nation start "+e.key();
    }
    public static void enroll(ServerLevel level,Nation n){
        for(Villager v:level.getEntitiesOfClass(Villager.class,new AABB(n.center()).inflate(RADIUS))){
            if(!v.isAlive()||v.isBaby()||Guardians.element(v)!=Element.parse(n.element))continue;
            boolean other=v.entityTags().stream().anyMatch(t->t.startsWith("cb_nation_")&&!t.equals("cb_nation_"+n.element));
            if(other)continue;
            v.addTag("cb_nation_"+n.element);n.members.add(v.getUUID().toString());
            if(n.offered)v.addTag("cb_guard_harmony");
        }
    }
    public static List<Villager> living(ServerLevel level,Nation n){
        return level.getEntitiesOfClass(Villager.class,new AABB(n.center()).inflate(RADIUS)).stream()
            .filter(v->v.isAlive()&&!v.isBaby()&&n.members.contains(v.getUUID().toString())&&v.entityTags().contains("cb_nation_"+n.element)&&Guardians.element(v)==Element.parse(n.element)).toList();
    }
    private static boolean nearby(Player p,Nation n){return p.level().dimension().identifier().toString().equals(n.dimension)&&p.position().distanceToSqr(Vec3.atCenterOf(n.center()))<=RADIUS*RADIUS;}
    public static String start(Player p,Element e){
        Nation n=get(e);if(n==null)return "Found that nation first.";
        if(!nearby(p,n)||p.isSpectator()||!p.isAlive())return "Stand inside the nation to activate its trial.";
        if(n.passed)return "This nation's raid trial is already complete.";
        if(n.raiding)return "This trial is already running.";
        ServerLevel level=(ServerLevel)p.level();enroll(level,n);
        if(living(level,n).size()<REQUIRED)return "This nation needs 9 living adult guardians of its element within 64 blocks.";
        n.raiding=true;n.wave=0;n.remaining=20*60*20;n.nextWave=0;n.enemies.clear();save();
        return e.title+" Nation raid trial activated! Three waves. Defend your guardians.";
    }
    private static void wave(ServerLevel level,Nation n){
        n.wave++;int count=4+n.wave*2;
        List<Mob> spawned=new ArrayList<>();
        for(int i=0;i<count;i++){
            EntityType<? extends Mob> type=switch(Element.parse(n.element)){
                case EARTH->i%4==0?EntityTypes.RAVAGER:EntityTypes.VINDICATOR;
                case WATER->i%4==0?EntityTypes.WITCH:EntityTypes.PILLAGER;
                case FIRE->i%4==0?EntityTypes.EVOKER:EntityTypes.VINDICATOR;
                case AIR->i%4==0?EntityTypes.EVOKER:EntityTypes.PILLAGER;
            };
            Mob mob=type.create(level,EntitySpawnReason.EVENT);if(mob==null)throw new IllegalStateException("Raid entity unavailable");
            double angle=2*Math.PI*i/count;int x=n.x+(int)(Math.cos(angle)*30),z=n.z+(int)(Math.sin(angle)*30);
            BlockPos at=level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,new BlockPos(x,n.y,z));
            mob.snapTo(x+.5,at.getY()+1,z+.5);mob.setPersistenceRequired();mob.addTag("cb_nation_raid_"+n.element);
            if(mob.getAttribute(Attributes.MAX_HEALTH)!=null){mob.getAttribute(Attributes.MAX_HEALTH).setBaseValue(mob.getMaxHealth()*(1.3+n.wave*.2));mob.setHealth(mob.getMaxHealth());}
            if(mob.getAttribute(Attributes.ATTACK_DAMAGE)!=null)mob.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(5+n.wave*2);
            mob.finalizeSpawn(level,level.getCurrentDifficultyAt(at),EntitySpawnReason.EVENT,null);
            n.enemies.add(mob.getUUID().toString());
            if(!level.addFreshEntity(mob)){spawned.forEach(Entity::discard);fail(level,n,"Could not spawn the trial safely. Retry when ready.");return;}
            spawned.add(mob);
        }
        announce(level,n,Element.parse(n.element).title+" trial — wave "+n.wave+"/3 ("+count+" attackers)");save();
    }
    private static void announce(ServerLevel level,Nation n,String text){for(var p:level.players())if(nearby(p,n))p.sendSystemMessage(Component.literal(text));}
    private static void fail(ServerLevel level,Nation n,String message){
        for(String id:n.enemies){Entity mob=level.getEntity(UUID.fromString(id));if(mob!=null)mob.discard();}
        n.enemies.clear();n.raiding=false;n.wave=0;announce(level,n,message);save();
    }
    public static void died(LivingEntity entity){
        for(Nation n:data.nations.values())if(n.raiding&&n.enemies.remove(entity.getUUID().toString())){n.nextWave=100;save();}
    }
    public static void loaded(Entity entity){
        for(String tag:entity.entityTags())if(tag.startsWith("cb_nation_raid_")){
            Element e=Element.parse(tag.substring("cb_nation_raid_".length()));Nation n=e==null?null:get(e);
            if(n==null||!n.raiding||!n.enemies.contains(entity.getUUID().toString()))entity.discard();
        }
        if(entity instanceof Villager v)for(Nation n:data.nations.values())if(n.offered&&n.members.contains(v.getUUID().toString()))v.addTag("cb_guard_harmony");
    }
    public static void tick(MinecraftServer s){
        if(s.getTickCount()%20!=0)return;
        for(Nation n:data.nations.values()){
            ServerLevel level=level(s,n);if(level==null)continue;
            boolean present=level.players().stream().anyMatch(p->!p.isSpectator()&&p.isAlive()&&nearby(p,n));
            if(!present)continue;
            enroll(level,n);if(!n.raiding)continue;
            n.remaining-=20;
            if(n.remaining<=0||living(level,n).isEmpty()){fail(level,n,"Trial failed. Rebuild to 9 living guardians, then activate a retry.");continue;}
            if(n.enemies.isEmpty()){
                if(n.nextWave>0){n.nextWave-=20;continue;}
                if(n.wave>=3){n.passed=true;n.raiding=false;announce(level,n,"Nation trial passed! Shared progress saved for everyone.");save();}
                else wave(level,n);
            }else{
                var guards=living(level,n);
                for(String id:n.enemies){Entity entity=level.getEntity(UUID.fromString(id));if(entity instanceof Mob mob&&mob.isAlive()){
                    LivingEntity target=guards.stream().min(Comparator.comparingDouble(mob::distanceToSqr)).orElse(null);
                    if(target!=null){mob.setTarget(target);mob.getNavigation().moveTo(target,1.1);}
                }}
            }
        }
        if(s.getTickCount()%200==0)save();
    }
    public static String offer(Player p,Element e){
        Nation n=get(e);if(n==null||!nearby(p,n)||p.isSpectator()||!p.isAlive())return "Stand inside that nation to make its offering.";
        if(!trialsPassed())return "Complete the raid trials of all four nations first.";
        if(n.offered)return "This nation already received its four-element offering.";
        Map<Element,Integer> slots=new EnumMap<>(Element.class);
        for(int i=0;i<p.getInventory().getContainerSize();i++){
            String kind=Relics.kind(p.getInventory().getItem(i));if(kind.startsWith("heart:")){Element heart=Element.parse(kind.substring(6));if(heart!=null)slots.putIfAbsent(heart,i);}
        }
        if(slots.size()!=4)return "Bring one Earth, Water, Fire, and Air heart. The offering consumes one of each.";
        // Commit completion before consuming the four distinct input items.
        n.offered=true;save();for(int slot:slots.values())p.getInventory().getItem(slot).shrink(1);
        enroll((ServerLevel)p.level(),n);for(Villager v:living((ServerLevel)p.level(),n))v.addTag("cb_guard_harmony");
        return unlocked()?"All four nations empowered! Everyone can now craft a Dragon Covenant: /creeperborn ritual":"Nation empowered with all four elements!";
    }
    public static String ritual(Player p){
        if(!unlocked()||p.isSpectator()||!p.isAlive())return "Empower all four nations to unlock the Dragon Covenant.";
        Map<net.minecraft.world.item.Item,Integer> costs=Map.of(net.minecraft.world.item.Items.END_CRYSTAL,4,net.minecraft.world.item.Items.NETHER_STAR,1);
        for(var entry:costs.entrySet()){int count=0;for(int i=0;i<p.getInventory().getContainerSize();i++){var item=p.getInventory().getItem(i);if(item.is(entry.getKey())&&Relics.kind(item).isEmpty())count+=item.getCount();}if(count<entry.getValue())return "A Dragon Covenant costs 4 End Crystals and 1 Nether Star per summon.";}
        for(var entry:costs.entrySet()){int left=entry.getValue();for(int i=0;i<p.getInventory().getContainerSize()&&left>0;i++){var item=p.getInventory().getItem(i);if(item.is(entry.getKey())&&Relics.kind(item).isEmpty()){int take=Math.min(left,item.getCount());item.shrink(take);left-=take;}}}
        Progress.give(p,Relics.covenant());return "Dragon Covenant crafted. Use it at the End exit portal after defeating the normal dragon.";
    }
    public static void show(Player p){
        for(Element e:Element.values()){
            Nation n=get(e);if(n==null){p.sendSystemMessage(Component.literal(e.title+": not founded | /creeperborn nation found "+e.key()));continue;}
            String state=n.offered?"Empowered":n.passed?"Trial passed":n.raiding?"Raid wave "+n.wave+"/3":"Awaiting trial";
            p.sendSystemMessage(Component.literal(e.title+": "+state+" | "+n.x+", "+n.y+", "+n.z+" | /creeperborn nation start|offer "+e.key()));
        }
    }
    public static void clear(){data=new Data();path=null;}
}
