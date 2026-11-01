package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record LuffyInputPayload(int buttons,int keys,float yaw,float pitch) implements CustomPacketPayload {
 public static final Type<LuffyInputPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","luffy_input"));
 public static final StreamCodec<RegistryFriendlyByteBuf,LuffyInputPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,LuffyInputPayload::buttons,ByteBufCodecs.VAR_INT,LuffyInputPayload::keys,ByteBufCodecs.FLOAT,LuffyInputPayload::yaw,ByteBufCodecs.FLOAT,LuffyInputPayload::pitch,LuffyInputPayload::new);
 public boolean valid(){return buttons>=0&&buttons<=4&&keys>=0&&keys<=127&&Float.isFinite(yaw)&&Float.isFinite(pitch)&&pitch>=-90&&pitch<=90;}
 @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
