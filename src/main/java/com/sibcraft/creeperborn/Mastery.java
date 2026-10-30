package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class Mastery {
    private static final Map<String, Long> lastCredit = new HashMap<>();
    private static final Map<UUID, Vec3> lastPosition = new HashMap<>();
    private static final Map<UUID, Long> recentAction = new HashMap<>();
    private Mastery() {}
    public static int tier(int uses, int seconds) {
        if (uses >= 40 && seconds >= 1200) return 3;
        if (uses >= 10 && seconds >= 300) return 2;
        return 1;
    }
    public static int tier(Player p, Element e) { return Progress.unlocked(p, e) ? tier(uses(p,e), seconds(p,e)) : 0; }
    public static boolean harmony(Player p) {
        for (Element e : Element.values()) if (tier(p,e) < 3) return false;
        return true;
    }
    public static int elements(Player p){int count=0;for(Element e:Element.values())if(Progress.unlocked(p,e))count++;return count;}
    public static double collectionBonus(Player p){return 1+.1*Math.max(0,elements(p)-1);}
    public static double elementalBonus(Player p){return collectionBonus(p)*multiplier(p);}
    public static String elementalPercent(Player p){double percent=Math.round(elementalBonus(p)*1000)/10.0;return percent==Math.rint(percent)?Integer.toString((int)percent):Double.toString(percent);}
    public static double multiplier(Player p) { return harmony(p) ? 1.15 : 1; }
    public static boolean allowed(Player p, Element e, int move) { return tier(p,e) >= move + 1; }
    public static int uses(Player p, Element e) { return get(p, "cb_uses_" + e.key() + "_"); }
    public static int seconds(Player p, Element e) { return get(p, "cb_time_" + e.key() + "_"); }
    public static int get(Player p, String prefix) {
        for (String tag : p.entityTags()) if (tag.startsWith(prefix)) {
            try { return Math.max(0, Integer.parseInt(tag.substring(prefix.length()))); }
            catch (NumberFormatException ignored) {}
        }
        return 0;
    }
    public static void set(Player p, String prefix, int value) {
        for (String tag : Set.copyOf(p.entityTags())) if (tag.startsWith(prefix)) p.removeTag(tag);
        p.addTag(prefix + Math.min(1000000, value));
    }
    public static void credit(Player p, Element e) {
        if (!Progress.unlocked(p,e) || !p.isAlive() || p.isSpectator()) return;
        String key = p.getUUID() + ":" + e.key(); long now = p.level().getGameTime();
        if (now < lastCredit.getOrDefault(key, Long.MIN_VALUE / 2) + 20) return;
        lastCredit.put(key, now); recentAction.put(p.getUUID(), now);
        int previous = tier(p,e); set(p, "cb_uses_" + e.key() + "_", uses(p,e) + 1);
        if (tier(p,e) > previous) Creeperborn.say(p, e.title + " mastery increased! Move " + tier(p,e) + " unlocked.");
    }
    public static void tick(MinecraftServer server) {
        for (Player p : server.getPlayerList().getPlayers()) {
            Vec3 previousPosition = lastPosition.put(p.getUUID(), p.position()); Element e = Progress.selected(p);
            if (WaterRules.suppressed(p) || Rubber.selected(p) || DoorFruit.selected(p) || Phoenix.selected(p) || Lightning.selected(p) || Dark.selected(p) || e == null || !p.isAlive() || p.isSpectator()) continue;
            boolean moving = previousPosition != null && previousPosition.distanceToSqr(p.position()) > .04;
            boolean casting = p.level().getGameTime() - recentAction.getOrDefault(p.getUUID(), Long.MIN_VALUE / 2) < 200;
            if (moving || casting) {
                int previous = tier(p,e); set(p, "cb_time_" + e.key() + "_", seconds(p,e) + 1);
                if (tier(p,e) > previous) Creeperborn.say(p, e.title + " mastery increased! Move " + tier(p,e) + " unlocked.");
            }
        }
    }
    public static void grant(Player p, Element e, int tier) {
        p.addTag("cb_unlock_" + e.key());
        set(p, "cb_uses_" + e.key() + "_", tier == 3 ? 40 : tier == 2 ? 10 : 0);
        set(p, "cb_time_" + e.key() + "_", tier == 3 ? 1200 : tier == 2 ? 300 : 0);
    }
    public static void clear() { lastCredit.clear(); lastPosition.clear(); recentAction.clear(); }
}



