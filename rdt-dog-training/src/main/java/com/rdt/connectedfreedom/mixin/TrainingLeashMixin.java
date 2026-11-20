package com.rdt.connectedfreedom.mixin;
import com.rdt.connectedfreedom.DogTraining;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Leashable.class)
public interface TrainingLeashMixin {
    @Inject(method="tickLeash",at=@At("HEAD"),cancellable=true)
    private static <E extends Entity & Leashable> void rdt$longLine(ServerLevel level,E entity,CallbackInfo ci){if(DogTraining.managed(entity))ci.cancel();}
}
