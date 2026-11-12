package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record GasVisualPayload(int entity,int flags) implements CustomPacketPayload{
    public static final Type<GasVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","gas_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,GasVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,GasVisualPayload::entity,ByteBufCodecs.VAR_INT,GasVisualPayload::flags,GasVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
