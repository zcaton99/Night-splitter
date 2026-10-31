package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.ZoanPowers;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(LivingEntity.class)
public abstract class ZoanDimensionsMixin {
    @Inject(method="getDimensions",at=@At("HEAD"),cancellable=true)
    private void creeperborn$zoanDimensions(Pose pose,CallbackInfoReturnable<EntityDimensions> cir){if((Object)this instanceof Player p){EntityDimensions d=ZoanPowers.dimensions(p);if(d!=null)cir.setReturnValue(d);}}
}
