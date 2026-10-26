package com.sibcraft.creeperborn;

import java.util.Locale;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.particles.SimpleParticleType;

public enum Element {
    EARTH("Earth", new String[]{"Boulder Smash", "Stone Guard", "Slab Shackles"}, new int[]{40, 200, 160}, ParticleTypes.CRIT),
    WATER("Water", new String[]{"Water Whip", "Tidal Guard", "Mending Flow"}, new int[]{40, 160, 240}, ParticleTypes.SPLASH),
    FIRE("Fire", new String[]{"Fire Lance", "Flame Ring", "Heat Shield"}, new int[]{40, 160, 200}, ParticleTypes.FLAME),
    AIR("Air", new String[]{"Air Gust", "Air Step", "Vortex"}, new int[]{40, 0, 160}, ParticleTypes.CLOUD);
    public final String title;
    public final String[] moves;
    public final int[] cooldowns;
    public final SimpleParticleType particle;
    Element(String title, String[] moves, int[] cooldowns, SimpleParticleType particle) {
        this.title = title; this.moves = moves; this.cooldowns = cooldowns; this.particle = particle;
    }
    public String key() { return name().toLowerCase(Locale.ROOT); }
    public static Element parse(String text) {
        try { return valueOf(text.toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException ex) { return null; }
    }
}
