package dev.zprestige.prestige.client.handler.impl;

import net.minecraft.client.MinecraftClient;
import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.module.Module;
import net.minecraft.client.gui.screen.Screen;
import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.impl.KeyEvent;
import dev.zprestige.prestige.client.handler.Handler;
import dev.zprestige.prestige.client.util.MC;

public class KeybindHanlder implements MC, Handler {

    public static double scale;

    @Override
    public void register() {
        Prestige.Companion.getEventBus().registerListener(this);
    }
    
    @EventListener
    public void event(KeyEvent event) {
        // Keep the original desktop menu bind: RIGHT_SHIFT (344).
        if (isPojavLauncher() && this.getMc().currentScreen == null
                && event.getAction() == 1
                && event.getKey() == 344) {
            openMenu();
            return;
        }
        if (this.getMc().currentScreen == null) {
            if (event.getKey() == ((Number)Prestige.Companion.getModuleManager().getMenu().getBind().getObject()).intValue()) {
                openMenu();
            }
        }
        if (this.getMc().currentScreen == null) {
            for (Module module : Prestige.Companion.getModuleManager().getModules()) {
                if (module.getKeybind().isListening()) {
                    if (event.getAction() == 0) {
                        if (!module.isEnabled() || module.getKey() != event.getKey()) continue;
                        module.toggle();
                        continue;
                    }
                    if (event.getAction() != 1 || module.isEnabled() || module.getKey() != event.getKey()) continue;
                    module.toggle();
                    continue;
                }
                if (module.getKey() == -1 || event.getAction() != 1 || module.getKey() != event.getKey()) continue;
                module.toggle();
            }
        }
    }

    private void openMenu() {
        Prestige.Companion.getClickGUI().setInitPos(true);
        scale = this.getMc().getWindow().getScaleFactor();
        this.getMc().getWindow().setScaleFactor(2.0);
        this.getMc().setScreen(Prestige.Companion.getClickGUI());
    }

    private boolean isPojavLauncher() {
        return System.getProperty("pojav.path.minecraft") != null
                || System.getProperty("os.version", "").startsWith("Android-");
    }

    @Override
    public MinecraftClient getMc() {
        return MinecraftClient.getInstance();
    }

    public static double getScale() {
        return scale;
    }

    public static void setScale(double s) {
        scale = s;
    }
}
