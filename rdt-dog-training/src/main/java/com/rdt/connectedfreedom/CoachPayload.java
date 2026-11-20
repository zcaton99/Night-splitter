package com.rdt.connectedfreedom;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record CoachPayload(String json,boolean open) implements CustomPacketPayload {
    public static final Type<CoachPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("rdt","coach"));
    public static final StreamCodec<RegistryFriendlyByteBuf,CoachPayload> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,CoachPayload::json,ByteBufCodecs.BOOL,CoachPayload::open,CoachPayload::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public record View(String name,int dogId,int step,int connections,int celebrations,int obstacles,int distance,int socialization,int comfort,int engagement,double line,boolean leashed,boolean complete,String mode,String notice,String next){}
}
