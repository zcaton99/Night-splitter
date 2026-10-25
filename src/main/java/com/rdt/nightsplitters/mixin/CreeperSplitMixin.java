package com.rdt.nightsplitters.mixin;

import com.rdt.nightsplitters.CreeperEncounters;
import com.rdt.nightsplitters.NightSplitters;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Creeper.class)
public abstract class CreeperSplitMixin {
    @Inject(method = "explodeCreeper", at = @At("RETURN"))
    private void nightSplitters$scheduleBrood(CallbackInfo ci) {
        NightSplitters.encounters().exploded((Creeper)(Object)this);
    }

    @ModifyArg(method = "explodeCreeper", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/server/level/ServerLevel;explode(Lnet/minecraft/world/entity/Entity;DDDFLnet/minecraft/world/level/Level$ExplosionInteraction;)V"), index = 5)
    private Level.ExplosionInteraction nightSplitters$blockDamage(Level.ExplosionInteraction original) {
        Creeper self = (Creeper)(Object)this;
        if (!self.entityTags().contains(CreeperEncounters.MARK)) return original;
        boolean damage = self.entityTags().contains(CreeperEncounters.PARENT)
            ? NightSplitters.settings().creeperBlockDamage : NightSplitters.settings().babyCreeperBlockDamage;
        return damage ? original : Level.ExplosionInteraction.NONE;
    }
}
