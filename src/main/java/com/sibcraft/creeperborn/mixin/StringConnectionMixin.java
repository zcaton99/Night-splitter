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
    private void creeperborn$stringControlMovement(ServerboundMovePlayerPacket packet,CallbackInfo ci){if(com.sibcraft.creeperborn.NaturePowers.connectionMove(player,packet)){ci.cancel();return;}if(com.sibcraft.creeperborn.ClimatePowers.connectionMove(player,packet)){ci.cancel();return;}if(com.sibcraft.creeperborn.Azure.locked(player)){com.sibcraft.creeperborn.Azure.connectionMove(player,packet);ci.cancel();return;}if(com.sibcraft.creeperborn.NaturePowers.rooted(player)||StringFruit.locked(player)||com.sibcraft.creeperborn.Paw.airborne(player)||com.sibcraft.creeperborn.Tremor.airborne(player)||com.sibcraft.creeperborn.PairPowers.rooted(player)||com.sibcraft.creeperborn.ClimatePowers.locked(player))ci.cancel();}
    @Inject(method="handleInteract",at=@At("HEAD"),cancellable=true)
    private void creeperborn$azureBasic(CallbackInfo ci){if(com.sibcraft.creeperborn.NaturePowers.rooted(player)||com.sibcraft.creeperborn.NaturePowers.phased(player)||com.sibcraft.creeperborn.Azure.selected(player)||StringFruit.locked(player)||com.sibcraft.creeperborn.PairPowers.rooted(player)||com.sibcraft.creeperborn.ClimatePowers.rooted(player))ci.cancel();}
    @Inject(method={"handlePlayerAction","handleUseItem","handleUseItemOn","handleContainerClick","handleSetCreativeModeSlot"},at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringNoBodyInteraction(CallbackInfo ci){if(com.sibcraft.creeperborn.NaturePowers.rooted(player)||com.sibcraft.creeperborn.NaturePowers.phased(player)||StringFruit.locked(player)||com.sibcraft.creeperborn.PairPowers.rooted(player)||com.sibcraft.creeperborn.ClimatePowers.rooted(player))ci.cancel();}
}
