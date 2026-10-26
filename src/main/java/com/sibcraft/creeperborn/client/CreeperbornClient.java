package com.sibcraft.creeperborn.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.sibcraft.creeperborn.Element;
import com.sibcraft.creeperborn.CastPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class CreeperbornClient implements ClientModInitializer {
    private boolean combat;
    private Element selected = Element.EARTH;
    private int ticks, lastAirStep = -100;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("creeperborn", "combat"));
    private static KeyMapping key(String name, int code) {
        return KeyMappingHelper.registerKeyMapping(new KeyMapping("key.creeperborn." + name, InputConstants.Type.KEYBOARD, code, CATEGORY));
    }
    @Override public void onInitializeClient() {
        KeyMapping toggle = key("toggle", InputConstants.KEY_B);
        KeyMapping[] elements = {key("earth", InputConstants.KEY_Z), key("water", InputConstants.KEY_X), key("fire", InputConstants.KEY_C), key("air", InputConstants.KEY_V)};
        KeyMapping[] moves = {key("move1", InputConstants.KEY_R), key("move2", InputConstants.KEY_G), key("move3", InputConstants.KEY_H)};
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ticks++;
            if (client.player == null || client.getConnection() == null) { combat = false; return; }
            if (client.gui.screen() != null || client.isPaused()) {
                while (toggle.consumeClick()) {}
                for (KeyMapping mapping : elements) while (mapping.consumeClick()) {}
                for (KeyMapping mapping : moves) while (mapping.consumeClick()) {}
                return;
            }
            while (toggle.consumeClick()) {
                combat = !combat;
                client.player.sendOverlayMessage(Component.literal("Creeperborn combat " + (combat ? "ON — Z/X/C/V element; R/G/H move" : "OFF")));
            }
            for (int i = 0; i < elements.length; i++) while (elements[i].consumeClick()) {
                if (combat) { selected = Element.values()[i]; client.player.sendOverlayMessage(Component.literal(selected.title + " — R/G/H: " + String.join(" / ", selected.moves))); }
            }
            for (int i = 0; i < moves.length; i++) while (moves[i].consumeClick()) {
                if (combat && ClientPlayNetworking.canSend(CastPayload.TYPE)) {
                    ClientPlayNetworking.send(new CastPayload(selected.ordinal(), i));
                    if (selected == Element.AIR && i == 1) lastAirStep = ticks;
                }
            }
            if (combat && selected == Element.AIR && moves[1].isDown() && ticks - lastAirStep >= 4
                    && ClientPlayNetworking.canSend(CastPayload.TYPE)) {
                ClientPlayNetworking.send(new CastPayload(Element.AIR.ordinal(), 1)); lastAirStep = ticks;
            }
        });
    }
}
