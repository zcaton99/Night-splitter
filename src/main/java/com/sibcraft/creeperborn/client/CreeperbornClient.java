package com.sibcraft.creeperborn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sibcraft.creeperborn.Element;
import com.sibcraft.creeperborn.ControlPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CreeperbornClient implements ClientModInitializer {
    private boolean combat;
    private Element selected;
    private int ticks, lastAirStep = -100;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("creeperborn", "combat"));
    private static KeyMapping key(String name, int code) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.creeperborn." + name, InputConstants.Type.KEYBOARD, code, CATEGORY));
    }
    @Override public void onInitializeClient() {
        KeyMapping toggle = key("toggle", InputConstants.KEY_B);
        KeyMapping cycle = key("cycle", InputConstants.KEY_R);
        KeyMapping[] moves = {key("move1", InputConstants.KEY_Z), key("move2", InputConstants.KEY_X), key("move3", InputConstants.KEY_C)};
        ClientPlayNetworking.registerGlobalReceiver(ControlPayload.TYPE, (payload, context) -> {
            if (payload.action() == 3) selected = payload.value() >= 0 && payload.value() < 4 ? Element.values()[payload.value()] : null;
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            if (client.player == null || client.getConnection() == null) { combat = false; selected = null; return; }
            if (client.gui.screen() != null || client.isPaused()) {
                while (toggle.consumeClick()) {}
                while (cycle.consumeClick()) {}
                for (KeyMapping mapping : moves) while (mapping.consumeClick()) {}
                return;
            }
            while (toggle.consumeClick()) {
                combat = !combat;
                if (combat && ClientPlayNetworking.canSend(ControlPayload.TYPE)) ClientPlayNetworking.send(new ControlPayload(0, 0));
                client.player.sendOverlayMessage(Component.literal("Creeperborn combat " + (combat ? "ON — R cycles elements; Z/X/C moves" : "OFF")));
            }
            while (cycle.consumeClick()) {
                if (combat && ClientPlayNetworking.canSend(ControlPayload.TYPE))
                    ClientPlayNetworking.send(new ControlPayload(1, client.options.keyShift.isDown() ? -1 : 1));
            }
            for (int i = 0; i < moves.length; i++) while (moves[i].consumeClick()) {
                if (combat && ClientPlayNetworking.canSend(ControlPayload.TYPE)) {
                    ClientPlayNetworking.send(new ControlPayload(2, i));
                    if (selected == Element.AIR && i == 1) lastAirStep = ticks;
                }
            }
            if (combat && selected == Element.AIR && moves[1].isDown() && ticks - lastAirStep >= 4
                    && ClientPlayNetworking.canSend(ControlPayload.TYPE)) {
                ClientPlayNetworking.send(new ControlPayload(2, 1)); lastAirStep = ticks;
            }
        });
    }
}
