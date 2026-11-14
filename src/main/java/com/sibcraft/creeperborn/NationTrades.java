package com.sibcraft.creeperborn;

import net.minecraft.core.component.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.trading.*;

/** Nation contributions use vanilla payment, rewards, merchant XP and profession locking. */
public final class NationTrades {
    private static boolean dirty;
    public interface MenuMerchant {Merchant creeperborn$merchant();}
    private NationTrades(){}
    public static Element[] offering(MerchantOffer offer){
        if(offer==null)return null;
        String kind=Relics.kind(offer.getResult());
        if(!kind.startsWith("nation_token:"))return null;
        Element nation=Element.parse(kind.substring("nation_token:".length()));
        return nation==null?null:new Element[]{nation,nation};
    }
    public static void populate(Villager villager,MerchantOffers offers){
        if(!(villager.level() instanceof ServerLevel))return;
        Element element=Guardians.element(villager);
        Nations.Nation n=element==null?null:Nations.get(element);
        // Conversion/release must never leave an offering belonging to the previous nation.
        offers.removeIf(o->{if(Relics.kind(o.getResult()).startsWith("nation_offering:"))return true;Element[] pair=offering(o);return pair!=null&&(n==null||pair[0]!=element||!n.members.contains(villager.getUUID().toString()));});
        if(n==null||!Nations.trialsPassed()||!n.members.contains(villager.getUUID().toString()))return;
        for(Element heart:new Element[]{element}){
            MerchantOffer row=offers.stream().filter(o->{Element[] pair=offering(o);return pair!=null&&pair[0]==element&&pair[1]==heart;}).findFirst().orElse(null);
            if(row==null){
                ItemStack input=Relics.heart(heart),reward=NationItems.stack(element);
                ItemCost cost=new ItemCost(Items.HEART_OF_THE_SEA.builtInRegistryHolder(),1,DataComponentExactPredicate.builder().expect(DataComponents.CUSTOM_DATA,input.get(DataComponents.CUSTOM_DATA)).expect(DataComponents.ITEM_MODEL,input.get(DataComponents.ITEM_MODEL)).expect(DataComponents.CUSTOM_NAME,input.get(DataComponents.CUSTOM_NAME)).build(),input);
                row=new MerchantOffer(cost,reward,Integer.MAX_VALUE,1,0f);offers.add(row);
            }
            if(row.isOutOfStock())row.resetUses();
        }
    }
    /** Every token, including replacements, still requires a genuine matching heart. */
    public static boolean canTake(Merchant merchant,MerchantOffer offer,Player player){
        Element[] pair=offering(offer);if(pair==null||!(merchant instanceof Villager v)||!(v.level() instanceof ServerLevel))return true;
        Nations.Nation n=Nations.get(pair[0]);
        return n!=null&&Nations.trialsPassed()
            &&Guardians.element(v)==pair[0]&&n.members.contains(v.getUUID().toString())
            &&Nations.nearby(player,n)&&!player.isSpectator()&&player.isAlive()&&v.isAlive()&&!v.isBaby();
    }
    public static void traded(Villager v,MerchantOffer offer){
        Element[] pair=offering(offer);Player p=v.getTradingPlayer();
        if(pair!=null&&p!=null&&v.level() instanceof ServerLevel){Nations.contribute(p,pair[0],pair[1]);dirty=true;}
    }
    public static void refresh(net.minecraft.server.MinecraftServer server){
        if(!dirty)return;dirty=false;
        for(var p:server.getPlayerList().getPlayers())if(p.containerMenu instanceof net.minecraft.world.inventory.MerchantMenu menu
            &&((MenuMerchant)menu).creeperborn$merchant() instanceof Villager v){
            MerchantOffers offers=v.getOffers();menu.updateSellItem();menu.broadcastChanges();
            p.sendMerchantOffers(menu.containerId,offers,v.getVillagerData().level(),v.getVillagerXp(),v.showProgressBar(),v.canRestock());
        }
    }
}
