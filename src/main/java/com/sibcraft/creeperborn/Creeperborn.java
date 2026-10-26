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
    public static final class Config { public boolean naturalCreepers = true; public double elementalChance = .02; }
    private static Config config = new Config();
    public static Config config() { return config; }
    @Override public void onInitialize() {
        CombatNetwork.init();
        Path path = FabricLoader.getInstance().getConfigDir().resolve("creeperborn.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(path)) config = gson.fromJson(Files.readString(path), Config.class);
            if (config == null || !Double.isFinite(config.elementalChance) || config.elementalChance < 0 || config.elementalChance > 1)
                throw new IllegalArgumentException("elementalChance must be 0–1");
            Files.createDirectories(path.getParent()); Files.writeString(path, gson.toJson(config) + "\n");
        } catch (Exception error) { throw new IllegalStateException("Invalid Creeperborn config", error); }
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { CREEPERS.track(entity); GUARDIANS.track(entity); });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> { CREEPERS.unload(entity); GUARDIANS.unload(entity); });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { ABILITIES.clear(); Mastery.clear(); CombatNetwork.clear(); GUARDIANS.clear(); CREEPERS.clear(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ABILITIES.tick();
            if (server.getTickCount() % 20 == 0) Mastery.tick(server);
            if (server.getTickCount() % 10 == 0) { CREEPERS.tick(); GUARDIANS.tick(); }
        });
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !ABILITIES.rocks.blocks(entity, source));
        ServerLivingEntityEvents.AFTER_DEATH.register(CREEPERS::died);
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            for (String tag : oldPlayer.entityTags()) if (tag.startsWith("cb_")) newPlayer.addTag(tag);
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand); String kind = Relics.kind(stack);
            if (kind.isEmpty()) return InteractionResult.PASS;
            if (player.isSpectator()) return InteractionResult.FAIL;
            if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
            if (kind.startsWith("heart:")) {
                say(player, Progress.consume(player, stack) ? "Element unlocked! Use your focus; /creeperborn move 1–3 selects a move."
                    : "You already know this element. Keep the heart for a friend.");
            } else if (kind.equals("focus") || kind.equals("trident")) {
                if (player.isShiftKeyDown()) {
                    Element e = Progress.cycle(player); say(player, e == null ? "No elements unlocked." : "Selected " + e.title);
                } else if (kind.equals("focus")) say(player, ABILITIES.cast(player));
                else say(player, "Use on an adult villager to grant the selected element. Sneak-use to change element.");
            }
            return InteractionResult.SUCCESS;
        });
        UseEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
            if (!Relics.kind(player.getItemInHand(hand)).equals("trident") || !(entity instanceof Villager v)) return InteractionResult.PASS;
            if (player.isSpectator()) return InteractionResult.FAIL;
            if (level instanceof ServerLevel) {
                Element e = Progress.selected(player);
                say(player, e != null && GUARDIANS.assign(player, v, e)
                    ? e.title + " guardian joined your faction. Trades and home/work routines are preserved."
                    : "Requires all four elements, an adult villager, and ownership (or an unclaimed villager).");
            }
            return InteractionResult.SUCCESS;
        });
        CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> {
            var root = Commands.literal("creeperborn").executes(ctx -> {
                ctx.getSource().sendSuccess(() -> Component.literal("Creeperborn: /creeperborn element earth|water|fire|air; /creeperborn move 1|2|3; /creeperborn cast; /creeperborn status. Use your focus to cast; sneak-use to cycle elements."), false);
                return 1;
            });
            root.then(Commands.literal("status").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                String known = Arrays.stream(Element.values()).filter(e -> Progress.unlocked(p, e)).map(e -> e.title).reduce((a,b) -> a + ", " + b).orElse("none");
                Element selected = Progress.selected(p);
                say(p, "Creeperborn 0.1.0-alpha.1 | unlocked: " + known + " | selected: " + (selected == null ? "none" : selected.title)
                    + " | move: " + (ABILITIES.move(p) + 1)
                    + " | power: " + (selected == null ? 0 : Math.round(EnvironmentPower.strength(p, selected) * 100)) + "%"
                    + " | mastery: " + (selected == null ? "none" : Mastery.tier(p, selected) + "/3, " + Mastery.uses(p, selected) + " uses, " + Mastery.seconds(p, selected) + " active seconds")
                    + " | natural creeper chance: " + config.elementalChance);
                return 1;
            }));
            root.then(Commands.literal("cast").executes(ctx -> { ServerPlayer p = ctx.getSource().getPlayerOrException(); say(p, ABILITIES.cast(p)); return 1; }));
            root.then(Commands.literal("move").then(Commands.argument("slot", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException(); int index = IntegerArgumentType.getInteger(ctx, "slot") - 1;
                ABILITIES.move(p, index); Element e = Progress.selected(p); say(p, "Move " + (index + 1) + (e == null ? "" : ": " + e.moves[index])); return 1;
            })));
            var element = Commands.literal("element");
            for (Element e : Element.values()) element.then(Commands.literal(e.key()).executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException(); say(p, Progress.select(p, e) ? "Selected " + e.title : "Consume this element's heart first."); return 1;
            }));
            root.then(element);
            var use = Commands.literal("use");
            var mastery = Commands.literal("mastery").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
            for (Element e : Element.values()) {
                use.then(Commands.literal(e.key()).then(Commands.argument("slot", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!Progress.select(p,e)) { say(p, "Consume the " + e.title + " heart first."); return 0; }
                    ABILITIES.move(p, IntegerArgumentType.getInteger(ctx, "slot") - 1); say(p, ABILITIES.cast(p)); return 1;
                })));
                mastery.then(Commands.literal(e.key()).then(Commands.argument("tier", IntegerArgumentType.integer(1, 3)).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException();
                    if (!Progress.unlocked(p,e)) { say(p, "Consume this heart first."); return 0; }
                    Mastery.grant(p, e, IntegerArgumentType.getInteger(ctx, "tier")); say(p, e.title + " testing mastery set to " + Mastery.tier(p,e)); return 1;
                })));
            }
            root.then(use); root.then(mastery);
            root.then(Commands.literal("focus").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException();
                if (Progress.selected(p) == null) { say(p, "Unlock an element first."); return 0; }
                for (int i = 0; i < p.getInventory().getContainerSize(); i++)
                    if (Relics.kind(p.getInventory().getItem(i)).equals("focus")) { say(p, "You already carry a focus."); return 0; }
                for (int i = 0; i < p.getInventory().getContainerSize(); i++) {
                    ItemStack item = p.getInventory().getItem(i);
                    if (item.is(net.minecraft.world.item.Items.STICK) && Relics.kind(item).isEmpty()) {
                        item.shrink(1); Progress.give(p, Relics.focus()); return 1;
                    }
                }
                say(p, "Carry one plain stick to replace your focus."); return 0;
            }));
            root.then(Commands.literal("guardpost").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException(); Villager v = nearestOwned(p);
                if (v == null) { say(p, "Stand within six blocks of one of your guardians."); return 0; }
                GUARDIANS.post(v, p.blockPosition()); say(p, "Nearest guardian's post set to your position."); return 1;
            }));
            root.then(Commands.literal("release").executes(ctx -> {
                ServerPlayer p = ctx.getSource().getPlayerOrException(); Villager v = nearestOwned(p);
                if (v == null) return 0; GUARDIANS.release(p, v); say(p, "Guardian released; vanilla villager routines continue."); return 1;
            }));
            var test = Commands.literal("testcreeper").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
            var hearts = Commands.literal("testheart").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER));
            for (Element e : Element.values()) {
                test.then(Commands.literal(e.key()).executes(ctx -> {
                    ServerPlayer p = ctx.getSource().getPlayerOrException(); ServerLevel level = (ServerLevel)p.level();
                    Creeper c = EntityTypes.CREEPER.create(level, EntitySpawnReason.COMMAND); if (c == null) return 0;
                    c.snapTo(p.getX() + 5, p.getY(), p.getZ()); CREEPERS.assign(c, e);
                    if (!level.noCollision(c, c.getBoundingBox()) || !level.addFreshEntity(c)) { CREEPERS.unload(c); say(p, "No clear spawn space five blocks east."); return 0; }
                    say(p, e.title + " test creeper spawned five blocks east. Kill it before it explodes to collect its heart."); return 1;
                }));
                hearts.then(Commands.literal(e.key()).executes(ctx -> { Progress.give(ctx.getSource().getPlayerOrException(), Relics.heart(e)); return 1; }));
            }
            root.then(test); root.then(hearts); dispatcher.register(root);
        });
    }
    private static Villager nearestOwned(Player p) {
        return p.level().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(6)).stream()
            .filter(v -> v.isAlive() && Guardians.owns(p, v) && p.hasLineOfSight(v))
            .min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    public static void say(Player p, String message) { p.sendOverlayMessage(Component.literal(message)); }
}
