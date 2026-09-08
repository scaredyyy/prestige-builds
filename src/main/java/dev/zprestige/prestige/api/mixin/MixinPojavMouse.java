package dev.zprestige.prestige.api.mixin;

import dev.zprestige.prestige.client.util.impl.PojavInput;
import net.minecraft.client.Mouse;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps physical mouse state separate from Minecraft's mutable useKey state. */
@Mixin(Mouse.class)
public final class MixinPojavMouse {
    @Inject(method = "method_1601", at = @At("HEAD"), remap = false)
    private void prestige$trackMouse(long window, int button, int action, int modifiers, CallbackInfo ci) {
        PojavInput.onMouseButton(button, action);
    }
}
