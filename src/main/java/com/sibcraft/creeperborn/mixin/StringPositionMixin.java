package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.StringFruit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class StringPositionMixin {
    @Inject(method="setPos(DDD)V",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringBoundary(double x,double y,double z,CallbackInfo ci){if(StringFruit.blocked((Entity)(Object)this,new Vec3(x,y,z)))ci.cancel();}
}
