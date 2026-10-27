package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Phoenix;
import java.util.Optional;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.waypoints.WaypointTransmitter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class PhoenixWaypointMixin{
    @Inject(method="makeWaypointConnectionWith",at=@At("HEAD"),cancellable=true)
    private void creeperborn$hidePhoenixMarker(ServerPlayer viewer,CallbackInfoReturnable<Optional<WaypointTransmitter.Connection>> cir){
        if((Object)this instanceof Player p&&Phoenix.form(p))cir.setReturnValue(Optional.empty());
    }
}
