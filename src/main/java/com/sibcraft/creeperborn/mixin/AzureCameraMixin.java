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
/** Wide cruising view frames the skull; looking down blends into a closer, target-aligned aiming view. */
@Mixin(Camera.class)
public abstract class AzureCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    private boolean creeperborn$dragon(){var c=Minecraft.getInstance();return c.player!=null&&((Camera)(Object)this).entity()==c.player&&((Camera)(Object)this).isDetached()&&(AzureVisuals.flags(c.player.getId())&3)==2;}
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$dragonZoom(float distance){return creeperborn$dragon()?AzureVisuals.cameraDistance():distance;}
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void creeperborn$overHead(float partial,CallbackInfo ci){if(!creeperborn$dragon()){AzureVisuals.cameraPosition(null);return;}var c=Minecraft.getInstance();if(c.level==null||c.player==null)return;Vec3 from=c.player.getEyePosition(partial);Vec3 to=from.add(AzureVisuals.cameraOffset(c.player.getViewYRot(partial),c.player.getViewXRot(partial)));var hit=c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));setPosition(hit.getType()==HitResult.Type.MISS?to:hit.getLocation().add(0,-.15,0));
        Vec3 origin=c.player.getEyePosition(),end=origin.add(c.player.getLookAngle().scale(70));var obstacle=c.level.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));Vec3 aim=obstacle.getType()==HitResult.Type.MISS?end:obstacle.getLocation();double closest=origin.distanceTo(aim);
        for(var e:c.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(origin,aim).inflate(.4))){if(e==c.player||!e.isAlive()||e.getCustomName()!=null&&e.getCustomName().getString().equals("cb_azure_hitbox:"+c.player.getUUID()))continue;var cross=e.getBoundingBox().inflate(.2).clip(origin,aim);if(cross.isPresent()&&origin.distanceTo(cross.get())<closest){aim=cross.get();closest=origin.distanceTo(aim);}}
        Vec3 cruise=c.player.getPosition(partial).add(0,1,0).add(c.player.getViewVector(partial).scale(3));Vec3 framed=cruise.lerp(aim,AzureVisuals.aimBlend(c.player.getViewXRot(partial)));
        Vec3 delta=framed.subtract(((Camera)(Object)this).position());setRotation((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));AzureVisuals.cameraPosition(((Camera)(Object)this).position());}
}
