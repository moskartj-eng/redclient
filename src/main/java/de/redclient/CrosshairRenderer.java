package de.redclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;

public class CrosshairRenderer {
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null || mc.currentScreen != null) return;
        if (!mc.options.getPerspective().isFirstPerson()) return;

        CrosshairProfile p = RedConfig.INSTANCE.profile(TargetType.current(mc));
        int x0 = (ctx.getScaledWindowWidth() - CrosshairProfile.SIZE) / 2;
        int y0 = (ctx.getScaledWindowHeight() - CrosshairProfile.SIZE) / 2;
        draw(ctx, p, x0, y0);
    }

    public static void draw(DrawContext ctx, CrosshairProfile p, int x0, int y0) {
        int argb = p.argb();
        for (int y = 0; y < CrosshairProfile.SIZE; y++) {
            for (int x = 0; x < CrosshairProfile.SIZE; x++) {
                if (p.get(x, y)) ctx.fill(x0 + x, y0 + y, x0 + x + 1, y0 + y + 1, argb);
            }
        }
    }
}
