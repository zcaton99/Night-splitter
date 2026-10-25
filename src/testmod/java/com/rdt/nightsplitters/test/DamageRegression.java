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
        // Exercise Minecraft's actual movement/collision/fall path, not just the launch formula.
        int floorY = (int)origin.y - 1;
        for (int x = -2; x <= 30; x++) {
            for (int z = -2; z <= 2; z++) {
                level.setBlockAndUpdate(new net.minecraft.core.BlockPos(x, floorY, z),
                    net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            }
        }
        for (int height : new int[] {2, 10, 25}) {
            CaveSpider jumper = EntityTypes.CAVE_SPIDER.create(level, EntitySpawnReason.EVENT);
            jumper.addTag("ns_swarm");
            jumper.snapTo(origin.x, origin.y, origin.z);
            jumper.setOnGround(true);
            Player flyer = player(level, GameType.CREATIVE);
            flyer.snapTo(origin.x + 8, origin.y + height, origin.z);
            flyer.setOnGround(false);
            SwarmPounce launch = new SwarmPounce();
            check(launch.tryLaunch(jumper, flyer), "Physical pounce launches at " + height);
            float before = jumper.getHealth();
            double peak = 0;
            boolean crossed = false;
            for (int tick = 0; tick < 200 && !jumper.onGround(); tick++) {
                jumper.travel(Vec3.ZERO);
                double rise = jumper.getY() - origin.y;
                peak = Math.max(peak, rise);
                if (!crossed && rise >= height - 0.1) {
                    check(Math.abs(jumper.getX() - flyer.getX()) < 1.0,
                        "Actual pounce crosses target position at target height");
                    crossed = true;
                }
            }
            check(crossed && peak >= height - 0.1 && peak <= 25.1,
                "Actual Minecraft pounce reaches height without exceeding 25 blocks: " + peak);
            check(jumper.onGround(), "Physical pounce lands");
            check(jumper.getHealth() == before, "Physical landing preserves spider health");
            check(!launch.isInFlight(jumper), "Physical landing restores ground pursuit");
            check(!jumper.isIgnoringFallDamageFromCurrentImpulse(),
                "Physical landing clears pounce protection");
            System.out.println("POUNCE_PHYSICS_PASS height=" + height + " peak=" + peak);
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
