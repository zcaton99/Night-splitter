package com.rdt.nightsplitters.mixin;

import com.rdt.nightsplitters.SwarmEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.spider.CaveSpider;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(CaveSpider.class)
public abstract class SwarmHitMixin {
    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void nightSplitters$effectOnly(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        CaveSpider self = (CaveSpider)(Object)this;
        if (SwarmEffects.darknessOnly(self) && target instanceof Player) {
            // Skip vanilla damage AND cave-spider poison in Darkness-only mode.
            cir.setReturnValue(SwarmEffects.applyDarkness(self, target, true));
        }
    }

    @Inject(method = "doHurtTarget", at = @At("RETURN"))
    private void nightSplitters$darkHit(ServerLevel level, Entity target, CallbackInfoReturnable<Boolean> cir) {
        CaveSpider self = (CaveSpider)(Object)this;
        if (cir.getReturnValueZ() && !SwarmEffects.darknessOnly(self))
            SwarmEffects.applyDarkness(self, target, false);
    }
}
