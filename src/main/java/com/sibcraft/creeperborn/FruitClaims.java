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
    private record Data(String rubberOwner){}
    private static UUID rubberOwner;
    private static Path path;
    private FruitClaims(){}
    public static void load(MinecraftServer server){
        path=server.getWorldPath(LevelResource.ROOT).resolve("creeperborn-fruits.json");
        try {
            rubberOwner=null;
            if(Files.exists(path)){
                Data data=new Gson().fromJson(Files.readString(path),Data.class);
                if(data!=null&&data.rubberOwner()!=null)rubberOwner=UUID.fromString(data.rubberOwner());
            }
        }catch(Exception error){throw new IllegalStateException("Cannot read Creeperborn fruit ownership",error);}
    }
    private static void save(UUID next){
        if(path==null)throw new IllegalStateException("Fruit ownership world has not loaded");
        try {
            Files.createDirectories(path.getParent());
            Path temporary=path.resolveSibling(path.getFileName()+".tmp");
            Files.writeString(temporary,new Gson().toJson(new Data(next==null?null:next.toString()))+"\n");
            try{Files.move(temporary,path,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}
            catch(AtomicMoveNotSupportedException ignored){Files.move(temporary,path,StandardCopyOption.REPLACE_EXISTING);}
            rubberOwner=next;
        }catch(Exception error){throw new IllegalStateException("Cannot save Creeperborn fruit ownership",error);}
    }
    public static boolean available(){return rubberOwner==null;}
    public static boolean owns(Player p){return p.getUUID().equals(rubberOwner);}
    public static boolean claim(Player p){
        if(!(p.level() instanceof ServerLevel)||!p.isAlive()||p.isSpectator()||!available())return false;
        save(p.getUUID());return true;
    }
    /** Upgrade old alpha saves on join. Disconnects and dimension changes never release a claim. */
    public static void reconcile(Player p){
        if(!p.entityTags().contains("cb_fruit_rubber"))return;
        if(available())claim(p);
        else if(!owns(p)){
            resetFruitProgress(p);
            Creeperborn.say(p,"Rubber is already claimed by another living player in this world.");
        }
    }
    private static void resetFruitProgress(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.equals("cb_fruit_rubber")||tag.equals("cb_mode_fruit")||tag.startsWith("cb_rubber_"))p.removeTag(tag);
    }
    public static void releaseOnDeath(Player p){
        Rubber.forget(p);
        if(owns(p))save(null);
        resetFruitProgress(p);
        WaterRules.forget(p);
    }
    public static void clear(){rubberOwner=null;path=null;}
}
