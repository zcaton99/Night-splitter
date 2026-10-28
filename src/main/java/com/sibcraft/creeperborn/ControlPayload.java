package com.sibcraft.creeperborn;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Client requests: 0 sync, 1 cycle, 2 cast. Server reply: 3 selected element. */
public record ControlPayload(int action, int value) implements CustomPacketPayload {
    public static final Type<ControlPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("creeperborn", "controls"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ControlPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, ControlPayload::action, ByteBufCodecs.VAR_INT, ControlPayload::value, ControlPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public boolean validRequest() {
        return action == 0 && value == 0 || action == 1 && (value == -1 || value == 1)
            || (action == 2 || action == 6) && value >= 0 && value < 3
            || action == 5 && value >= 0 && value < 64 && (value & 3) < 3
            || (action == 4 || action == 7 || action == 8) && value == 0
            || action == 9 && (value == 0 || value == 1)
            || action == 10 && value>=0&&value<=127;
    }
}

