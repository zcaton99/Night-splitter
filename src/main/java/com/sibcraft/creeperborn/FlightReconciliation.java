package com.sibcraft.creeperborn;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
/** Reconcile predicted flight once, with server collision and a per-tick distance budget; keep vanilla tracking current. */
public final class FlightReconciliation {
 private static final class Budget {long tick;double remaining=4;}
 private static final Map<UUID,Budget> budgets=new HashMap<>();
 public static void accept(ServerPlayer p,ServerboundMovePlayerPacket packet,double speed,Predicate<Vec3> mover){Budget b=budgets.computeIfAbsent(p.getUUID(),id->new Budget());long now=p.level().getGameTime();b.remaining=Math.min(8,b.remaining+Math.max(0,now-b.tick)*speed);b.tick=now;Vec3 at=new Vec3(packet.getX(p.getX()),packet.getY(p.getY()),packet.getZ(p.getZ()));double distance=at.distanceTo(p.position());boolean safe=Double.isFinite(at.x)&&Double.isFinite(at.y)&&Double.isFinite(at.z)&&distance<=b.remaining+.001&&at.y>=p.level().getMinY()&&at.y+p.getBbHeight()<p.level().getMaxY()&&p.level().hasChunkAt(BlockPos.containing(at))&&p.level().getWorldBorder().isWithinBounds(BlockPos.containing(at))&&!CelestialPowers.rooted(p)&&!NaturePowers.rooted(p)&&!PairPowers.rooted(p)&&!ClimatePowers.rooted(p)&&!Shackles.rooted(p)&&!StringFruit.controlled(p);if(safe){b.remaining-=distance;safe=mover.test(at);}if(!safe||p.position().distanceToSqr(at)>.0025){p.teleportTo(p.getX(),p.getY(),p.getZ());return;}p.level().getChunkSource().move(p);p.connection.resetPosition();}
 public static void forget(UUID id){budgets.remove(id);}
 public static void clear(){budgets.clear();}
}
