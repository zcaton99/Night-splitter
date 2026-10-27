package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record DarkVisualPayload(int entity,int flags) implements CustomPacketPayload{
    public static final Type<DarkVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","dark_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,DarkVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,DarkVisualPayload::entity,ByteBufCodecs.VAR_INT,DarkVisualPayload::flags,DarkVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
