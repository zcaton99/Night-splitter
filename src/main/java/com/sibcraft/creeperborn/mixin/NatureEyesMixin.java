package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.NaturePowers;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(Entity.class)
public abstract class NatureEyesMixin {
    @Inject(method="getEyePosition()Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true)
    private void creeperborn$giantEyes(CallbackInfoReturnable<Vec3> cir){if((Object)this instanceof Player p){double h=com.sibcraft.creeperborn.ZoanPowers.headHeight(p);if(h==0)h=Math.max(NaturePowers.headHeight(p),com.sibcraft.creeperborn.CelestialPowers.headHeight(p));if(h>0)cir.setReturnValue(p.position().add(0,h,0));}}
    @Inject(method="getEyePosition(F)Lnet/minecraft/world/phys/Vec3;",at=@At("HEAD"),cancellable=true)
    private void creeperborn$giantInterpolatedEyes(float partial,CallbackInfoReturnable<Vec3> cir){if((Object)this instanceof Player p){double h=com.sibcraft.creeperborn.ZoanPowers.headHeight(p);if(h==0)h=Math.max(NaturePowers.headHeight(p),com.sibcraft.creeperborn.CelestialPowers.headHeight(p));if(h>0)cir.setReturnValue(p.getPosition(partial).add(0,h,0));}}
}
