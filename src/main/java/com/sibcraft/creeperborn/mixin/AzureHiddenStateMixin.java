package com.sibcraft.creeperborn.mixin;
import org.spongepowered.asm.mixin.*;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
@Mixin(EntityRenderState.class)
public abstract class AzureHiddenStateMixin implements com.sibcraft.creeperborn.client.AzureHiddenState {
    @Unique private boolean creeperborn$azureHidden;
    public boolean creeperborn$azureHidden(){return creeperborn$azureHidden;}
    public void creeperborn$azureHidden(boolean value){creeperborn$azureHidden=value;}
}
