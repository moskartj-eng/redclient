package de.redclient;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.PointerBuffer;

import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** Crosshair-Einstellungen: Pixel-Editor links, scrollbare Optionen rechts. */
public class CrosshairScreen extends RedScreen {
    private static final int LEFT_X = 14;
    private static final int LEFT_W = 165;

    private TargetType tab = TargetType.NORMAL;
    private final List<Row> rows = new ArrayList<>();
    private double scroll;
    private Slider activeSlider;
    private volatile String status = "";

    private int cell, gridY, listX, listW, listTop, listBottom;

    public CrosshairScreen() {
        super(Text.literal("Crosshair"));
    }

    private CrosshairProfile profile() {
        return RedConfig.INSTANCE.profile(tab);
    }

    @Override
    protected void init() {
        cell = Math.max(5, Math.min(11, (height - 60 - 70) / 15));
        gridY = 58;
        listX = LEFT_X + LEFT_W + 16;
        listW = Math.max(140, width - listX - 18);
        listTop = 34;
        listBottom = height - 10;
        buildRows();
    }

    // ------------------------------------------------------------------ Optionen

    private void buildRows() {
        rows.clear();
        CrosshairProfile p = profile();

        rows.add(new Toggle("Red Crosshair aktiv (aus = Vanilla)",
                () -> RedConfig.INSTANCE.customCrosshair, v -> RedConfig.INSTANCE.customCrosshair = v));

        rows.add(new Header("Farbe & Blending"));
        addChannels(() -> p.color, v -> p.color = v);
        rows.add(new Presets(v -> p.color = v));
        rows.add(new Toggle("Vanilla-Blending", () -> p.vanillaBlending, v -> p.vanillaBlending = v));

        rows.add(new Header("Dicke"));
        rows.add(new Toggle("Dicke ändern (aus = Vanilla-Breite)", () -> p.thicknessEnabled, v -> p.thicknessEnabled = v));
        rows.add(new Slider("Dicke", 1, 8, () -> p.thickness, v -> p.thickness = v));

        rows.add(new Header("Outline"));
        rows.add(new Toggle("Outline", () -> p.outlineEnabled, v -> p.outlineEnabled = v));
        rows.add(new Slider("Outline-Dicke", 1, 4, () -> p.outlineThickness, v -> p.outlineThickness = v));
        addChannels(() -> p.outlineColor, v -> p.outlineColor = v);
        rows.add(new Presets(v -> p.outlineColor = v));

        rows.add(new Header("Eigenes Bild"));
        rows.add(new Toggle("Bild als Crosshair verwenden", () -> p.imageEnabled, v -> p.imageEnabled = v));
        rows.add(new Button("Bild auswählen...", this::pickFile));
        rows.add(new Slider("Bildgröße", 4, 128, () -> p.imageSize, v -> p.imageSize = v));
        rows.add(new Info(() -> {
            String s = status;
            if (!s.isEmpty()) return s;
            return CrosshairTextures.hasImage(tab)
                    ? "Bild vorhanden. Neues Bild: Button oder Datei ins Fenster ziehen."
                    : "Kein Bild. Button klicken oder Datei ins Fenster ziehen.";
        }));

        rows.add(new Header("Pixel-Raster"));
        rows.add(new Button("Alles leeren", p::clearPixels));
        rows.add(new Button("Standard-Kreuz", p::resetPixels));
    }

    private void addChannels(IntSupplier get, IntConsumer set) {
        String[] names = {"Rot", "Grün", "Blau"};
        int[] shifts = {16, 8, 0};
        for (int i = 0; i < 3; i++) {
            int sh = shifts[i];
            rows.add(new Slider(names[i], 0, 255,
                    () -> (get.getAsInt() >> sh) & 255,
                    v -> set.accept((get.getAsInt() & ~(255 << sh)) | (v << sh))));
        }
    }

    // ------------------------------------------------------------------ Bild-Import

    private void pickFile() {
        status = "Datei-Dialog geöffnet...";
        Thread th = new Thread(() -> {
            String path = null;
            try {
                Class<?> c = Class.forName("org.lwjgl.util.tinyfd.TinyFileDialogs");
                Method m = c.getMethod("tinyfd_openFileDialog",
                        CharSequence.class, CharSequence.class, PointerBuffer.class, CharSequence.class, boolean.class);
                path = (String) m.invoke(null, "Crosshair-Bild wählen", "", null, "Bilder", false);
                if (path == null) status = "";
            } catch (Throwable t) {
                status = "Dialog nicht verfügbar - Bild einfach ins Fenster ziehen.";
            }
            if (path != null) {
                Path file = Path.of(path);
                client.execute(() -> importFile(file));
            }
        }, "redclient-filepicker");
        th.setDaemon(true);
        th.start();
    }

    private void importFile(Path file) {
        try {
            CrosshairTextures.importImage(tab, file);
            profile().imageEnabled = true;
            status = "Bild geladen: " + file.getFileName();
        } catch (Exception e) {
            status = "Bild konnte nicht gelesen werden (am besten PNG).";
        }
    }

    @Override
    public void onFilesDropped(List<Path> paths) {
        if (!paths.isEmpty()) importFile(paths.get(0));
    }

    // ------------------------------------------------------------------ Zeichnen

    private double maxScroll() {
        int total = 0;
        for (Row r : rows) total += r.h;
        return Math.max(0, total - (listBottom - listTop));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawHeader(ctx, "Crosshair");

        // Zurueck-Button
        int bx = width - 78, by = 5;
        boolean bh = inside(mouseX, mouseY, bx, by, 66, 16);
        ctx.fill(bx, by, bx + 66, by + 16, bh ? RED : 0xFF2A0808);
        border(ctx, bx, by, bx + 66, by + 16, RED);
        ctx.drawTextWithShadow(textRenderer, Text.literal("< Menü"), bx + 33 - textRenderer.getWidth("< Menü") / 2, by + 4, 0xFFFFFFFF);

        // Tabs
        int tw = LEFT_W / 3;
        TargetType[] types = TargetType.values();
        for (int i = 0; i < types.length; i++) {
            int tx = LEFT_X + i * tw;
            boolean sel = types[i] == tab;
            boolean hov = inside(mouseX, mouseY, tx, 34, tw - 2, 18);
            ctx.fill(tx, 34, tx + tw - 2, 52, sel ? RED : (hov ? PANEL_HOVER : PANEL));
            String l = types[i].label;
            ctx.drawTextWithShadow(textRenderer, Text.literal(l), tx + (tw - 2) / 2 - textRenderer.getWidth(l) / 2, 39, 0xFFFFFFFF);
        }

        // Pixel-Raster
        CrosshairProfile p = profile();
        int size = CrosshairProfile.SIZE;
        int gridPx = size * cell;
        ctx.fill(LEFT_X - 1, gridY - 1, LEFT_X + gridPx + 1, gridY + gridPx + 1, RED_DARK);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int cx = LEFT_X + x * cell, cy = gridY + y * cell;
                int bg = ((x + y) % 2 == 0) ? 0xFF222222 : 0xFF2A2A2A;
                ctx.fill(cx, cy, cx + cell - 1, cy + cell - 1, bg);
                if (p.get(x, y)) ctx.fill(cx, cy, cx + cell - 1, cy + cell - 1, 0xFF000000 | p.color);
            }
        }
        ctx.drawTextWithShadow(textRenderer, Text.literal("Links malen · Rechts löschen"), LEFT_X, gridY + gridPx + 4, 0xFF888888);

        // Vorschau (zwei Hintergruende, damit man das Blending sieht)
        int py = gridY + gridPx + 16;
        int ph = height - py - 10;
        if (ph >= 20) {
            ctx.fill(LEFT_X, py, LEFT_X + LEFT_W / 2, py + ph, 0xFFB8B8B8);
            ctx.fill(LEFT_X + LEFT_W / 2, py, LEFT_X + LEFT_W, py + ph, 0xFF2A2A2A);
            border(ctx, LEFT_X - 1, py - 1, LEFT_X + LEFT_W + 1, py + ph + 1, RED_DARK);
            ctx.enableScissor(LEFT_X, py, LEFT_X + LEFT_W, py + ph);
            CrosshairRenderer.drawIn(ctx, tab, p, LEFT_X, py, LEFT_W, ph);
            ctx.disableScissor();
            ctx.drawTextWithShadow(textRenderer, Text.literal("Vorschau"), LEFT_X + 3, py + 3, 0xFF666666);
        }

        // Optionsliste
        scroll = Math.max(0, Math.min(scroll, maxScroll()));
        ctx.enableScissor(listX, listTop, listX + listW, listBottom);
        int y = listTop - (int) scroll;
        for (Row r : rows) {
            if (y + r.h > listTop && y < listBottom) {
                boolean hov = mouseY >= listTop && mouseY < listBottom && inside(mouseX, mouseY, listX, y, listW, r.h - 2);
                r.draw(ctx, textRenderer, listX, y, listW, hov);
            }
            y += r.h;
        }
        ctx.disableScissor();

        double max = maxScroll();
        if (max > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(16, (int) (trackH * (trackH / (trackH + max))));
            int barY = listTop + (int) ((trackH - barH) * (scroll / max));
            ctx.fill(listX + listW + 5, listTop, listX + listW + 8, listBottom, 0xFF1A1A1A);
            ctx.fill(listX + listW + 5, barY, listX + listW + 8, barY + barH, RED);
        }
    }

    // ------------------------------------------------------------------ Maus

    private boolean paint(double mx, double my, int button) {
        int x = (int) Math.floor((mx - LEFT_X) / cell);
        int y = (int) Math.floor((my - gridY) / cell);
        int size = CrosshairProfile.SIZE;
        if (x < 0 || y < 0 || x >= size || y >= size) return false;
        if (button == 0) profile().set(x, y, true);
        else if (button == 1) profile().set(x, y, false);
        else return false;
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        int b = click.button();
        activeSlider = null;

        if (b == 0 && inside(mx, my, width - 78, 5, 66, 16)) {
            client.setScreen(new RedMenuScreen());
            return true;
        }

        if (b == 0) {
            int tw = LEFT_W / 3;
            TargetType[] types = TargetType.values();
            for (int i = 0; i < types.length; i++) {
                if (inside(mx, my, LEFT_X + i * tw, 34, tw - 2, 18)) {
                    tab = types[i];
                    scroll = 0;
                    status = "";
                    buildRows();
                    return true;
                }
            }
        }

        if (paint(mx, my, b)) return true;

        if (my >= listTop && my < listBottom && mx >= listX && mx < listX + listW) {
            int y = listTop - (int) scroll;
            for (Row r : rows) {
                if (my >= y && my < y + r.h - 2) {
                    if (r.click(listX, y, listW, mx, my, b)) {
                        if (r instanceof Slider s) activeSlider = s;
                        return true;
                    }
                    break;
                }
                y += r.h;
            }
        }
        return false;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (activeSlider != null) {
            activeSlider.setFromMouse(click.x(), listX, listW);
            return true;
        }
        return paint(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll(), scroll - verticalAmount * 22));
        return true;
    }

    @Override
    public void removed() {
        RedConfig.save();
    }

    // ------------------------------------------------------------------ Zeilen-Typen

    private abstract static class Row {
        final int h;

        Row(int h) {
            this.h = h;
        }

        abstract void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover);

        boolean click(int x, int y, int w, double mx, double my, int button) {
            return false;
        }

        static void bg(DrawContext ctx, int x, int y, int w, int h, boolean hover) {
            ctx.fill(x, y, x + w, y + h - 2, hover ? PANEL_HOVER : PANEL);
        }
    }

    private static final class Header extends Row {
        final String text;

        Header(String text) {
            super(22);
            this.text = text;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover) {
            ctx.drawTextWithShadow(tr, Text.literal(text.toUpperCase()), x + 2, y + 8, RED_BRIGHT);
            ctx.fill(x, y + h - 3, x + w, y + h - 2, RED_DARK);
        }
    }

    private static final class Toggle extends Row {
        final String label;
        final BooleanSupplier get;
        final Consumer<Boolean> set;

        Toggle(String label, BooleanSupplier get, Consumer<Boolean> set) {
            super(22);
            this.label = label;
            this.get = get;
            this.set = set;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover) {
            bg(ctx, x, y, w, h, hover);
            boolean on = get.getAsBoolean();
            ctx.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(label, w - 56)), x + 8, y + 6, 0xFFFFFFFF);
            int sx = x + w - 34;
            ctx.fill(sx, y + 4, sx + 26, y + 16, on ? RED : 0xFF3A3A3A);
            int kx = on ? sx + 14 : sx + 2;
            ctx.fill(kx, y + 6, kx + 10, y + 14, 0xFFFFFFFF);
        }

        @Override
        boolean click(int x, int y, int w, double mx, double my, int button) {
            if (button != 0) return false;
            set.accept(!get.getAsBoolean());
            return true;
        }
    }

    private static final class Slider extends Row {
        final String label;
        final int min, max;
        final IntSupplier get;
        final IntConsumer set;

        Slider(String label, int min, int max, IntSupplier get, IntConsumer set) {
            super(22);
            this.label = label;
            this.min = min;
            this.max = max;
            this.get = get;
            this.set = set;
        }

        int trackX(int x, int w) {
            return x + w / 2;
        }

        int trackW(int w) {
            return w / 2 - 10;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover) {
            bg(ctx, x, y, w, h, hover);
            int v = get.getAsInt();
            ctx.drawTextWithShadow(tr, Text.literal(label + ": " + v), x + 8, y + 6, 0xFFFFFFFF);
            int tx = trackX(x, w), tw = trackW(w);
            double r = (v - min) / (double) (max - min);
            int fx = tx + (int) (tw * r);
            ctx.fill(tx, y + 8, tx + tw, y + 12, 0xFF3A3A3A);
            ctx.fill(tx, y + 8, fx, y + 12, RED);
            ctx.fill(fx - 2, y + 4, fx + 2, y + 16, 0xFFFFFFFF);
        }

        void setFromMouse(double mx, int x, int w) {
            double r = (mx - trackX(x, w)) / trackW(w);
            r = Math.max(0, Math.min(1, r));
            set.accept(min + (int) Math.round(r * (max - min)));
        }

        @Override
        boolean click(int x, int y, int w, double mx, double my, int button) {
            if (button != 0) return false;
            setFromMouse(mx, x, w);
            return true;
        }
    }

    private static final class Presets extends Row {
        static final int[] COLORS = {0xFFFFFF, 0xFF0000, 0x00FF00, 0x3366FF, 0xFFFF00, 0x00FFFF, 0x000000};
        final IntConsumer set;

        Presets(IntConsumer set) {
            super(22);
            this.set = set;
        }

        int sw(int w) {
            return (w - 16 - 4 * (COLORS.length - 1)) / COLORS.length;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover) {
            bg(ctx, x, y, w, h, false);
            int sw = sw(w);
            for (int i = 0; i < COLORS.length; i++) {
                int sx = x + 8 + i * (sw + 4);
                ctx.fill(sx, y + 4, sx + sw, y + 16, 0xFF000000 | COLORS[i]);
                border(ctx, sx, y + 4, sx + sw, y + 16, RED_DARK);
            }
        }

        @Override
        boolean click(int x, int y, int w, double mx, double my, int button) {
            if (button != 0) return false;
            int sw = sw(w);
            for (int i = 0; i < COLORS.length; i++) {
                int sx = x + 8 + i * (sw + 4);
                if (mx >= sx && mx < sx + sw) {
                    set.accept(COLORS[i]);
                    return true;
                }
            }
            return false;
        }
    }

    private static final class Button extends Row {
        final String label;
        final Runnable action;

        Button(String label, Runnable action) {
            super(22);
            this.label = label;
            this.action = action;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover) {
            ctx.fill(x, y, x + w, y + h - 2, hover ? RED : 0xFF2A0808);
            border(ctx, x, y, x + w, y + h - 2, RED);
            ctx.drawTextWithShadow(tr, Text.literal(label), x + w / 2 - tr.getWidth(label) / 2, y + 6, 0xFFFFFFFF);
        }

        @Override
        boolean click(int x, int y, int w, double mx, double my, int button) {
            if (button != 0) return false;
            action.run();
            return true;
        }
    }

    private static final class Info extends Row {
        final Supplier<String> text;

        Info(Supplier<String> text) {
            super(22);
            this.text = text;
        }

        @Override
        void draw(DrawContext ctx, TextRenderer tr, int x, int y, int w, boolean hover) {
            ctx.drawTextWithShadow(tr, Text.literal(tr.trimToWidth(text.get(), w - 8)), x + 4, y + 6, 0xFF999999);
        }
    }
}
