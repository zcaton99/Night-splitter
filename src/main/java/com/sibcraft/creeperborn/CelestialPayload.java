package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record CelestialPayload(int entity,int flags) implements CustomPacketPayload {
 public static final Type<CelestialPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","celestial_visual"));
 public static final StreamCodec<RegistryFriendlyByteBuf,CelestialPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,CelestialPayload::entity,ByteBufCodecs.VAR_INT,CelestialPayload::flags,CelestialPayload::new);
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
