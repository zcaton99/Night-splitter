package com.sibcraft.creeperborn.mixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.world.entity.LivingEntity;
import com.sibcraft.creeperborn.WaterRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(LivingEntityRenderer.class)
public abstract class DoorGhostMixin {
    @Inject(method="extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V",at=@At("TAIL"))
    private void creeperborn$ghosts(LivingEntity entity,LivingEntityRenderState state,float partialTick,CallbackInfo ci){
        var p=Minecraft.getInstance().player;
        if(p!=null&&entity!=p&&(WaterRules.flags(p)&4)!=0){
            state.isInvisible=true;state.isInvisibleToPlayer=false;state.outlineColor=0xFF00DDEB;state.hasRedOverlay=false;
        }
    }
}
