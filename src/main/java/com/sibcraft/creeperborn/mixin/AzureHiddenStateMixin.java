package com.sibcraft.creeperborn.mixin;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
@Mixin(EntityRenderState.class)
public abstract class AzureHiddenStateMixin implements com.sibcraft.creeperborn.client.AzureHiddenState,com.sibcraft.creeperborn.client.CreeperBadgeState {
    @Unique private String creeperborn$badge="";
    @Unique private final net.minecraft.client.renderer.item.ItemStackRenderState creeperborn$badgeItem=new net.minecraft.client.renderer.item.ItemStackRenderState();
    public net.minecraft.client.renderer.item.ItemStackRenderState creeperborn$badgeItem(){return creeperborn$badgeItem;}
    public String creeperborn$badge(){return creeperborn$badge;}
    public void creeperborn$badge(String key){creeperborn$badge=key;}
    @Unique private boolean creeperborn$azureHidden;
    public boolean creeperborn$azureHidden(){return creeperborn$azureHidden;}
    public void creeperborn$azureHidden(boolean value){creeperborn$azureHidden=value;}
}
