package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.NationTrades;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.trading.Merchant;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(MerchantMenu.class)
public abstract class NationTradeQuickMoveMixin implements NationTrades.MenuMerchant {
    @Shadow @Final private MerchantContainer tradeContainer;
    @Shadow @Final private Merchant trader;
    public Merchant creeperborn$merchant(){return trader;}
    @Inject(method="quickMoveStack",at=@At("HEAD"),cancellable=true)
    private void creeperborn$sharedStock(Player player,int slot,CallbackInfoReturnable<ItemStack> cir){if(slot==2&&!NationTrades.canTake(trader,tradeContainer.getActiveOffer(),player))cir.setReturnValue(ItemStack.EMPTY);}
}
