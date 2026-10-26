package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;

public final class CombatNetwork {
    private record Window(long second, int used) {}
    private static final Map<UUID, Window> limits = new HashMap<>();
    private CombatNetwork() {}
    public static boolean allow(Player p) {
        long second = p.level().getGameTime() / 20;
        Window w = limits.get(p.getUUID());
        if (w == null || w.second() != second) { limits.put(p.getUUID(), new Window(second, 1)); return true; }
        if (w.used() >= 20) return false;
        limits.put(p.getUUID(), new Window(second, w.used() + 1)); return true;
    }
    public static void clear() { limits.clear(); }
    public static void init() {
        PayloadTypeRegistry.serverboundPlay().register(CastPayload.TYPE, CastPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CastPayload.TYPE, (payload, context) -> {
            Player p = context.player();
            if (!payload.valid() || !p.isAlive() || p.isSpectator() || !allow(p)) return;
            Element e = Element.values()[payload.element()];
            if (!Progress.select(p,e)) { Creeperborn.say(p, "Consume the " + e.title + " heart first."); return; }
            Creeperborn.ABILITIES.move(p, payload.move()); Creeperborn.say(p, Creeperborn.ABILITIES.cast(p));
        });
    }
}
