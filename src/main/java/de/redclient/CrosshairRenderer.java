package de.redclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class CrosshairRenderer {
    /** Ingame-HUD. */
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        TargetType t = TargetType.current(mc);
        drawIn(ctx, t, RedConfig.INSTANCE.profile(t), 0, 0, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
    }

    /** Zeichnet das Crosshair zentriert in das Rechteck (ox, oy, aw, ah). */
    public static void drawIn(DrawContext ctx, TargetType t, CrosshairProfile p, int ox, int oy, int aw, int ah) {
        CrosshairTextures.Tex tex = null;
        int dw = 0, dh = 0;

        if (p.imageEnabled && CrosshairTextures.hasImage(t)) {
            tex = CrosshairTextures.image(t);
            if (tex != null) {
                dw = Math.max(4, Math.min(256, p.imageSize));
                dh = Math.max(1, Math.round(dw * tex.h() / (float) tex.w()));
            }
        }
        if (tex == null) {
            tex = CrosshairTextures.pixel(t, p);
            dw = tex.w();
            dh = tex.h();
        }

        // Vanilla-Blending = die echte Minecraft-Crosshair-Pipeline (invertiert den Hintergrund)
        var pipeline = p.vanillaBlending ? RenderPipelines.CROSSHAIR : RenderPipelines.GUI_TEXTURED;
        int x = ox + (aw - dw) / 2;
        int y = oy + (ah - dh) / 2;
        ctx.drawTexture(pipeline, tex.id(), x, y, 0f, 0f, dw, dh, tex.w(), tex.h(), tex.w(), tex.h());
    }
}
