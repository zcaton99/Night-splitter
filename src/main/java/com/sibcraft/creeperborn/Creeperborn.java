package com.sibcraft.creeperborn;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
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
        ServerLifecycleEvents.SERVER_STARTING.register(Hearts::load);
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler,sender,server) -> { DoorSpace.recover(handler.player); Rubber.recoverFlight(handler.player); Phoenix.recoverFlight(handler.player); Lightning.recoverFlight(handler.player); FruitClaims.reconcile(handler.player);Hearts.join(handler.player);Relics.refreshFruitModels(handler.player); });
        Path path = FabricLoader.getInstance().getConfigDir().resolve("creeperborn.json");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        try {
            if (Files.exists(path)) config = gson.fromJson(Files.readString(path), Config.class);
            if (config == null || !Double.isFinite(config.elementalChance) || config.elementalChance < 0 || config.elementalChance > 1 || !Double.isFinite(config.fruitChance) || config.fruitChance < 0 || config.fruitChance > 1)
                throw new IllegalArgumentException("elementalChance must be 0–1");
            Files.createDirectories(path.getParent()); Files.writeString(path, gson.toJson(config) + "\n");
        } catch (Exception error) { throw new IllegalStateException("Invalid Creeperborn config", error); }
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> { if(entity instanceof net.minecraft.world.entity.item.ItemEntity item)Relics.updateFruitModel(item.getItem()); FruitCreepers.track(entity); CREEPERS.track(entity); GUARDIANS.track(entity); });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> { FruitCreepers.unload(entity); CREEPERS.unload(entity); GUARDIANS.unload(entity); });
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> {DoorSpace.clear();Phoenix.clear();Lightning.clear();Dark.clear();Gravity.clear();StringFruit.clear();Paw.clear();Tremor.clear();PairPowers.clear();ClimatePowers.clear();Azure.clear();Op.clear();});
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> { DoorSpace.clear(); DoorFruit.clear(); Phoenix.clear(); Lightning.clear(); Dark.clear();Gravity.clear();StringFruit.clear();Paw.clear();Tremor.clear();PairPowers.clear();ClimatePowers.clear();Azure.clear();Op.clear(); Hearts.clear();FruitClaims.clear(); WaterRules.clear(); Rubber.clear(); FruitCreepers.clear(); ABILITIES.clear(); Mastery.clear(); CombatNetwork.clear(); GUARDIANS.clear(); CREEPERS.clear(); });
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            DoorSpace.tick(server); CombatNetwork.tick(server); WaterRules.tick(server); ABILITIES.tick(); Rubber.tick(server); Phoenix.tick(server); Lightning.tick(server); Dark.tick(server);Gravity.tick(server);StringFruit.tick(server);Paw.tick(server);Tremor.tick(server);PairPowers.tick(server);ClimatePowers.tick(server);Azure.tick(server);Op.tick(server);Hearts.tick(server);
            if (server.getTickCount() % 20 == 0) {Mastery.tick(server);DoorFruit.tick(server);for(var p:server.getPlayerList().getPlayers())Relics.refreshFruitModels(p);}
            if (server.getTickCount() % 10 == 0) { CREEPERS.tick(); GUARDIANS.tick(); }
        });
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity,source,baseDamage,damageTaken,blocked)->{if(entity instanceof Player p&&damageTaken>0)StringFruit.damaged(p);});
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> !ABILITIES.rocks.blocks(entity, source));
        ServerLivingEntityEvents.AFTER_DEATH.register(Dark::died);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity,source)->Hearts.died(entity));
        ServerLivingEntityEvents.AFTER_DEATH.register(CREEPERS::died);
        ServerLivingEntityEvents.AFTER_DEATH.register(FruitCreepers::died);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> { if(entity instanceof Player p) FruitClaims.releaseOnDeath(p); });
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> { StringFruit.forget(handler.player);Paw.forget(handler.player);Tremor.forget(handler.player);PairPowers.forget(handler.player);ClimatePowers.forget(handler.player);Azure.forget(handler.player);Op.forget(handler.player);Hearts.forget(handler.player);Gravity.forget(handler.player);Dark.forget(handler.player); Lightning.forget(handler.player); Phoenix.forget(handler.player); DoorSpace.forget(handler.player); DoorFruit.forget(handler.player); Rubber.forget(handler.player); WaterRules.forget(handler.player); CombatNetwork.forget(handler.player); });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            for (String tag : oldPlayer.entityTags()) if (tag.startsWith("cb_")) newPlayer.addTag(tag);
            WaterRules.forget(newPlayer); CombatNetwork.forget(newPlayer);
        });
        net.fabricmc.fabric.api.event.player.UseBlockCallback.EVENT.register((player,level,hand,hit)->{
            ItemStack cube=player.getItemInHand(hand);
            if(!Relics.kind(cube).equals("op:heart"))return InteractionResult.PASS;
            if(player.isSpectator())return InteractionResult.FAIL;
            if(level instanceof ServerLevel)say(player,Hearts.use(player,cube));
            return InteractionResult.SUCCESS;
        });
        UseItemCallback.EVENT.register((player, level, hand) -> {
            ItemStack stack = player.getItemInHand(hand); String kind = Relics.kind(stack);
            if (kind.isEmpty()) return InteractionResult.PASS;
            if (player.isSpectator()) return InteractionResult.FAIL;
            if (!(level instanceof ServerLevel)) return InteractionResult.SUCCESS;
            if(StringFruit.locked(player))return InteractionResult.FAIL;
            if(kind.equals("op:heart")){say(player,Hearts.use(player,stack));
            }else if(kind.equals("fruit:azure")){say(player,Azure.consume(player,stack)?Azure.status(player):"Fruit slot full, or Azure already claimed.");
            }else if(kind.equals("fruit:ice")||kind.equals("fruit:magma")){String f=kind.substring(6);say(player,ClimatePowers.consume(player,stack,f)?ClimatePowers.status(player,f):"Fruit slot full, or fruit already claimed.");
            }else if(kind.equals("fruit:love")||kind.equals("fruit:sand")){String f=kind.substring(6);say(player,PairPowers.consume(player,stack,f)?PairPowers.status(player,f):"Fruit slot full, or fruit already claimed.");
            }else if(kind.equals("fruit:tremor")){say(player,Tremor.consume(player,stack)?Tremor.status(player):"Fruit slot full, or Tremor already claimed.");
            }else if(kind.equals("fruit:paw")){say(player,Paw.consume(player,stack)?"Paw-Paw unlocked! G selects fruit; Z Cannon/Shock, X Step/Launch, C Guard/Pain Bubble.":"Fruit slot full, or Paw is already claimed.");
            }else if(kind.equals("fruit:string")){say(player,StringFruit.consume(player,stack)?"String unlocked! G selects fruit; Z Slash/Whip, X Glide/Parasite, C Knight/Birdcage.":"Fruit slot full, or String is already claimed.");
            }else if(kind.equals("fruit:gravity")){say(player,Gravity.consume(player,stack)?"Gravity unlocked! G selects fruit; Z Press/Wave, X Levitate/Rock Ride, C Guard/Meteor.":"Fruit slot full, or Gravity is already claimed.");
            }else if(kind.equals("fruit:op")){say(player,Op.consume(player,stack)?"Op-Op unlocked! G selects fruit; Z Gamma Knife/ROOM; X Shock/Heart; C Shambles/Takt.":"Fruit slot full, or Op-Op is already claimed.");
            }else if(kind.equals("fruit:dark")){
                say(player,Dark.consume(player,stack)?"Dark unlocked! G selects fruit; Z Vortex/Grasp; X Mantle/Liberation; C Surge/Void Collapse.":"Dark may already be claimed, or both fruit slots are full.");
            }else if(kind.equals("fruit:lightning")){
                say(player,Lightning.consume(player,stack)?"Lightning unlocked! G selects fruit; tap Z bolt, hold Z eight seconds Judgment; X electric movement, C Storm Form.":"Fruit slot full, or Lightning is already claimed.");
            }else if(kind.equals("fruit:phoenix")){
                say(player,Phoenix.consume(player,stack)?"Phoenix unlocked! G selects fruit; Z attacks, X wings, hold X form, tap C healing circle; hold C ally blessing.":"Fruit slot full, or Phoenix is already claimed.");
            }else if(kind.equals("fruit:door")){
                say(player,DoorFruit.consume(player,stack)?"Door-Door unlocked! G selects fruit, Z opens a passage.":"Fruit slot full, or this fruit is already claimed.");
            } else if (kind.equals("fruit:rubber")) {
                say(player, Rubber.consume(player,stack) ? "Rubber unlocked! B enables combat; G selects Rubber; Z punches." : "Fruit slot full, or Rubber is already claimed.");
            } else if (kind.startsWith("heart:")) {
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
            if(Relics.kind(player.getItemInHand(hand)).equals("op:heart")&&player.isShiftKeyDown()&&entity instanceof net.minecraft.world.entity.LivingEntity target){
                if(level instanceof ServerLevel)say(player,Hearts.giveBack(player,target,player.getItemInHand(hand))?"Heart returned.":"This heart belongs to someone else.");
                return InteractionResult.SUCCESS;
            }
            if (!Relics.kind(player.getItemInHand(hand)).equals("trident")) return InteractionResult.PASS;
            if (!(entity instanceof Villager) && !(entity instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager)) return InteractionResult.PASS;
            if (player.isSpectator()) return InteractionResult.FAIL;
            if (level instanceof ServerLevel) {
                if(entity instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager zombie){
                    Element original=Guardians.element(zombie);
                    boolean cured=GUARDIANS.cure(player,zombie);
                    say(player,cured?(original==null?"Zombie villager cured!":original.title+" guardian restored to its original faction.")
                        :"Curing with the Elemental Trident requires all four unlocked elements.");
                }else if(entity instanceof Villager v){
                    Element e = Progress.selected(player);
                    say(player, e != null && GUARDIANS.assign(player, v, e)
                        ? e.title + " guardian joined your faction. Trades and home/work routines are preserved."
                        : "Requires all four elements, an adult villager, and ownership (or an unclaimed villager).");
                }
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
                String active=Azure.selected(p)?"Azure Dragon":ClimatePowers.selected(p,"ice")?"Ice-Ice":ClimatePowers.selected(p,"magma")?"Magma-Magma":PairPowers.selected(p,"love")?"Love-Love":PairPowers.selected(p,"sand")?"Sand-Sand":Tremor.selected(p)?"Tremor":Paw.selected(p)?"Paw-Paw":StringFruit.selected(p)?"String":Gravity.selected(p)?"Gravity":Op.selected(p)?"Op-Op":Dark.selected(p)?"Dark":Lightning.selected(p)?"Lightning":Phoenix.selected(p)?"Phoenix":DoorFruit.selected(p)?"Door-Door":Rubber.selected(p)?"Rubber":selected==null?"none":selected.title;
                chat(p,"Creeperborn 0.1.0-alpha.47 | Active: "+active+" | Mode: "+p.gameMode());
                chat(p,"Elements unlocked: "+known);
                if(selected!=null)chat(p,selected.title+" mastery "+Mastery.tier(p,selected)+"/3 | Uses: "+Mastery.uses(p,selected)
                    +" | Active time: "+Mastery.seconds(p,selected)+"s | Power: "+Math.round(EnvironmentPower.strength(p,selected)*100)+"%");
                chat(p,Azure.status(p));chat(p,Rubber.status(p));chat(p,DoorFruit.status(p));chat(p,Phoenix.status(p));chat(p,Lightning.status(p));chat(p,Dark.status(p));chat(p,Op.status(p));chat(p,Gravity.status(p));chat(p,StringFruit.status(p));chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));
                chat(p,"Dark claim: "+(FruitClaims.darkAvailable()?"available":FruitClaims.ownsDark(p)?"yours":"another living owner or reserved capture"));
                chat(p,"Lightning claim: "+(FruitClaims.lightningAvailable()?"available":FruitClaims.ownsLightning(p)?"yours":"another living player"));
                chat(p,"Phoenix claim: "+(FruitClaims.phoenixAvailable()?"available":FruitClaims.ownsPhoenix(p)?"yours":"another living player"));
                chat(p,"Door claim: "+(FruitClaims.doorAvailable()?"available":FruitClaims.ownsDoor(p)?"yours":"another living player"));
                chat(p,"Rubber claim: "+(FruitClaims.available()?"available":FruitClaims.owns(p)?"yours":"another living player"));
                chat(p,"All-four mastery: "+(Mastery.harmony(p)?"ON — +15%, water breathing, lava/fire/fall/freezing immunity":"not yet mastered"));
                chat(p,"Water: "+(WaterRules.mastered(p)?"mastery protection":WaterRules.suppressed(p)?"SINKING — powers disabled":WaterRules.fruitOwner(p)?"fruit weakness applies when submerged":"normal")
                    +" | Eyes submerged: "+WaterRules.submerged(p)+" | Air: "+p.getAirSupply()+"/"+p.getMaxAirSupply());
                if(Progress.unlocked(p,Element.AIR)){
                    long now=p.level().getGameTime();int steps=ABILITIES.airSteps.remaining(p,now);
                    chat(p,steps<0?"Air Step: unlimited (all-four mastery)":"Air Step: "+steps+"/5 | Full refill 3s after last step");
                }
                chat(p,"Controls: R elements, Shift+R backward, G fruit, Z/X/C moves, B combat.");
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
            root.then(Commands.literal("door").then(Commands.literal("exit").executes(ctx->{say(ctx.getSource().getPlayerOrException(),DoorSpace.exit(ctx.getSource().getPlayerOrException()));return 1;})));
            root.then(Commands.literal("doormastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{
                    ServerPlayer p=ctx.getSource().getPlayerOrException();if(!DoorFruit.unlocked(p))return 0;
                    DoorFruit.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));say(p,DoorFruit.status(p));return 1;
                })));
            root.then(Commands.literal("testfruit").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .executes(ctx -> { chat(ctx.getSource().getPlayerOrException(),"Usage: /creeperborn testfruit rubber|door|phoenix|lightning|dark|op|gravity|string|paw|tremor|love|sand|ice|magma|azure"); return 0; })
                .then(Commands.literal("azure").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.azureFruit());return 1;}))
                .then(Commands.literal("ice").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.iceFruit());return 1;}))
                .then(Commands.literal("magma").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.magmaFruit());return 1;}))
                .then(Commands.literal("love").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.loveFruit());return 1;}))
                .then(Commands.literal("sand").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.sandFruit());return 1;}))
                .then(Commands.literal("tremor").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.tremorFruit());return 1;}))
                .then(Commands.literal("paw").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.pawFruit());return 1;}))
                .then(Commands.literal("string").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.stringFruit());return 1;}))
                .then(Commands.literal("gravity").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.gravityFruit());return 1;}))
                .then(Commands.literal("op").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.opFruit());return 1;}))
                .then(Commands.literal("rubber").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.fruit());return 1;}))
                .then(Commands.literal("door").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.doorFruit());return 1;}))
                .then(Commands.literal("phoenix").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.phoenixFruit());return 1;}))
                .then(Commands.literal("lightning").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.lightningFruit());return 1;}))
                .then(Commands.literal("dark").executes(ctx->{Progress.give(ctx.getSource().getPlayerOrException(),Relics.darkFruit());return 1;})));
            root.then(Commands.literal("lightningmastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{
                    var p=ctx.getSource().getPlayerOrException();if(!Lightning.unlocked(p))return 0;
                    Lightning.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Lightning.status(p));return 1;
                })));
            root.then(Commands.literal("phoenixmastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{
                    var p=ctx.getSource().getPlayerOrException();if(!Phoenix.unlocked(p))return 0;
                    Phoenix.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));say(p,Phoenix.status(p));return 1;
                })));
            root.then(Commands.literal("fruitmastery").requires(source -> source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx -> {
                    ServerPlayer p=ctx.getSource().getPlayerOrException();int tier=IntegerArgumentType.getInteger(ctx,"tier");
                    if(Dark.unlocked(p)){Dark.grantFruit(p,Dark.choice(p),tier);chat(p,Dark.status(p));}
                    else if(Azure.unlocked(p)){Azure.grant(p,tier);chat(p,Azure.status(p));}
                    else if(ClimatePowers.unlocked(p,"ice")){ClimatePowers.grant(p,"ice",tier);chat(p,ClimatePowers.status(p,"ice"));}
                    else if(ClimatePowers.unlocked(p,"magma")){ClimatePowers.grant(p,"magma",tier);chat(p,ClimatePowers.status(p,"magma"));}
                    else if(PairPowers.unlocked(p,"love")){PairPowers.grant(p,"love",tier);chat(p,PairPowers.status(p,"love"));}
                    else if(PairPowers.unlocked(p,"sand")){PairPowers.grant(p,"sand",tier);chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));}
                    else if(Tremor.unlocked(p)){Tremor.grant(p,tier);chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));}
                    else if(Paw.unlocked(p)){Paw.grant(p,tier);chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));}
                    else if(StringFruit.unlocked(p)){StringFruit.grant(p,tier);chat(p,StringFruit.status(p));chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));}
                    else if(Gravity.unlocked(p)){Gravity.grant(p,tier);chat(p,Gravity.status(p));chat(p,StringFruit.status(p));chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));}
                    else if(Op.unlocked(p)){Op.grant(p,tier);chat(p,Op.status(p));}
                    else if(Lightning.unlocked(p)){Lightning.grant(p,tier);chat(p,Lightning.status(p));}
                    else if(Phoenix.unlocked(p)){Phoenix.grant(p,tier);chat(p,Phoenix.status(p));}
                    else if(DoorFruit.unlocked(p)){DoorFruit.grant(p,tier);chat(p,DoorFruit.status(p));}
                    else if(Rubber.unlocked(p)){Rubber.grant(p,tier);chat(p,Rubber.status(p));}
                    else {chat(p,"Eat a Devil Fruit first; fruitmastery changes the fruit you own.");return 0;}
                    return 1;
                })));
            root.then(Commands.literal("fruit").executes(ctx -> {
                ServerPlayer p=ctx.getSource().getPlayerOrException();if(!Azure.unlocked(p)&&!Rubber.unlocked(p)&&!DoorFruit.unlocked(p)&&!Phoenix.unlocked(p)&&!Lightning.unlocked(p)&&!Dark.unlocked(p)&&!Op.unlocked(p)&&!Gravity.unlocked(p)&&!StringFruit.unlocked(p)&&!Paw.unlocked(p)&&!Tremor.unlocked(p)&&!PairPowers.unlocked(p,"love")&&!PairPowers.unlocked(p,"sand")&&!ClimatePowers.unlocked(p,"ice")&&!ClimatePowers.unlocked(p,"magma"))return 0;
                p.addTag("cb_mode_fruit");say(p,Dark.unlocked(p)?Dark.status(p):Azure.unlocked(p)?Azure.status(p):ClimatePowers.unlocked(p,"ice")?ClimatePowers.status(p,"ice"):ClimatePowers.unlocked(p,"magma")?ClimatePowers.status(p,"magma"):PairPowers.unlocked(p,"love")?PairPowers.status(p,"love"):PairPowers.unlocked(p,"sand")?PairPowers.status(p,"sand"):Tremor.unlocked(p)?Tremor.status(p):Paw.unlocked(p)?Paw.status(p):StringFruit.unlocked(p)?StringFruit.status(p):Gravity.unlocked(p)?Gravity.status(p):Op.unlocked(p)?Op.status(p):Lightning.unlocked(p)?Lightning.status(p):Phoenix.unlocked(p)?Phoenix.status(p):DoorFruit.unlocked(p)?DoorFruit.status(p):Rubber.status(p));return 1;
            }));
            test.then(Commands.literal("rubber").executes(ctx -> {
                ServerPlayer p=ctx.getSource().getPlayerOrException();ServerLevel level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assign(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            test.then(Commands.literal("door").executes(ctx->{
                ServerPlayer p=ctx.getSource().getPlayerOrException();ServerLevel level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignDoor(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            test.then(Commands.literal("phoenix").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignPhoenix(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            test.then(Commands.literal("lightning").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignLightning(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            test.then(Commands.literal("dark").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignDark(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            var dark=Commands.literal("dark").executes(ctx->{for(var capture:Dark.captures(ctx.getSource().getPlayerOrException()))Dark.show(ctx.getSource().getPlayerOrException(),capture);return 1;});
            dark.then(Commands.literal("captures").executes(ctx->{for(var capture:Dark.captures(ctx.getSource().getPlayerOrException()))Dark.show(ctx.getSource().getPlayerOrException(),capture);return 1;}));
            for(String action:new String[]{"eat","materialize"})dark.then(Commands.literal(action)
                .executes(ctx->{chat(ctx.getSource().getPlayerOrException(),Dark.decide(ctx.getSource().getPlayerOrException(),"",action.equals("eat")));return 1;})
                .then(Commands.argument("token",StringArgumentType.word()).executes(ctx->{chat(ctx.getSource().getPlayerOrException(),Dark.decide(ctx.getSource().getPlayerOrException(),StringArgumentType.getString(ctx,"token"),action.equals("eat")));return 1;})));
            root.then(dark);
            root.then(Commands.literal("darkmastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!Dark.unlocked(p))return 0;Dark.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Dark.status(p));return 1;})));
            test.then(Commands.literal("op").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignOp(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            root.then(Commands.literal("opmastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!Op.unlocked(p))return 0;Op.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Op.status(p));return 1;})));
            test.then(Commands.literal("gravity").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignGravity(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            root.then(Commands.literal("gravitymastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!Gravity.unlocked(p))return 0;Gravity.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Gravity.status(p));chat(p,StringFruit.status(p));chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));return 1;})));
            test.then(Commands.literal("string").executes(ctx->{
                var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();
                Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;
                c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignString(c);
                if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;
            }));
            root.then(Commands.literal("stringmastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER))
                .then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!StringFruit.unlocked(p))return 0;StringFruit.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,StringFruit.status(p));chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));return 1;})));
            root.then(Commands.literal("azuremastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!Azure.unlocked(p))return 0;Azure.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Azure.status(p));return 1;})));
            test.then(Commands.literal("azure").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var l=(ServerLevel)p.level();var c=EntityTypes.CREEPER.create(l,EntitySpawnReason.COMMAND);if(c==null)return 0;c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignAzure(c);if(!l.noCollision(c,c.getBoundingBox())||!l.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;}));
            for(String f:java.util.List.of("ice","magma")){
                test.then(Commands.literal(f).executes(ctx->{var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;c.snapTo(p.getX()+5,p.getY(),p.getZ());if(f.equals("ice"))FruitCreepers.assignIce(c);else FruitCreepers.assignMagma(c);if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;}));
                root.then(Commands.literal(f+"mastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!ClimatePowers.unlocked(p,f))return 0;ClimatePowers.grant(p,f,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,ClimatePowers.status(p,f));return 1;})));
            }
            for(String f:java.util.List.of("love","sand")){
                test.then(Commands.literal(f).executes(ctx->{var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;c.snapTo(p.getX()+5,p.getY(),p.getZ());if(f.equals("love"))FruitCreepers.assignLove(c);else FruitCreepers.assignSand(c);if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;}));
                root.then(Commands.literal(f+"mastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!PairPowers.unlocked(p,f))return 0;PairPowers.grant(p,f,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,PairPowers.status(p,f));return 1;})));
            }
            test.then(Commands.literal("tremor").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignTremor(c);if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;}));
            root.then(Commands.literal("tremormastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!Tremor.unlocked(p))return 0;Tremor.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));return 1;})));
            test.then(Commands.literal("paw").executes(ctx->{var p=ctx.getSource().getPlayerOrException();var level=(ServerLevel)p.level();Creeper c=EntityTypes.CREEPER.create(level,EntitySpawnReason.COMMAND);if(c==null)return 0;c.snapTo(p.getX()+5,p.getY(),p.getZ());FruitCreepers.assignPaw(c);if(!level.noCollision(c,c.getBoundingBox())||!level.addFreshEntity(c)){FruitCreepers.unload(c);return 0;}return 1;}));
            root.then(Commands.literal("pawmastery").requires(source->source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)).then(Commands.argument("tier",IntegerArgumentType.integer(1,3)).executes(ctx->{var p=ctx.getSource().getPlayerOrException();if(!Paw.unlocked(p))return 0;Paw.grant(p,IntegerArgumentType.getInteger(ctx,"tier"));chat(p,Paw.status(p));chat(p,Tremor.status(p));chat(p,PairPowers.status(p,"love"));chat(p,PairPowers.status(p,"sand"));chat(p,ClimatePowers.status(p,"ice"));chat(p,ClimatePowers.status(p,"magma"));return 1;})));
            root.then(test); root.then(hearts); dispatcher.register(root);
        });
    }
    private static Villager nearestOwned(Player p) {
        return p.level().getEntitiesOfClass(Villager.class, p.getBoundingBox().inflate(6)).stream()
            .filter(v -> v.isAlive() && Guardians.owns(p, v) && p.hasLineOfSight(v))
            .min(Comparator.comparingDouble(p::distanceToSqr)).orElse(null);
    }
    private static void chat(Player p,String message){p.sendSystemMessage(Component.literal(message));}
    public static void say(Player p, String message) { p.sendOverlayMessage(Component.literal(message)); }
}




