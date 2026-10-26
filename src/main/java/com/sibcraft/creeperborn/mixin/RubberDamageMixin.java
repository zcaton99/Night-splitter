package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Rubber;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
@Mixin(LivingEntity.class)
public abstract class RubberDamageMixin {
    @ModifyVariable(method="hurtServer",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$rubberDamage(float amount,ServerLevel level,DamageSource source,float original){
        return (Object)this instanceof Player p ? Rubber.damage(p,source,amount) : amount;
    }
}
