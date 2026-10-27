package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record LightningVisualPayload(int entity,int flags) implements CustomPacketPayload{
    public static final Type<LightningVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","lightning_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,LightningVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,LightningVisualPayload::entity,ByteBufCodecs.VAR_INT,LightningVisualPayload::flags,LightningVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
