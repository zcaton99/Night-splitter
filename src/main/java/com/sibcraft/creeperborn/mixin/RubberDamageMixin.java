package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Rubber;
import com.sibcraft.creeperborn.Phoenix;
import com.sibcraft.creeperborn.Lightning;
import com.sibcraft.creeperborn.Dark;
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
        if(com.sibcraft.creeperborn.Azure.redirect((LivingEntity)(Object)this,source,amount)){cir.setReturnValue(false);return;}
        if((Object)this instanceof Player azure&&com.sibcraft.creeperborn.Azure.immune(azure,source)){cir.setReturnValue(false);return;}
        if(com.sibcraft.creeperborn.ClimatePowers.interceptCoffin((LivingEntity)(Object)this,source,amount)){cir.setReturnValue(false);return;}
        if((Object)this instanceof Player climate&&com.sibcraft.creeperborn.ClimatePowers.piercing(climate,source))return;
        if(com.sibcraft.creeperborn.ClimatePowers.immune((LivingEntity)(Object)this,source)){cir.setReturnValue(false);return;}
        if(com.sibcraft.creeperborn.PairPowers.immune((LivingEntity)(Object)this,source)||com.sibcraft.creeperborn.Tremor.immune((LivingEntity)(Object)this,source)||com.sibcraft.creeperborn.Paw.immune((LivingEntity)(Object)this,source)||(Object)this instanceof Player paw&&com.sibcraft.creeperborn.Paw.guard(paw,source)){cir.setReturnValue(false);return;}
        // Zero damage alone still enters vanilla's hurt sound, animation and knockback path.
        if((Object)this instanceof Player p&&(Rubber.masteryImmune(p,source)||Phoenix.immune(p,source)||Lightning.immune(p,source)||com.sibcraft.creeperborn.Gravity.immune(p,source)||com.sibcraft.creeperborn.StringFruit.immune(p,source)))cir.setReturnValue(false);
    }
    @Inject(method="getDamageAfterArmorAbsorb",at=@At("RETURN"),cancellable=true)
    private void creeperborn$kanaboArmor(DamageSource source,float amount,CallbackInfoReturnable<Float> cir){cir.setReturnValue(com.sibcraft.creeperborn.Azure.kanaboArmor(source,amount,cir.getReturnValue()));}
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$rubberDamage(float amount,ServerLevel level,DamageSource source,float original){
        if((Object)this instanceof Player climate&&com.sibcraft.creeperborn.ClimatePowers.piercing(climate,source))return com.sibcraft.creeperborn.ClimatePowers.damage(climate,source,amount);
        float value=(Object)this instanceof Player p ? Rubber.damage(p,source,Phoenix.damage(p,source,Lightning.damage(p,source,Dark.damage(p,source,com.sibcraft.creeperborn.Gravity.damage(p,source,com.sibcraft.creeperborn.StringFruit.damage(p,source,amount)))))) : amount;
        return (Object)this instanceof Player p?com.sibcraft.creeperborn.Azure.damage(p,source,com.sibcraft.creeperborn.ClimatePowers.damage(p,source,value)):value;
    }
}
