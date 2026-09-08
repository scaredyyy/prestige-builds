package dev.zprestige.prestige.client.module.impl.visuals;

import dev.zprestige.prestige.client.Prestige;
import dev.zprestige.prestige.client.event.EventListener;
import dev.zprestige.prestige.client.event.impl.Render2DEvent;
import dev.zprestige.prestige.client.module.Category;
import dev.zprestige.prestige.client.module.Module;
import dev.zprestige.prestige.client.setting.impl.BooleanSetting;
import dev.zprestige.prestige.client.ui.font.FontRenderer;
import dev.zprestige.prestige.client.util.impl.RenderUtil;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import java.awt.Color;

/** 1.21/Pojav-safe projected player tags. */
public class NameTags extends Module {
    public BooleanSetting glow;

    public NameTags() {
        super("Name Tags", Category.Visual, "Renders player tags above visible players");
        glow = setting("Glow", true).description("Glow behind each tag");
    }

    @EventListener
    public void event(Render2DEvent event) {
        if (getMc().player == null || getMc().world == null || getMc().getNetworkHandler() == null) return;
        FontRenderer font = Prestige.Companion.getFontManager().getFontRenderer();
        Color theme = Prestige.Companion.getModuleManager().getMenu().getColor().getObject();
        for (PlayerEntity player : getMc().world.getPlayers()) {
            if (player == getMc().player || !Prestige.Companion.getAntiBotManager().isNotBot(player)) continue;
            PlayerListEntry entry = getMc().getNetworkHandler().getPlayerListEntry(player.getUuid());
            if (entry == null) continue;
            Vec3d screen = RenderUtil.worldSpaceToScreenSpace(player.getPos().add(0, player.getHeight() + 0.5, 0));
            if (screen.z <= 0 || screen.z >= 1) continue;

            String name = player.getName().getString();
            String health = String.valueOf((int) Math.ceil(player.getHealth() + player.getAbsorptionAmount()));
            float width = Math.max(72.0f, font.getStringWidth(name) + font.getStringWidth(health) + 28.0f);
            float x = (float) screen.x - width / 2.0f;
            float y = (float) screen.y - 20.0f;
            if (glow.getObject()) RenderUtil.renderShaderRect(event.getMatrixStack(), theme, theme, theme, theme, x, y, width, 18, 4, 8);
            RenderUtil.renderRoundedRect(x, y, x + width, y + 18, new Color(12, 12, 12, 230), 4);
            RenderUtil.renderTexturedQuad(entry.getSkinTextures().texture(), x + 2, y + 2, 0, 14, 14, 14, 14, 64, 64);
            Color nameColor = Prestige.Companion.getSocialsManager().isFriend(name) ? Color.CYAN
                    : Prestige.Companion.getSocialsManager().isEnemy(name) ? Color.RED : Color.WHITE;
            font.drawString(name, x + 19, y + 2, nameColor);
            font.drawString(health, x + width - font.getStringWidth(health) - 3, y + 2, Color.WHITE);
        }
    }
}
