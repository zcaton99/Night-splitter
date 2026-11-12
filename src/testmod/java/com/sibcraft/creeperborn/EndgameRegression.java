package com.sibcraft.creeperborn;

import java.util.*;
import com.mojang.authlib.GameProfile;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.*;

public final class EndgameRegression implements net.fabricmc.api.ModInitializer {
    @Override public void onInitialize(){if(Boolean.getBoolean("creeperborn.multiplayer"))return;net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STARTED.register(server->{for(Element e:Element.values()){int x=-8+e.ordinal()*256;server.overworld().setChunkForced(x>>4,24>>4,true);}});}

    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);System.out.println("ENDGAME_CHECK: "+text);}
    private static Player player(ServerLevel level){return new Player(level,new GameProfile(UUID.randomUUID(),"NationTest")){@Override public GameType gameMode(){return GameType.SURVIVAL;}};}
    private static boolean eat(Player p,String f){var item=Relics.fruit(f);return switch(f){
        case "time"->TimePowers.consume(p,item);case "fox","axolotl"->ZoanPowers.consume(p,item,f);
        case "rubber"->Rubber.consume(p,item);case "door"->DoorFruit.consume(p,item);case "phoenix"->Phoenix.consume(p,item);case "lightning"->Lightning.consume(p,item);case "dark"->Dark.consume(p,item);case "op"->Op.consume(p,item);case "gravity"->Gravity.consume(p,item);case "string"->StringFruit.consume(p,item);case "paw"->Paw.consume(p,item);case "tremor"->Tremor.consume(p,item);
        case "love","sand"->PairPowers.consume(p,item,f);case "ice","magma"->ClimatePowers.consume(p,item,f);case "azure"->Azure.consume(p,item);case "tree","petal"->NaturePowers.consume(p,item,f);case "glint","straw","flame","buddha"->CelestialPowers.consume(p,item,f);default->false;};}
    static void run(MinecraftServer server){
        ServerLevel l=server.overworld();Player p=player(l);
        for(Element e:Element.values())Progress.consume(p,Relics.heart(e));
        List<Villager> all=new ArrayList<>();
        for(Element e:Element.values()){
            int x=-8+e.ordinal()*256,z=24;l.getChunk(x>>4,z>>4);p.snapTo(x,120,z);
            List<Villager> guards=new ArrayList<>();
            for(int i=0;i<9;i++){var v=EntityTypes.VILLAGER.create(l,EntitySpawnReason.COMMAND);v.setNoAi(true);v.snapTo(x+i*.3,120,z);l.addFreshEntity(v);check(Creeperborn.GUARDIANS.assign(p,v,e),"Adult villager accepts "+e.key());guards.add(v);all.add(v);}
            String founded=Nations.found(p,e);check(founded.contains("founded"),"A distinct "+e.key()+" nation can be founded: "+founded);
            Nations.Nation n=Nations.get(e);check(Nations.living(l,n).size()==9,"Nine distinct living guardians count toward the trial");
            check(!Creeperborn.GUARDIANS.assign(p,guards.getFirst(),Element.values()[(e.ordinal()+1)%4]),"An enrolled villager cannot count for another nation");
            guards.getFirst().setHealth(0);check(Nations.start(p,e).contains("needs 9"),"Dead guardians do not satisfy the starting threshold");guards.getFirst().setHealth(20);
            check(Nations.start(p,e).contains("activated"),"Nation trial requires deliberate activation");
            check(!n.passed&&!Nations.trialsPassed(),"Starting a trial never grants completion");
            for(int wave=1;wave<=3;wave++){
                n.nextWave=0;Nations.advanceTrial(l,n);check(n.wave==wave&&n.enemies.size()==4+wave*2,"Trial spawns the full stronger wave "+wave);
                // An unload/missing lookup cannot count as a kill: the UUID ledger remains authoritative.
                String first=n.enemies.iterator().next();check(l.getEntity(UUID.fromString(first))!=null,"Raid mobs survive their load callback");
                for(String id:List.copyOf(n.enemies)){var mob=(LivingEntity)l.getEntity(UUID.fromString(id));mob.hurtServer(l,l.damageSources().genericKill(),10000);}
                check(n.enemies.isEmpty(),"Actual mob deaths advance the trial ledger");
            }
            n.nextWave=0;Nations.advanceTrial(l,n);check(n.passed&&!n.raiding,"Defeating all three waves completes "+e.key()+" trial");
        }
        check(Nations.trialsPassed()&&!Nations.unlocked(),"All four raid wins precede the four offerings");
        Player sibling=player(l);
        for(Element e:Element.values()){
            var n=Nations.get(e);sibling.snapTo(n.x,n.y,n.z);
            check(Nations.offer(sibling,e).contains("Bring one"),"An offering cannot succeed without all four hearts");
            for(Element heart:Element.values())sibling.addItem(Relics.heart(heart));
            check(Nations.offer(sibling,e).contains("empowered"),"A sibling can complete a shared nation offering");
            check(Nations.living(l,n).stream().allMatch(v->v.entityTags().contains("cb_guard_harmony")),"The offering empowers that nation's guardians");
        }
        check(Nations.unlocked(),"All four offerings unlock the ritual for everyone");
        Nations.load(server);check(Nations.unlocked(),"Completed nations and offerings survive world-ledger reload");
        check(Nations.ritual(sibling).contains("costs"),"Repeat summons require fresh ingredients");
        sibling.addItem(new ItemStack(Items.END_CRYSTAL,4));sibling.addItem(new ItemStack(Items.NETHER_STAR));
        check(Nations.ritual(sibling).contains("crafted"),"Shared progress enables a sibling to craft the summoning item");
        check(ThreeHeadedDragon.summon(sibling,Relics.covenant()).contains("End exit portal"),"Summoning is rejected outside the End");
        // Combat and rewards use production damage interception, with independent simulated attackers.
        ThreeHeadedDragon.begin(l,new Vec3(-8,130,24),2);var f=ThreeHeadedDragon.state();
        check(f.max==3600&&f.headMax==240,"Boss health scales for two participants");
        Entity head=l.getEntity(UUID.fromString(f.heads[0]));float before=f.health;
        for(int i=0;i<3;i++){Player attacker=player(l);((LivingEntity)head).hurtServer(l,l.damageSources().playerAttack(attacker),100);}
        check(f.headHealth[0]==0&&f.headHealth[1]>0&&f.headHealth[2]>0&&f.exposed>f.age,"A head can be severed independently and exposes the body");
        check(f.health<before,"Head strikes also damage the shared body pool");
        Entity body=l.getEntity(UUID.fromString(f.body));
        for(int i=0;i<40&&ThreeHeadedDragon.active();i++)((LivingEntity)body).hurtServer(l,l.damageSources().playerAttack(player(l)),100);
        check(!ThreeHeadedDragon.active()&&f.rewarded,"Defeating the body completes the encounter exactly once");
        var bags=l.getEntitiesOfClass(ItemEntity.class,new AABB(-100,0,-100,100,400,100)).stream().filter(v->Relics.kind(v.getItem()).equals("holy_bag")).toList();
        check(bags.size()==1&&bags.getFirst().getItem().getCount()==1,"Exactly one physical Holy Bag drops per dragon kill");
        ThreeHeadedDragon.damage((LivingEntity)body,l.damageSources().playerAttack(p),100);
        check(l.getEntitiesOfClass(ItemEntity.class,new AABB(-100,0,-100,100,400,100)).stream().filter(v->Relics.kind(v.getItem()).equals("holy_bag")).count()==1,"Repeated death processing cannot duplicate the bag");
        for(String fruit:Dark.FRUITS)FruitClaims.setOwner(fruit,null);
        Player owner=player(l);check(HolyBag.consume(owner,bags.getFirst().getItem()),"Holy Bag is consumed to permanently unlock collection");bags.getFirst().discard();
        check(Rubber.consume(owner,Relics.fruit()),"Bag user can consume first fruit");Rubber.grant(owner,3);
        check(ZoanPowers.consume(owner,Relics.zoanFruit("fox"),"fox"),"Bag user can collect a second non-Dark fruit");
        check(!Rubber.unlocked(owner)&&ZoanPowers.unlocked(owner,"fox"),"Stored fruits cannot stack passive powers");
        HolyBag.select(owner,"rubber");check(Rubber.unlocked(owner)&&Rubber.tier(owner)==3&&!ZoanPowers.unlocked(owner,"fox"),"Switching restores exact mastery and equips only one fruit");
        for(Element e:Element.values())Progress.consume(owner,Relics.heart(e));
        FruitClaims.releaseOnDeath(owner);check(HolyBag.unlocked(owner)&&Rubber.unlocked(owner)&&Rubber.tier(owner)==3,"Death preserves Holy Bag, active fruit and mastery");
        HolyBag.select(owner,"fox");check(ZoanPowers.unlocked(owner,"fox")&&Progress.all(owner),"Death also preserves stored powers and elemental progress");
        HolyBag.died(owner);HolyBag.load(server);FruitClaims.load(server);HolyBag.select(owner,"rubber");
        check(Rubber.unlocked(owner)&&Rubber.tier(owner)==3,"Collection, claims and mastery survive ledger reload");
        for(String fruit:Dark.FRUITS){
            if(!owner.getUUID().equals(FruitClaims.owner(fruit)))check(eat(owner,fruit),"Holy Bag accepts collected fruit "+fruit);
            HolyBag.select(owner,fruit);Dark.grantFruit(owner,fruit,3);
        }
        HolyBag.died(owner);HolyBag.load(server);FruitClaims.load(server);
        for(String fruit:Dark.FRUITS){HolyBag.select(owner,fruit);check(Dark.fruitTier(owner,fruit)==3&&Dark.FRUITS.stream().filter(fruitId->owner.entityTags().contains("cb_fruit_"+fruitId)).count()==1,"All-fruit collection restores only "+fruit+" at its own mastery");}
        owner.hurtServer(l,l.damageSources().genericKill(),10000);
        check(!owner.isAlive()&&HolyBag.unlocked(owner)&&Dark.FRUITS.stream().allMatch(fruit->owner.getUUID().equals(FruitClaims.owner(fruit))),"Actual player death retains all 24 fruit claims");
        for(String fruit:Dark.FRUITS)FruitClaims.setOwner(fruit,null);
        all.forEach(Entity::discard);ThreeHeadedDragon.clear();ThreeHeadedDragon.load(server);
        HotfixRegression.run(server);System.out.println("CREEPERBORN_ENDGAME_REGRESSION_PASSED");
    }
}
