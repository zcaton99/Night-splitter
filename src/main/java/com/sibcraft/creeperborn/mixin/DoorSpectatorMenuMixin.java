package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.DoorSpace;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundTeleportToEntityPacket;
import net.minecraft.network.protocol.game.ServerboundSpectatorActionPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class DoorSpectatorMenuMixin {
    @Shadow public ServerPlayer player;
    private boolean creeperborn$travel(){
        // On the network thread, let vanilla schedule the packet onto the server thread first.
        return player.level().getServer().isSameThread()&&DoorSpace.active(player);
    }
    @Inject(method="handleTeleportToEntityPacket",at=@At("HEAD"),cancellable=true)
    private void creeperborn$noSpectatorTeleport(ServerboundTeleportToEntityPacket packet,CallbackInfo ci){if(creeperborn$travel())ci.cancel();}
    @Inject(method="handleSpectatorAction",at=@At("HEAD"),cancellable=true)
    private void creeperborn$noSpectatorMenu(ServerboundSpectatorActionPacket packet,CallbackInfo ci){if(creeperborn$travel())ci.cancel();}
}
