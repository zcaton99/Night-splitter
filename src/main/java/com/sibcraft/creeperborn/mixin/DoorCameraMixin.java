package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.DoorSpace;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ServerPlayer.class)
public abstract class DoorCameraMixin {
    @Inject(method="setCamera",at=@At("HEAD"),cancellable=true)
    private void creeperborn$keepOwnCamera(Entity camera,CallbackInfo ci){
        if(camera!=(Object)this&&DoorSpace.active((ServerPlayer)(Object)this))ci.cancel();
    }
}
