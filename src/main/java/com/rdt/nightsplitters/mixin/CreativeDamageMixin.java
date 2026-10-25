package com.rdt.nightsplitters.mixin;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Abilities;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Allow only tagged swarm melee hits through Creative's separate ability check. */
@Mixin(Player.class)
public abstract class CreativeDamageMixin {
    private boolean nightSplitters$isCreativeSwarmHit(DamageSource source) {
        Player self = (Player)(Object)this;
        return self.isCreative() && !self.isSpectator()
            && source.getEntity() instanceof CaveSpider spider
            && source.getDirectEntity() == spider
            && spider.entityTags().contains("ns_swarm");
    }

    @ModifyVariable(method = "hurtServer", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private float nightSplitters$halveCreativeDamage(float amount, ServerLevel level, DamageSource source) {
        return nightSplitters$isCreativeSwarmHit(source) ? amount * 0.5F : amount;
    }

    @Redirect(method = "hurtServer", at = @At(value = "FIELD",
        target = "Lnet/minecraft/world/entity/player/Abilities;invulnerable:Z"))
    private boolean nightSplitters$allowCreativeSwarmHit(Abilities abilities,
            ServerLevel level, DamageSource source, float amount) {
        // Change this one read, never the player's actual abilities or game mode.
        return abilities.invulnerable && !nightSplitters$isCreativeSwarmHit(source);
    }
}
