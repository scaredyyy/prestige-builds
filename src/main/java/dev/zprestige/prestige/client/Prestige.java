package dev.zprestige.prestige.client;

import dev.zprestige.prestige.client.bypass.ScreenshareBypass;
import dev.zprestige.prestige.client.bypass.impl.LogBypass;
import dev.zprestige.prestige.client.event.EventBus;
import dev.zprestige.prestige.client.event.impl.Render2DEvent;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.event.impl.Render3DEvent;
import dev.zprestige.prestige.client.event.impl.MoveEvent;
import dev.zprestige.prestige.client.event.Phase;
import dev.zprestige.prestige.client.handler.Handler;
import dev.zprestige.prestige.client.handler.impl.KeybindHanlder;
import dev.zprestige.prestige.client.handler.impl.LoginHandler;
import dev.zprestige.prestige.client.handler.impl.ProtectionHandler;
import dev.zprestige.prestige.client.managers.*;
import dev.zprestige.prestige.client.protection.Session;
import dev.zprestige.prestige.client.shader.impl.GradientGlowShader;
import dev.zprestige.prestige.client.ui.Interface;
import dev.zprestige.prestige.client.ui.drawables.gui.screens.impl.ConfigScreen;
import dev.zprestige.prestige.client.util.impl.RenderUtil;
import dev.zprestige.prestige.client.util.impl.RenderHelper;
import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;

import java.util.Arrays;
import java.util.List;

public class Prestige implements ModInitializer {

    static {
        try {
            System.setProperty("jna.nosys", "true");
            System.setProperty("jna.nounpack", "true");
            System.setProperty("oshi.os.windows.loaddll", "false");
        } catch (Throwable ignored) {
            // Android must still be able to continue if a restricted JVM rejects a property update.
        }
    }

    public static Companion Companion = new Companion();
    private static final EventBus eventBus = new EventBus();
    private static FontManager fontManager;
    private static ModuleManager moduleManager;
    private static ConfigManager configManager;
    private static DamageManager damageManager;
    private static SocialsManager socialsManager;
    private static RenderManager renderManager;
    private static AntiBotManager antiBotManager;
    private static ProtectionManager protectionManager;
    private static ClickManager clickManager;
    private static ScreenshareBypass screenshareBypass;
    private static Interface clickGUI;
    private static ConfigScreen configScreen;
    private static TargetManager targetManager;
    private static RotationManager rotationManager;
    private static Session session;
    public static boolean selfDestructed;

    @Override
    public void onInitialize() {
        if (isPojavLauncher()) {
            // Pojav uses a restricted LWJGL bridge. Keep only keyboard-driven
            // controls; mouse polling, shaders, and the desktop login UI stay off.
            new KeybindHanlder().register();
            HudRenderCallback.EVENT.register((drawContext, tickCounter) -> {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client.getWindow() == null) return;
                RenderHelper.setContext(drawContext);
                RenderHelper.setMatrixStack(drawContext.getMatrices());
                new Render2DEvent(drawContext.getMatrices(),
                        client.getWindow().getScaledWidth(),
                        client.getWindow().getScaledHeight()).invoke();
            });
            ClientTickEvents.END_CLIENT_TICK.register(client -> {
                if (client.player != null && client.world != null) {
                    new TickEvent().invoke();
                    MoveEvent move = new MoveEvent(Phase.PRE, client.player.getYaw(), client.player.getPitch());
                    move.invoke();
                    client.player.setYaw(move.getYaw());
                    client.player.setPitch(move.getPitch());
                }
            });
            WorldRenderEvents.LAST.register(context -> {
                RenderHelper.setMatrixStack(context.matrixStack());
                RenderHelper.getProjectionMatrix().set(RenderSystem.getProjectionMatrix());
                RenderHelper.getModelViewMatrix().set(RenderSystem.getModelViewMatrix());
                RenderHelper.getPositionMatrix().set(context.matrixStack().peek().getPositionMatrix());
                new Render3DEvent(context.matrixStack(), context.tickCounter().getTickDelta(false)).invoke();
            });
            getScreenshareBypass().setList(List.of(new LogBypass()));
            return;
        }
        List<Handler> handlers = Arrays.asList(new KeybindHanlder(), new LoginHandler(), new ProtectionHandler());
        handlers.forEach(Handler::register);
        if (handlers.size() != 3) {
            ProtectionManager.exit("K");
        }
        RenderUtil.shader = new GradientGlowShader();
        getScreenshareBypass().setList(List.of(new LogBypass()));
    }

    private static boolean isPojavLauncher() {
        return System.getProperty("pojav.path.minecraft") != null
                || System.getProperty("os.version", "").startsWith("Android-");
    }

    private static ScreenshareBypass getScreenshareBypass() {
        if (screenshareBypass == null) screenshareBypass = new ScreenshareBypass();
        return screenshareBypass;
    }

    public static class Companion {

        public static EventBus getEventBus() {
            return eventBus;
        }

        public static FontManager getFontManager() {
            if (fontManager == null) fontManager = new FontManager();
            return fontManager;
        }

        public static ModuleManager getModuleManager() {
            if (moduleManager == null) moduleManager = new ModuleManager();
            return moduleManager;
        }

        public static ConfigManager getConfigManager() {
            if (configManager == null) configManager = new ConfigManager();
            return configManager;
        }

        public static DamageManager getDamageManager() {
            if (damageManager == null) damageManager = new DamageManager();
            return damageManager;
        }

        public static SocialsManager getSocialsManager() {
            if (socialsManager == null) socialsManager = new SocialsManager();
            return socialsManager;
        }

        public static RenderManager getRenderManager() {
            if (renderManager == null) renderManager = new RenderManager();
            return renderManager;
        }

        public static AntiBotManager getAntiBotManager() {
            if (antiBotManager == null) antiBotManager = new AntiBotManager();
            return antiBotManager;
        }

        public static ProtectionManager getProtectionManager() {
            if (protectionManager == null) protectionManager = new ProtectionManager();
            return protectionManager;
        }

        public static ClickManager getClickManager() {
            if (clickManager == null) clickManager = new ClickManager();
            return clickManager;
        }

        public static Interface getClickGUI() {
            if (clickGUI == null) clickGUI = new Interface();
            return clickGUI;
        }

        public static ConfigScreen getConfigScreen() {
            return configScreen;
        }

        public static void setConfigScreen(ConfigScreen value) {
            configScreen = value;
        }

        public static TargetManager getTargetManager() {
            if (targetManager == null) targetManager = new TargetManager();
            return targetManager;
        }

        public static RotationManager getRotationManager() {
            if (rotationManager == null) rotationManager = new RotationManager();
            return rotationManager;
        }

        public static Session getSession() {
            return session;
        }

        public static void setSession(Session value) {
            session = value;
        }

        public static boolean getSelfDestructed() {
            return selfDestructed;
        }

        public static void setSelfDestructed(boolean value) {
            selfDestructed = value;
        }
    }
}
