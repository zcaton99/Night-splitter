package com.sibcraft.creeperborn;

import java.util.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;

public final class WaterRules {
    private static UUID clientPlayer;
    private static int clientFlags;
    private static final Map<UUID,Integer> sent=new HashMap<>();
    private WaterRules(){}
    public static void clientState(UUID id,int flags){clientPlayer=id;clientFlags=flags&3;}
    public static void clearClient(){clientPlayer=null;clientFlags=0;}
    public static int flags(Player p){
        if(!(p.level() instanceof ServerLevel))return p.getUUID().equals(clientPlayer)?clientFlags:0;
        return (Rubber.unlocked(p)?1:0)|(Mastery.harmony(p)?2:0);
    }
    public static boolean fruitOwner(Player p){return (flags(p)&1)!=0;}
    public static boolean mastered(Player p){return (flags(p)&2)!=0;}
    public static boolean vulnerable(Player p){return fruitOwner(p)&&!mastered(p)&&p.isAlive()&&!p.isSpectator();}
    /** Eye below actual source/flowing-water surface. Rain and ankle-deep water do not qualify. */
    public static boolean submerged(Player p){
        Vec3 eye=p.getEyePosition();BlockPos pos=BlockPos.containing(eye);
        var fluid=p.level().getFluidState(pos);
        return fluid.is(FluidTags.WATER)&&eye.y<pos.getY()+fluid.getHeight(p.level(),pos);
    }
    public static boolean suppressed(Player p){return vulnerable(p)&&submerged(p);}
    public static Vec3 sinkingVelocity(Vec3 motion){return new Vec3(motion.x*.8,Math.max(-.18,Math.min(-.08,motion.y-.025)),motion.z*.8);}
    /** Run in both normal client prediction and server travel; collision remains vanilla. */
    public static void travel(Player p,Vec3 input){
        p.setSwimming(false);p.setSprinting(false);
        p.moveRelative(.008f,new Vec3(input.x,0,input.z));
        p.setDeltaMovement(sinkingVelocity(p.getDeltaMovement()));
        p.move(MoverType.SELF,p.getDeltaMovement());
    }
    public static void forget(Player p){sent.remove(p.getUUID());}
    public static void clear(){sent.clear();clearClient();}
    public static void tick(MinecraftServer server){
        for(ServerPlayer p:server.getPlayerList().getPlayers()){
            int flags=flags(p);
            if(!Objects.equals(sent.get(p.getUUID()),flags)&&ServerPlayNetworking.canSend(p,WaterStatePayload.TYPE)){
                ServerPlayNetworking.send(p,new WaterStatePayload(flags));sent.put(p.getUUID(),flags);
            }
            apply(p);
        }
    }
    public static void apply(Player p){
        if(!p.isAlive()||p.isSpectator())return;
        if(Mastery.harmony(p))p.clearFire();
        if(mastered(p)){p.setAirSupply(p.getMaxAirSupply());return;}
        if(!suppressed(p))return;
        Rubber.suspendInWater(p);
        p.setSwimming(false);p.setSprinting(false);
        p.setDeltaMovement(sinkingVelocity(p.getDeltaMovement()));
        if(p instanceof ServerPlayer sp)sp.connection.send(new ClientboundSetEntityMotionPacket(p));
    }
}

