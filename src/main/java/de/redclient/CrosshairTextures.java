package de.redclient;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Baut aus dem Pixel-Raster bzw. dem hochgeladenen Bild echte Texturen.
 * <p>
 * Das Pixel-Crosshair wird in ECHTEN Bildschirm-Pixeln (nicht GUI-Pixeln) gerendert und mit Supersampling
 * geglaettet. Dadurch sind Dicke und Outline stufenlos und koennen duenner als ein Minecraft-Pixel sein.
 */
public final class CrosshairTextures {
    /** cx/cy = Mittelpunkt des Crosshairs innerhalb der Textur (in Textur-Pixeln). */
    public record Tex(Identifier id, int w, int h, double cx, double cy) {}

    private static final Map<TargetType, Tex> PIXEL = new EnumMap<>(TargetType.class);
    private static final Map<TargetType, Integer> PIXEL_KEY = new EnumMap<>(TargetType.class);
    private static final Map<TargetType, Tex> IMAGE = new EnumMap<>(TargetType.class);
    private static final Set<TargetType> IMAGE_STALE = EnumSet.allOf(TargetType.class);

    private static final float SQ2 = 1.4142135f;

    private CrosshairTextures() {}

    public static Path imagePath(TargetType t) {
        return FabricLoader.getInstance().getConfigDir().resolve("redclient").resolve("crosshair_" + t.key + ".png");
    }

    public static boolean hasImage(TargetType t) {
        return Files.exists(imagePath(t));
    }

    private static Tex register(String name, NativeImage img, double cx, double cy) {
        Identifier id = Identifier.of("redclient", name);
        var tm = MinecraftClient.getInstance().getTextureManager();
        tm.destroyTexture(id);
        tm.registerTexture(id, new NativeImageBackedTexture(() -> name, img));
        return new Tex(id, img.getWidth(), img.getHeight(), cx, cy);
    }

    // ------------------------------------------------------------------ Pixel-Crosshair

    /**
     * @param g GUI-Skalierung (echte Pixel pro GUI-Pixel)
     */
    public static Tex pixel(TargetType t, CrosshairProfile p, int g) {
        boolean premult = p.vanillaPipeline();
        int key = Objects.hash(p.visualKey(), g, premult);
        Tex cur = PIXEL.get(t);
        if (cur != null && PIXEL_KEY.get(t) == key) return cur;

        final int s = CrosshairProfile.SIZE;
        final double half = p.effectiveThickness() * g / 2.0;   // halbe Strichdicke in echten Pixeln
        final double olDev = p.effectiveOutline() * g;           // Outline-Breite in echten Pixeln
        final int pad = (int) Math.ceil(half) + (int) Math.ceil(olDev) + 2;
        final int nDev = s * g + 2 * pad;
        final int ss = nDev <= 96 ? 8 : (nDev <= 192 ? 4 : 2);   // Subsamples pro echtem Pixel
        final int n = nDev * ss;

        // 1) Fuellung als Strich-Modell: Punkt pro Zelle + Verbindungen zu gesetzten Nachbarn
        byte[] fill = new byte[n * n];
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                if (!p.get(x, y)) continue;
                double cx = pad + (x + 0.5) * g;
                double cy = pad + (y + 0.5) * g;
                rect(fill, n, ss, cx - half, cy - half, cx + half, cy + half);
                if (x + 1 < s && p.get(x + 1, y)) rect(fill, n, ss, cx - half, cy - half, cx + g + half, cy + half);
                if (y + 1 < s && p.get(x, y + 1)) rect(fill, n, ss, cx - half, cy - half, cx + half, cy + g + half);
            }
        }

        // 2) Abstand zur Fuellung (Chamfer) fuer die Outline
        float[] dist = null;
        float olSub = 0;
        if (olDev > 0) {
            olSub = (float) Math.max(1.0, olDev * ss);
            dist = distanceTransform(fill, n);
        }

        // 3) Auf echte Pixel herunterrechnen (Abdeckung -> Alpha)
        int fc = 0xFF000000 | (p.color & 0xFFFFFF);
        int oc = 0xFF000000 | (p.outlineColor & 0xFFFFFF);
        int fr = (fc >> 16) & 255, fg = (fc >> 8) & 255, fb = fc & 255;
        int or = (oc >> 16) & 255, og = (oc >> 8) & 255, ob = oc & 255;
        float total = ss * ss;

        NativeImage img = new NativeImage(NativeImage.Format.RGBA, nDev, nDev, true);
        for (int py = 0; py < nDev; py++) {
            for (int px = 0; px < nDev; px++) {
                int fCount = 0, oCount = 0;
                for (int sy = 0; sy < ss; sy++) {
                    int row = (py * ss + sy) * n + px * ss;
                    for (int sx = 0; sx < ss; sx++) {
                        int i = row + sx;
                        if (fill[i] != 0) fCount++;
                        else if (dist != null && dist[i] <= olSub) oCount++;
                    }
                }
                int argb = 0;
                if (fCount > 0 || oCount > 0) {
                    float af = fCount / total, ao = oCount / total;
                    float a = af + ao;
                    if (premult) {
                        // Vanilla-Blending: Farbe * Abdeckung, so wirkt duenn = schwaecher invertiert
                        argb = 0xFF000000
                                | (Math.round(fr * af) << 16) | (Math.round(fg * af) << 8) | Math.round(fb * af);
                    } else {
                        int r = Math.round((fr * af + or * ao) / a);
                        int gg = Math.round((fg * af + og * ao) / a);
                        int b = Math.round((fb * af + ob * ao) / a);
                        int al = Math.max(1, Math.min(255, Math.round(a * 255f)));
                        argb = (al << 24) | (r << 16) | (gg << 8) | b;
                    }
                }
                img.setColorArgb(px, py, argb);
            }
        }

        Tex tex = register("pixel_" + t.key, img, pad + s * g / 2.0, pad + s * g / 2.0);
        PIXEL.put(t, tex);
        PIXEL_KEY.put(t, key);
        return tex;
    }

    /** Fuellt ein Rechteck (in echten Pixeln, Subsample-genau). Mindestens 1 Subsample breit. */
    private static void rect(byte[] fill, int n, int ss, double ax, double ay, double bx, double by) {
        int w = Math.max(1, (int) Math.round((bx - ax) * ss));
        int h = Math.max(1, (int) Math.round((by - ay) * ss));
        int x0 = (int) Math.round(ax * ss), y0 = (int) Math.round(ay * ss);
        int x1 = Math.min(n, x0 + w), y1 = Math.min(n, y0 + h);
        x0 = Math.max(0, x0);
        y0 = Math.max(0, y0);
        for (int y = y0; y < y1; y++) {
            int row = y * n;
            for (int x = x0; x < x1; x++) fill[row + x] = 1;
        }
    }

    private static float[] distanceTransform(byte[] fill, int n) {
        float[] d = new float[n * n];
        final float inf = 1e9f;
        for (int i = 0; i < d.length; i++) d[i] = fill[i] != 0 ? 0f : inf;
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int i = y * n + x;
                float v = d[i];
                if (v == 0f) continue;
                if (x > 0) v = Math.min(v, d[i - 1] + 1f);
                if (y > 0) {
                    v = Math.min(v, d[i - n] + 1f);
                    if (x > 0) v = Math.min(v, d[i - n - 1] + SQ2);
                    if (x < n - 1) v = Math.min(v, d[i - n + 1] + SQ2);
                }
                d[i] = v;
            }
        }
        for (int y = n - 1; y >= 0; y--) {
            for (int x = n - 1; x >= 0; x--) {
                int i = y * n + x;
                float v = d[i];
                if (v == 0f) continue;
                if (x < n - 1) v = Math.min(v, d[i + 1] + 1f);
                if (y < n - 1) {
                    v = Math.min(v, d[i + n] + 1f);
                    if (x < n - 1) v = Math.min(v, d[i + n + 1] + SQ2);
                    if (x > 0) v = Math.min(v, d[i + n - 1] + SQ2);
                }
                d[i] = v;
            }
        }
        return d;
    }

    // ------------------------------------------------------------------ Eigenes Bild

    public static Tex image(TargetType t) {
        if (!IMAGE_STALE.contains(t)) return IMAGE.get(t);
        IMAGE_STALE.remove(t);
        IMAGE.remove(t);
        if (!hasImage(t)) return null;
        try (InputStream in = Files.newInputStream(imagePath(t))) {
            NativeImage img = NativeImage.read(in);
            IMAGE.put(t, register("image_" + t.key, img, img.getWidth() / 2.0, img.getHeight() / 2.0));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return IMAGE.get(t);
    }

    /** Prueft das Bild, kopiert es in den Config-Ordner und markiert es zum Neuladen. */
    public static void importImage(TargetType t, Path src) throws IOException {
        try (InputStream in = Files.newInputStream(src)) {
            NativeImage test = NativeImage.read(in);
            test.close();
        }
        Path dst = imagePath(t);
        Files.createDirectories(dst.getParent());
        Files.copy(src, dst, StandardCopyOption.REPLACE_EXISTING);
        IMAGE_STALE.add(t);
    }
}
