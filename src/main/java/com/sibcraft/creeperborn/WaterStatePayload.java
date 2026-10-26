package com.sibcraft.creeperborn;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server-only state: fruit ownership and all-four mastery, never client supplied. */
public record WaterStatePayload(int flags) implements CustomPacketPayload {
    public static final Type<WaterStatePayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","water_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WaterStatePayload> CODEC=StreamCodec.composite(
        ByteBufCodecs.VAR_INT,WaterStatePayload::flags,WaterStatePayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
