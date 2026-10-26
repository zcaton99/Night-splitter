package com.sibcraft.creeperborn;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

public final class Regression implements ModInitializer {
    private MinecraftServer server;
    private int ticks;
    private double y;
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(s -> {
            server = s; Creeperborn.config().naturalCreepers = false;
            for (int x = -1; x <= 0; x++) for (int z = -1; z <= 0; z++) s.overworld().setChunkForced(x, z, true);
        });
        ServerTickEvents.END_SERVER_TICK.register(s -> {
            if (server == null || ++ticks != 30) return;
            try { run(s.overworld()); System.out.println("CREEPERBORN_REGRESSION_PASSED"); }
            catch (Throwable error) { error.printStackTrace(); }
            finally { s.halt(false); }
        });
    }
    private Player player(ServerLevel level) {
        Player p = new Player(level, new GameProfile(UUID.randomUUID(), "BenderTest")) {
            @Override public GameType gameMode() { return GameType.SURVIVAL; }
        };
        p.snapTo(.5, y, .5); p.setHealth(20); p.setYRot(0); p.setXRot(0); return p;
    }
    private void run(ServerLevel level) {
        try { Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl"); }
        catch (ClassNotFoundException error) { throw new AssertionError(error); }
        y = level.getMaxY() - 40;
        for (int x = -5; x <= 5; x++) for (int z = -5; z <= 12; z++) {
            level.setBlockAndUpdate(new BlockPos(x, (int)y - 1, z), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 4; dy++)
                level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, z), Blocks.AIR.defaultBlockState());
        }
        Player p = player(level);
        check(!Progress.all(p) && Progress.selected(p) == null, "Starts without powers");
        ItemStack renamed = new ItemStack(Items.HEART_OF_THE_SEA);
        renamed.set(DataComponents.CUSTOM_NAME, Component.literal("Earth Creeper Heart"));
        check(!Progress.consume(p, renamed), "Renamed vanilla items cannot unlock powers");
        for (Element e : Element.values()) {
            ItemStack heart = Relics.heart(e);
            check(Progress.consume(p, heart) && heart.isEmpty() && Progress.unlocked(p, e), "Each heart unlocks and consumes once");
            ItemStack duplicate = Relics.heart(e);
            check(!Progress.consume(p, duplicate) && duplicate.getCount() == 1, "Duplicates stay available for trading");
        }
        check(Progress.all(p) && count(p, "trident") == 1 && count(p, "focus") == 1, "One focus and one all-elements trident");
        check(Progress.cycle(p) == Element.EARTH, "Cycles all unlocked elements");

        Progress.select(p, Element.WATER); Creeperborn.ABILITIES.move(p, 2); p.setHealth(12);
        check(Creeperborn.ABILITIES.cast(p).equals("Mending Flow!") && p.getHealth() > 12 && p.getHealth() < 16,
            "Water still works at reduced strength without a source");
        Creeperborn.ABILITIES.clear(); Creeperborn.ABILITIES.move(p, 2); p.setHealth(12);
        ItemStack bucket = new ItemStack(Items.WATER_BUCKET);
        bucket.set(DataComponents.CUSTOM_NAME, Component.literal("Travel water")); p.addItem(bucket);
        check(Creeperborn.ABILITIES.cast(p).equals("Mending Flow!") && p.getHealth() == 16, "Bucket enables healing");
        check(Creeperborn.ABILITIES.cast(p).startsWith("Cooling down") && p.getHealth() == 16, "Cooldown prevents repeated healing");
        Progress.select(p, Element.EARTH); Creeperborn.ABILITIES.move(p, 1); Creeperborn.ABILITIES.cast(p);
        check(Creeperborn.ABILITIES.rocks.guarded(p), "Stone Guard creates frontal rocks");
        Zombie front = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND); front.snapTo(.5, y, 3.5);
        Zombie rear = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND); rear.snapTo(.5, y, -3.5);
        check(Creeperborn.ABILITIES.rocks.blocks(p, p.damageSources().mobAttack(front)), "Stone Guard blocks front");
        check(!Creeperborn.ABILITIES.rocks.blocks(p, p.damageSources().mobAttack(rear)), "Stone Guard leaves rear exposed");
        Progress.select(p, Element.FIRE); Creeperborn.ABILITIES.move(p, 2); Creeperborn.ABILITIES.cast(p);
        check(p.hasEffect(MobEffects.FIRE_RESISTANCE), "Heat Shield grants fire protection");
        Progress.select(p, Element.AIR); Creeperborn.ABILITIES.move(p, 1); Creeperborn.ABILITIES.cast(p);
        check(p.getDeltaMovement().y > .6, "Air Step launches player");
        p.setDeltaMovement(0, 0, 0);

        Zombie z = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        z.snapTo(.5, y, 6.5); z.setNoAi(true); level.addFreshEntity(z);
        Progress.select(p, Element.EARTH); Creeperborn.ABILITIES.move(p, 0); Creeperborn.ABILITIES.cast(p);
        for (int i = 0; i < 10; i++) Creeperborn.ABILITIES.rocks.tick();
        check(z.getHealth() < 20, "Earth Bolt hits hostile in line of sight");
        Zombie blocked = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        blocked.snapTo(.5, y, 9.5); blocked.setNoAi(true); level.addFreshEntity(blocked);
        z.discard();
        for (int x = -1; x <= 1; x++) for (int dy = 0; dy <= 2; dy++)
            level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, 3), Blocks.STONE.defaultBlockState());
        Progress.select(p, Element.FIRE); Creeperborn.ABILITIES.move(p, 0); Creeperborn.ABILITIES.cast(p);
        check(blocked.getHealth() == 20, "Solid wall blocks targeted abilities"); blocked.discard();
        for (int x = -1; x <= 1; x++) for (int dy = 0; dy <= 2; dy++)
            level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, 3), Blocks.AIR.defaultBlockState());

        Villager v = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
        v.snapTo(2.5, y, .5); level.addFreshEntity(v); var offers = v.getOffers();
        Player other = player(level);
        check(!Creeperborn.GUARDIANS.assign(other, v, Element.AIR), "Four elements required for villagers");
        check(Creeperborn.GUARDIANS.assign(p, v, Element.WATER), "Trident progression enables guardian assignment");
        for (Element e : Element.values()) other.addTag("cb_unlock_" + e.key());
        check(!Creeperborn.GUARDIANS.assign(other, v, Element.FIRE), "Another player cannot steal a guardian");
        check(v.getOffers() == offers, "Villager offers preserved");
        Zombie invader = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
        invader.snapTo(5.5, y, .5); invader.setNoAi(true); level.addFreshEntity(invader);
        Creeperborn.GUARDIANS.tick();
        check(invader.getHealth() < 20 && v.getHealth() == 20, "Guardian attacks hostile without harming itself");
        check(Creeperborn.GUARDIANS.release(p, v) && Guardians.element(v) == null, "Owner can release guardian");
        invader.discard(); v.discard();

        Creeper c = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        c.snapTo(.5, y, 4.5); Creeperborn.CREEPERS.assign(c, Element.FIRE); level.addFreshEntity(c);
        c.hurtServer(level, p.damageSources().playerAttack(p), 200);
        var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(-5, y - 2, -5, 6, y + 3, 12),
            item -> Relics.kind(item.getItem()).equals("heart:fire"));
        check(drops.size() == 1, "Player kill drops one correct elemental heart");
        Creeperborn.CREEPERS.died(c, p.damageSources().playerAttack(p));
        check(level.getEntitiesOfClass(ItemEntity.class, new AABB(-5, y - 2, -5, 6, y + 3, 12),
            item -> Relics.kind(item.getItem()).equals("heart:fire")).size() == 1, "Duplicate callbacks cannot duplicate hearts");
        boolean rejected = false;
        try { server.getCommands().getDispatcher().execute("creeperborn move 4", server.createCommandSourceStack()); }
        catch (Exception expected) { rejected = true; }
        check(rejected, "Invalid move slot rejected");
        for (Element e : Element.values()) for (int slot = 0; slot < 3; slot++) {
            Player caster = player(level); caster.addTag("cb_unlock_" + e.key()); Progress.select(caster, e);
            caster.addItem(new ItemStack(Items.WATER_BUCKET)); caster.setHealth(12);
            Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            target.snapTo(.5, y, 3.5); target.setNoAi(true); check(level.addFreshEntity(target), "Move target added");
            Creeperborn.ABILITIES.move(caster, slot);
            check(Creeperborn.ABILITIES.cast(caster).equals(e.moves[slot] + "!"), "Every move casts: " + e.moves[slot]);
            if (e == Element.EARTH && slot == 0) for (int i = 0; i < 10; i++) Creeperborn.ABILITIES.rocks.tick();
            if (slot == 0 || (slot == 2 && (e == Element.EARTH || e == Element.AIR)) || (slot == 1 && e == Element.FIRE))
                check(target.getHealth() < 20, "Offensive move damages hostile: " + e.moves[slot]);
            if (slot == 1 && e == Element.WATER)
                check(caster.hasEffect(MobEffects.RESISTANCE), "Defensive move grants resistance");
            if (slot == 1 && e == Element.EARTH) check(Creeperborn.ABILITIES.rocks.guarded(caster), "Frontal rock guard created");
            if (slot == 2 && e == Element.EARTH) check(Creeperborn.ABILITIES.pushRemaining(target) == 10, "Quake requests ten-block push");
            if (slot == 1 && e == Element.WATER) check(Creeperborn.ABILITIES.pushRemaining(target) == 5, "Tidal Guard requests five-block push");
            if (slot == 2 && e == Element.WATER) check(caster.getHealth() == 16, "Mending Flow heals four health");
            if (slot == 2 && e == Element.FIRE) check(caster.hasEffect(MobEffects.FIRE_RESISTANCE), "Heat Shield protects");
            if (slot == 1 && e == Element.AIR) check(caster.getDeltaMovement().y > .6, "Air Step has upward impulse");
            if (slot == 2 && e == Element.AIR) check(target.getDeltaMovement().z < 0, "Vortex pulls target inward");
            if (e == Element.AIR && slot == 1)
                check(Creeperborn.ABILITIES.cast(caster).equals("Air Step!"), "Air Step can repeat with no cooldown");
            else check(Creeperborn.ABILITIES.cast(caster).startsWith("Cooling down"), "Other moves enforce cooldowns");
            target.discard();
        }
        Creeper natural = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND);
        Creeperborn.config().naturalCreepers = true; Creeperborn.config().elementalChance = 1;
        Creeperborn.CREEPERS.track(natural); Element rolled = ElementalCreepers.element(natural);
        check(rolled != null, "Natural conversion uses configured probability");
        natural.removeTag("cb_creeper_" + rolled.key()); Creeperborn.CREEPERS.track(natural);
        check(ElementalCreepers.element(natural) == null, "Already examined creeper cannot reroll");
        Creeper split = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND); split.addTag("ns_creeper");
        Creeperborn.CREEPERS.track(split); check(ElementalCreepers.element(split) == null, "Night Splitters creepers excluded");
        for (Element e : new Element[]{Element.EARTH, Element.WATER}) {
            Creeperborn.ABILITIES.clear(); Player caster = player(level); caster.addTag("cb_unlock_" + e.key());
            caster.addItem(new ItemStack(Items.WATER_BUCKET)); Progress.select(caster, e);
            Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            target.snapTo(.5, y, 2.5); target.setNoAi(true); level.addFreshEntity(target);
            Creeperborn.ABILITIES.move(caster, e == Element.EARTH ? 2 : 1); Creeperborn.ABILITIES.cast(caster);
            double start = target.getZ(), expected = e == Element.EARTH ? 10 : 5;
            for (int tick = 0; tick < 30; tick++) { target.travel(net.minecraft.world.phys.Vec3.ZERO); Creeperborn.ABILITIES.tick(); }
            double travelled = target.getZ() - start;
            check(travelled >= expected - 1 && travelled <= expected + 1,
                e.title + " actual push distance: " + travelled + ", expected about " + expected);
            target.discard();
        }
        Creeperborn.ABILITIES.clear(); Player tidal = player(level); tidal.addTag("cb_unlock_water");
        tidal.addItem(new ItemStack(Items.WATER_BUCKET)); Progress.select(tidal, Element.WATER);
        // Keep all four directions inside the same fully loaded entity chunk.
        for (int x = 5; x <= 12; x++) for (int z = 5; z <= 12; z++) {
            level.setBlockAndUpdate(new BlockPos(x, (int)y - 1, z), Blocks.STONE.defaultBlockState());
            for (int dy = 0; dy <= 4; dy++) level.setBlockAndUpdate(new BlockPos(x, (int)y + dy, z), Blocks.AIR.defaultBlockState());
        }
        tidal.snapTo(8.5, y, 8.5);
        java.util.List<Zombie> surrounding = new java.util.ArrayList<>();
        for (int[] offset : new int[][]{{2,0},{-2,0},{0,2},{0,-2}}) {
            Zombie target = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.COMMAND);
            target.snapTo(8.5 + offset[0], y, 8.5 + offset[1]); target.setNoAi(true);
            check(level.addFreshEntity(target), "Tidal target added"); surrounding.add(target);
        }
        Creeperborn.ABILITIES.move(tidal, 1); Creeperborn.ABILITIES.cast(tidal);
        for (Zombie target : surrounding) {
            check(Creeperborn.ABILITIES.pushRemaining(target) == 5, "Tidal Guard covers front, rear and both sides: pos="
                + target.position() + ", remaining=" + Creeperborn.ABILITIES.pushRemaining(target) + ", visible=" + tidal.hasLineOfSight(target)); target.discard();
        }
        tidal.addTag("cb_unlock_earth"); Progress.select(tidal, Element.EARTH); Creeperborn.ABILITIES.move(tidal, 1);
        Creeperborn.ABILITIES.cast(tidal);
        var rockVisuals = level.getEntitiesOfClass(net.minecraft.world.entity.Display.BlockDisplay.class,
            new AABB(5, y - 2, 5, 12, y + 5, 12));
        check(rockVisuals.size() == 3 && rockVisuals.stream().allMatch(display -> !display.shouldBeSaved()),
            "Temporary rock visuals are excluded from world saves");
    }
    private static int count(Player p, String kind) {
        int count = 0;
        for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
            ItemStack item = p.getInventory().getItem(i); if (Relics.kind(item).equals(kind)) count += item.getCount();
        }
        return count;
    }
    private static void check(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
