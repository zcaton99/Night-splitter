package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record AzureVisualPayload(int entity,int flags) implements CustomPacketPayload{
    public static final Type<AzureVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","azure_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AzureVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AzureVisualPayload::entity,ByteBufCodecs.VAR_INT,AzureVisualPayload::flags,AzureVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
