package de.redclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.client.util.Window;

public class CrosshairRenderer {
    private static int guiScale() {
        Window w = MinecraftClient.getInstance().getWindow();
        return Math.max(1, (int) Math.round(w.getScaleFactor()));
    }

    /** Ingame-HUD. */
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        TargetType t = TargetType.current(mc);
        drawIn(ctx, t, RedConfig.INSTANCE.profile(t), 0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
    }

    /** Zeichnet das Crosshair (inkl. Verschiebung) um die Mitte des Rechtecks (ox, oy, aw, ah) in GUI-Pixeln. */
    public static void drawIn(DrawContext ctx, TargetType t, CrosshairProfile p, int ox, int oy, int aw, int ah) {
        int g = guiScale();
        // gleiche Mitte wie das Vanilla-Crosshair ((breite - 15) / 2 + 7.5)
        double cx = ((aw - 15) / 2 + ox + 7.5) * g;
        double cy = ((ah - 15) / 2 + oy + 7.5) * g;

        CrosshairTextures.Tex tex = null;
        boolean image = false;
        int wDev = 0, hDev = 0;

        if (p.imageEnabled && CrosshairTextures.hasImage(t)) {
            tex = CrosshairTextures.image(t);
            if (tex != null) {
                image = true;
                wDev = Math.max(4, Math.min(256, p.imageSize)) * g;
                hDev = Math.max(1, Math.round(wDev * tex.h() / (float) tex.w()));
            }
        }
        if (tex == null) {
            tex = CrosshairTextures.pixel(t, p, g);
            wDev = tex.w();
            hDev = tex.h();
        }

        float offX = image ? p.imageOffsetX : p.offsetX;
        float offY = image ? p.imageOffsetY : p.offsetY;
        double midX = image ? wDev / 2.0 : tex.cx();
        double midY = image ? hDev / 2.0 : tex.cy();
        int x0 = (int) Math.round(cx + offX * g - midX);
        int y0 = (int) Math.round(cy + offY * g - midY);

        // Vanilla-Blending = die echte Minecraft-Crosshair-Pipeline (invertiert den Hintergrund)
        boolean vanilla = image ? p.vanillaBlending : p.vanillaPipeline();
        var pipeline = vanilla ? RenderPipelines.CROSSHAIR : RenderPipelines.GUI_TEXTURED;

        // In echten Bildschirm-Pixeln zeichnen: 1 Textur-Pixel = 1 Bildschirm-Pixel
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x0 / (float) g, y0 / (float) g);
        m.scale(1f / g, 1f / g);
        ctx.drawTexture(pipeline, tex.id(), 0, 0, 0f, 0f, wDev, hDev, tex.w(), tex.h(), tex.w(), tex.h());
        m.popMatrix();
    }
}
