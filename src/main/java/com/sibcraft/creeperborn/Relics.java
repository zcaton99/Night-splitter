package com.sibcraft.creeperborn;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;

public final class Relics {
    private Relics() {}
    public static ItemStack heart(Element element) {
        ItemStack item = new ItemStack(Items.HEART_OF_THE_SEA);
        mark(item, "heart:" + element.key());
        item.set(DataComponents.CUSTOM_NAME, Component.literal(element.title + " Creeper Heart — right-click to consume"));
        return item;
    }
    public static ItemStack gravityFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:gravity");item.set(DataComponents.CUSTOM_NAME,Component.literal("Gravity Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack opFruit(){ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:op");item.set(DataComponents.CUSTOM_NAME,Component.literal("Op-Op Creeper Fruit — right-click to consume"));return item;}
    public static ItemStack darkFruit(){
        ItemStack item=new ItemStack(Items.CHORUS_FRUIT);mark(item,"fruit:dark");
        item.set(DataComponents.CUSTOM_NAME,Component.literal("Dark Creeper Fruit — right-click to consume"));return item;
    }
    public static ItemStack fruit(String type){
        return switch(type){case "rubber"->fruit();case "door"->doorFruit();case "phoenix"->phoenixFruit();case "lightning"->lightningFruit();case "dark"->darkFruit();case "op"->opFruit();case "gravity"->gravityFruit();default->ItemStack.EMPTY;};
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
    }
    public static String kind(ItemStack stack) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        return data == null ? "" : data.copyTag().getString("creeperborn").orElse("");
    }
}


