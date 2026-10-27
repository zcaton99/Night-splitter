package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Dedicated, bounded possession input. No inventory, interaction or arbitrary entity commands. */
public record StringControlPayload(int entity,int flags,float yaw,float pitch) implements CustomPacketPayload {
    public static final Type<StringControlPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","string_control"));
    public static final StreamCodec<RegistryFriendlyByteBuf,StringControlPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,StringControlPayload::entity,ByteBufCodecs.VAR_INT,StringControlPayload::flags,ByteBufCodecs.FLOAT,StringControlPayload::yaw,ByteBufCodecs.FLOAT,StringControlPayload::pitch,StringControlPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public boolean valid(){return flags>=0&&flags<64&&Float.isFinite(yaw)&&Float.isFinite(pitch)&&Math.abs(yaw)<1000000&&Math.abs(pitch)<=90;}
}
