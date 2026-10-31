package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record ZoanPayload(int entity,int flags) implements CustomPacketPayload {
 public static final Type<ZoanPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","zoan_visual"));
 public static final StreamCodec<RegistryFriendlyByteBuf,ZoanPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,ZoanPayload::entity,ByteBufCodecs.VAR_INT,ZoanPayload::flags,ZoanPayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
