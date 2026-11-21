package de.redclient;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Dropped Item Tweaks: search an item, tick the checkbox, then set how big it is when dropped on the ground. */
public class DroppedItemsScreen extends RedScreen {
    private static final int ROW_H = 28;
    private static final int GAP = 3;

    private static final class Entry {
        final Item item;
        final ItemStack stack;
        final String id, name, nameLower;

        Entry(Item item) {
            this.item = item;
            this.stack = new ItemStack(item);
            this.id = DroppedItemTweaks.idOf(item);
            this.name = stack.getName().getString();
            this.nameLower = name.toLowerCase(Locale.ROOT);
        }
    }

    private static List<Entry> ALL;

    private static String lastQuery = "";

    private TextFieldWidget search;
    private final SmoothScroll scroll = new SmoothScroll(13.0);
    private List<Entry> shown = new ArrayList<>();
    private Entry activeSlider;
    private final long openedAt = System.currentTimeMillis();

    private int panelX, panelW, listTop, listBottom;

    public DroppedItemsScreen() {
        super(Text.literal("Dropped Item Tweaks"));
        if (ALL == null) {
            List<Entry> l = new ArrayList<>();
            for (Item item : Registries.ITEM) {
                if (item == Items.AIR) continue;
                l.add(new Entry(item));
            }
            l.sort(Comparator.comparing((Entry e) -> e.nameLower));
            ALL = l;
        }
    }

    @Override
    protected void init() {
        panelW = Math.min(420, width - 24);
        panelX = (width - panelW) / 2;
        listTop = 70;
        listBottom = height - 10;

        search = new TextFieldWidget(textRenderer, panelX, 40, panelW, 18, Text.literal("Search"));
        search.setPlaceholder(Text.literal("Search item..."));
        search.setText(lastQuery);
        search.setChangedListener(s -> {
            lastQuery = s;
            scroll.reset();
            refilter();
        });
        addDrawableChild(search);
        setInitialFocus(search);
        refilter();
    }

    private boolean selected(Entry e) {
        return RedConfig.INSTANCE.itemScales.containsKey(e.id);
    }

    /** Selected items first, then alphabetical. Re-sorted only when the search changes, so rows don't jump while ticking. */
    private void refilter() {
        String q = lastQuery.trim().toLowerCase(Locale.ROOT);
        List<Entry> out = new ArrayList<>();
        for (Entry e : ALL) {
            if (q.isEmpty() || e.nameLower.contains(q) || e.id.contains(q)) out.add(e);
        }
        out.sort(Comparator.comparing((Entry e) -> !selected(e)).thenComparing(e -> e.nameLower));
        shown = out;
    }

    private double maxScroll() {
        return Math.max(0, shown.size() * (ROW_H + GAP) - (listBottom - listTop));
    }

    private int sliderX() {
        return panelX + panelW - 170;
    }

    private int sliderW() {
        return 100;
    }

    private float scaleOf(Entry e) {
        Float f = RedConfig.INSTANCE.itemScales.get(e.id);
        return f == null ? 1f : f;
    }

    // ------------------------------------------------------------------ Render

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        double max = maxScroll();
        scroll.update(max);
        double sc = scroll.get();

        super.render(ctx, mouseX, mouseY, delta);
        drawHeader(ctx, "Dropped Item Tweaks");

        // back button
        int bx = width - 78, by = 5;
        boolean bh = inside(mouseX, mouseY, bx, by, 66, 16);
        ctx.fill(bx, by, bx + 66, by + 16, bh ? RED : 0xFF2A0808);
        border(ctx, bx, by, bx + 66, by + 16, RED);
        ctx.drawTextWithShadow(textRenderer, Text.literal("< Menu"), bx + 33 - textRenderer.getWidth("< Menu") / 2, by + 4, 0xFFFFFFFF);

        border(ctx, panelX - 1, 39, panelX + panelW + 1, 59, RED);
        String info = RedConfig.INSTANCE.itemScales.size() + " item(s) selected · tick a checkbox, then set the size";
        ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(info, panelW)), panelX, 58, 0xFF888888);

        ctx.enableScissor(panelX - 4, listTop, panelX + panelW + 4, listBottom);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(0f, (float) -sc);
        int first = Math.max(0, (int) (sc / (ROW_H + GAP)) - 1);
        int last = Math.min(shown.size(), (int) ((sc + listBottom - listTop) / (ROW_H + GAP)) + 2);
        for (int i = first; i < last; i++) {
            Entry e = shown.get(i);
            int y = listTop + i * (ROW_H + GAP);
            boolean sel = selected(e);
            boolean hov = mouseY >= listTop && mouseY < listBottom && inside(mouseX, mouseY + sc, panelX, y, panelW, ROW_H);

            ctx.fill(panelX, y, panelX + panelW, y + ROW_H, hov ? PANEL_HOVER : PANEL);
            ctx.fill(panelX, y, panelX + 3, y + ROW_H, sel ? RED_BRIGHT : RED_DARK);

            // checkbox
            int cx = panelX + 12, cy = y + 8;
            ctx.fill(cx, cy, cx + 12, cy + 12, 0xFF0A0A0A);
            border(ctx, cx, cy, cx + 12, cy + 12, sel ? RED_BRIGHT : RED_DARK);
            if (sel) {
                ctx.fill(cx + 3, cy + 3, cx + 9, cy + 9, RED_BRIGHT);
            }

            ctx.drawItem(e.stack, panelX + 32, y + 6);
            int textW = (sel ? sliderX() - 8 : panelX + panelW - 8) - (panelX + 54);
            ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(e.name, textW)),
                    panelX + 54, y + 5, sel ? 0xFFFFFFFF : 0xFFCCCCCC);
            ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(e.id, textW)),
                    panelX + 54, y + 16, 0xFF777777);

            if (sel) {
                float v = scaleOf(e);
                int tx = sliderX(), tw = sliderW();
                double r = (v - DroppedItemTweaks.MIN_SCALE) / (DroppedItemTweaks.MAX_SCALE - DroppedItemTweaks.MIN_SCALE);
                int fx = tx + (int) (tw * Math.max(0, Math.min(1, r)));
                ctx.fill(tx, y + 12, tx + tw, y + 16, 0xFF3A3A3A);
                ctx.fill(tx, y + 12, fx, y + 16, RED);
                ctx.fill(fx - 2, y + 8, fx + 2, y + 20, 0xFFFFFFFF);
                String t = String.format(Locale.ROOT, "%.2fx", v);
                ctx.drawTextWithShadow(textRenderer, Text.literal(t), tx + tw + 8, y + 10, 0xFFFFFFFF);
            }
        }
        m.popMatrix();
        ctx.disableScissor();

        if (shown.isEmpty()) {
            String s = "No items found";
            ctx.drawTextWithShadow(textRenderer, Text.literal(s), width / 2 - textRenderer.getWidth(s) / 2, listTop + 20, 0xFF888888);
        }

        if (max > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(18, (int) (trackH * (trackH / (trackH + max))));
            int barY = listTop + (int) ((trackH - barH) * (sc / max));
            int sbx = panelX + panelW + 6;
            ctx.fill(sbx, listTop, sbx + 3, listBottom, 0xFF1A1A1A);
            ctx.fill(sbx, barY, sbx + 3, barY + barH, RED);
        }

        float open = Math.max(0f, 1f - (System.currentTimeMillis() - openedAt) / 250f);
        if (open > 0.01f) ctx.fill(0, 0, width, height, withAlpha(0x000000, open * open));
    }

    // ------------------------------------------------------------------ Input

    private void setScaleFromMouse(Entry e, double mx) {
        double r = Math.max(0, Math.min(1, (mx - sliderX()) / sliderW()));
        double v = DroppedItemTweaks.MIN_SCALE + r * (DroppedItemTweaks.MAX_SCALE - DroppedItemTweaks.MIN_SCALE);
        v = Math.round(v / 0.05) * 0.05;
        RedConfig.INSTANCE.itemScales.put(e.id, (float) Math.max(DroppedItemTweaks.MIN_SCALE, Math.min(DroppedItemTweaks.MAX_SCALE, v)));
        DroppedItemTweaks.rebuild();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double mx = click.x(), my = click.y();
        activeSlider = null;

        if (inside(mx, my, width - 78, 5, 66, 16)) {
            client.setScreen(new RedMenuScreen());
            return true;
        }
        if (my < listTop || my >= listBottom) return false;

        double cy = my + scroll.get();
        int i = (int) ((cy - listTop) / (ROW_H + GAP));
        if (i < 0 || i >= shown.size()) return false;
        int y = listTop + i * (ROW_H + GAP);
        if (!inside(mx, cy, panelX, y, panelW, ROW_H)) return false;

        Entry e = shown.get(i);
        Map<String, Float> map = RedConfig.INSTANCE.itemScales;
        if (map.containsKey(e.id) && mx >= sliderX() - 6 && mx < sliderX() + sliderW() + 6) {
            activeSlider = e;
            setScaleFromMouse(e, mx);
        } else if (map.containsKey(e.id)) {
            map.remove(e.id);
            DroppedItemTweaks.rebuild();
        } else {
            map.put(e.id, DroppedItemTweaks.DEFAULT_SCALE);
            DroppedItemTweaks.rebuild();
        }
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (activeSlider != null) {
            setScaleFromMouse(activeSlider, click.x());
            return true;
        }
        return super.mouseDragged(click, offsetX, offsetY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll.add(-verticalAmount * 40, maxScroll());
        return true;
    }

    @Override
    public void removed() {
        RedConfig.save();
        super.removed();
    }
}
