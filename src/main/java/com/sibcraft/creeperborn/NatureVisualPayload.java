package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record NatureVisualPayload(int entity,int flags) implements CustomPacketPayload{
    public static final Type<NatureVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","nature_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,NatureVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,NatureVisualPayload::entity,ByteBufCodecs.VAR_INT,NatureVisualPayload::flags,NatureVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
