package dev.zprestige.prestige.client.ui.font;

import dev.zprestige.prestige.client.Prestige;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.render.BufferBuilder;
import net.minecraft.client.render.BufferRenderer;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.Tessellator;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import org.joml.Matrix4f;

import java.awt.Color;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Pojav-safe text renderer. It uses Minecraft's loaded font pipeline and
 * never invokes LWJGL STB, FreeType, AWT, or native font code.
 */
public final class FontRenderer {
    private static final Identifier PRESTIGE_ATLAS = Identifier.of("prestige", "font/prestige.png");
    private static final Glyph[] GLYPHS = loadGlyphs();

    private final float lineHeight;

    public FontRenderer(InputStream font) {
        this(font, 18.0F);
    }

    public FontRenderer(InputStream font, float size) {
        this.lineHeight = Math.max(1.0F, size / 2.0F);
        if (font != null) {
            try {
                font.close();
            } catch (IOException ignored) {
                // Android-safe fallback does not need the bundled font stream.
            }
        }
    }

    public void drawString(MatrixStack matrices, String text, float x, float y, Color color, Color shadow, boolean withShadow) {
        if (matrices == null || text == null || text.isEmpty()) return;

        if (withShadow && shadow != null) drawAtlas(matrices, text, x + 0.5F, y + 0.5F, shadow);
        drawAtlas(matrices, text, x, y, color);
    }

    public void drawString(String text, float x, float y, Color color) {
        int shadowAlpha = Math.min(187, color.getAlpha());
        drawString(Prestige.Companion.getFontManager().getMatrixStack(), text, x, y, color,
                new Color(0, 0, 0, shadowAlpha), true);
    }

    public float getStringWidth(String text) {
        if (text == null || text.isEmpty()) return 0.0F;
        float width = 0.0F;
        for (int i = 0; i < text.length(); i++) width += glyphWidth(text.charAt(i));
        return width;
    }

    public float getWidth(String text) {
        return getStringWidth(text);
    }

    public float getStringHeight() {
        return lineHeight;
    }

    private static void drawAtlas(MatrixStack matrices, String text, float x, float y, Color color) {
        RenderSystem.setShaderColor(color.getRed() / 255F, color.getGreen() / 255F, color.getBlue() / 255F, color.getAlpha() / 255F);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderTexture(0, PRESTIGE_ATLAS);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE);
        float cursor = x;
        for (int i = 0; i < text.length(); i++) {
            char glyph = text.charAt(i);
            if (glyph < 32 || glyph >= GLYPHS.length) glyph = '?';
            Glyph source = GLYPHS[glyph];
            float width = source.width / 2F;
            float height = source.height / 2F;
            float u1 = source.x / 256F, v1 = source.y / 256F;
            float u2 = (source.x + source.width) / 256F, v2 = (source.y + source.height) / 256F;
            buffer.vertex(matrix, cursor, y + height, 0).texture(u1, v2);
            buffer.vertex(matrix, cursor + width, y + height, 0).texture(u2, v2);
            buffer.vertex(matrix, cursor + width, y, 0).texture(u2, v1);
            buffer.vertex(matrix, cursor, y, 0).texture(u1, v1);
            cursor += glyphWidth(glyph);
        }
        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
    }

    private static float glyphWidth(char glyph) {
        return GLYPHS[glyph < GLYPHS.length ? glyph : '?'].width / 2F;
    }

    private static Glyph[] loadGlyphs() {
        Glyph[] glyphs = new Glyph[255];
        java.util.Arrays.fill(glyphs, new Glyph(0, 0, 8, 16));
        try (InputStream input = FontRenderer.class.getClassLoader().getResourceAsStream("assets/prestige/font/prestige-widths.txt")) {
            Properties properties = new Properties();
            if (input != null) properties.load(input);
            for (int code = 32; code < glyphs.length; code++) {
                String[] value = properties.getProperty(String.valueOf(code), "0,0,8,16").split(",");
                glyphs[code] = new Glyph(Float.parseFloat(value[0]), Float.parseFloat(value[1]), Float.parseFloat(value[2]), Float.parseFloat(value[3]));
            }
        } catch (Exception ignored) { }
        return glyphs;
    }

    private record Glyph(float x, float y, float width, float height) { }
}
