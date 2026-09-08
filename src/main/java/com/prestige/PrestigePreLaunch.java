package com.prestige;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/**
 * Sets Android-safe native-library flags before Minecraft client initialization.
 */
public final class PrestigePreLaunch implements PreLaunchEntrypoint {
    @Override
    public void onPreLaunch() {
        try {
            System.setProperty("jna.nosys", "true");
            System.setProperty("jna.nounpack", "true");
            System.setProperty("oshi.os.windows.loaddll", "false");
        } catch (Throwable ignored) {
            // A restricted Android JVM must not stop the game because a property is unavailable.
        }
    }
}
