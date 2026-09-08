package dev.zprestige.prestige.api.mixin;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.impl.FloatingItemEvent;
import dev.zprestige.prestige.client.event.impl.ReachEvent;
import dev.zprestige.prestige.client.event.impl.TiltEvent;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Minimal 1.21 hook for Totem Animation; deliberately excludes old reach and shader overwrites. */
@Mixin(GameRenderer.class)
public class MixinPojavGameRenderer {
    @Shadow private int floatingItemTimeLeft;

    @Inject(method = "tick", at = @At("HEAD"))
    private void prestige$adjustFloatingItemTime(CallbackInfo ci) {
        if (Prestige.Companion.getSelfDestructed()) return;
        FloatingItemEvent event = new FloatingItemEvent(0);
        event.invoke();
        if (event.getSpeed() > 0) {
            floatingItemTimeLeft = Math.max(0, floatingItemTimeLeft - event.getSpeed());
        }
    }

    @ModifyConstant(method = "updateTargetedEntity", constant = @Constant(doubleValue = 4.5D))
    private double prestige$modifyReach(double original) {
        if (Prestige.Companion.getSelfDestructed()) return original;
        ReachEvent event = new ReachEvent(0);
        event.invoke();
        return original + event.getReach();
    }

    @Inject(method = "tiltViewWhenHurt", at = @At("HEAD"), cancellable = true)
    private void prestige$cancelHurtTilt(net.minecraft.client.util.math.MatrixStack matrices, float tickDelta, CallbackInfo ci) {
        if (!Prestige.Companion.getSelfDestructed() && new TiltEvent().invoke()) {
            ci.cancel();
        }
    }
}
