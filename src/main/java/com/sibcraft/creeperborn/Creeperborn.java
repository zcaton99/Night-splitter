package com.sibcraft.creeperborn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Creeperborn implements ModInitializer {
    public static final Abilities ABILITIES = new Abilities();
    public static final Guardians GUARDIANS = new Guardians();
    public static final ElementalCreepers CREEPERS = new ElementalCreepers();
    public static final class Config { public boolean naturalCreepers = true; public double elementalChance = .02; public double fruitChance = .002; }
    private static Config config = new Config();
    public static Config config() { return config; }
    @Override public void onInitialize() {
        CombatNetwork.init();
        ServerLifecycleEvents.SERVER_STARTING.register(FruitClaims::load);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> { DoorSpace.recover(handler.player); Rubber.recoverFlight(handler.player); FruitClaims.reconcile(handler.player); });
        Path path = FabricLoader.getInstance().getConfigDir().resolve("creeperborn.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create…8226 tokens truncated…uals("cb_mode_fruit")||tag.startsWith("cb_rubber_"))p.removeTag(tag);
    }
    private static void resetDoorProgress(Player p){
        for(String tag:Set.copyOf(p.entityTags()))if(tag.equals("cb_fruit_door")||tag.startsWith("cb_door_")||tag.equals("cb_mode_fruit"))p.removeTag(tag);
    }
    public static void releaseOnDeath(Player p){
        DoorSpace.forget(p);DoorFruit.forget(p);
        if(ownsDoor(p))save(rubberOwner,null);
        resetDoorProgress(p);
        Rubber.forget(p);
        if(owns(p))save(null);
        resetFruitProgress(p);
        WaterRules.forget(p);
    }
    public static void clear(){rubberOwner=null;doorOwner=null;path=null;}
}

