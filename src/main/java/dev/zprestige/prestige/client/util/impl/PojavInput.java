package dev.zprestige.prestige.client.util.impl;

import dev.zprestige.prestige.api.mixin.IKeyBinding;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import org.lwjgl.glfw.GLFW;

/**
 * Input boundary for Android.  Pojav delivers touch/keyboard state through
 * Minecraft key bindings; querying GLFW directly can load an incompatible
 * native bridge.  Desktop keeps the original GLFW behaviour.
 */
public final class PojavInput {
    private static volatile boolean rightMouseDown;
    private PojavInput() { }

    public static boolean isPojav() {
        return System.getProperty("pojav.path.minecraft") != null
                || System.getProperty("os.version", "").startsWith("Android-");
    }

    public static boolean isGameFocused() {
        MinecraftClient client = MinecraftClient.getInstance();
        return isPojav() || (client != null && client.isWindowFocused());
    }

    public static boolean isKeyPressed(int keyCode) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        if (isPojav()) {
            for (KeyBinding binding : client.options.allKeys) {
                if (((IKeyBinding) binding).getBoundKey().getCode() == keyCode && binding.isPressed()) return true;
            }
            return false;
        }
        try {
            return GLFW.glfwGetKey(client.getWindow().getHandle(), keyCode) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static boolean isMousePressed(int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        if (isPojav()) {
            return switch (button) {
                case GLFW.GLFW_MOUSE_BUTTON_LEFT -> client.options.attackKey.isPressed();
                case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> client.options.useKey.isPressed();
                case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> client.options.pickItemKey.isPressed();
                default -> false;
            };
        }
        try {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), button) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    /** Mouse callback state survives clearing Minecraft's useKey binding. */
    public static boolean isTrackedMousePressed(int button) {
        return button == GLFW.GLFW_MOUSE_BUTTON_RIGHT && rightMouseDown;
    }

    public static void onMouseButton(int button, int action) {
        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) rightMouseDown = action != GLFW.GLFW_RELEASE;
    }

    /**
     * Argon's combat macros use Pojav's GLFW mouse bridge directly.  Keep it
     * available for those modules, but return false if a launcher has no bridge.
     */
    public static boolean isArgonMousePressed(int button) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return false;
        try {
            return GLFW.glfwGetMouseButton(client.getWindow().getHandle(), button) == GLFW.GLFW_PRESS;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static void setWindowWidth(int width) {
        if (isPojav()) return;
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        try {
            GLFW.glfwSetWindowSize(client.getWindow().getHandle(), width, client.getWindow().getHeight());
        } catch (Throwable ignored) {
            // Window resizing is optional and unsupported on Android.
        }
    }
}
