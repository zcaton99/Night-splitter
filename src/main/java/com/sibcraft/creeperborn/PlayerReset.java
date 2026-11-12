package com.sibcraft.creeperborn;

import java.util.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

/** Explicit operator testing reset, scoped to one player rather than shared world progress. */
public final class PlayerReset {
    private PlayerReset() {}
    private static boolean powerItem(ItemStack item) {
        if (!Relics.kind(item).isEmpty()) return true;
        var model=item.get(DataComponents.ITEM_MODEL);
        return model!=null && model.getNamespace().equals("creeperborn");
    }
    public static void apply(Player p) {
        // Restore saved movement/game mode before deleting the tags used for recovery.
        FormCarry.cancel(p);
        TimePowers.forget(p);ZoanPowers.forget(p);CelestialPowers.forget(p);NaturePowers.forget(p);
        Azure.forget(p);Rubber.forget(p);Phoenix.forget(p);Lightning.forget(p);
        Dark.resetCaptures(p);Dark.forget(p);Op.forget(p);Hearts.reset(p);
        Gravity.forget(p);StringFruit.forget(p);Paw.forget(p);Tremor.forget(p);
        PairPowers.forget(p);ClimatePowers.forget(p);DoorSpace.forget(p);DoorFruit.forget(p);
        Creeperborn.ABILITIES.forget(p);Mastery.forget(p);FlightReconciliation.forget(p.getUUID());
        Azure.resetGrace(p);Paw.resetGrace(p);Tremor.resetGrace(p);NaturePowers.resetGrace(p);
        CelestialPowers.resetGrace(p);PairPowers.resetGrace(p);ClimatePowers.resetGrace(p);Phoenix.resetGrace(p);
        HolyBag.reset(p);
        for(String fruit:Dark.FRUITS)if(p.getUUID().equals(FruitClaims.owner(fruit)))FruitClaims.setOwner(fruit,null);
        for(String tag:Set.copyOf(p.entityTags()))if(tag.startsWith("cb_"))p.removeTag(tag);
        for(int i=0;i<p.getInventory().getContainerSize();i++)if(powerItem(p.getInventory().getItem(i)))p.getInventory().setItem(i,ItemStack.EMPTY);
        if(powerItem(p.containerMenu.getCarried()))p.containerMenu.setCarried(ItemStack.EMPTY);
        for(var attribute:p.getAttributes().getSyncableAttributes())for(var modifier:Set.copyOf(attribute.getModifiers()))
            if(modifier.id().getNamespace().equals("creeperborn"))attribute.removeModifier(modifier.id());
        p.removeAllEffects();p.setAbsorptionAmount(0);p.clearFire();p.setTicksFrozen(0);
        p.setInvisible(false);p.noPhysics=p.isSpectator();p.refreshDimensions();
        p.getAbilities().mayfly=p.gameMode()==GameType.CREATIVE||p.isSpectator();
        if(!p.getAbilities().mayfly)p.getAbilities().flying=false;
        p.getAbilities().setFlyingSpeed(.05f);p.onUpdateAbilities();p.resetFallDistance();
        p.setHealth(Math.min(p.getHealth(),p.getMaxHealth()));
        p.getInventory().setChanged();p.containerMenu.broadcastChanges();
        WaterRules.forget(p);CombatNetwork.forget(p);LuffyPowers.refresh(p);
        if(p instanceof ServerPlayer sp){
            if(ServerPlayNetworking.canSend(sp,ControlPayload.TYPE))ServerPlayNetworking.send(sp,new ControlPayload(3,-1));
            if(ServerPlayNetworking.canSend(sp,WaterStatePayload.TYPE))ServerPlayNetworking.send(sp,new WaterStatePayload(0));
        }
    }
}
