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
        PayloadTypeRegistry.serverboundPlay().register(ControlPayload.TYPE, ControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ControlPayload.TYPE, ControlPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(WaterStatePayload.TYPE,WaterStatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(ControlPayload.TYPE, (payload, context) -> {
            Player p = context.player();
            if (!payload.validRequest() || !p.isAlive() || p.isSpectator() || !allow(p)) return;
            Element selected = Progress.selected(p);
            if (payload.action() == 1) {
                selected = Progress.cycle(p, payload.value());
                Creeperborn.say(p, selected == null ? "Consume an elemental heart first."
                    : selected.title + " — Z/X/C: " + String.join(" / ", selected.moves));
            }
            if (payload.action() == 2 && !Rubber.selected(p)) {
                if (selected == null) Creeperborn.say(p, "Consume an elemental heart first.");
                else { Creeperborn.ABILITIES.move(p, payload.value()); Creeperborn.say(p, Creeperborn.ABILITIES.cast(p)); }
            }
            if (payload.action() == 4) {
                Rubber.cancelInput(p);
                if(Rubber.unlocked(p)) { p.addTag("cb_mode_fruit"); Creeperborn.say(p,"Rubber — Z: Stretch Punch | X: Directional dodge / hold Grapple | C: Gear 5 / hold Guard"); }
                else Creeperborn.say(p,"Consume a Rubber Fruit first.");
            }
            if(payload.action()==5)Rubber.press(p,payload.value() & 3,payload.value() >>> 2);
            if(payload.action()==6)Rubber.release(p,payload.value());
            if(payload.action()==7)Rubber.heartbeat(p);
            if(payload.action()==8)Rubber.cancelInput(p);
            ServerPlayNetworking.send(context.player(), new ControlPayload(3, Rubber.selected(p) ? 4 : selected == null ? -1 : selected.ordinal()));
        });
        ServerPlayNetworking.registerGlobalReceiver(CastPayload.TYPE, (payload, context) -> {
            Player p = context.player();
            if (!payload.valid() || !p.isAlive() || p.isSpectator() || !allow(p)) return;
            Element e = Element.values()[payload.element()];
            if (!Progress.select(p,e)) { Creeperborn.say(p, "Consume the " + e.title + " heart first."); return; }
            Creeperborn.ABILITIES.move(p, payload.move()); Creeperborn.say(p, Creeperborn.ABILITIES.cast(p));
        });
    }
}


