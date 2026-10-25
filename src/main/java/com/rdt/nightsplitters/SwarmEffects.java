package com.rdt.nightsplitters;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class SwarmEffects {
    private SwarmEffects() {}

    public static boolean darknessOnly(CaveSpider spider) {
        SwarmConfig config = NightSplitters.settings();
        return config.enabled && config.darknessOnly && spider.entityTags().contains("ns_swarm");
    }

    public static boolean applyDarkness(CaveSpider spider, Entity target, boolean only) {
        SwarmConfig config = NightSplitters.settings();
        if (!config.enabled || !spider.entityTags().contains("ns_swarm")
                || !(target instanceof Player player) || !player.isAlive()
                || player.isSpectator() || player.isPermanentlyInvulnerable()) return false;
        if (!only && (!config.darknessEnabled || !spider.entityTags().contains("ns_dark"))) return false;
        return player.addEffect(new MobEffectInstance(MobEffects.DARKNESS,
            config.darknessSeconds * 20, 0), spider);
    }
}
