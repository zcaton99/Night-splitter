package com.sibcraft.creeperborn;

import com.google.gson.Gson;
import java.nio.file.*;
import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.entity.player.Player;

/** One living owner per fruit in this world, including offline owners. Written before claims succeed. */
public final class FruitClaims {
    private record Data(String rubberOwner,String doorOwner,String phoenixOwner,String lightningOwner,String darkOwner){}
    private static UUID rubberOwner,doorOwner,phoenixOwner,lightningOwner,darkOwner;
    private static Path path;
    private FruitClaims(){}
    public static void load(MinecraftServer server){
        path=server.getWorldPath(LevelResource.ROOT).resolve("creeperborn-fruits.json");
        try {
            rubberOwner=null;doorOwner=null;phoenixOwner=null;lightningOwner=null;darkOwner=null;
            if(Files.exists(path)){
                Data data=new Gson().fromJson(Files.readString(path),Data.class);
                if(data!=null&&data.rubberOwner()!=null)rubberOwner=UUID.fromString(data.rubberOwner());
                if(data!=null&&data.doorOwner()!=null)doorOwner=UUID.fromString(data.doorOwner());
                if(data!=null&&data.phoenixOwner()!=null)phoenixOwner=UUID.fromString(data.phoenixOwner());
                if(data!=null&&data.lightningOwner()!=null)lightningOwner=UUID.fromString(data.lightningOwner());
                if(data!=null&&data.darkOwner()!=null)darkOwner=UUID.fromString(data.darkOwner());
            }
        }catch(Exception error){throw new IllegalStateException("Cannot read Creeperborn fruit ownership",error);}
    }
    private static void save(UUID next){save(next,doorOwner);}
    private static void save(UUID next,UUID nextDoor){save(next,nextDoor,phoenixOwner);}
    private static void save(UUID next,UUID nextDoor,UUID nextPhoenix){save(next,nextDoor,nextPhoenix,lightningOwner);}
    private static void save(UUID next,UUID nextDoor,UUID nextPhoenix,UUID nextLightning){save(next,nextDoor,nextPhoenix,nextLightning,darkOwner);}
    private static void save(UUID next,UUID nextDoor,UUID nextPhoenix,UUID nextLightning,UUID nextDark){
        if(path==null)throw new IllegalStateException("Fruit ownership world has not loaded");
        try {
            Files.createDirectories(path.getParent());
            Path temporary=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(temporary,new Gson().toJson(new Data(next==null?null:next.toString(),nextDoor==null?null:nextDoor.toString(),nextPhoenix==null?null:nextPhoenix.toString(),nextLightning==null?null:nextLightning.toString(),nextDark==null?null:nextDark.toString()))+"\n");
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
            rubberOwner=next;doorOwner=nextDoor;phoenixOwner=nextPhoenix;lightningOwner=nextLightning;darkOwner=nextDark;
        }catch(Exception error){throw new IllegalStateException("Cannot save Creeperborn fruit ownership",error);}
    }
    public static boolean darkAvailable(){return darkOwner==null;}
    public static boolean ownsDark(Player p){return p.getUUID().equals(darkOwner);}
    public static boolean claimDark(Player p){
        if(!(p.level() instanceof ServerLevel)||!p.isAlive()||p.isSpectator()||!darkAvailable()||hasFruit(p))return false;
        setOwner("dark",p.getUUID());return true;
    }
    public static UUID owner(String fruit){
        return switch(fruit){case "rubber"->rubberOwner;case "door"->doorOwner;case "phoenix"->phoenixOwner;case "lightning"->lightningOwner;case "dark"->darkOwner;default->null;};
    }
    public static void setOwner(String fruit,UUID id){
        switch(fruit){
            case "rubber"->save(id,doorOwner,phoenixOwner,lightningOwner,darkOwner);
            case "door"->save(rubberOwner,id,phoenixOwner,lightningOwner,darkOwner);
            case "phoenix"->save(rubberOwner,doorOwner,id,lightningOwner,darkOwner);
            case "lightning"->save(rubberOwner,doorOwner,phoenixOwner,id,darkOwner);
            case "dark"->save(rubberOwner,doorOwner,phoenixOwner,lightningOwner,id);
            default->throw new IllegalArgumentException("Unknown fruit");
        }
    }
    public static boolean claimItem(Player p,net.minecraft.world.item.ItemStack item,String fruit){
        String token=Relics.captureToken(item);
        if(!token.isEmpty()){
            UUID id;try{id=UUID.fromString(token);}catch(IllegalArgumentException error){return false;}
            boolean room=!hasFruit(p)||Dark.unlocked(p)&&Dark.extra(p)==null&&!fruit.equals("dark");
            if(!room||!id.equals(owner(fruit))||!p.isAlive()||p.isSpectator())return false;
            setOwner(fruit,p.getUUID());return true;
        }
        return switch(fruit){case "rubber"->claim(p);case "door"->claimDoor(p);case "phoenix"->claimPhoenix(p);case "lightning"->claimLightning(p);case "dark"->claimDark(p);default->false;};
    }
    public static boolean eatCapture(Player p,String fruit,UUID token){
        if(!Dark.unlocked(p)||Dark.extra(p)!=null||fruit.equals("dark")||!token.equals(owner(fruit))||!p.isAlive()||p.isSpectator())return false;
        setOwner(fruit,p.getUUID());return true;
    }
    public static void releaseItem(net.minecraft.world.item.ItemStack item){
        String token=Relics.captureToken(item),kind=Relics.kind(item);
        if(token.isEmpty()||!kind.startsWith("fruit:"))return;
        try{String fruit=kind.substring(6);if(UUID.fromString(token).equals(owner(fruit)))setOwner(fruit,null);}catch(IllegalArgumentException ignored){}
    }
    public static boolean available(){return rubberOwner==null;}
    public static boolean owns(Player p){return p.getUUID().equals(rubberOwner);}
    public static boolean doorAvailable(){return doorOwner==null;}
    public static boolean ownsDoor(Player p){return p.getUUID().equals(doorOwner);}
    public static boolean phoenixAvailable(){return phoenixOwner==null;}
    public static boolean ownsPhoenix(Player p){return p.getUUID().equals(phoenixOwner);}
    public static boolean lightningAvailable(){return lightningOwner==null;}
    public static boolean ownsLightning(Player p){return p.getUUID().equals(lightningOwner);}
    public static boolean claimLightning(Player p){
        if(!(p.level() instanceof ServerLevel)||!p.isAlive()||p.isSpectator()||!lightningAvailable()||hasFruit(p))return false;
        save(rubberOwner,doorOwner,phoenixOwner,p.getUUID());return true;
    }
    public static boolean claimPhoenix(Player p){
        if(!(p.level() instanceof ServerLevel)||!p.isAlive()||p.isSpectator()||!phoenixAvailable()||hasFruit(p))return false;
        save(rubberOwner,doorOwner,p.getUUID());return true;
    }
    public static boolean hasFruit(Player p){return ownsDark(p)||p.entityTags().contains("cb_fruit_dark")||ownsLightning(p)||p.entityTags().contains("cb_fruit_lightning")||ownsPhoenix(p)||p.entityTags().contains("cb_fruit_phoenix")||owns(p)||ownsDoor(p)||p.entityTags().contains("cb_fruit_rubber")||p.entityTags().contains("cb_fruit_door");}
    public static boolean claimDoor(Player p){
        if(!(p.level() instanceof ServerLevel)||!p.isAlive()||p.isSpectator()||!doorAvailable()||hasFruit(p))return false;
        save(rubberOwner,p.getUUID());return true;
    }
    public static boolean claim(Player p){
        if(!(p.level() instanceof ServerLevel)||!p.isAlive()||p.isSpectator()||!available()||hasFruit(p))return false;
        save(p.getUUID());return true;
    }
    /** Upgrade old alpha saves on join. Disconnects and dimension changes never release a claim. */
    public static void reconcile(Player p){
        if(p.entityTags().contains("cb_fruit_dark")){
            if(darkAvailable())setOwner("dark",p.getUUID());
            else if(!ownsDark(p))for(String t:Set.copyOf(p.entityTags()))if(t.equals("cb_fruit_dark")||t.startsWith("cb_dark_"))p.removeTag(t);
        }
        if(p.entityTags().contains("cb_fruit_door")){
            if(owns(p)||p.entityTags().contains("cb_fruit_rubber")){
                if(ownsDoor(p))save(rubberOwner,null);
                resetDoorProgress(p);
            }else if(doorAvailable())save(rubberOwner,p.getUUID());
            else if(!ownsDoor(p))resetDoorProgress(p);
        }
        if(p.entityTags().contains("cb_fruit_phoenix")){
            if(owns(p)||ownsDoor(p)||p.entityTags().contains("cb_fruit_rubber")||p.entityTags().contains("cb_fruit_door")){
                if(ownsPhoenix(p))save(rubberOwner,doorOwner,null);
                resetPhoenix(p);
            }else if(phoenixAvailable())save(rubberOwner,doorOwner,p.getUUID());
            else if(!ownsPhoenix(p))resetPhoenix(p);
        }
        if(p.entityTags().contains("cb_fruit_lightning")){
            if(owns(p)||ownsDoor(p)||ownsPhoenix(p)||p.entityTags().contains("cb_fruit_rubber")||p.entityTags().contains("cb_fruit_door")||p.entityTags().contains("cb_fruit_phoenix")){
                if(ownsLightning(p))save(rubberOwner,doorOwner,phoenixOwner,null);resetLightning(p);
            }else if(lightningAvailable())save(rubberOwner,doorOwner,phoenixOwner,p.getUUID());
            else if(!ownsLightning(p))resetLightning(p);
        }
        if(!p.entityTags().contains("cb_fruit_rubber"))return;
        if(available())save(p.getUUID());
        else if(!owns(p)){
            resetFruitProgress(p);
            Creeperborn.say(p,"Rubber is already claimed by another living player in this world.");
        }
    }
    private static void resetFruitProgress(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.equals("cb_fruit_rubber")||tag.equals("cb_mode_fruit")||tag.startsWith("cb_rubber_"))p.removeTag(tag);
    }
    private static void resetDoorProgress(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.equals("cb_fruit_door")||tag.startsWith("cb_door_")||tag.equals("cb_mode_fruit"))p.removeTag(tag);
    }
    private static void resetPhoenix(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.equals("cb_fruit_phoenix")||tag.startsWith("cb_phoenix_"))p.removeTag(tag);
    }
    private static void resetLightning(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.equals("cb_fruit_lightning")||tag.startsWith("cb_lightning_"))p.removeTag(tag);
    }
    public static void releaseOnDeath(Player p){
        for(String t:Set.copyOf(p.entityTags()))if(t.startsWith("cb_fruit_suppressed_"))p.removeTag(t);
        Dark.ownerDied(p);Dark.forget(p);if(ownsDark(p))setOwner("dark",null);
        for(String t:Set.copyOf(p.entityTags()))if(t.equals("cb_fruit_dark")||t.startsWith("cb_dark_")&&!t.startsWith("cb_dark_capture|"))p.removeTag(t);
        Lightning.forget(p);if(ownsLightning(p))save(rubberOwner,doorOwner,phoenixOwner,null);resetLightning(p);
        Phoenix.forget(p);if(ownsPhoenix(p))save(rubberOwner,doorOwner,null);resetPhoenix(p);
        DoorSpace.forget(p);DoorFruit.forget(p);
        if(ownsDoor(p))save(rubberOwner,null);
        resetDoorProgress(p);
        Rubber.forget(p);
        if(owns(p))save(null);
        resetFruitProgress(p);
        WaterRules.forget(p);
    }
    public static void clear(){rubberOwner=null;doorOwner=null;phoenixOwner=null;lightningOwner=null;darkOwner=null;path=null;}
}

