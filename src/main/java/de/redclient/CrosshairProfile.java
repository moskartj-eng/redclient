package de.redclient;

import java.util.Arrays;
import java.util.Objects;

public class CrosshairProfile {
    public static final int SIZE = 15;

    public boolean[] pixels = new boolean[SIZE * SIZE];
    public int color = 0xFFFFFF;
    public boolean vanillaBlending = true;

    public boolean thicknessEnabled = false; // aus = Vanilla-Breite (1)
    public int thickness = 2;

    public boolean outlineEnabled = false;
    public int outlineColor = 0x000000;
    public int outlineThickness = 1;

    public boolean imageEnabled = false;
    public int imageSize = 32;

    public static CrosshairProfile plus(int rgb) {
        CrosshairProfile p = new CrosshairProfile();
        p.color = rgb;
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

    public int effectiveThickness() {
        return thicknessEnabled ? Math.max(1, Math.min(8, thickness)) : 1;
    }

    public int effectiveOutline() {
        return outlineEnabled ? Math.max(1, Math.min(4, outlineThickness)) : 0;
    }

    public int visualKey() {
        return Objects.hash(Arrays.hashCode(pixels), color & 0xFFFFFF, effectiveThickness(),
                effectiveOutline(), outlineColor & 0xFFFFFF);
    }
}
