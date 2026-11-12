package com.sibcraft.creeperborn;

import java.nio.file.Path;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.network.chat.*;

/** Only the equipped fruit's tags are live: stored fruits cannot stack their passive defenses. */
public final class HolyBag {
    public static final class Collection {
        public String active = "";
        public Map<String,Set<String>> fruits = new HashMap<>();
    }
    public static final class Data { public Map<String,Collection> players = new HashMap<>(); }
    private static Data data = new Data();
    private static Path path;
    public static void load(MinecraftServer server) {
        path=server.getWorldPath(LevelResource.ROOT).resolve("creeperborn-holy-bags.json");
        data=WorldJson.read(path,Data.class,new Data());
        if(data.players==null)throw new IllegalStateException("Invalid Holy Bag ledger");
        for(var c:data.players.values())if(c==null||c.active==null||c.fruits==null||c.fruits.keySet().stream().anyMatch(f->!Dark.FRUITS.contains(f))||c.fruits.values().stream().anyMatch(java.util.Objects::isNull))throw new IllegalStateException("Invalid Holy Bag collection");
    }
    public static boolean unlocked(Player p) { return data.players.containsKey(p.getUUID().toString()); }
    private static Collection collection(Player p) { return data.players.get(p.getUUID().toString()); }
    private static boolean tag(String t,String f) {
        return t.equals("cb_fruit_"+f)||t.startsWith("cb_"+(f.equals("time")?"chrono":f)+"_")&&!t.startsWith("cb_dark_capture|");
    }
    public static String active(Player p) {
        if(!unlocked(p))return "";
        String f=collection(p).active;
        if(Dark.FRUITS.contains(f)&&p.entityTags().contains("cb_fruit_"+f))return f;
        for(String known:Dark.FRUITS)if(p.entityTags().contains("cb_fruit_"+known)&&p.getUUID().equals(FruitClaims.owner(known)))return known;
        return "";
    }
    static void removeTags(Player p,String fruit){for(String t:Set.copyOf(p.entityTags()))if(tag(t,fruit))p.removeTag(t);}
    public static void snapshot(Player p) {
        if(!unlocked(p))return;
        Collection c=collection(p);String f=active(p);if(!f.isEmpty())c.active=f;
        for(String fruit:java.util.List.of(f,FormCarry.held(p)))if(!fruit.isEmpty()){
            Set<String> tags=new HashSet<>();for(String t:p.entityTags())if(tag(t,fruit))tags.add(t);
            if(tags.contains("cb_fruit_"+fruit))c.fruits.put(fruit,tags);
        }
    }
    public static boolean consume(Player p,ItemStack item) {
        if(item.isEmpty()||!Relics.kind(item).equals("holy_bag")||p.isSpectator()||!p.isAlive()||unlocked(p))return false;
        Collection c=new Collection();data.players.put(p.getUUID().toString(),c);
        // Preserve both powers if the player previously used Dark's second slot.
        String chosen=Dark.unlocked(p)?Dark.choice(p):"";
        Dark.stopForms(p);
        for(String f:Dark.FRUITS)if(p.getUUID().equals(FruitClaims.owner(f))&&p.entityTags().contains("cb_fruit_"+f)) {
            Set<String> saved=new HashSet<>();for(String t:p.entityTags())if(tag(t,f))saved.add(t);
            c.fruits.put(f,saved);if(chosen.isEmpty())chosen=f;
        }
        for(String t:Set.copyOf(p.entityTags()))for(String f:Dark.FRUITS)if(tag(t,f))p.removeTag(t);
        c.active=chosen;if(c.fruits.containsKey(chosen))c.fruits.get(chosen).forEach(p::addTag);if(chosen.equals("dark"))Dark.choose(p,"dark");
        WorldJson.write(path,data);item.shrink(1);return true;
    }
    /** Called only after a fruit claim has been validated. */
    public static void beforeConsume(Player p) {
        if(!unlocked(p))return;
        Dark.stopForms(p);snapshot(p);FormCarry.drop(p);WorldJson.write(path,data);
        for(String t:Set.copyOf(p.entityTags()))for(String f:Dark.FRUITS)if(tag(t,f))p.removeTag(t);
        collection(p).active="";
    }
    public static String select(Player p,String fruit) {
        if(!unlocked(p)||!collection(p).fruits.containsKey(fruit)||!p.getUUID().equals(FruitClaims.owner(fruit)))return "That fruit is not in your Holy Bag.";
        if(!p.isAlive()||p.isSpectator()||StringFruit.locked(p))return "You cannot switch fruits right now.";
        FormCarry.prepare(p);snapshot(p);
        String pin=FormCarry.held(p);
        for(String f:Dark.FRUITS)if(!f.equals(pin))removeTags(p,f);
        Collection c=collection(p);c.active=fruit;c.fruits.get(fruit).forEach(p::addTag);if(fruit.equals("dark"))Dark.choose(p,"dark");
        p.addTag("cb_mode_fruit");WorldJson.write(path,data);
        return "Holy Bag: "+fruit+" | G cycles collected fruits | /creeperborn bag";
    }
    public static String cycle(Player p) {
        snapshot(p);List<String> known=Dark.FRUITS.stream().filter(f->collection(p).fruits.containsKey(f)).toList();
        if(known.isEmpty())return "Your Holy Bag is empty. Find and eat a Creeper Fruit.";
        String f=active(p);int i=known.indexOf(f);
        return select(p,!p.entityTags().contains("cb_mode_fruit")&&i>=0?f:known.get((i+1)%known.size()));
    }
    public static void died(Player p) { FormCarry.cancel(p);WorldJson.write(path,data); }
    public static void show(Player p) {
        if(!unlocked(p)){p.sendSystemMessage(Component.literal("Earn the Holy Bag by defeating the Three-Headed Dragon."));return;}
        snapshot(p);p.sendSystemMessage(Component.literal("Holy Bag — permanent collection, retained on death. Active: "+active(p)));
        for(String f:Dark.FRUITS)if(collection(p).fruits.containsKey(f))p.sendSystemMessage(Component.literal("[Equip "+f+"]").withStyle(s->s.withClickEvent(new ClickEvent.RunCommand("/creeperborn bag "+f))));
    }
    public static void save(MinecraftServer server) {
        for(var p:server.getPlayerList().getPlayers())snapshot(p);
        WorldJson.write(path,data);
    }
    public static void clear() { data=new Data();path=null;FormCarry.clear(); }
}
