package com.rdt.connectedfreedom.mixin;
import com.rdt.connectedfreedom.client.CoachOverlay;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.DeltaTracker;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(Hud.class)
public abstract class CoachHudMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void rdt$coach(GuiGraphicsExtractor gui,DeltaTracker delta,CallbackInfo ci){CoachOverlay.render(gui);}
}
