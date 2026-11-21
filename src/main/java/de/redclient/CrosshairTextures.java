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
import java.util.Set;

/** Baut aus dem Pixel-Raster (mit Dicke/Outline) bzw. dem hochgeladenen Bild echte Texturen. */
public final class CrosshairTextures {
    public record Tex(Identifier id, int w, int h) {}

    private static final Map<TargetType, Tex> PIXEL = new EnumMap<>(TargetType.class);
    private static final Map<TargetType, Integer> PIXEL_KEY = new EnumMap<>(TargetType.class);
    private static final Map<TargetType, Tex> IMAGE = new EnumMap<>(TargetType.class);
    private static final Set<TargetType> IMAGE_STALE = EnumSet.allOf(TargetType.class);

    private CrosshairTextures() {}

    public static Path imagePath(TargetType t) {
        return FabricLoader.getInstance().getConfigDir().resolve("redclient").resolve("crosshair_" + t.key + ".png");
    }

    public static boolean hasImage(TargetType t) {
        return Files.exists(imagePath(t));
    }

    private static Tex register(String name, NativeImage img) {
        Identifier id = Identifier.of("redclient", name);
        var tm = MinecraftClient.getInstance().getTextureManager();
        tm.destroyTexture(id);
        tm.registerTexture(id, new NativeImageBackedTexture(() -> name, img));
        return new Tex(id, img.getWidth(), img.getHeight());
    }

    public static Tex pixel(TargetType t, CrosshairProfile p) {
        int key = p.visualKey();
        Tex cur = PIXEL.get(t);
        if (cur != null && PIXEL_KEY.get(t) == key) return cur;

        int s = CrosshairProfile.SIZE;
        int tk = p.effectiveThickness();
        int ol = p.effectiveOutline();
        int pad = tk + ol;
        int n = s + 2 * pad;

        boolean[] fill = new boolean[n * n];
        int lo = -((tk - 1) / 2);
        int hi = tk / 2;
        for (int y = 0; y < s; y++) {
            for (int x = 0; x < s; x++) {
                if (!p.get(x, y)) continue;
                for (int dy = lo; dy <= hi; dy++) {
                    for (int dx = lo; dx <= hi; dx++) {
                        fill[(y + pad + dy) * n + (x + pad + dx)] = true;
                    }
                }
            }
        }

        int fc = 0xFF000000 | (p.color & 0xFFFFFF);
        int oc = 0xFF000000 | (p.outlineColor & 0xFFFFFF);
        NativeImage img = new NativeImage(NativeImage.Format.RGBA, n, n, true);
        for (int y = 0; y < n; y++) {
            for (int x = 0; x < n; x++) {
                int c = 0;
                if (fill[y * n + x]) c = fc;
                else if (ol > 0 && nearFill(fill, n, x, y, ol)) c = oc;
                img.setColorArgb(x, y, c);
            }
        }

        Tex tex = register("pixel_" + t.key, img);
        PIXEL.put(t, tex);
        PIXEL_KEY.put(t, key);
        return tex;
    }

    private static boolean nearFill(boolean[] fill, int n, int x, int y, int r) {
        for (int dy = -r; dy <= r; dy++) {
            for (int dx = -r; dx <= r; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= n || yy >= n) continue;
                if (fill[yy * n + xx]) return true;
            }
        }
        return false;
    }

    public static Tex image(TargetType t) {
        if (!IMAGE_STALE.contains(t)) return IMAGE.get(t);
        IMAGE_STALE.remove(t);
        IMAGE.remove(t);
        if (!hasImage(t)) return null;
        try (InputStream in = Files.newInputStream(imagePath(t))) {
            NativeImage img = NativeImage.read(in);
            IMAGE.put(t, register("image_" + t.key, img));
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
