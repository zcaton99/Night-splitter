package com.sibcraft.creeperborn;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public final class Progress {
    private Progress() {}
    public static boolean unlocked(Player player, Element e) { return player.entityTags().contains("cb_unlock_" + e.key()); }
    public static boolean all(Player player) {
        for (Element e : Element.values()) if (!unlocked(player, e)) return false;
        return true;
    }
    public static Element selected(Player player) {
        for (Element e : Element.values()) if (player.entityTags().contains("cb_select_" + e.key()) && unlocked(player, e)) return e;
        for (Element e : Element.values()) if (unlocked(player, e)) return e;
        return null;
    }
    public static boolean select(Player player, Element e) {
        if (!unlocked(player, e)) return false;
        Rubber.cancelInput(player); player.removeTag("cb_mode_fruit");
        for (Element old : Element.values()) player.removeTag("cb_select_" + old.key());
        player.addTag("cb_select_" + e.key()); return true;
    }
    public static Element cycle(Player player) {
        return cycle(player, 1);
    }
    public static Element cycle(Player player, int direction) {
        Element current = selected(player);
        for (int i = 1; i <= 4; i++) {
            Element next = Element.values()[Math.floorMod((current == null ? (direction > 0 ? -1 : 0) : current.ordinal()) + i * direction, 4)];
            if (select(player, next)) return next;
        }
        return null;
    }
    public static void give(Player player, ItemStack stack) {
        if (!player.addItem(stack) && player.level() instanceof net.minecraft.server.level.ServerLevel level)
            player.spawnAtLocation(level, stack);
    }
    public static boolean consume(Player player, ItemStack stack) {
        String kind = Relics.kind(stack);
        if (!kind.startsWith("heart:") || stack.isEmpty() || player.isSpectator()) return false;
        Element e = Element.parse(kind.substring(6));
        if (e == null || unlocked(player, e)) return false;
        boolean first = selected(player) == null;
        player.addTag("cb_unlock_" + e.key()); select(player, e); stack.shrink(1);
        if (first) give(player, Relics.focus());
        if (all(player) && !player.entityTags().contains("cb_trident_claimed")) {
            give(player, Relics.trident()); player.addTag("cb_trident_claimed");
        }
        return true;
    }
}

