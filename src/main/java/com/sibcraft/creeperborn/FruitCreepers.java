package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.network.chat.Component;

public final class FruitCreepers {
    private static final Map<UUID,Creeper> loaded=new HashMap<>();
    public static boolean fruit(Entity c){return c.entityTags().contains("cb_fruitborn_rubber")||door(c);}
    public static boolean door(Entity c){return c.entityTags().contains("cb_fruitborn_door");}
    public static void assignDoor(Creeper c){c.addTag("cb_fruitborn_door");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Door Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean nearby(Creeper c){return loaded.values().stream().anyMatch(other->other!=c&&other.isAlive()&&!other.isRemoved()&&other.level()==c.level()&&other.distanceToSqr(c)<128*128);}
    public static void assign(Creeper c){c.addTag("cb_fruitborn_rubber");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Rubber Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static void track(Entity entity){
        if(!(entity instanceof Creeper c)||!(c.level() instanceof ServerLevel level))return;
        if(fruit(c)){loaded.put(c.getUUID(),c);return;}
        if(c.entityTags().contains("cb_fruit_checked")||c.entityTags().contains("ns_creeper"))return;
        c.addTag("cb_fruit_checked");
        if(ElementalCreepers.element(c)!=null||c.entityTags().contains("cb_checked"))return;
        if((FruitClaims.available()||FruitClaims.doorAvailable())&&Creeperborn.config().naturalCreepers&&level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL
            &&level.getRandom().nextDouble()<Creeperborn.config().fruitChance&&!nearby(c)){
            if(FruitClaims.doorAvailable()&&(!FruitClaims.available()||level.getRandom().nextBoolean()))assignDoor(c);else assign(c);
        }
    }
    public static void unload(Entity c){loaded.remove(c.getUUID());}
    public static void clear(){loaded.clear();}
    public static void died(LivingEntity entity,DamageSource source){
        if((door(entity)?!FruitClaims.doorAvailable():!FruitClaims.available())||!fruit(entity)||!(entity instanceof Creeper c)||!(c.level() instanceof ServerLevel level)||!(source.getEntity() instanceof Player)||c.entityTags().contains("cb_fruit_dropped"))return;
        c.addTag("cb_fruit_dropped");c.spawnAtLocation(level,door(c)?Relics.doorFruit():Relics.fruit());unload(c);
    }
}


