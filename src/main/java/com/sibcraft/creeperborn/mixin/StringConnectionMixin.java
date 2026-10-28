package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.StringFruit;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class StringConnectionMixin {
    @Shadow public ServerPlayer player;
    @Inject(method="handleMovePlayer",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringControlMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci){if(StringFruit.locked(player)||com.sibcraft.creeperborn.Paw.airborne(player)||com.sibcraft.creeperborn.Tremor.airborne(player))ci.cancel();}
    @Inject(method={"handleInteract","handlePlayerAction","handleUseItem","handleUseItemOn","handleContainerClick","handleSetCreativeModeSlot"},at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringNoBodyInteraction(CallbackInfo ci){if(StringFruit.locked(player))ci.cancel();}
}
