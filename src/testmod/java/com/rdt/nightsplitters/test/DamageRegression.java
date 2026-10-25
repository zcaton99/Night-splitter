package com.rdt.nightsplitters.test;

import com.mojang.authlib.GameProfile;
import com.rdt.nightsplitters.SwarmPounce;
import net.minecraft.world.phys.Vec3;
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

            checkPounce(level);
            System.out.println("NIGHT_SPLITTERS_POUNCE_REGRESSION_PASSED");
            System.out.println("NIGHT_SPLITTERS_DAMAGE_REGRESSION_PASSED");
            server.halt(false);
        });
    }

    private static void checkPounce(ServerLevel level) {
        Vec3 origin = new Vec3(0.5, level.getMaxY() - 40, 0.5);
        CaveSpider spider = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
        spider.addTag("ns_swarm");
        spider.snapTo(origin.x, origin.y, origin.z);
        spider.setOnGround(true);
        Player target = player(level, GameType.CREATIVE);
        target.snapTo(origin.x + 8, origin.y + 25, origin.z);
        target.setOnGround(false);
        SwarmPounce pounce = new SwarmPounce();
        check(pounce.tryLaunch(spider, target), "Airborne Creative target triggers pounce");
        check(pounce.isInFlight(spider), "Pounce enters airborne state");
        check(spider.getDeltaMovement().x > 0 && spider.getDeltaMovement().y > 0,
            "Pounce launches upward toward sampled target");
        Vec3 launched = spider.getDeltaMovement();
        target.snapTo(origin.x - 8, origin.y + 25, origin.z);
        check(!pounce.tryLaunch(spider, target), "No repeated midair launch");
        check(spider.getDeltaMovement().equals(launched), "Moving target cannot redirect launch");
        check(pounce.cooldownTicksRemaining(spider) >= 100
                && pounce.cooldownTicksRemaining(spider) <= 160, "Cooldown is 5-8 seconds");
        float health = spider.getHealth();
        spider.causeFallDamage(25.0, 1.0F, level.damageSources().fall());
        check(spider.getHealth() == health, "Pounce landing does not damage spider");
        check(!spider.isIgnoringFallDamageFromCurrentImpulse(),
            "Impulse immunity clears at launch-height landing");
        spider.setOnGround(true);
        check(!pounce.isInFlight(spider), "Landing ends airborne state");
        check(!pounce.tryLaunch(spider, target), "Landing cannot bypass pounce cooldown");
        check(spider.hurtServer(level, level.damageSources().generic(), 1.0F),
            "Pounce does not grant general damage immunity");

        CaveSpider ordinary = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
        ordinary.snapTo(origin.x, origin.y, origin.z);
        ordinary.setOnGround(true);
        check(!new SwarmPounce().tryLaunch(ordinary, target), "Untagged spider cannot pounce");
        ordinary.causeFallDamage(25.0, 1.0F, level.damageSources().fall());
        check(ordinary.getHealth() < ordinary.getMaxHealth(), "Ordinary fall damage still applies");

        CaveSpider child = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
        child.addTag("ns_swarm");
        child.addTag("ns_child");
        child.getAttribute(Attributes.SCALE).setBaseValue(0.5);
        child.snapTo(origin.x, origin.y, origin.z);
        child.setOnGround(true);
        Player grounded = player(level, GameType.CREATIVE);
        grounded.snapTo(origin.x + 8, origin.y + 3, origin.z);
        grounded.setOnGround(true);
        check(!new SwarmPounce().tryLaunch(child, grounded), "Ground target keeps normal pursuit");
        grounded.setOnGround(false);
        check(new SwarmPounce().tryLaunch(child, grounded), "Small offspring can pounce too");

        check(SwarmPounce.calculateVelocity(origin, origin.add(0, 26, 0), 0.08) == null,
            "Reject targets above 25 blocks");
        check(SwarmPounce.calculateVelocity(origin, origin.add(25, 10, 0), 0.08) == null,
            "Reject targets outside horizontal pounce range");
        check(SwarmPounce.calculateVelocity(origin, origin.add(0, 1, 0), 0.08) == null,
            "Reject ordinary ground-height targets");
        for (int height = 2; height <= 25; height++) {
            Vec3 launch = SwarmPounce.calculateVelocity(Vec3.ZERO, new Vec3(8, height, 0), 0.08);
            double y = 0, peak = 0, vy = launch.y, x = 0, vx = launch.x;
            boolean crossed = false;
            for (int tick = 0; tick < 200 && vy > 0; tick++) {
                y += vy;
                x += vx;
                peak = Math.max(peak, y);
                if (!crossed && y >= height - 0.01) {
                    check(Math.abs(x - 8) < 0.001, "Pounce reaches sampled X at target height");
                    crossed = true;
                }
                vy = (vy - 0.08) * 0.98;
                vx *= 0.91;
            }
            check(crossed && peak >= height - 0.01 && peak <= 25.001,
                "Pounce reaches target height without exceeding 25-block apex");
        }
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
