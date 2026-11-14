package com.sibcraft.creeperborn;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.npc.villager.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.trading.*;
public final class NationTradeRegression {
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);System.out.println("NATION_TRADE_CHECK: "+text);}
    static void run(ServerLevel level,Player player,Element nation){
        var n=Nations.get(nation);var guards=Nations.living(level,n);Villager v=guards.getFirst(),other=guards.get(1);
        for(var merchant:java.util.List.of(v,other)){
            merchant.setVillagerData(merchant.getVillagerData().withProfession(level.registryAccess(),VillagerProfession.CLERIC).withLevel(merchant==v?1:5));
            merchant.setVillagerXp(0);merchant.setTradingPlayer(player);
            var normal=new MerchantOffers();normal.add(new MerchantOffer(new ItemCost(Items.EMERALD,2),new ItemStack(Items.BREAD,3),12,5,.05f));merchant.setOffers(normal);
        }
        var offers=v.getOffers();check(offers.size()==5&&other.getOffers().size()==5,"Novice and master retain normal trades and receive four offering rows");
        v.getOffers();check(offers.size()==5,"Reopening does not duplicate offerings");
        var menu=new MerchantMenu(20,player.getInventory(),v);var stale=new MerchantMenu(21,player.getInventory(),other);
        for(Element heart:Element.values()){
            var row=offers.stream().filter(o->{var pair=NationTrades.offering(o);return pair!=null&&pair[1]==heart;}).findFirst().orElseThrow();int index=offers.indexOf(row);
            menu.setSelectionHint(index);menu.getSlot(0).set(new ItemStack(Items.HEART_OF_THE_SEA));menu.slotsChanged(menu.getSlot(0).container);
            check(menu.getSlot(2).getItem().isEmpty(),"Vanilla hearts cannot substitute for "+heart.key()+" hearts");
            stale.setSelectionHint(index);stale.getSlot(0).set(Relics.heart(heart));stale.slotsChanged(stale.getSlot(0).container);
            check(stale.getSlot(2).hasItem(),"Second villager has the unclaimed offering ready");
            menu.getSlot(0).set(Relics.heart(heart));menu.slotsChanged(menu.getSlot(0).container);
            check(menu.getSlot(2).mayPickup(player),"Genuine heart enables native result pickup");
            menu.clicked(2,0,ClickType.PICKUP,player);
            check(menu.getCarried().is(Items.EMERALD)&&menu.getSlot(0).getItem().isEmpty(),"Native pickup consumes exactly one heart and delivers its emerald");menu.setCarried(ItemStack.EMPTY);
            check(n.offerings.contains(heart.key())&&v.getVillagerXp()>0,"Accepted trade persists contribution and locks the profession with XP");
            check(!stale.getSlot(2).mayPickup(player)&&stale.quickMoveStack(player,2).isEmpty(),"An already-open second menu cannot repeat the shared offering, even with shift-click");
            check(stale.getSlot(0).getItem().getCount()==1,"Rejected duplicate leaves payment intact");
            other.getOffers();check(other.getOffers().get(index).isOutOfStock(),"Other villagers display shared completion");
            row.resetUses();v.getOffers();check(row.isOutOfStock(),"Villager restocking cannot reset a completed nation contribution");
            Nations.load(level.getServer());n=Nations.get(nation);check(n.offerings.contains(heart.key()),"Partial offerings survive ledger reload");
        }
        check(n.offered,"Four native trades empower the nation");
        check(offers.getFirst().getUses()==0,"Ordinary trades are unaffected");
        v.setTradingPlayer(null);other.setTradingPlayer(null);
    }
}
