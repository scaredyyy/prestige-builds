package dev.zprestige.prestige.client.module.impl.visuals;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.Render2DEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.ui.font.FontRenderer;
import dev.zprestige.prestige.client.util.impl.RenderUtil;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;

/** 1.21 HUD projection implementation; no old EntityRenderer hook required. */
public class HealthIndicators extends Module {
    public HealthIndicators() {
        super("Health Indicators", Category.Visual, "Shows health above visible players");
    }

    @EventListener
    public void event(Render2DEvent event) {
        if (getMc().player == null || getMc().world == null) return;
        FontRenderer font = Prestige.Companion.getFontManager().getFontRenderer();
        for (PlayerEntity player : getMc().world.getPlayers()) {
            if (player == getMc().player || !Prestige.Companion.getAntiBotManager().isNotBot(player)) continue;
            Vec3d screen = RenderUtil.worldSpaceToScreenSpace(player.getPos().add(0, player.getHeight() + 0.35, 0));
            if (screen.z <= 0 || screen.z >= 1) continue;
            float health = (float) Math.ceil(player.getHealth() + player.getAbsorptionAmount());
            String text = String.valueOf(health);
            font.drawString(text, (float) screen.x - font.getStringWidth(text) / 2.0f, (float) screen.y, getColor(health));
        }
    }

    private Color getColor(float health) {
        if (health <= 5) return Color.RED;
        if (health <= 10) return Color.ORANGE;
        if (health <= 15) return Color.YELLOW;
        return health <= 20 ? Color.GREEN : new Color(0, 130, 0);
    }
}
