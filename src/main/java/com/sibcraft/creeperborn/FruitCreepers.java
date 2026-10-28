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
    public static boolean fruit(Entity c){return c.entityTags().contains("cb_fruitborn_rubber")||door(c)||phoenix(c)||lightning(c)||dark(c)||op(c)||gravity(c)||string(c)||paw(c)||tremor(c)||love(c)||sand(c)||ice(c)||magma(c);}
    public static boolean ice(Entity c){return c.entityTags().contains("cb_fruitborn_ice");}
    public static boolean magma(Entity c){return c.entityTags().contains("cb_fruitborn_magma");}
    public static void assignIce(Creeper c){assignPair(c,"ice","Ice-Ice");}
    public static void assignMagma(Creeper c){assignPair(c,"magma","Magma-Magma");}
    public static boolean love(Entity c){return c.entityTags().contains("cb_fruitborn_love");}
    public static boolean sand(Entity c){return c.entityTags().contains("cb_fruitborn_sand");}
    public static void assignLove(Creeper c){assignPair(c,"love","Love-Love");}
    public static void assignSand(Creeper c){assignPair(c,"sand","Sand-Sand");}
    private static void assignPair(Creeper c,String fruit,String label){c.addTag("cb_fruitborn_"+fruit);c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal(label+" Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean tremor(Entity c){return c.entityTags().contains("cb_fruitborn_tremor");}
    public static void assignTremor(Creeper c){c.addTag("cb_fruitborn_tremor");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Tremor-Tremor Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean paw(Entity c){return c.entityTags().contains("cb_fruitborn_paw");}
    public static void assignPaw(Creeper c){c.addTag("cb_fruitborn_paw");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Paw-Paw Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean string(Entity c){return c.entityTags().contains("cb_fruitborn_string");}
    public static void assignString(Creeper c){c.addTag("cb_fruitborn_string");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("String Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean gravity(Entity c){return c.entityTags().contains("cb_fruitborn_gravity");}
    public static void assignGravity(Creeper c){c.addTag("cb_fruitborn_gravity");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Gravity Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean op(Entity c){return c.entityTags().contains("cb_fruitborn_op");}
    public static void assignOp(Creeper c){c.addTag("cb_fruitborn_op");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Op-Op Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean dark(Entity c){return c.entityTags().contains("cb_fruitborn_dark");}
    public static void assignDark(Creeper c){c.addTag("cb_fruitborn_dark");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Dark Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean lightning(Entity c){return c.entityTags().contains("cb_fruitborn_lightning");}
    public static void assignLightning(Creeper c){c.addTag("cb_fruitborn_lightning");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Lightning Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
    public static boolean phoenix(Entity c){return c.entityTags().contains("cb_fruitborn_phoenix");}
    public static void assignPhoenix(Creeper c){c.addTag("cb_fruitborn_phoenix");c.addTag("cb_checked");c.addTag("cb_fruit_checked");c.setCustomName(Component.literal("Phoenix Fruitborn Creeper"));c.setCustomNameVisible(true);loaded.put(c.getUUID(),c);}
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
        if((FruitClaims.available()||FruitClaims.doorAvailable()||FruitClaims.phoenixAvailable()||FruitClaims.lightningAvailable()||FruitClaims.darkAvailable()||FruitClaims.owner("op")==null||FruitClaims.owner("gravity")==null||FruitClaims.owner("string")==null||FruitClaims.owner("paw")==null||FruitClaims.owner("tremor")==null||FruitClaims.owner("love")==null||FruitClaims.owner("sand")==null||FruitClaims.owner("ice")==null||FruitClaims.owner("magma")==null)&&Creeperborn.config().naturalCreepers&&level.getDifficulty()!=net.minecraft.world.Difficulty.PEACEFUL
            &&level.getRandom().nextDouble()<Creeperborn.config().fruitChance&&!nearby(c)){
            List<String> choices=new ArrayList<>();if(FruitClaims.available())choices.add("rubber");if(FruitClaims.doorAvailable())choices.add("door");if(FruitClaims.phoenixAvailable())choices.add("phoenix");if(FruitClaims.lightningAvailable())choices.add("lightning");if(FruitClaims.darkAvailable())choices.add("dark");if(FruitClaims.owner("op")==null)choices.add("op");if(FruitClaims.owner("gravity")==null)choices.add("gravity");if(FruitClaims.owner("string")==null)choices.add("string");if(FruitClaims.owner("paw")==null)choices.add("paw");if(FruitClaims.owner("tremor")==null)choices.add("tremor");if(FruitClaims.owner("love")==null)choices.add("love");if(FruitClaims.owner("sand")==null)choices.add("sand");if(FruitClaims.owner("ice")==null)choices.add("ice");if(FruitClaims.owner("magma")==null)choices.add("magma");
            switch(choices.get(level.getRandom().nextInt(choices.size()))){case "ice"->assignIce(c);case "magma"->assignMagma(c);case "love"->assignLove(c);case "sand"->assignSand(c);case "tremor"->assignTremor(c);case "paw"->assignPaw(c);case "string"->assignString(c);case "gravity"->assignGravity(c);case "op"->assignOp(c);case "dark"->assignDark(c);case "lightning"->assignLightning(c);case "door"->assignDoor(c);case "phoenix"->assignPhoenix(c);default->assign(c);}
        }
    }
    public static void unload(Entity c){loaded.remove(c.getUUID());}
    public static void clear(){loaded.clear();}
    public static void died(LivingEntity entity,DamageSource source){
        if((ice(entity)?FruitClaims.owner("ice")!=null:magma(entity)?FruitClaims.owner("magma")!=null:love(entity)?FruitClaims.owner("love")!=null:sand(entity)?FruitClaims.owner("sand")!=null:tremor(entity)?FruitClaims.owner("tremor")!=null:paw(entity)?FruitClaims.owner("paw")!=null:string(entity)?FruitClaims.owner("string")!=null:gravity(entity)?FruitClaims.owner("gravity")!=null:op(entity)?FruitClaims.owner("op")!=null:dark(entity)?!FruitClaims.darkAvailable():lightning(entity)?!FruitClaims.lightningAvailable():phoenix(entity)?!FruitClaims.phoenixAvailable():door(entity)?!FruitClaims.doorAvailable():!FruitClaims.available())||!fruit(entity)||!(entity instanceof Creeper c)||!(c.level() instanceof ServerLevel level)||!(source.getEntity() instanceof Player)||c.entityTags().contains("cb_fruit_dropped"))return;
        c.addTag("cb_fruit_dropped");c.spawnAtLocation(level,ice(c)?Relics.iceFruit():magma(c)?Relics.magmaFruit():love(c)?Relics.loveFruit():sand(c)?Relics.sandFruit():tremor(c)?Relics.tremorFruit():paw(c)?Relics.pawFruit():string(c)?Relics.stringFruit():gravity(c)?Relics.gravityFruit():op(c)?Relics.opFruit():dark(c)?Relics.darkFruit():lightning(c)?Relics.lightningFruit():phoenix(c)?Relics.phoenixFruit():door(c)?Relics.doorFruit():Relics.fruit());unload(c);
    }
}


