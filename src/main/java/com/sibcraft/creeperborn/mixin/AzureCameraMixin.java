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
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    private boolean creeperborn$dragon(){var c=Minecraft.getInstance();return c.player!=null&&((Camera)(Object)this).entity()==c.player&&((Camera)(Object)this).isDetached()&&(AzureVisuals.flags(c.player.getId())&3)==2;}
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$dragonZoom(float distance){return creeperborn$dragon()?AzureVisuals.cameraDistance():distance;}
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void creeperborn$overHead(float partial,CallbackInfo ci){if(!creeperborn$dragon())return;var c=Minecraft.getInstance();if(c.level==null||c.player==null)return;Vec3 from=((Camera)(Object)this).position(),to=from.add(0,AzureVisuals.cameraLift(),0);var hit=c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));setPosition(hit.getType()==HitResult.Type.MISS?to:hit.getLocation().add(0,-.15,0));
        Vec3 origin=c.player.getEyePosition(),end=origin.add(c.player.getLookAngle().scale(70));var obstacle=c.level.clip(new ClipContext(origin,end,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));Vec3 aim=obstacle.getType()==HitResult.Type.MISS?end:obstacle.getLocation();double closest=origin.distanceTo(aim);
        for(var e:c.level.getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class,new net.minecraft.world.phys.AABB(origin,aim).inflate(.4))){if(e==c.player||!e.isAlive()||e.getCustomName()!=null&&e.getCustomName().getString().equals("cb_azure_hitbox:"+c.player.getUUID()))continue;var cross=e.getBoundingBox().inflate(.2).clip(origin,aim);if(cross.isPresent()&&origin.distanceTo(cross.get())<closest){aim=cross.get();closest=origin.distanceTo(aim);}}
        Vec3 delta=aim.subtract(((Camera)(Object)this).position());setRotation((float)Math.toDegrees(Math.atan2(-delta.x,delta.z)),(float)-Math.toDegrees(Math.atan2(delta.y,Math.sqrt(delta.x*delta.x+delta.z*delta.z))));}
}
