package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.NationTrades;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.item.trading.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;
@Mixin(AbstractVillager.class)
public abstract class NationMerchantMixin {
    @Inject(method="getOffers",at=@At("RETURN"))
    private void creeperborn$offerings(CallbackInfoReturnable<MerchantOffers> cir){if((Object)this instanceof Villager v)NationTrades.populate(v,cir.getReturnValue());}
    @Inject(method="notifyTrade",at=@At("TAIL"))
    private void creeperborn$contribute(MerchantOffer offer,CallbackInfo ci){if((Object)this instanceof Villager v)NationTrades.traded(v,offer);}
}
