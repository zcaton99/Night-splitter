package com.sibcraft.creeperborn.client;

import com.sibcraft.creeperborn.LuffyPowers;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;

/** Mouse combat yields to ordinary block interaction and held items. Z/X/C stay explicit moves. */
public final class LuffyControls {
    private LuffyControls() {}

    public static boolean attack(Minecraft client) {
        return client.player != null && LuffyPowers.overrides(client.player)
            && client.player.getMainHandItem().isEmpty()
            && (client.hitResult == null || client.hitResult.getType() != HitResult.Type.BLOCK);
    }

    public static boolean use(Minecraft client) {
        if(!attack(client)||!client.player.getOffhandItem().isEmpty())return false;
        if(client.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit
            && !(hit.getEntity() instanceof net.minecraft.world.entity.monster.Enemy)
            && !(hit.getEntity() instanceof net.minecraft.world.entity.player.Player))return false;
        return true;
    }
}
