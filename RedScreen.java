package de.redclient;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Random;

/** Gemeinsame rot-schwarze Optik + Hintergrundmusik fuer alle Red-Client-Screens. */
public abstract class RedScreen extends Screen {
    public static final int RED = 0xFFD01010;
    public static final int RED_BRIGHT = 0xFFE01818;
    public static final int RED_DARK = 0xFF5A0A0A;
    public static final int PANEL = 0xFF141414;
    public static final int PANEL_HOVER = 0xFF2A0C0C;

    // schwebende Glut-Partikel im Hintergrund
    private static final int PARTICLES = 46;
    private static final float[] PX = new float[PARTICLES], PS = new float[PARTICLES],
            PPH = new float[PARTICLES], PSZ = new float[PARTICLES], PDR = new float[PARTICLES];

    static {
        Random r = new Random(1337);
        for (int i = 0; i < PARTICLES; i++) {
            PX[i] = r.nextFloat();
            PS[i] = 0.015f + r.nextFloat() * 0.035f;
            PPH[i] = r.nextFloat();
            PSZ[i] = r.nextFloat() < 0.2f ? 2f : 1f;
            PDR[i] = r.nextFloat() * 6.28f;
        }
    }

    protected RedScreen(Text title) {
        super(title);
        MenuMusic.start();
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        MenuMusic.ensure();
        ctx.fillGradient(0, 0, width, height, 0xF0180505, 0xF0050505);
        // roter Schein von unten
        ctx.fillGradient(0, height - height / 3, width, height, 0x00000000, 0x30D01010);

        double t = System.nanoTime() / 1_000_000_000.0;
        for (int i = 0; i < PARTICLES; i++) {
            double life = (t * PS[i] + PPH[i]) % 1.0;
            float x = (float) (PX[i] * width + Math.sin(t * 0.6 + PDR[i]) * 6);
            float y = (float) ((1.0 - life) * height);
            float a = (float) Math.sin(life * Math.PI);            // ein- und ausblenden
            int alpha = Math.max(5, (int) (a * 110));
            int col = (alpha << 24) | 0xE01818;
            int sz = (int) PSZ[i];
            ctx.fill((int) x, (int) y, (int) x + sz, (int) y + sz, col);
        }
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

    /** Setzt die Deckkraft (0..1) einer RGB/ARGB-Farbe. Mindestens 5/255, weil Minecraft Alpha <= 3 als "voll" liest. */
    protected static int withAlpha(int rgb, float a) {
        int al = Math.max(5, Math.min(255, Math.round(a * 255f)));
        return (al << 24) | (rgb & 0xFFFFFF);
    }

    protected static int mix(int c1, int c2, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = Math.round(((c1 >>> 24) & 255) * (1 - t) + ((c2 >>> 24) & 255) * t);
        int r = Math.round(((c1 >> 16) & 255) * (1 - t) + ((c2 >> 16) & 255) * t);
        int g = Math.round(((c1 >> 8) & 255) * (1 - t) + ((c2 >> 8) & 255) * t);
        int b = Math.round((c1 & 255) * (1 - t) + (c2 & 255) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void removed() {
        MenuMusic.requestStop();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
