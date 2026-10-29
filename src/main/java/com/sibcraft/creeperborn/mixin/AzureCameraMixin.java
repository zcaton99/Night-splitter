package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.AzureVisuals;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Wide reveal settles above/behind the head. Vanilla zoom and an upward ray retain terrain collision. */
@Mixin(Camera.class)
public abstract class AzureCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    private boolean creeperborn$dragon(){var c=Minecraft.getInstance();return c.player!=null&&((Camera)(Object)this).entity()==c.player&&((Camera)(Object)this).isDetached()&&(AzureVisuals.flags(c.player.getId())&3)==2;}
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$dragonZoom(float distance){return creeperborn$dragon()?AzureVisuals.cameraDistance():distance;}
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void creeperborn$overHead(float partial,CallbackInfo ci){if(!creeperborn$dragon())return;var c=Minecraft.getInstance();if(c.level==null||c.player==null)return;Vec3 from=((Camera)(Object)this).position(),to=from.add(0,AzureVisuals.cameraLift(),0);var hit=c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));setPosition(hit.getType()==HitResult.Type.MISS?to:hit.getLocation().add(0,-.15,0));}
}
