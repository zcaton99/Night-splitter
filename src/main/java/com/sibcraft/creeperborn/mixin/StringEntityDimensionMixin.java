package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.StringFruit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class StringEntityDimensionMixin {
    @Inject(method="teleport",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringPortal(CallbackInfoReturnable<Entity> cir){Entity self=(Entity)(Object)this;if(self.level() instanceof ServerLevel&&StringFruit.insideCage(self))cir.setReturnValue(null);}
}
