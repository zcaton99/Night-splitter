package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.*;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class ZoanCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @ModifyVariable(method="getMaxZoom",at=@At("HEAD"),argsOnly=true,ordinal=0)
    private float creeperborn$zoanZoom(float distance){var c=Minecraft.getInstance();if(c.player==null||((Camera)(Object)this).entity()!=c.player)return distance;int f=ZoanVisuals.flags(c.player.getId());if(((f>>2)&3)==3)return 4;if((f&3)==2&&((f>>2)&3)==1)return 3;return distance;}
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void creeperborn$headAim(float partial,CallbackInfo ci){var c=Minecraft.getInstance();var cam=(Camera)(Object)this;if(c.player==null||c.level==null||cam.entity()!=c.player)return;int f=ZoanVisuals.flags(c.player.getId());if(((f>>2)&3)!=3)return;
        if((f&3)==1&&ZoanVisuals.revealingFox()&&cam.isDetached()){float blend=ZoanVisuals.foxRevealBlend(partial);Vec3 eye=com.sibcraft.creeperborn.ZoanPowers.foxViewEye(c.player,partial),look=c.player.getViewVector(partial),flat=look.multiply(1,0,1).normalize();if(flat.lengthSqr()<.01)flat=new Vec3(0,0,1);Vec3 wide=c.player.getPosition(partial).subtract(flat.scale(22)).add(0,10,0),to=wide.lerp(eye,blend);var wall=c.level.clip(new ClipContext(eye,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));Vec3 at=wall.getType()==HitResult.Type.MISS?to:wall.getLocation().lerp(eye,.08);setPosition(at);Vec3 focus=c.player.getPosition(partial).add(0,4,0).lerp(eye.add(look.scale(60)),blend),d=focus.subtract(at);setRotation((float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));return;}
        Vec3 eye=(f&3)==1?com.sibcraft.creeperborn.ZoanPowers.foxViewEye(c.player,partial):c.player.getEyePosition(partial),look=c.player.getViewVector(partial);if(!cam.isDetached()){setPosition(eye);setRotation(c.player.getViewYRot(partial),c.player.getViewXRot(partial));return;}
        Vec3 to=eye.subtract(look.scale(3.5)).add(0,(f&3)==1?1.6:1.1,0);var wall=c.level.clip(new ClipContext(eye,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));Vec3 at=wall.getType()==HitResult.Type.MISS?to:wall.getLocation().lerp(eye,.08);setPosition(at);Vec3 aim=eye.add(look.scale(60));var hit=c.level.clip(new ClipContext(eye,aim,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));if(hit.getType()!=HitResult.Type.MISS)aim=hit.getLocation();Vec3 d=aim.subtract(at);setRotation((float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,d.horizontalDistance())));
    }
}
