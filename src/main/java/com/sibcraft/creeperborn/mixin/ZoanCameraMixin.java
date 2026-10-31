package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
@Mixin(Camera.class)
public abstract class ZoanCameraMixin {
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$zoanZoom(float distance){var c=Minecraft.getInstance();if(c.player==null||((Camera)(Object)this).entity()!=c.player)return distance;int f=ZoanVisuals.flags(c.player.getId());if(((f>>2)&3)==3)return (f&3)==1?15:10;if((f&3)==2&&((f>>2)&3)==1)return 3;return distance;}
}
