package de.redclient;

public class CrosshairProfile {
    public static final int SIZE = 15;

    public boolean[] pixels = new boolean[SIZE * SIZE];
    public int color = 0xFFFFFF;           // RGB
    public boolean vanillaBlending = true; // true = leicht durchsichtig

    public static CrosshairProfile plus(int rgb) {
        CrosshairProfile p = new CrosshairProfile();
        p.color = rgb;
        int c = SIZE / 2;
        for (int i = -5; i <= 5; i++) {
            p.pixels[c * SIZE + (c + i)] = true;
            p.pixels[(c + i) * SIZE + c] = true;
        }
        return p;
    }

    public int argb() {
        int alpha = vanillaBlending ? 0xB0 : 0xFF;
        return (alpha << 24) | (color & 0xFFFFFF);
    }

    public boolean get(int x, int y) {
        return pixels != null && pixels.length == SIZE * SIZE && pixels[y * SIZE + x];
    }

    public void set(int x, int y, boolean v) {
        if (pixels == null || pixels.length != SIZE * SIZE) pixels = new boolean[SIZE * SIZE];
        pixels[y * SIZE + x] = v;
    }
}
