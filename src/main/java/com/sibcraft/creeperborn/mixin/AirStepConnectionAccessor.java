package com.sibcraft.creeperborn.mixin;

import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerGamePacketListenerImpl.class)
public interface AirStepConnectionAccessor {
    @Accessor("aboveGroundTickCount")
    void creeperborn$resetAirStepCounter(int ticks);
}

