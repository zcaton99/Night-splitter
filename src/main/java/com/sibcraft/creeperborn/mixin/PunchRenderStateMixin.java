package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.client.PunchRenderer;
import com.sibcraft.creeperborn.client.PunchVisualState;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockDisplayEntityRenderState.class)
public abstract class PunchRenderStateMixin implements PunchVisualState,com.sibcraft.creeperborn.client.BoltVisualState {
    @Unique private PunchRenderer.SkinData creeperborn$punch;
    @Unique private float creeperborn$boltLength=1;
    public float creeperborn$boltLength(){return creeperborn$boltLength;}
    public void creeperborn$boltLength(float length){creeperborn$boltLength=length;}
    @Unique private int creeperborn$bolt=-1;
    public int creeperborn$bolt(){return creeperborn$bolt;}
    public void creeperborn$bolt(int variant){creeperborn$bolt=variant;}
    public PunchRenderer.SkinData creeperborn$punch(){return creeperborn$punch;}
    public void creeperborn$punch(PunchRenderer.SkinData data){creeperborn$punch=data;}
}
