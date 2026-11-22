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

    // ------------------------------------------------------------------ Custom image

    private record Src(int[] argb, int w, int h) {}

    private static final Map<TargetType, Src> SRC = new EnumMap<>(TargetType.class);
    private static final Map<TargetType, Tex> IMAGE = new EnumMap<>(TargetType.class);

    /**
     * Returns the custom image scaled (area-averaged, so it stays sharp) to exactly wDev real screen pixels wide,
     * so it can be drawn 1:1 without the GPU blurring or aliasing it.
     */
    public static Tex image(TargetType t, int wDev) {
        if (IMAGE_STALE.remove(t)) {
            SRC.remove(t);
            IMAGE.remove(t);
            if (hasImage(t)) {
                try (InputStream in = Files.newInputStream(imagePath(t)); NativeImage img = NativeImage.read(in)) {
                    int[] px = new int[img.getWidth() * img.getHeight()];
                    for (int y = 0; y < img.getHeight(); y++)
                        for (int x = 0; x < img.getWidth(); x++) px[y * img.getWidth() + x] = img.getColorArgb(x, y);
                    SRC.put(t, new Src(px, img.getWidth(), img.getHeight()));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        Src src = SRC.get(t);
        if (src == null) return null;
        Tex cur = IMAGE.get(t);
        if (cur != null && cur.w() == wDev) return cur;

        int hDev = Math.max(1, Math.round(wDev * src.h() / (float) src.w()));
        NativeImage out = scale(src, wDev, hDev);
        Tex tex = register("image_" + t.key, out, wDev / 2.0, hDev / 2.0);
        IMAGE.put(t, tex);
        return tex;
    }

    /** Box/bilinear supersampling with premultiplied alpha (no dark fringes on transparent edges). */
    private static NativeImage scale(Src s, int dw, int dh) {
        double sx = s.w() / (double) dw, sy = s.h() / (double) dh;
        int kx = Math.max(1, Math.min(4, (int) Math.ceil(sx)));
        int ky = Math.max(1, Math.min(4, (int) Math.ceil(sy)));
        NativeImage out = new NativeImage(NativeImage.Format.RGBA, dw, dh, true);
        for (int y = 0; y < dh; y++) {
            for (int x = 0; x < dw; x++) {
                double a = 0, r = 0, g = 0, b = 0;
                for (int j = 0; j < ky; j++) {
                    double v = (y + (j + 0.5) / ky) * sy - 0.5;
                    for (int i = 0; i < kx; i++) {
                        double u = (x + (i + 0.5) / kx) * sx - 0.5;
                        int x0 = (int) Math.floor(u), y0 = (int) Math.floor(v);
                        double fx = u - x0, fy = v - y0;
                        for (int q = 0; q < 4; q++) {
                            int xx = Math.max(0, Math.min(s.w() - 1, x0 + (q & 1)));
                            int yy = Math.max(0, Math.min(s.h() - 1, y0 + (q >> 1)));
                            double wgt = ((q & 1) == 0 ? 1 - fx : fx) * ((q >> 1) == 0 ? 1 - fy : fy);
                            int c = s.argb()[yy * s.w() + xx];
                            double al = ((c >>> 24) & 255) / 255.0 * wgt;
                            a += al;
                            r += ((c >> 16) & 255) * al;
                            g += ((c >> 8) & 255) * al;
                            b += (c & 255) * al;
                        }
                    }
                }
                double n = kx * ky;
                int argb = 0;
                if (a > 1e-6) {
                    int al = (int) Math.round(255 * a / n);
                    argb = (Math.max(0, Math.min(255, al)) << 24)
                            | (Math.max(0, Math.min(255, (int) Math.round(r / a))) << 16)
                            | (Math.max(0, Math.min(255, (int) Math.round(g / a))) << 8)
                            | Math.max(0, Math.min(255, (int) Math.round(b / a)));
                }
                out.setColorArgb(x, y, argb);
            }
        }
        return out;
    }

    public static boolean imageReady(TargetType t) {
        return hasImage(t);
    }

    /** Validates the image, copies it into the config folder and marks it for reloading. */
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
