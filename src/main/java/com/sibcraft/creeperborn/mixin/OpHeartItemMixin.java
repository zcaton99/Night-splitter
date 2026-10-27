package com.sibcraft.creeperborn.mixin;
import com.sibcraft.creeperborn.Hearts;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(ItemEntity.class)
public abstract class OpHeartItemMixin{
    @Shadow private int age;
    @Unique private String creeperborn$pickupHeart="";
    @Inject(method="tick",at=@At("HEAD"),cancellable=true)
    private void creeperborn$heartTick(CallbackInfo ci){
        ItemEntity self=(ItemEntity)(Object)this;
        if(!Hearts.token(self.getItem()).isEmpty()){
            age=-32768;
            if(self.level() instanceof ServerLevel&&Hearts.groundTick(self))ci.cancel();
        }
    }
    @Inject(method="playerTouch",at=@At("HEAD"))
    private void creeperborn$beforePickup(Player p,CallbackInfo ci){creeperborn$pickupHeart=Hearts.token(((ItemEntity)(Object)this).getItem());}
    @Inject(method="playerTouch",at=@At("TAIL"))
    private void creeperborn$afterPickup(Player p,CallbackInfo ci){
        if(p.level() instanceof ServerLevel&&!creeperborn$pickupHeart.isEmpty()&&((ItemEntity)(Object)this).getItem().isEmpty())Hearts.picked(creeperborn$pickupHeart);
        creeperborn$pickupHeart="";
    }
}
