package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.NatureVisuals;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.*;
import net.minecraft.world.level.ClipContext;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Camera.class)
public abstract class NatureCameraMixin {
    @Shadow protected abstract void setPosition(Vec3 position);
    @Shadow protected abstract void setRotation(float yaw,float pitch);
    @Inject(method="alignWithEntity",at=@At("TAIL"))
    private void creeperborn$natureFrame(float partial,CallbackInfo ci){var c=Minecraft.getInstance();var camera=(Camera)(Object)this;if(c.player==null||c.level==null||camera.entity()!=c.player||!camera.isDetached())return;int flags=NatureVisuals.flags(c.player.getId());if((flags&4)==0)return;double zoom=NatureVisuals.revealZoom();Vec3 from=c.player.getEyePosition(partial).lerp(c.player.getPosition(partial).add(0,(flags&3)==1?4:2.8,0),zoom),look=c.player.getViewVector(partial),to=from.subtract(look.scale(((flags&1024)!=0?15:11)*zoom)).add(0,2*zoom,0);var hit=c.level.clip(new ClipContext(from,to,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,c.player));Vec3 at=hit.getType()==HitResult.Type.MISS?to:hit.getLocation().lerp(from,.05);setPosition(at);Vec3 d=from.add(look.scale(5)).subtract(at);setRotation((float)Math.toDegrees(Math.atan2(-d.x,d.z)),(float)-Math.toDegrees(Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))));}
}
