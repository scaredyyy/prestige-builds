package dev.zprestige.prestige.client.module.impl.visuals;

import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.Render2DEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.ModeSetting;
import dev.zprestige.prestige.client.util.impl.PojavInput;
import dev.zprestige.prestige.client.util.impl.RenderUtil;
import java.awt.Color;

public class AspectRatio extends Module {
    public ModeSetting mode;

    public AspectRatio() {
        super("AspectRatio", Category.Visual, "Changes the aspect ratio of the game");
        mode = setting("Ratio", "1720", new String[]{"1800", "1720", "1600", "1440", "1300"});
    }

    @Override
    public void onDisable() {
        PojavInput.setWindowWidth(1920);
    }

    @EventListener
    public void event(Render2DEvent event) {
        if (PojavInput.isPojav()) {
            // Android cannot resize Pojav's native surface. Letterboxing gives
            // the selected aspect ratio without calling GLFW/window APIs.
            float target = Integer.parseInt(mode.getObject()) / 1000.0f;
            float actual = (float) event.getScaledWidth() / event.getScaledHeight();
            if (actual > target) {
                float side = (event.getScaledWidth() - event.getScaledHeight() * target) / 2.0f;
                RenderUtil.renderColoredQuad(0, 0, side, event.getScaledHeight(), Color.BLACK);
                RenderUtil.renderColoredQuad(event.getScaledWidth() - side, 0, event.getScaledWidth(), event.getScaledHeight(), Color.BLACK);
            } else if (actual < target) {
                float top = (event.getScaledHeight() - event.getScaledWidth() / target) / 2.0f;
                RenderUtil.renderColoredQuad(0, 0, event.getScaledWidth(), top, Color.BLACK);
                RenderUtil.renderColoredQuad(0, event.getScaledHeight() - top, event.getScaledWidth(), event.getScaledHeight(), Color.BLACK);
            }
            return;
        }
        PojavInput.setWindowWidth(Integer.parseInt(mode.getObject()));
    }
}
