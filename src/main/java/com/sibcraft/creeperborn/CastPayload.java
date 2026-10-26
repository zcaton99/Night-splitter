package com.sibcraft.creeperborn;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record CastPayload(int element, int move) implements CustomPacketPayload {
    public static final Type<CastPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("creeperborn", "cast"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CastPayload> CODEC = StreamCodec.composite(
        ByteBufCodecs.VAR_INT, CastPayload::element, ByteBufCodecs.VAR_INT, CastPayload::move, CastPayload::new);
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    public boolean valid() { return element >= 0 && element < 4 && move >= 0 && move < 3; }
}

