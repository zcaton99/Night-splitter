package com.sibcraft.creeperborn.mixin;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import com.sibcraft.creeperborn.WaterRules;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class DoorHudMixin {
    @Inject(method="extractRenderState",at=@At("HEAD"))
    private void creeperborn$cyanSpace(GuiGraphicsExtractor gui,DeltaTracker tracker,CallbackInfo ci){
        var p=Minecraft.getInstance().player;
        if(p!=null&&(WaterRules.flags(p)&4)!=0)gui.fill(0,0,gui.guiWidth(),gui.guiHeight(),0x3200CFE8);
    }
}
