package com.sibcraft.creeperborn;
import net.fabricmc.fabric.api.networking.v1.*;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;

/** Separate selection help from the rapidly updating form timer. */
public record SelectionInfo(String text,boolean selection) implements CustomPacketPayload {
    public static final Type<SelectionInfo> TYPE=new Type<>(Identifier.fromNamespaceAndPath("creeperborn","selection_info"));
    public static final StreamCodec<RegistryFriendlyByteBuf,SelectionInfo> CODEC=StreamCodec.composite(ByteBufCodecs.STRING_UTF8,SelectionInfo::text,ByteBufCodecs.BOOL,SelectionInfo::selection,SelectionInfo::new);
    public Type<? extends CustomPacketPayload> type(){return TYPE;}
    public static void init(){PayloadTypeRegistry.clientboundPlay().register(TYPE,CODEC);}
    public static void send(Player p,String text){deliver(p,text,true);}
    public static void toast(Player p,String text){deliver(p,text,false);}
    private static void deliver(Player p,String text,boolean help){
        if(p instanceof ServerPlayer sp&&ServerPlayNetworking.canSend(sp,TYPE))ServerPlayNetworking.send(sp,new SelectionInfo(text,help));
        else p.sendOverlayMessage(Component.literal(text));
    }
}
