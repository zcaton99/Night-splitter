package com.sibcraft.creeperborn;

import java.util.EnumMap;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;

/** Real registered tokens let the normal recipe book place the exact ingredients. */
public final class NationItems {
    private static final EnumMap<Element,Item> TOKENS=new EnumMap<>(Element.class);
    public static final ResourceKey<net.minecraft.world.item.crafting.Recipe<?>> COVENANT_RECIPE=ResourceKey.create(Registries.RECIPE,Identifier.fromNamespaceAndPath("creeperborn","dragon_covenant"));
    public static void init(){
        for(Element e:Element.values()){
            Identifier id=Identifier.fromNamespaceAndPath("creeperborn",e.key()+"_nation_token");
            CompoundTag tag=new CompoundTag();tag.putString("creeperborn","nation_token:"+e.key());
            Item item=new Item(new Item.Properties().setId(ResourceKey.create(Registries.ITEM,id)).rarity(Rarity.RARE)
                .component(DataComponents.CUSTOM_DATA,CustomData.of(tag)).component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE,true));
            TOKENS.put(e,Registry.register(BuiltInRegistries.ITEM,id,item));
        }
    }
    public static Item token(Element e){return TOKENS.get(e);}
    public static ItemStack stack(Element e){return new ItemStack(token(e));}
    /** Alpha 103's named emerald receipts remain useful after upgrading. */
    public static ItemStack migrate(ItemStack stack){
        String kind=Relics.kind(stack);if(!kind.startsWith("nation_offering:"))return stack;
        String[] parts=kind.split(":");Element e=parts.length==3?Element.parse(parts[1]):null;
        if(e==null)return stack;
        ItemStack token=stack(e);token.setCount(stack.getCount());return token;
    }
}
