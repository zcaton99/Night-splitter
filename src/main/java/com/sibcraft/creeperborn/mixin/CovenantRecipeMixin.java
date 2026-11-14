package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ShapedRecipe.class)
public abstract class CovenantRecipeMixin {
    @Inject(method="matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",at=@At("RETURN"),cancellable=true)
    private void creeperborn$nationGate(CraftingInput input,Level level,CallbackInfoReturnable<Boolean> cir){
        if(cir.getReturnValueZ()&&level instanceof ServerLevel&&!Nations.unlocked()
            &&Relics.kind(((ShapedRecipe)(Object)this).assemble(input)).equals("dragon_covenant"))cir.setReturnValue(false);
    }
}
