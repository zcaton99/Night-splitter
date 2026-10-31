package com.sibcraft.creeperborn;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
/** Test-only wire acknowledgments; never included in the distributable mod. */
public record MultiplayerProbe(int action,int stage,int peer,int tick,int celestial,int otherCelestial,int azure,int otherAzure,int nature,int otherNature,int zoan,int otherZoan,int displays,double x,double y,double z) implements CustomPacketPayload {
 public static final Type<MultiplayerProbe> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn_tests","multiplayer_probe"));
 public static final StreamCodec<RegistryFriendlyByteBuf,MultiplayerProbe> CODEC=new StreamCodec<>(){
  public void encode(RegistryFriendlyByteBuf b,MultiplayerProbe p){for(int v:new int[]{p.action,p.stage,p.peer,p.tick,p.celestial,p.otherCelestial,p.azure,p.otherAzure,p.nature,p.otherNature,p.zoan,p.otherZoan,p.displays})b.writeVarInt(v);b.writeDouble(p.x);b.writeDouble(p.y);b.writeDouble(p.z);}
  public MultiplayerProbe decode(RegistryFriendlyByteBuf b){return new MultiplayerProbe(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readDouble(),b.readDouble(),b.readDouble());}
 };
 public Type<? extends CustomPacketPayload> type(){return TYPE;}
 public static MultiplayerProbe command(int stage,int peer){return new MultiplayerProbe(0,stage,peer,0,0,0,0,0,0,0,0,0,0,0,0,0);}
}
