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
        var offers=v.getOffers();check(offers.size()==2&&other.getOffers().size()==2,"Novice and master retain normal trades and get one matching-heart token trade");
        v.getOffers();check(offers.size()==2,"Reopening does not duplicate the token trade");
        var menu=new MerchantMenu(20,player.getInventory(),v);var second=new MerchantMenu(21,player.getInventory(),other);
        var row=offers.get(1);var cost=row.getItemCostA();var wireCost=new ItemCost(cost.item(),cost.count(),cost.components());
        check(wireCost.itemStack().getHoverName().getString().contains(nation.title)&&wireCost.itemStack().get(net.minecraft.core.component.DataComponents.ITEM_MODEL)!=null,"Trade payment keeps its elemental name and icon on the client");
        menu.setSelectionHint(1);menu.getSlot(0).set(new ItemStack(Items.HEART_OF_THE_SEA));menu.slotsChanged(menu.getSlot(0).container);
        check(!menu.getSlot(2).hasItem(),"Ordinary hearts cannot buy Nation Tokens");
        for(Element wrong:Element.values())if(wrong!=nation){menu.getSlot(0).set(Relics.heart(wrong));menu.slotsChanged(menu.getSlot(0).container);check(!menu.getSlot(2).hasItem(),"Wrong-element heart cannot buy "+nation.title+" token");}
        second.setSelectionHint(1);second.getSlot(0).set(Relics.heart(nation));second.slotsChanged(second.getSlot(0).container);
        menu.getSlot(0).set(Relics.heart(nation));menu.slotsChanged(menu.getSlot(0).container);
        check(menu.getSlot(2).mayPickup(player),"Genuine matching heart enables native trade pickup");menu.clicked(2,0,ContainerInput.PICKUP,player);
        check(menu.getCarried().is(NationItems.token(nation))&&menu.getSlot(0).getItem().isEmpty(),"Native trade consumes exactly one heart and gives the matching registered Nation Token");
        player.addItem(menu.getCarried().copy());menu.setCarried(ItemStack.EMPTY);
        check(n.offered&&n.offerings.size()==1&&v.getVillagerXp()>0,"One matching offering empowers the nation and locks the trader's profession");
        check(second.getSlot(2).mayPickup(player),"Another trader can supply a replacement token for another matching heart");
        check(second.quickMoveStack(player,2).is(NationItems.token(nation))&&!second.getSlot(0).hasItem(),"Native shift-click replacement consumes its own heart");
        check(!second.getSlot(2).hasItem(),"A replacement cannot be taken without another payment");
        Nations.load(level.getServer());check(Nations.get(nation).offered,"Matching-heart completion survives reload");
        check(offers.getFirst().getUses()==0,"Ordinary trades are unaffected");
        v.setTradingPlayer(null);other.setTradingPlayer(null);
    }
}
