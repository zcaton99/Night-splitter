package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.StringVisuals;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Player.class)
public abstract class StringClientMovementMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringCameraBody(Vec3 input,CallbackInfo ci){if((Object)this==Minecraft.getInstance().player&&StringVisuals.controlling()){((Player)(Object)this).setDeltaMovement(Vec3.ZERO);ci.cancel();}}
}
