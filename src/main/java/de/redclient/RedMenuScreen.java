package de.redclient;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Supplier;

/**
 * Hauptmenue (Rechts-Shift): Logo, vier Kategorie-Kacheln, Suche und eine weich scrollende Modul-Liste.
 */
public class RedMenuScreen extends RedScreen {
    private static final Identifier LOGO = Identifier.of("redclient", "textures/gui/logo.png");
    private static final int LOGO_W = 1024, LOGO_H = 367;

    private enum Kind { ACTION, TOGGLE, SLIDER }

    private record Tab(String name, Identifier icon) {}

    private static final Tab[] TABS = {
            new Tab("Mods", Identifier.of("redclient", "textures/gui/icon_bolt.png")),
            new Tab("Settings", Identifier.of("redclient", "textures/gui/icon_gear.png")),
            new Tab("HUD", Identifier.of("redclient", "textures/gui/icon_crosshair.png")),
    };

    private static final class Card {
        final int tab;
        final String name, desc;
        final Kind kind;
        Supplier<Screen> opener;
        Identifier icon;
        int iconTex = 64; // texture size in px
        BooleanSupplier state;
        Consumer<Boolean> setState;
        DoubleSupplier value;
        DoubleConsumer setValue;

        Card(int tab, String name, String desc, Kind kind) {
            this.tab = tab;
            this.name = name;
            this.desc = desc;
            this.kind = kind;
        }
    }

    private static final List<Card> CARDS = new ArrayList<>();

    static {
        Card c = new Card(0, "Crosshair Editor", "Pixels, outline, thickness, position, custom image", Kind.ACTION);
        c.opener = CrosshairScreen::new;
        c.icon = Identifier.of("redclient", "textures/gui/icon_crosshair.png");
        CARDS.add(c);

        c = new Card(0, "Dropped Item Tweaks", "Change the size of dropped items per item type", Kind.ACTION);
        c.opener = DroppedItemsScreen::new;
        c.icon = Identifier.of("redclient", "textures/gui/icon_apple.png");
        c.iconTex = 128;
        CARDS.add(c);

        c = new Card(0, "TeamView", "Mark teammates by name, in a color of your choice", Kind.ACTION);
        c.opener = TeamViewScreen::new;
        c.icon = Identifier.of("redclient", "textures/gui/icon_team.png");
        c.iconTex = 128;
        CARDS.add(c);
    }

    private static final int CARD_H = 44;
    private static final int GAP = 6;

    private static String lastQuery = "";
    private static int lastTab = 0;

    private TextFieldWidget search;
    private final SmoothScroll scroll = new SmoothScroll(13.0);
    private final float[] tileHover = new float[TABS.length];
    private final Map<String, Float> cardHover = new HashMap<>();
    private Card activeSlider;
    private long openedAt = System.currentTimeMillis();
    private long tabChangedAt = System.currentTimeMillis();
    private long lastFrame = System.nanoTime();

    // Layout (in init/render berechnet)
    private int panelX, panelW, logoX, logoY, logoW, logoH, searchY, tilesY, tileH, tileW, listTop, listBottom;

    public RedMenuScreen() {
        super(Text.literal("Menu"));
    }

    @Override
    protected void init() {
        panelW = Math.min(400, width - 24);
        panelX = (width - panelW) / 2;

        logoH = Math.max(40, Math.min(128, (int) (height * 0.27)));
        logoW = logoH * LOGO_W / LOGO_H;
        if (logoW > width - 8) {
            logoW = width - 8;
            logoH = logoW * LOGO_H / LOGO_W;
        }
        logoX = (width - logoW) / 2;
        logoY = 4;

        searchY = logoY + logoH - 6;
        tilesY = searchY + 18 + 8;
        tileH = height < 300 ? 40 : 52;
        int gap = 6;
        tileW = (panelW - (TABS.length - 1) * gap) / TABS.length;
        listTop = tilesY + tileH + 12;
        listBottom = height - 10;

        search = new TextFieldWidget(textRenderer, panelX, searchY, panelW, 18, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search..."));
        search.setText(lastQuery);
        search.setChangedListener(s -> {
            lastQuery = s;
            scroll.reset();
            tabChangedAt = System.currentTimeMillis();
        });
        addDrawableChild(search);
        setInitialFocus(search);
    }

    private List<Card> visible() {
        String q = lastQuery.trim().toLowerCase(Locale.ROOT);
        List<Card> out = new ArrayList<>();
        for (Card c : CARDS) {
            if (q.isEmpty()) {
                if (c.tab == lastTab) out.add(c);
            } else if (c.name.toLowerCase(Locale.ROOT).contains(q) || c.desc.toLowerCase(Locale.ROOT).contains(q)
                    || TABS[c.tab].name().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(c);
            }
        }
        return out;
    }

    private double maxScroll(int count) {
        int total = count * (CARD_H + GAP);
        return Math.max(0, total - (listBottom - listTop));
    }

    private static float ease(float t) {
        t = Math.max(0f, Math.min(1f, t));
        return 1f - (1f - t) * (1f - t) * (1f - t);
    }

    private float approach(float cur, boolean on, float dt) {
        float target = on ? 1f : 0f;
        return cur + (target - cur) * (1f - (float) Math.exp(-dt * 16f));
    }

    // ------------------------------------------------------------------ Zeichnen

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long nowN = System.nanoTime();
        float dt = Math.min(0.1f, (nowN - lastFrame) / 1_000_000_000f);
        lastFrame = nowN;
        long now = System.currentTimeMillis();

        List<Card> list = visible();
        double max = maxScroll(list.size());
        scroll.update(max);
        double sc = scroll.get();

        super.render(ctx, mouseX, mouseY, delta); // Hintergrund + Suchfeld

        // ---- Logo (fixed, no movement)
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, LOGO, logoX, logoY,
                0f, 0f, logoW, logoH, LOGO_W, LOGO_H, LOGO_W, LOGO_H);

        border(ctx, panelX - 1, searchY - 1, panelX + panelW + 1, searchY + 19, RED);

        // ---- Kacheln
        for (int i = 0; i < TABS.length; i++) {
            int tx = panelX + i * (tileW + 6);
            boolean hov = inside(mouseX, mouseY, tx, tilesY, tileW, tileH);
            boolean sel = i == lastTab && lastQuery.isBlank();
            tileHover[i] = approach(tileHover[i], hov, dt);
            float h = tileHover[i];

            int bg = mix(0xFF120808, PANEL_HOVER, h);
            ctx.fill(tx, tilesY, tx + tileW, tilesY + tileH, bg);
            if (sel) {
                ctx.fillGradient(tx, tilesY, tx + tileW, tilesY + tileH, 0x30D01010, 0x60D01010);
                ctx.fill(tx, tilesY + tileH - 2, tx + tileW, tilesY + tileH, RED_BRIGHT);
            }
            border(ctx, tx, tilesY, tx + tileW, tilesY + tileH, sel ? RED_BRIGHT : mix(RED_DARK, RED, h));
            // abgeschnittene Ecken wie im Vorbild
            int c = 0xFF050505;
            ctx.fill(tx, tilesY, tx + 3, tilesY + 1, c);
            ctx.fill(tx, tilesY, tx + 1, tilesY + 3, c);
            ctx.fill(tx + tileW - 3, tilesY + tileH - 1, tx + tileW, tilesY + tileH, c);
            ctx.fill(tx + tileW - 1, tilesY + tileH - 3, tx + tileW, tilesY + tileH, c);

            int icon = tileH >= 50 ? 24 : 18;
            int iy = tilesY + 6 - Math.round(h * 2);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TABS[i].icon(), tx + (tileW - icon) / 2, iy,
                    0f, 0f, icon, icon, 64, 64, 64, 64);
            String label = TABS[i].name();
            int lc = sel ? 0xFFFFFFFF : mix(0xFFAAAAAA, 0xFFFFFFFF, h);
            ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(label, tileW - 4)),
                    tx + tileW / 2 - textRenderer.getWidth(textRenderer.trimToWidth(label, tileW - 4)) / 2,
                    tilesY + tileH - 12, lc);
        }

        // ---- Module list: smooth scrolling with a light motion blur (faint trailing copies while moving)
        double vel = scroll.target() - sc;                       // how far the scroll still has to travel
        double trail = Math.max(-16, Math.min(16, vel * 0.22));  // trail length in px, follows the motion
        boolean blur = Math.abs(trail) > 0.6;
        ctx.enableScissor(panelX - 4, listTop, panelX + panelW + 4, listBottom);
        var m = ctx.getMatrices();
        int passes = blur ? 3 : 1;
        for (int pass = 0; pass < passes; pass++) {
            boolean main = pass == passes - 1;
            // ghosts first (furthest behind), the real list last and fully opaque
            double off = main ? 0 : trail * (passes - 1 - pass) / (double) (passes - 1);
            float ghostAlpha = main ? 1f : (pass == 0 ? 0.16f : 0.28f);
            m.pushMatrix();
            m.translate(0f, (float) -(sc - off));
            for (int i = 0; i < list.size(); i++) {
                Card card = list.get(i);
                float p = ease((now - tabChangedAt - i * 45L) / 260f);
                int y = listTop + i * (CARD_H + GAP) + Math.round((1f - p) * 12f);
                if (y + CARD_H < listTop + sc - 20 || y > listBottom + sc + 20) continue;

                float h;
                if (main) {
                    boolean hov = mouseY >= listTop && mouseY < listBottom
                            && inside(mouseX, mouseY + sc, panelX, y, panelW, CARD_H);
                    h = approach(cardHover.getOrDefault(card.name, 0f), hov, dt);
                    cardHover.put(card.name, h);
                } else {
                    h = cardHover.getOrDefault(card.name, 0f);
                }
                drawCard(ctx, card, y, h, p * ghostAlpha);
            }
            m.popMatrix();
        }
        ctx.disableScissor();

        if (list.isEmpty()) {
            String s = lastQuery.isBlank() ? "Coming soon ..." : "No results";
            ctx.drawTextWithShadow(textRenderer, Text.literal(s),
                    width / 2 - textRenderer.getWidth(s) / 2, listTop + 20, 0xFF888888);
        }

        // ---- Scrollbar (gleitet mit)
        if (max > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(18, (int) (trackH * (trackH / (trackH + max))));
            int barY = listTop + (int) ((trackH - barH) * (sc / max));
            int bx = panelX + panelW + 6;
            ctx.fill(bx, listTop, bx + 3, listBottom, 0xFF1A1A1A);
            ctx.fill(bx, barY, bx + 3, barY + barH, RED);
        }

        // ---- Einblenden beim Oeffnen
        float open = Math.max(0f, 1f - (now - openedAt) / 320f);
        if (open > 0.01f) ctx.fill(0, 0, width, height, withAlpha(0x000000, open * open));
    }

    private void drawCard(DrawContext ctx, Card c, int y, float h, float alpha) {
        int x = panelX, w = panelW;
        ctx.fill(x, y, x + w, y + CARD_H, withAlpha(mix(PANEL, PANEL_HOVER, h) & 0xFFFFFF, alpha));
        ctx.fill(x, y, x + 4 + Math.round(h * 2), y + CARD_H, withAlpha(mix(RED, RED_BRIGHT, h) & 0xFFFFFF, alpha));
        border(ctx, x, y, x + w, y + CARD_H, withAlpha(mix(RED_DARK, RED, h) & 0xFFFFFF, alpha * (0.5f + 0.5f * h)));

        // Icon-Kachel mit Kategorie-Symbol
        ctx.fill(x + 14, y + 8, x + 42, y + 36, withAlpha(0x0A0A0A, alpha));
        border(ctx, x + 14, y + 8, x + 42, y + 36, withAlpha(RED_DARK & 0xFFFFFF, alpha));
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, c.icon != null ? c.icon : TABS[c.tab].icon(), x + 18, y + 12, 0f, 0f, 20, 20,
                c.iconTex, c.iconTex, c.iconTex, c.iconTex);

        ctx.drawTextWithShadow(textRenderer, Text.literal(c.name), x + 52, y + 10, withAlpha(0xFFFFFF, alpha));
        int rightPad = c.kind == Kind.SLIDER ? 130 : 60;
        ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(c.desc, w - 52 - rightPad)),
                x + 52, y + 25, withAlpha(0x999999, alpha));

        switch (c.kind) {
            case ACTION -> ctx.drawTextWithShadow(textRenderer, Text.literal(">"), x + w - 16 + Math.round(h * 3), y + 18,
                    withAlpha(h > 0.5f ? 0xFFFFFF : 0xD01010, alpha));
            case TOGGLE -> {
                boolean on = c.state.getAsBoolean();
                int sx = x + w - 40;
                ctx.fill(sx, y + 15, sx + 28, y + 29, withAlpha(on ? 0xD01010 : 0x3A3A3A, alpha));
                int kx = on ? sx + 16 : sx + 2;
                ctx.fill(kx, y + 17, kx + 10, y + 27, withAlpha(0xFFFFFF, alpha));
            }
            case SLIDER -> {
                int tx = sliderX(), tw = sliderW();
                double r = Math.max(0, Math.min(1, c.value.getAsDouble()));
                int fx = tx + (int) (tw * r);
                ctx.fill(tx, y + 20, tx + tw, y + 24, withAlpha(0x3A3A3A, alpha));
                ctx.fill(tx, y + 20, fx, y + 24, withAlpha(0xD01010, alpha));
                ctx.fill(fx - 2, y + 16, fx + 2, y + 28, withAlpha(0xFFFFFF, alpha));
                String pct = Math.round(r * 100) + "%";
                ctx.drawTextWithShadow(textRenderer, Text.literal(pct), tx + tw + 6, y + 18, withAlpha(0xCCCCCC, alpha));
            }
        }
    }

    private int sliderX() {
        return panelX + panelW - 118;
    }

    private int sliderW() {
        return 76;
    }

    // ------------------------------------------------------------------ Eingabe

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double mx = click.x(), my = click.y();
        activeSlider = null;

        // Kacheln
        for (int i = 0; i < TABS.length; i++) {
            int tx = panelX + i * (tileW + 6);
            if (inside(mx, my, tx, tilesY, tileW, tileH)) {
                if (i != lastTab || !lastQuery.isBlank()) {
                    lastTab = i;
                    lastQuery = "";
                    search.setText("");
                    scroll.reset();
                    tabChangedAt = System.currentTimeMillis();
                }
                return true;
            }
        }

        // Karten
        if (my < listTop || my >= listBottom) return false;
        List<Card> list = visible();
        double cy = my + scroll.get();
        for (int i = 0; i < list.size(); i++) {
            int y = listTop + i * (CARD_H + GAP);
            if (!inside(mx, cy, panelX, y, panelW, CARD_H)) continue;
            Card c = list.get(i);
            switch (c.kind) {
                case ACTION -> client.setScreen(c.opener.get());
                case TOGGLE -> c.setState.accept(!c.state.getAsBoolean());
                case SLIDER -> {
                    activeSlider = c;
                    setSlider(c, mx);
                }
            }
            return true;
        }
        return false;
    }

    private void setSlider(Card c, double mx) {
        double r = (mx - sliderX()) / sliderW();
        c.setValue.accept(Math.max(0, Math.min(1, r)));
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (activeSlider != null) {
            setSlider(activeSlider, click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll.add(-verticalAmount * 34, maxScroll(visible().size()));
        return true;
    }

    @Override
    public void removed() {
        RedConfig.save();
        super.removed();
    }
}
