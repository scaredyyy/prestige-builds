package dev.zprestige.prestige.api.mixin;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.impl.CrosshairEvent;
import dev.zprestige.prestige.client.event.impl.StatusEffectOverlayEvent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value={InGameHud.class})
public class MixinInGameHud {
    @Inject(at={@At(value="HEAD")}, method={"renderStatusEffectOverlay"}, cancellable=true)
    void renderStatusEffectOverlay(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
        if (Prestige.Companion.getSelfDestructed()) {
            return;
        }
        if (new StatusEffectOverlayEvent().invoke()) {
            callbackInfo.cancel();
        }
    }

    @Inject(method={"renderCrosshair"}, at={@At(value="HEAD")}, cancellable=true)
    void renderCrosshair(DrawContext drawContext, RenderTickCounter tickCounter, CallbackInfo callbackInfo) {
        if (Prestige.Companion.getSelfDestructed()) {
            return;
        }
        if (new CrosshairEvent().invoke()) {
            callbackInfo.cancel();
        }
    }

}
