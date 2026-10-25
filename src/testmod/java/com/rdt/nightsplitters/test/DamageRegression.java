package com.rdt.nightsplitters.test;

import com.mojang.authlib.GameProfile;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.GameType;

public final class DamageRegression implements ModInitializer {
    @Override public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            ServerLevel level = server.overworld();
            server.setDifficulty(Difficulty.NORMAL, true);
            CaveSpider swarm = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
            CaveSpider ordinary = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
            swarm.addTag("ns_swarm");
            swarm.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
            Player creative = player(level, GameType.CREATIVE);
            check(swarm.doHurtTarget(level, creative), "Tagged swarm hit must succeed in Creative");
            health(creative, 18.0F, "Creative takes half melee damage");
            check(creative.getAbilities().invulnerable && creative.isCreative(),
                "Creative abilities and game mode remain intact");

            Player immune = player(level, GameType.CREATIVE);
            check(!immune.hurtServer(level, level.damageSources().mobAttack(ordinary), 4.0F),
                "Ordinary spiders cannot damage Creative");
            health(immune, 20.0F, "Ordinary spider immunity");
            check(!immune.hurtServer(level, level.damageSources().fall(), 4.0F),
                "Fall damage remains blocked in Creative");
            health(immune, 20.0F, "Environmental immunity");

            Player survival = player(level, GameType.SURVIVAL);
            check(survival.hurtServer(level, level.damageSources().mobAttack(swarm), 4.0F),
                "Survival hit succeeds");
            health(survival, 16.0F, "Survival damage is not halved");

            Player spectator = player(level, GameType.SPECTATOR);
            check(!spectator.hurtServer(level, level.damageSources().mobAttack(swarm), 4.0F),
                "Spectator remains immune");
            health(spectator, 20.0F, "Spectator immunity");

            Player protectedPlayer = player(level, GameType.CREATIVE);
            protectedPlayer.setPermanentlyInvulnerable(true);
            check(!protectedPlayer.hurtServer(level, level.damageSources().mobAttack(swarm), 4.0F),
                "Explicit entity invulnerability remains honored");
            health(protectedPlayer, 20.0F, "Explicit invulnerability");

            System.out.println("NIGHT_SPLITTERS_DAMAGE_REGRESSION_PASSED");
            server.halt(false);
        });
    }

    private static Player player(ServerLevel level, GameType mode) {
        Player player = new Player(level, new GameProfile(UUID.randomUUID(), "Regression")) {
            @Override public GameType gameMode() { return mode; }
        };
        mode.updatePlayerAbilities(player.getAbilities());
        player.setHealth(20.0F);
        return player;
    }

    private static void health(Player player, float expected, String message) {
        check(Math.abs(player.getHealth() - expected) < 0.001F,
            message + ": expected " + expected + ", got " + player.getHealth());
    }

    private static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
}
