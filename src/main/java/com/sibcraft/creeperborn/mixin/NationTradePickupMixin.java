package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.NationTrades;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MerchantResultSlot.class)
public abstract class NationTradePickupMixin {
    @Shadow @Final private MerchantContainer slots;
    @Shadow @Final private Merchant merchant;
    @Inject(method="mayPickup",at=@At("HEAD"),cancellable=true)
    private void creeperborn$sharedStock(Player player,CallbackInfoReturnable<Boolean> cir){if(!NationTrades.canTake(merchant,slots.getActiveOffer(),player))cir.setReturnValue(false);}
}
