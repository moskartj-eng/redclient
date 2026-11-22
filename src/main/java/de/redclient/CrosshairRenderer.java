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

    /** Draws the pixel crosshair and/or the custom image around the center of the rectangle (ox, oy, aw, ah) in GUI pixels. */
    public static void drawIn(DrawContext ctx, TargetType t, CrosshairProfile p, int ox, int oy, int aw, int ah) {
        int g = guiScale();
        // same center as the vanilla crosshair ((width - 15) / 2 + 7.5)
        double cx = ((aw - 15) / 2 + ox + 7.5) * g;
        double cy = ((ah - 15) / 2 + oy + 7.5) * g;

        if (p.pixelEnabled) {
            CrosshairTextures.Tex tex = CrosshairTextures.pixel(t, p, g);
            blit(ctx, tex, cx + p.offsetX * g - tex.cx(), cy + p.offsetY * g - tex.cy(), p.vanillaPipeline(), g);
        }

        if (p.imageEnabled && CrosshairTextures.hasImage(t)) {
            int wDev = Math.max(4, Math.min(256, p.imageSize)) * g;
            CrosshairTextures.Tex tex = CrosshairTextures.image(t, wDev);
            if (tex != null) {
                blit(ctx, tex, cx + p.imageOffsetX * g - tex.w() / 2.0, cy + p.imageOffsetY * g - tex.h() / 2.0, p.vanillaBlending, g);
            }
        }
    }

    /** Draws a texture 1:1 in real screen pixels (no resampling), top-left corner at (px, py) in device pixels. */
    private static void blit(DrawContext ctx, CrosshairTextures.Tex tex, double px, double py, boolean vanilla, int g) {
        int x0 = (int) Math.round(px);
        int y0 = (int) Math.round(py);
        var pipeline = vanilla ? RenderPipelines.CROSSHAIR : RenderPipelines.GUI_TEXTURED;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x0 / (float) g, y0 / (float) g);
        m.scale(1f / g, 1f / g);
        ctx.drawTexture(pipeline, tex.id(), 0, 0, 0f, 0f, tex.w(), tex.h(), tex.w(), tex.h());
        m.popMatrix();
    }
}
