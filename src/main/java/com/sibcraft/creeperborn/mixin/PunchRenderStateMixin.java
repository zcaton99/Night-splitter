package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.client.PunchRenderer;
import com.sibcraft.creeperborn.client.PunchVisualState;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockDisplayEntityRenderState.class)
public abstract class PunchRenderStateMixin implements PunchVisualState {
    @Unique private PunchRenderer.SkinData creeperborn$punch;
    public PunchRenderer.SkinData creeperborn$punch(){return creeperborn$punch;}
    public void creeperborn$punch(PunchRenderer.SkinData data){creeperborn$punch=data;}
}

