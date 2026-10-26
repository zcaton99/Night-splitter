package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.WaterRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class FruitWaterPlayerMixin {
    @Inject(method="travel",at=@At("HEAD"),cancellable=true)
    private void creeperborn$sinkInsteadOfSwimming(Vec3 input,CallbackInfo ci){
        Player p=(Player)(Object)this;
        if(WaterRules.suppressed(p)&&!p.isPassenger()){WaterRules.travel(p,input);ci.cancel();}
    }
}

