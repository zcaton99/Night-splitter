package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Relics;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ItemEntity.class)
public abstract class HolyBagItemMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void creeperborn$holyReward(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        if(Relics.kind(((ItemEntity)(Object)this).getItem()).equals("holy_bag"))cir.setReturnValue(false);
    }
}
