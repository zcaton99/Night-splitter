package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Rubber;
import com.sibcraft.creeperborn.Phoenix;
import com.sibcraft.creeperborn.Lightning;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class RubberDamageMixin {
    @Inject(method="hurtServer",at=@At("HEAD"),cancellable=true)
    private void creeperborn$masteryImmunity(ServerLevel level,DamageSource source,float amount,CallbackInfoReturnable<Boolean> cir){
        // Zero damage alone still enters vanilla's hurt sound, animation and knockback path.
        if((Object)this instanceof Player p&&(Rubber.masteryImmune(p,source)||Phoenix.immune(p,source)||Lightning.immune(p,source)))cir.setReturnValue(false);
    }
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$rubberDamage(float amount,ServerLevel level,DamageSource source,float original){
        return (Object)this instanceof Player p ? Rubber.damage(p,source,Phoenix.damage(p,source,Lightning.damage(p,source,amount))) : amount;
    }
}
