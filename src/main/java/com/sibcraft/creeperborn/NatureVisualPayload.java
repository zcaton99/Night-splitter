package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
public record NatureVisualPayload(int entity,int flags,double x,double y,double z) implements CustomPacketPayload{
    public NatureVisualPayload(int entity,int flags,Vec3 goal){this(entity,flags,goal.x,goal.y,goal.z);}
    public static final Type<NatureVisualPayload> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","nature_visual"));
    public static final StreamCodec<RegistryFriendlyByteBuf,NatureVisualPayload> CODEC=StreamCodec.composite(ByteBufCodecs.VAR_INT,NatureVisualPayload::entity,ByteBufCodecs.VAR_INT,NatureVisualPayload::flags,ByteBufCodecs.DOUBLE,NatureVisualPayload::x,ByteBufCodecs.DOUBLE,NatureVisualPayload::y,ByteBufCodecs.DOUBLE,NatureVisualPayload::z,NatureVisualPayload::new);
    @Override public Type<? extends CustomPacketPayload> type(){return TYPE;}
}
