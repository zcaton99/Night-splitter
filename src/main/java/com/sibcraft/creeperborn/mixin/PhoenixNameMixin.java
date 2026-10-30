package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.client.PhoenixVisuals;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(EntityRenderer.class)
public abstract class PhoenixNameMixin{
    @Inject(method="extractNameTags(Lnet/minecraft/world/entity/Entity;Lnet/minecraft/client/renderer/entity/state/EntityRenderState;FDD)V",at=@At("RETURN"))
    private void creeperborn$hidePhoenixName(Entity entity,EntityRenderState state,float partialTick,double nameRange,double scoreRange,CallbackInfo ci){
        ((com.sibcraft.creeperborn.client.AzureHiddenState)state).creeperborn$azureHidden(entity.getCustomName()!=null&&(entity.getCustomName().getString().startsWith("cb_azure_hitbox:")||entity.getCustomName().getString().startsWith("cb_nature_limb:")||entity.getCustomName().getString().startsWith("cb_nature_body:")));
        if(entity instanceof Player)PhoenixVisuals.hideIdentity(state,PhoenixVisuals.flags(entity.getId()));
    }
}
