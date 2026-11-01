package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record LuffyPayload(int entity,int flags) implements CustomPacketPayload {
 public static final Type<LuffyPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","luffy_visual"));
 public static final StreamCodec<RegistryFriendlyByteBuf,LuffyPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,LuffyPayload::entity,ByteBufCodecs.VAR_INT,LuffyPayload::flags,LuffyPayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
