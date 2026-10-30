package com.sibcraft.creeperborn.mixin;

import com.sibcraft.creeperborn.client.PunchRenderer;
import com.sibcraft.creeperborn.client.PunchVisualState;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(BlockDisplayEntityRenderState.class)
public abstract class PunchRenderStateMixin implements PunchVisualState,com.sibcraft.creeperborn.client.BoltVisualState {
    @Unique private int creeperborn$celestialKind;
    public int creeperborn$celestialKind(){return creeperborn$celestialKind;}public void creeperborn$celestialKind(int v){creeperborn$celestialKind=v;}
    @Unique private int creeperborn$natureKind,creeperborn$naturePalette;
    public int creeperborn$natureKind(){return creeperborn$natureKind;}public void creeperborn$natureKind(int v){creeperborn$natureKind=v;}
    public int creeperborn$naturePalette(){return creeperborn$naturePalette;}public void creeperborn$naturePalette(int v){creeperborn$naturePalette=v;}
    @Unique private int creeperborn$fireKind;
    public int creeperborn$fireKind(){return creeperborn$fireKind;}public void creeperborn$fireKind(int value){creeperborn$fireKind=value;}
    @Unique private float creeperborn$fireballLength=1;
    public float creeperborn$fireballLength(){return creeperborn$fireballLength;}public void creeperborn$fireballLength(float length){creeperborn$fireballLength=length;}
    @Unique private int creeperborn$waterKind;
    public int creeperborn$waterKind(){return creeperborn$waterKind;}public void creeperborn$waterKind(int kind){creeperborn$waterKind=kind;}
    @Unique private float creeperborn$waterLength=1;
    public float creeperborn$waterLength(){return creeperborn$waterLength;}public void creeperborn$waterLength(float length){creeperborn$waterLength=length;}
    @Unique private int creeperborn$azureKind;
    public int creeperborn$azureKind(){return creeperborn$azureKind;}
    public void creeperborn$azureKind(int value){creeperborn$azureKind=value;}
    @Unique private int creeperborn$climateKind;
    public int creeperborn$climateKind(){return creeperborn$climateKind;}
    public void creeperborn$climateKind(int kind){creeperborn$climateKind=kind;}
    @Unique private int creeperborn$pairKind;
    public int creeperborn$pairKind(){return creeperborn$pairKind;}
    public void creeperborn$pairKind(int kind){creeperborn$pairKind=kind;}
    @Unique private int creeperborn$tremorKind;
    public int creeperborn$tremorKind(){return creeperborn$tremorKind;}
    public void creeperborn$tremorKind(int kind){creeperborn$tremorKind=kind;}
    @Unique private int creeperborn$pawKind;
    public int creeperborn$pawKind(){return creeperborn$pawKind;}
    public void creeperborn$pawKind(int kind){creeperborn$pawKind=kind;}
    @Unique private int creeperborn$stringKind;
    public int creeperborn$stringKind(){return creeperborn$stringKind;}
    public void creeperborn$stringKind(int kind){creeperborn$stringKind=kind;}
    @Unique private int creeperborn$gravity;
    public int creeperborn$gravity(){return creeperborn$gravity;}
    public void creeperborn$gravity(int kind){creeperborn$gravity=kind;}
    @Unique private boolean creeperborn$opRoom;
    public boolean creeperborn$opRoom(){return creeperborn$opRoom;}
    public void creeperborn$opRoom(boolean value){creeperborn$opRoom=value;}
    @Unique private String creeperborn$darkGround="";
    public String creeperborn$darkGround(){return creeperborn$darkGround;}
    public void creeperborn$darkGround(String data){creeperborn$darkGround=data;}
    @Unique private boolean creeperborn$darkVoid;
    public boolean creeperborn$darkVoid(){return creeperborn$darkVoid;}
    public void creeperborn$darkVoid(boolean value){creeperborn$darkVoid=value;}
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
