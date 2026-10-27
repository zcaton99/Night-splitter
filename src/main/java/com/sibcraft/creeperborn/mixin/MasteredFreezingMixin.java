package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.WaterRules;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class MasteredFreezingMixin {
    @Inject(method="canFreeze",at=@At("HEAD"),cancellable=true)
    private void creeperborn$masteredCold(CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Player p&&WaterRules.mastered(p))cir.setReturnValue(false);
    }
}
