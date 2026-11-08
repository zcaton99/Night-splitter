package com.sibcraft.creeperborn.mixin;
import net.minecraft.world.entity.Display;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemDisplayContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(Display.ItemDisplay.class)
public interface DragonHeadDisplayAccessor {
    @Invoker("setItemStack") void creeperborn$item(ItemStack item);
    @Invoker("setItemTransform") void creeperborn$context(ItemDisplayContext context);
}
