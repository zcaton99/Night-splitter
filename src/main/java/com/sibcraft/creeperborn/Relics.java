package com.sibcraft.creeperborn;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class Relics {
    private Relics() {}
    public static boolean updateFruitModel(ItemStack item){
        if(item.isEmpty())return false;
        String kind=kind(item);
        if(kind.startsWith("heart:")){Element element=Element.parse(kind.substring(6));if(element==null)return false;var icon=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","element_"+element.key());if(icon.equals(item.get(DataComponents.ITEM_MODEL)))return false;item.set(DataComponents.ITEM_MODEL,icon);return true;}
        if(!kind.startsWith("fruit:")||!Dark.FRUITS.contains(kind.substring(6)))return false;
        var model=net.minecraft.resources.Identifier.fromNamespaceAndPath("creeperborn","fruit_"+kind.substring(6));
        if(model.equals(item.get(DataComponents.ITEM_MODEL)))return false;
        item.set(DataComponents.ITEM_MODEL,model);return true;
    }
    /** Refresh old saves, including held items, cursor stacks and currently open containers. */
    public static void refreshFruitModels(net.minecraft.world.entity.player.Player player){
        for(int i=0;i<player.getInventory().getContainerSize();i++)updateFruitModel(player.getInventory().getItem(i));
        updateFruitModel(player.getOffhandItem());
        updateFruitModel(player.containerMenu.getCarried());
        for(var slot:player.containerMenu.slots)updateFruitModel(slot.getItem());
    }
    public static ItemStack heart(Element element) {
        ItemStack item = new ItemStack(Items.HEART_OF_THE_SEA);
        mark(item, "heart:" + element.key());
        item.set(DataComponents.CUSTOM_NAME, Component.literal(element.title + " Creeper Heart — right-click to consume"));
        return item;
    }
    public static ItemStack zoanFruit(String f){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:"+f);item.set(DataComponents.CUSTOM_NAME,Component.literal((f.equals("fox")?"Dog-Dog, Model: Nine-Tailed Fox":"Salamander-Salamander, Model: Axolotl")+" Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack celestialFruit(String f){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:"+f);item.set(DataComponents.CUSTOM_NAME,Component.literal((f.equals("flame")?"Flame-Flame":f.equals("buddha")?"Human-Human, Model: Buddha":f.equals("glint")?"Glint-Glint":"Straw-Straw")+" Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack natureFruit(String f){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:"+f);item.set(DataComponents.CUSTOM_NAME,Component.literal((f.equals("tree")?"Tree-Tree":"Petal-Petal")+" Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack azureFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:azure");item.set(DataComponents.CUSTOM_NAME,Component.literal("Fish-Fish: Azure Dragon Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack iceFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:ice");item.set(DataComponents.CUSTOM_NAME,Component.literal("Ice-Ice Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack magmaFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:magma");item.set(DataComponents.CUSTOM_NAME,Component.literal("Magma-Magma Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack loveFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:love");item.set(DataComponents.CUSTOM_NAME,Component.literal("Love-Love Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack sandFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:sand");item.set(DataComponents.CUSTOM_NAME,Component.literal("Sand-Sand Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack tremorFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:tremor");item.set(DataComponents.CUSTOM_NAME,Component.literal("Tremor-Tremor Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack pawFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:paw");item.set(DataComponents.CUSTOM_NAME,Component.literal("Paw-Paw Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack stringFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:string");item.set(DataComponents.CUSTOM_NAME,Component.literal("String Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack gravityFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:gravity");item.set(DataComponents.CUSTOM_NAME,Component.literal("Gravity Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack opFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:op");item.set(DataComponents.CUSTOM_NAME,Component.literal("Op-Op Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack darkFruit(){
        ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:dark");
        item.set(DataComponents.CUSTOM_NAME,Component.literal("Dark Creeper Fruit — right-click to consume"));return item;
    }
    public static ItemStack fruit(String type){
        return switch(type){case "fox","axolotl"->zoanFruit(type);case "rubber"->fruit();case "door"->doorFruit();case "phoenix"->phoenixFruit();case "lightning"->lightningFruit();case "dark"->darkFruit();case "op"->opFruit();case "glint","straw","flame","buddha"->celestialFruit(type);case "tree","petal"->natureFruit(type);case "azure"->azureFruit();case "ice"->iceFruit();case "magma"->magmaFruit();case "love"->loveFruit();case "sand"->sandFruit();case "tremor"->tremorFruit();case "paw"->pawFruit();case "string"->stringFruit();case "gravity"->gravityFruit();default->ItemStack.EMPTY;};
    }
    public static ItemStack captured(String type,String token,int tier){
        ItemStack item=fruit(type);if(item.isEmpty())return item;
        CompoundTag tag=item.get(DataComponents.CUSTOM_DATA).copyTag();tag.putString("cb_capture",token);tag.putInt("cb_capture_tier",Math.min(2,Math.max(1,tier)));
        item.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return item;
    }
    public static String captureToken(ItemStack item){
        CustomData data=item.get(DataComponents.CUSTOM_DATA);return data==null?"":data.copyTag().getString("cb_capture").orElse("");
    }
    public static int captureTier(ItemStack item){
        CustomData data=item.get(DataComponents.CUSTOM_DATA);return data==null?1:Math.min(2,Math.max(1,data.copyTag().getInt("cb_capture_tier").orElse(1)));
    }
    public static ItemStack doorFruit(){
        ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:door");
        item.set(DataComponents.CUSTOM_NAME,Component.literal("Door-Door Fruit — right-click to consume"));return item;
    }
    public static ItemStack lightningFruit(){
        ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:lightning");
        item.set(DataComponents.CUSTOM_NAME,Component.literal("Lightning Creeper Fruit — right-click to consume"));return item;
    }
    public static ItemStack phoenixFruit(){
        ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:phoenix");
        item.set(DataComponents.CUSTOM_NAME,Component.literal("Phoenix Fruit — right-click to consume"));return item;
    }
    public static ItemStack fruit() {
        ItemStack item = new ItemStack(Items.CHORUS_FRUIT); mark(item,"fruit:rubber");
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Rubber Fruit — right-click to consume")); return item;
    }
    public static ItemStack focus() {
        ItemStack item = new ItemStack(Items.STICK);
        mark(item, "focus");
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Elemental Focus — sneak-use to change element"));
        return item;
    }
    public static ItemStack trident() {
        ItemStack item = new ItemStack(Items.TRIDENT);
        mark(item, "trident");
        item.set(DataComponents.CUSTOM_NAME, Component.literal("Elemental Trident — empower villagers"));
        return item;
    }
    private static void mark(ItemStack item, String kind) {
        CompoundTag tag = new CompoundTag(); tag.putString("creeperborn", kind);
        item.set(DataComponents.CUSTOM_DATA, CustomData.of(tag));
        updateFruitModel(item);
    }
    public static String kind(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString("creeperborn").orElse("");
    }
}


