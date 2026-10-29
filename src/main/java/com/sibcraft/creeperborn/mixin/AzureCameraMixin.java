package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.AzureVisuals;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
/** Request enough room for the long body; vanilla's zoom ray tests still enforce camera collision. */
@Mixin(Camera.class)
public abstract class AzureCameraMixin {
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$dragonZoom(float distance){var c=Minecraft.getInstance();return c.player!=null&&((Camera)(Object)this).entity()==c.player&&(AzureVisuals.flags(c.player.getId())&3)==2?Math.max(distance,34):distance;}
}
