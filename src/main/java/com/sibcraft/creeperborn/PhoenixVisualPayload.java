package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record PhoenixVisualPayload(int entity,int flags) implements CustomPacketPayload{
    public static final Type<PhoenixVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","phoenix_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,PhoenixVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,PhoenixVisualPayload::entity,ByteBufCodecs.VAR_INT,PhoenixVisualPayload::flags,PhoenixVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
