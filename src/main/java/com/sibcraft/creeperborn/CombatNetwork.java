package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.world.entity.player.Player;

public final class CombatNetwork {
    private record Window(long second, int used) {}
    private static final Map<UUID, Window> limits = new HashMap<>();
    private static final Map<UUID,Integer> sentSelection=new HashMap<>();
    private CombatNetwork() {}
    public static boolean allow(Player p) {
        long second = p.level().getGameTime() / 20;
        Window w = limits.get(p.getUUID());
        if (w == null || w.second() != second) { limits.put(p.getUUID(), new Window(second, 1)); return true; }
        if (w.used() >= 20) return false;
        limits.put(p.getUUID(), new Window(second, w.used() + 1)); return true;
    }
    public static void clear() { limits.clear();sentSelection.clear(); }
    public static int selection(Player p){
        Element e=Progress.selected(p);return Rubber.selected(p)?4:e==null?-1:e.ordinal();
    }
    public static void forget(Player p){limits.remove(p.getUUID());sentSelection.remove(p.getUUID());}
    public static void tick(net.minecraft.server.MinecraftServer server){
        for(var p:server.getPlayerList().getPlayers()){
            int value=selection(p);
            if(!Objects.equals(sentSelection.get(p.getUUID()),value)&&ServerPlayNetworking.canSend(p,ControlPayload.TYPE)){
                ServerPlayNetworking.send(p,new ControlPayload(3,value));sentSelection.put(p.getUUID(),value);
            }
        }
    }
    /** Move presses always use server selection, even before a client selection reply arrives. */
    public static String press(Player p,int packed){
        int move=packed&3;
        if(move>2)return "";
        if(Rubber.selected(p)){Rubber.press(p,move,packed>>>2);return "";}
        if(Progress.selected(p)==null)return "Select an unlocked element with R or Rubber with G.";
        Creeperborn.ABILITIES.move(p,move);return Creeperborn.ABILITIES.cast(p);
    }
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
            if(payload.action()==5){String result=press(p,payload.value());if(!result.isEmpty())Creeperborn.say(p,result);}
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


