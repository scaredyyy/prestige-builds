package dev.zprestige.prestige.client.module.impl.misc;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;

public class SelfDestruct extends Module {

    public SelfDestruct() {
        super("Self Destruct", Category.Misc, "Destructs the client to hopefully prevent detection when screenshared");
    }

    @Override
    public void onEnable() {
        Prestige.Companion.setSelfDestructed(true);
        getMc().setScreen(null);
        for (Module module : Prestige.Companion.getModuleManager().getModules()) {
            if (module.isEnabled()) {
                module.toggle();
            }
            module.clear();
            module.getKeybind().invokeValue(-1);
        }
        Prestige.Companion.getClickGUI().onSelfDestruct();
        // Android can stall or terminate when old finalization/sleep loops run here.
        // Clearing the client state above is sufficient for this module's purpose.
        System.gc();
    }
}
