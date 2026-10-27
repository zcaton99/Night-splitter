package com.sibcraft.creeperborn.mixin;
import java.util.Set;
import com.sibcraft.creeperborn.StringFruit;
import net.minecraft.server.level.*;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayer.class)
public abstract class StringTeleportMixin {
    @Inject(method="teleportTo(Lnet/minecraft/server/level/ServerLevel;DDDLjava/util/Set;FFZ)Z",at=@At("HEAD"),cancellable=true)
    private void creeperborn$stringTeleport(ServerLevel level,double x,double y,double z,Set<Relative> relative,float yaw,float pitch,boolean reset,CallbackInfoReturnable<Boolean> cir){
        ServerPlayer p=(ServerPlayer)(Object)this;Vec3 to=new Vec3(relative.contains(Relative.X)?p.getX()+x:x,relative.contains(Relative.Y)?p.getY()+y:y,relative.contains(Relative.Z)?p.getZ()+z:z);
        if(!StringFruit.canTeleport(p,level,to))cir.setReturnValue(false);
    }
}
