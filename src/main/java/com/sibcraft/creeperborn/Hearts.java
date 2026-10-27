package com.sibcraft.creeperborn;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.*;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.storage.LevelResource;

/** Persistent token authority. Unload is not destruction; only an active token can hurt its victim. */
public final class Hearts {
    private static final class Link {
        String token,owner,victim,victimName,ownerName; long ground=-1; boolean pending,crushed;
        Link(String token,String owner,String victim){this.token=token;this.owner=owner;this.victim=victim;}
    }
    private static final Map<String,Link> links=new HashMap<>();
    private static final Map<UUID,Long> squeezeReady=new HashMap<>();
    private record Crush(String token,long start,long heartbeat){}
    private static final Map<UUID,Crush> crushing=new HashMap<>();
    private static MinecraftServer server;private static Path path;
    private static long clock(){return server.overworld().getGameTime();}
    public static void load(MinecraftServer s){
        server=s;path=s.getWorldPath(LevelResource.ROOT).resolve("creeperborn-hearts.json");links.clear();squeezeReady.clear();crushing.clear();
        try{if(Files.exists(path)){Link[] data=new Gson().fromJson(Files.readString(path),Link[].class);if(data!=null)for(Link link:data){
            UUID.fromString(link.token);UUID.fromString(link.owner);UUID.fromString(link.victim);links.put(link.token,link);
        }}}catch(Exception error){throw new IllegalStateException("Cannot load extracted hearts",error);}
    }
    private static void save(){
        try{Files.createDirectories(path.getParent());Path temp=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(temp,new Gson().toJson(links.values())+"\n");
            try{Files.move(temp,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException ignored){Files.move(temp,path,StandardCopyOption.REPLACE_EXISTING);}
        }catch(Exception error){throw new IllegalStateException("Cannot save extracted hearts",error);}
    }
    public static String token(ItemStack item){
        if(!Relics.kind(item).equals("op:heart"))return "";
        var data=item.get(DataComponents.CUSTOM_DATA);return data==null?"":data.copyTag().getString("cb_heart_token").orElse("");
    }
    public static ItemStack item(String token){
        Link link=links.get(token);
        ItemStack item=new ItemStack(Items.GLASS);CompoundTag tag=new CompoundTag();tag.putString("creeperborn","op:heart");tag.putString("cb_heart_token",token);
        item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));item.set(DataComponents.MAX_STACK_SIZE,1);
        item.set(DataComponents.ITEM_MODEL,Identifier.fromNamespaceAndPath("creeperborn","captured_heart"));
        item.set(DataComponents.CUSTOM_NAME,Component.literal("Heart of "+(link==null?"Unknown":link.victimName)+" — extracted by "+(link==null?"Unknown":link.ownerName)));
        return item;
    }
    private static LivingEntity victim(Link link){
        UUID id=UUID.fromString(link.victim);var p=server.getPlayerList().getPlayer(id);if(p!=null)return p;
        for(ServerLevel level:server.getAllLevels())if(level.getEntity(id) instanceof LivingEntity e)return e;return null;
    }
    public static long count(Player owner){return links.values().stream().filter(l->l.owner.equals(owner.getUUID().toString())).count();}
    public static boolean extract(Player owner,LivingEntity target){
        if(!Op.unlocked(owner)||!owner.isAlive()||!target.isAlive()||target==owner||!Op.inside(owner,owner)||!Op.inside(owner,target)
            ||links.values().stream().anyMatch(l->l.victim.equals(target.getUUID().toString())))return false;
        String token=UUID.randomUUID().toString();Link link=new Link(token,owner.getUUID().toString(),target.getUUID().toString());
        // Reserve before handing out the cube. A full inventory leaves it in durable pending storage.
        link.victimName=target.getName().getString();link.ownerName=owner.getName().getString();
        link.pending=true;links.put(token,link);save();deliver(owner,link);return true;
    }
    private static void deliver(Player p,Link link){
        if(!link.pending||link.crushed)return;
        ItemStack cube=item(link.token);if(!p.addItem(cube))return;
        link.pending=false;link.ground=-1;save();
    }
    public static String squeeze(Player holder,ItemStack cube){
        Link link=links.get(token(cube));if(link==null||link.crushed)return "That heart is inert.";
        if(!holder.isAlive()||holder.isSpectator())return "";
        long now=clock();if(now<squeezeReady.getOrDefault(holder.getUUID(),0L))return "Wait a moment between squeezes.";
        LivingEntity target=victim(link);if(target==null||!target.isAlive())return "The linked target is not currently loaded.";
        squeezeReady.put(holder.getUUID(),now+20);
        if(target.getHealth()<=2)destroy(cube);else target.setHealth(target.getHealth()-2);
        return "Heart squeezed: one heart of damage.";
    }
    public static void destroy(ItemStack cube){
        Link link=links.get(token(cube));if(link==null||link.crushed)return;
        link.crushed=true;link.pending=false;save();kill(link);
    }
    private static void kill(Link link){
        LivingEntity target=victim(link);if(target==null)return;
        target.damageCooldownTime=0;target.hurtServer((ServerLevel)target.level(),target.damageSources().genericKill(),Float.MAX_VALUE);
        if(!target.isAlive()){links.remove(link.token);save();}
    }
    public static String use(Player p,ItemStack cube){
        if(!links.containsKey(token(cube)))return "That heart is inert.";
        if(!p.isShiftKeyDown()){crushing.remove(p.getUUID());return squeeze(p,cube);}
        Crush old=crushing.get(p.getUUID());long now=clock();
        if(old==null||!old.token().equals(token(cube)))crushing.put(p.getUUID(),new Crush(token(cube),now,now+15));
        return "Keep sneaking with the heart in hand for three seconds to crush it.";
    }
    public static void heartbeat(Player p,boolean held){
        Crush c=crushing.get(p.getUUID());if(c==null)return;
        if(!held)crushing.remove(p.getUUID());else crushing.put(p.getUUID(),new Crush(c.token(),c.start(),clock()+15));
    }
    public static boolean giveBack(Player holder,LivingEntity target,ItemStack cube){
        Link l=links.get(token(cube));if(l==null||l.crushed||!l.victim.equals(target.getUUID().toString())||!target.isAlive())return false;
        links.remove(l.token);save();cube.shrink(1);crushing.remove(holder.getUUID());return true;
    }
    public static void picked(String token){Link l=links.get(token);if(l!=null&&l.ground>=0){l.ground=-1;save();}}
    public static boolean groundTick(ItemEntity entity){
        String token=token(entity.getItem());if(token.isEmpty())return false;
        Link l=links.get(token);
        if(l==null||l.crushed){entity.setItem(ItemStack.EMPTY);entity.discard();return true;}
        if(l.ground<0){l.ground=clock();save();}
        if(clock()-l.ground<6000)return false;
        returnHeart(l);
        entity.setItem(ItemStack.EMPTY);entity.discard();return true;
    }
    private static void returnHeart(Link link){
        // Rotate the token so a cube in an unloaded chunk cannot duplicate the returned live heart.
        links.remove(link.token);link.token=UUID.randomUUID().toString();link.pending=true;link.ground=-1;links.put(link.token,link);save();
    }
    /** Called by Entity.remove only for destructive reasons, never on chunk unload. */
    public static void removed(ItemEntity entity,Entity.RemovalReason reason){
        if(reason.shouldDestroy()&&!entity.getItem().isEmpty())destroy(entity.getItem());
    }
    public static void died(LivingEntity entity){
        String id=entity.getUUID().toString();
        if(links.values().removeIf(l->l.victim.equals(id)||l.owner.equals(id)&&!l.crushed))save();
        crushing.remove(entity.getUUID());squeezeReady.remove(entity.getUUID());
        for(var p:server.getPlayerList().getPlayers())cleanup(p);
    }
    public static void join(Player p){
        cleanup(p);
        for(Link l:List.copyOf(links.values())){
            if(l.crushed&&l.victim.equals(p.getUUID().toString())){p.damageCooldownTime=0;p.hurtServer((ServerLevel)p.level(),p.damageSources().genericKill(),Float.MAX_VALUE);if(!p.isAlive()){links.remove(l.token);save();}}
            else if(l.owner.equals(p.getUUID().toString()))deliver(p,l);
        }
    }
    public static void tick(MinecraftServer s){
        for(Link l:List.copyOf(links.values())){if(l.crushed)kill(l);else if(l.ground>=0&&clock()-l.ground>=6000)returnHeart(l);}
        for(var p:s.getPlayerList().getPlayers()){
            cleanup(p);
            if(s.getTickCount()%20==0)for(Link l:List.copyOf(links.values()))if(l.owner.equals(p.getUUID().toString()))deliver(p,l);
            Crush c=crushing.get(p.getUUID());if(c==null)continue;
            ItemStack cube=p.getMainHandItem();if(!token(cube).equals(c.token()))cube=p.getOffhandItem();
            if(!p.isAlive()||p.isSpectator()||!p.isShiftKeyDown()||clock()>c.heartbeat()||!token(cube).equals(c.token())){crushing.remove(p.getUUID());continue;}
            if(clock()-c.start()>=60){destroy(cube);cube.shrink(1);crushing.remove(p.getUUID());Creeperborn.say(p,"Heart crushed.");}
        }
    }
    private static boolean stale(ItemStack cube){
        return !cube.isEmpty()&&Relics.kind(cube).equals("op:heart")&&!links.containsKey(token(cube));
    }
    private static int removeStale(ItemStack cube){
        if(!stale(cube))return 0;int count=cube.getCount();cube.shrink(count);return count;
    }
    /** Any holder, including offline holders on join; unrelated glass and live hearts survive. */
    public static int cleanup(Player p){
        int removed=0;
        for(int i=0;i<p.getInventory().getContainerSize();i++)removed+=removeStale(p.getInventory().getItem(i));
        removed+=removeStale(p.getOffhandItem());
        removed+=removeStale(p.containerMenu.getCarried());
        return removed;
    }
    public static void forget(Player p){crushing.remove(p.getUUID());squeezeReady.remove(p.getUUID());}
    public static void clear(){links.clear();squeezeReady.clear();crushing.clear();server=null;path=null;}
}
