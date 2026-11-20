package com.rdt.connectedfreedom;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record JournalPayload(String action,String value) implements CustomPacketPayload {
    public static final Type<JournalPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("rdt","journal"));
    public static final StreamCodec<RegistryFriendlyByteBuf,JournalPayload> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,JournalPayload::action,ByteBufCodecs.STRING_UTF8,JournalPayload::value,JournalPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public boolean valid(){return action.length()<=16&&value.length()<=48;}
}
