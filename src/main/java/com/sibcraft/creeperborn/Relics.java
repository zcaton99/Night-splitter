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
    public static ItemStack focus() {
        ItemStack item = new ItemStack(Items.BLAZE_ROD);
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
