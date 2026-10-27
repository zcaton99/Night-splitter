package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.FruitClaims;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Entity.class)
public abstract class CapturedFruitCleanupMixin{
    @Inject(method="remove",at=@At("HEAD"))
    private void creeperborn$releaseDestroyedCapture(Entity.RemovalReason reason,CallbackInfo ci){
        if((Object)this instanceof ItemEntity item&&item.level() instanceof ServerLevel&&reason.shouldDestroy()&&!item.getItem().isEmpty())FruitClaims.releaseItem(item.getItem());
    }
}
