package dev.zprestige.prestige.client.module.impl.visuals;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.TickEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.setting.impl.FloatSetting;
import dev.zprestige.prestige.client.setting.impl.IntSetting;
import net.minecraft.client.option.CloudRenderMode;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.option.ParticlesMode;

/**
 * Uses Minecraft's own options only.  No native calls, OpenGL hacks, or packets.
 * Every changed value is restored when the module is disabled.
 */
public class RenderOptimization extends Module {
    public final IntSetting renderDistance;
    public final IntSetting simulationDistance;
    public final FloatSetting entityDistance;
    public final BooleanSetting minimalParticles;
    public final BooleanSetting cloudsOff;
    public final BooleanSetting entityShadows;
    public final BooleanSetting fastGraphics;

    private boolean saved;
    private int oldRenderDistance;
    private int oldSimulationDistance;
    private double oldEntityDistance;
    private ParticlesMode oldParticles;
    private CloudRenderMode oldClouds;
    private boolean oldEntityShadows;
    private GraphicsMode oldGraphics;

    public RenderOptimization() {
        super("Render Optimization", Category.Visual, "Lowers expensive Minecraft rendering options while enabled");
        renderDistance = setting("Render Distance", 8, 2, 32).description("Chunks rendered around you");
        simulationDistance = setting("Simulation Distance", 5, 2, 12).description("Chunks that tick around you");
        entityDistance = setting("Entity Distance", 0.5f, 0.25f, 1.0f).description("How far entities render");
        minimalParticles = setting("Minimal Particles", true).description("Uses Minecraft minimal particle mode");
        cloudsOff = setting("Clouds Off", true).description("Stops cloud rendering");
        entityShadows = setting("Entity Shadows", false).description("Turns entity shadows off");
        fastGraphics = setting("Fast Graphics", true).description("Uses Minecraft fast graphics mode");
    }

    @Override
    public void onEnable() {
        saveOptions();
        applyOptions();
    }

    @Override
    public void onDisable() {
        restoreOptions();
    }

    @EventListener
    public void event(TickEvent event) {
        applyOptions();
    }

    private void saveOptions() {
        try {
            if (saved || getMc().options == null) return;
            oldRenderDistance = getMc().options.getViewDistance().getValue();
            oldSimulationDistance = getMc().options.getSimulationDistance().getValue();
            oldEntityDistance = getMc().options.getEntityDistanceScaling().getValue();
            oldParticles = getMc().options.getParticles().getValue();
            oldClouds = getMc().options.getCloudRenderMode().getValue();
            oldEntityShadows = getMc().options.getEntityShadows().getValue();
            oldGraphics = getMc().options.getGraphicsMode().getValue();
            saved = true;
        } catch (Throwable ignored) {
            saved = false;
        }
    }

    private void applyOptions() {
        try {
            if (getMc().options == null) return;
            getMc().options.getViewDistance().setValue(renderDistance.getObject());
            getMc().options.getSimulationDistance().setValue(simulationDistance.getObject());
            getMc().options.getEntityDistanceScaling().setValue((double) entityDistance.getObject());
            if (minimalParticles.getObject()) getMc().options.getParticles().setValue(ParticlesMode.MINIMAL);
            if (cloudsOff.getObject()) getMc().options.getCloudRenderMode().setValue(CloudRenderMode.OFF);
            if (!entityShadows.getObject()) getMc().options.getEntityShadows().setValue(false);
            if (fastGraphics.getObject()) getMc().options.getGraphicsMode().setValue(GraphicsMode.FAST);
        } catch (Throwable ignored) {
            // Some launchers expose a partial options object during startup. Try next tick.
        }
    }

    private void restoreOptions() {
        try {
            if (!saved || getMc().options == null) return;
            getMc().options.getViewDistance().setValue(oldRenderDistance);
            getMc().options.getSimulationDistance().setValue(oldSimulationDistance);
            getMc().options.getEntityDistanceScaling().setValue(oldEntityDistance);
            getMc().options.getParticles().setValue(oldParticles);
            getMc().options.getCloudRenderMode().setValue(oldClouds);
            getMc().options.getEntityShadows().setValue(oldEntityShadows);
            getMc().options.getGraphicsMode().setValue(oldGraphics);
        } catch (Throwable ignored) {
            // Never let restoring a visual setting stop the client.
        } finally {
            saved = false;
        }
    }
}
