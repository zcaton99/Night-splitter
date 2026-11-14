package com.sibcraft.creeperborn;
import java.util.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
public final class CovenantRegression {
    private static void check(boolean value,String text){if(!value)throw new AssertionError(text);System.out.println("COVENANT_CHECK: "+text);}
    static void run(ServerLevel level,Player player){
        var manager=level.getServer().getRecipeManager();
        var holder=manager.byKey(NationItems.COVENANT_RECIPE).orElseThrow(()->new AssertionError("Covenant datapack recipe failed to load"));
        var recipe=(ShapedRecipe)holder.value();
        check(recipe.getWidth()==3&&recipe.getHeight()==3&&!recipe.isSpecial()&&!recipe.display().isEmpty(),"Covenant has a normal visible 3x3 recipe-book blueprint");
        var items=new ArrayList<ItemStack>(List.of(NationItems.stack(Element.EARTH),new ItemStack(Items.END_CRYSTAL),NationItems.stack(Element.WATER),new ItemStack(Items.END_CRYSTAL),new ItemStack(Items.NETHER_STAR),new ItemStack(Items.END_CRYSTAL),NationItems.stack(Element.FIRE),new ItemStack(Items.END_CRYSTAL),NationItems.stack(Element.AIR)));
        check(recipe.matches(CraftingInput.of(3,3,items),level),"Four different corner tokens, four edge crystals and center star match the loaded recipe");
        var n=Nations.get(Element.AIR);n.offered=false;
        check(!recipe.matches(CraftingInput.of(3,3,items),level),"Server rejects Covenant crafting until all four nations are empowered");n.offered=true;
        ItemStack air=items.get(8);items.set(8,NationItems.stack(Element.EARTH));check(!recipe.matches(CraftingInput.of(3,3,items),level),"Duplicate tokens cannot replace a missing nation");items.set(8,air);
        items.set(4,ItemStack.EMPTY);check(!recipe.matches(CraftingInput.of(3,3,items),level),"The Nether Star is required");items.set(4,new ItemStack(Items.NETHER_STAR));
        items.set(1,new ItemStack(Items.GLASS));check(!recipe.matches(CraftingInput.of(3,3,items),level),"Each edge requires an End Crystal");items.set(1,new ItemStack(Items.END_CRYSTAL));
        var dummy=new AbstractContainerMenu(null,77){public boolean stillValid(Player p){return true;}public ItemStack quickMoveStack(Player p,int slot){return ItemStack.EMPTY;}};
        var grid=new TransientCraftingContainer(dummy,3,3);for(int i=0;i<9;i++){var item=items.get(i).copy();item.setCount(2);grid.setItem(i,item);}
        var result=new ResultContainer();var slot=new ResultSlot(player,grid,result,0,0,0);
        for(int craft=0;craft<2;craft++){
            var found=manager.getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),level).orElseThrow();
            ItemStack covenant=found.value().assemble(grid.asCraftInput());
            check(Relics.kind(covenant).equals("dragon_covenant")&&covenant.hasFoil(),"Native recipe lookup assembles the actual usable glowing Dragon Covenant");
            result.setItem(0,covenant);slot.onTake(player,slot.remove(1));
            for(int i=0;i<9;i++)check(grid.getItem(i).getCount()==1-craft,"Native result pickup consumes exactly one ingredient from slot "+i);
        }
        check(manager.getRecipeFor(RecipeType.CRAFTING,grid.asCraftInput(),level).isEmpty(),"An empty crafting grid cannot produce another Covenant");
        var legacy=new ItemStack(Items.EMERALD,3);var tag=new net.minecraft.nbt.CompoundTag();tag.putString("creeperborn","nation_offering:air:water");legacy.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
        ItemStack migrated=NationItems.migrate(legacy);check(migrated.is(NationItems.token(Element.AIR))&&migrated.getCount()==3,"Existing alpha-103 receipts retain their nation and count when upgraded to tokens");
        check(NationItems.migrate(new ItemStack(Items.EMERALD)).is(Items.EMERALD),"Ordinary emeralds are never converted into tokens");
    }
}
