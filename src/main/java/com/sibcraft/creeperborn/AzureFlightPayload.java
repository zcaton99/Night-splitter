package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Separate once-per-tick flight stream, so movement does not exhaust the combat request budget. */
public record AzureFlightPayload(int keys,float yaw,float pitch) implements CustomPacketPayload {
    public static final Type<AzureFlightPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","azure_flight"));
    public static final StreamCodec<RegistryFriendlyByteBuf,AzureFlightPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,AzureFlightPayload::keys,ByteBufCodecs.FLOAT,AzureFlightPayload::yaw,ByteBufCodecs.FLOAT,AzureFlightPayload::pitch,AzureFlightPayload::new);
    public boolean valid(){return keys>=0&&keys<=127&&Float.isFinite(yaw)&&Float.isFinite(pitch);}
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
