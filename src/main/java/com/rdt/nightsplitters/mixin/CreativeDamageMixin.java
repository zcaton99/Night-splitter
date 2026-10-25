package com.rdt.nightsplitters.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Restrict the Creative immunity exception to attacks from Night Splitters spiders. */
@Mixin(Player.class)
public abstract class CreativeDamageMixin {
    /** Halve incoming damage only when a tagged Night Splitters spider attacks a Creative player. */
    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float nightSplitters$halveCreativeDamage(float amount, ServerLevel level, DamageSource source) {
        Player self = (Player)(Object)this;
        Entity attacker = source.getEntity();
        if (self.isCreative() && !self.isSpectator()
                && attacker instanceof CaveSpider spider
                && spider.entityTags().contains("ns_swarm")) {
            return amount * 0.5F;
        }
        return amount;
    }

    @Inject(method = "isInvulnerableTo", at = @At("HEAD"), cancellable = true)
    private void nightSplitters$creativeSpiderDamage(ServerLevel level, DamageSource source, CallbackInfoReturnable<Boolean> cir) {
        Player self = (Player)(Object)this;
        Entity attacker = source.getEntity();
        if (self.isCreative() && !self.isSpectator()
                && attacker instanceof CaveSpider spider
                && spider.entityTags().contains("ns_swarm")) {
            cir.setReturnValue(false);
        }
    }
}
