package de.redclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** Gemeinsame rot-schwarze Optik. */
public abstract class RedScreen extends Screen {
    public static final int RED = 0xFFD01010;
    public static final int RED_BRIGHT = 0xFFE01818;
    public static final int RED_DARK = 0xFF5A0A0A;
    public static final int PANEL = 0xFF141414;
    public static final int PANEL_HOVER = 0xFF2A0C0C;

    protected RedScreen(Text title) {
        super(title);
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        ctx.fillGradient(0, 0, width, height, 0xF0180505, 0xF0050505);
    }

    protected void drawHeader(DrawContext ctx, String subtitle) {
        ctx.fill(0, 0, width, 26, 0xFF120303);
        ctx.fill(0, 26, width, 28, RED);
        ctx.drawTextWithShadow(textRenderer, Text.literal("RED CLIENT").formatted(Formatting.BOLD), 10, 9, RED_BRIGHT);
        ctx.drawTextWithShadow(textRenderer, Text.literal(subtitle), 10 + textRenderer.getWidth("RED CLIENT") + 10, 9, 0xFFAAAAAA);
    }

    protected static void border(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        ctx.fill(x1, y1, x2, y1 + 1, color);
        ctx.fill(x1, y2 - 1, x2, y2, color);
        ctx.fill(x1, y1, x1 + 1, y2, color);
        ctx.fill(x2 - 1, y1, x2, y2, color);
    }

    protected static boolean inside(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
