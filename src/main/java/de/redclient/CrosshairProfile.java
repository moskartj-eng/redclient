package de.redclient;

import java.util.Arrays;
import java.util.Objects;

public class CrosshairProfile {
    public static final int SIZE = 15;

    public static final float MIN_THICKNESS = 0.05f;
    public static final float MAX_THICKNESS = 8f;
    public static final float MIN_OUTLINE = 0.05f;
    public static final float MAX_OUTLINE = 4f;
    public static final float MAX_OFFSET = 64f;

    public boolean[] pixels = new boolean[SIZE * SIZE];
    public int color = 0xFFFFFF;
    public boolean vanillaBlending = true;

    /** Dicke in GUI-Pixeln (1.0 = ein Minecraft-Pixel). Darf kleiner als 1 sein. */
    public boolean thicknessEnabled = false; // aus = Vanilla-Breite (1)
    public float thickness = 1f;

    /** Outline in GUI-Pixeln. Darf kleiner als 1 sein. */
    public boolean outlineEnabled = false;
    public int outlineColor = 0x000000;
    public float outlineThickness = 1f;

    /** Pixel crosshair visible (can be combined with the custom image). */
    public boolean pixelEnabled = true;
    public boolean imageEnabled = false;
    public int imageSize = 32;

    /** Offset in GUI pixels relative to the screen center. */
    public float offsetX = 0f;
    public float offsetY = 0f;
    public float imageOffsetX = 0f;
    public float imageOffsetY = 0f;

    public static CrosshairProfile plus(int rgb) {
        CrosshairProfile p = new CrosshairProfile();
        p.color = rgb;
        p.outlineEnabled = true;       // standardmaessig schwarz umrandet
        p.outlineColor = 0x000000;
        p.outlineThickness = 0.5f;
        p.vanillaBlending = false;
        p.resetPixels();
        return p;
    }

    public void resetPixels() {
        pixels = new boolean[SIZE * SIZE];
        int c = SIZE / 2;
        for (int i = -5; i <= 5; i++) {
            pixels[c * SIZE + (c + i)] = true;
            pixels[(c + i) * SIZE + c] = true;
        }
    }

    public void clearPixels() {
        pixels = new boolean[SIZE * SIZE];
    }

    public boolean get(int x, int y) {
        return pixels != null && pixels.length == SIZE * SIZE && pixels[y * SIZE + x];
    }

    public void set(int x, int y, boolean v) {
        if (pixels == null || pixels.length != SIZE * SIZE) pixels = new boolean[SIZE * SIZE];
        pixels[y * SIZE + x] = v;
    }

    public float effectiveThickness() {
        return thicknessEnabled ? Math.max(MIN_THICKNESS, Math.min(MAX_THICKNESS, thickness)) : 1f;
    }

    public float effectiveOutline() {
        return outlineEnabled ? Math.max(MIN_OUTLINE, Math.min(MAX_OUTLINE, outlineThickness)) : 0f;
    }

    /** Die Vanilla-Invertier-Pipeline kann keine schwarze Outline zeigen, daher nur ohne Outline. */
    public boolean vanillaPipeline() {
        return vanillaBlending && !outlineEnabled;
    }

    public int visualKey() {
        return Objects.hash(Arrays.hashCode(pixels), color & 0xFFFFFF, effectiveThickness(),
                effectiveOutline(), outlineColor & 0xFFFFFF);
    }
}
