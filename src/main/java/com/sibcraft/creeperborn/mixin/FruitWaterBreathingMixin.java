package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.WaterRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class FruitWaterBreathingMixin {
    @Inject(method="canBreatheUnderwater",at=@At("HEAD"),cancellable=true)
    private void creeperborn$masteredBreathing(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Player p&&WaterRules.fruitOwner(p))cir.setReturnValue(WaterRules.mastered(p));
    }
    @Redirect(method="baseTick",at=@At(value="INVOKE",target="Lnet/minecraft/world/effect/MobEffectUtil;hasWaterBreathing(Lnet/minecraft/world/entity/LivingEntity;)Z"))
    private boolean creeperborn$fruitCurseBypassesBreathingPotion(LivingEntity entity){
        if(entity instanceof Player p&&WaterRules.vulnerable(p))return false;
        return MobEffectUtil.hasWaterBreathing(entity);
    }
}
