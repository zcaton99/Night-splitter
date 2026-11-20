package com.rdt.connectedfreedom;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record Probe(int stage,int value) implements CustomPacketPayload {
    public static final Type<Probe> TYPE=new Type<>(Identifier.fromNamespaceAndPath("rdt_tests","probe"));
    public static final StreamCodec<RegistryFriendlyByteBuf,Probe> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,Probe::stage,ByteBufCodecs.VAR_INT,Probe::value,Probe::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
