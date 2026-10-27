package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Phoenix;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class PhoenixRevivalMixin{
    @Inject(method="checkTotemDeathProtection",at=@At("HEAD"),cancellable=true)
    private void creeperborn$rebirth(DamageSource source,CallbackInfoReturnable<Boolean> cir){
        if((Object)this instanceof Player p&&Phoenix.revive(p,source))cir.setReturnValue(true);
    }
}
