package de.redclient;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** TeamView: add players by name, pick a color per player. Marked players glow in that color. */
public class TeamViewScreen extends RedScreen {
    private static final int ROW_H = 28;
    private static final int GAP = 3;
    private static final int[] COLORS = {
            0xFF2020, 0xFF8A00, 0xFFE600, 0x30FF30, 0x20E0FF, 0x3060FF, 0xB030FF, 0xFF50C0, 0xFFFFFF
    };
    private static final int SW = 12, SW_GAP = 3;

    private TextFieldWidget field;
    private final SmoothScroll scroll = new SmoothScroll(13.0);
    private final long openedAt = System.currentTimeMillis();
    private String message = "";

    private int panelX, panelW, listTop, listBottom;

    public TeamViewScreen() {
        super(Text.literal("TeamView"));
    }

    @Override
    protected void init() {
        panelW = Math.min(420, width - 24);
        panelX = (width - panelW) / 2;
        listTop = 84;
        listBottom = height - 10;

        field = new TextFieldWidget(textRenderer, panelX, 40, panelW - 130, 18, Text.literal("Player name"));
        field.setPlaceholder(Text.literal("Player name..."));
        field.setMaxLength(32);
        addDrawableChild(field);
        setInitialFocus(field);
    }

    private List<Map.Entry<String, Integer>> entries() {
        return new ArrayList<>(RedConfig.INSTANCE.teamPlayers.entrySet());
    }

    private double maxScroll(int n) {
        return Math.max(0, n * (ROW_H + GAP) - (listBottom - listTop));
    }

    private int addX() {
        return panelX + panelW - 124;
    }

    private int resetX() {
        return panelX + panelW - 62;
    }

    private int swatchesX() {
        return panelX + panelW - 26 - COLORS.length * (SW + SW_GAP);
    }

    // ------------------------------------------------------------------ Render

    private void button(DrawContext ctx, int x, int y, int w, String label, boolean hov) {
        ctx.fill(x, y, x + w, y + 18, hov ? RED : 0xFF2A0808);
        border(ctx, x, y, x + w, y + 18, RED);
        ctx.drawTextWithShadow(textRenderer, Text.literal(label), x + w / 2 - textRenderer.getWidth(label) / 2, y + 5, 0xFFFFFFFF);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        List<Map.Entry<String, Integer>> list = entries();
        double max = maxScroll(list.size());
        scroll.update(max);
        double sc = scroll.get();

        super.render(ctx, mouseX, mouseY, delta);
        drawHeader(ctx, "TeamView");

        int bx = width - 78, by = 5;
        boolean bh = inside(mouseX, mouseY, bx, by, 66, 16);
        ctx.fill(bx, by, bx + 66, by + 16, bh ? RED : 0xFF2A0808);
        border(ctx, bx, by, bx + 66, by + 16, RED);
        ctx.drawTextWithShadow(textRenderer, Text.literal("< Menu"), bx + 33 - textRenderer.getWidth("< Menu") / 2, by + 4, 0xFFFFFFFF);

        border(ctx, panelX - 1, 39, panelX + panelW - 129, 59, RED);
        button(ctx, addX(), 40, 58, "Add", inside(mouseX, mouseY, addX(), 40, 58, 18));
        button(ctx, resetX(), 40, 62, "Reset", inside(mouseX, mouseY, resetX(), 40, 62, 18));

        boolean hook = TeamView.hookLoaded();
        String info = !hook ? "Mixins not loaded - add redclient.mixins.json to fabric.mod.json"
                : message.isEmpty() ? list.size() + " player(s) marked \u00b7 their name is shown in that color"
                : message;
        ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(info, panelW)), panelX, 64,
                hook ? 0xFF888888 : 0xFFFF5555);

        double trail = blurTrail(scroll);
        int passes = Math.abs(trail) > 0.6 ? 3 : 1;
        ctx.enableScissor(panelX - 4, listTop, panelX + panelW + 4, listBottom);
        var m = ctx.getMatrices();
        for (int pass = 0; pass < passes; pass++) {
            boolean main = pass == passes - 1;
            double off = main ? 0 : trail * (passes - 1 - pass) / (double) (passes - 1);
            m.pushMatrix();
            m.translate(0f, (float) -(sc - off));
            for (int i = 0; i < list.size(); i++) {
                int y = listTop + i * (ROW_H + GAP);
                if (y + ROW_H < listTop + sc - 20 || y > listBottom + sc + 20) continue;
                if (!main) {
                    ctx.fill(panelX, y, panelX + panelW, y + ROW_H, withAlpha(0xD01010, pass == 0 ? 0.08f : 0.14f));
                    continue;
                }
                String name = list.get(i).getKey();
                int col = list.get(i).getValue() == null ? TeamView.DEFAULT_COLOR : list.get(i).getValue();
                boolean hov = mouseY >= listTop && mouseY < listBottom && inside(mouseX, mouseY + sc, panelX, y, panelW, ROW_H);

                ctx.fill(panelX, y, panelX + panelW, y + ROW_H, hov ? PANEL_HOVER : PANEL);
                ctx.fill(panelX, y, panelX + 4, y + ROW_H, 0xFF000000 | col);

                int textW = swatchesX() - 10 - (panelX + 14);
                ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(name, textW)),
                        panelX + 14, y + 10, 0xFF000000 | col);

                for (int c = 0; c < COLORS.length; c++) {
                    int sx = swatchesX() + c * (SW + SW_GAP), sy = y + 8;
                    ctx.fill(sx, sy, sx + SW, sy + SW, 0xFF000000 | COLORS[c]);
                    if (COLORS[c] == col) border(ctx, sx - 2, sy - 2, sx + SW + 2, sy + SW + 2, 0xFFFFFFFF);
                }
                int xx = panelX + panelW - 20;
                ctx.drawTextWithShadow(textRenderer, Text.literal("x"), xx + 4, y + 10,
                        inside(mouseX, mouseY + sc, xx, y + 4, 16, 20) ? 0xFFFFFFFF : 0xFFD01010);
            }
            m.popMatrix();
        }
        ctx.disableScissor();

        if (list.isEmpty()) {
            String s = "No players yet - type a name and press Add";
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

    private void addPlayer() {
        String n = field.getText().trim();
        if (n.isEmpty()) return;
        if (n.contains(" ")) {
            message = "Player names have no spaces.";
            return;
        }
        String key = n.toLowerCase(Locale.ROOT);
        RedConfig.INSTANCE.teamPlayers.putIfAbsent(key, TeamView.DEFAULT_COLOR);
        RedConfig.save();
        field.setText("");
        message = "";
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double mx = click.x(), my = click.y();

        if (inside(mx, my, width - 78, 5, 66, 16)) {
            client.setScreen(new RedMenuScreen());
            return true;
        }
        if (inside(mx, my, addX(), 40, 58, 18)) {
            addPlayer();
            return true;
        }
        if (inside(mx, my, resetX(), 40, 62, 18)) {
            RedConfig.INSTANCE.teamPlayers.clear();
            RedConfig.save();
            message = "All players removed.";
            return true;
        }
        if (my < listTop || my >= listBottom) return false;

        List<Map.Entry<String, Integer>> list = entries();
        double cy = my + scroll.get();
        int i = (int) ((cy - listTop) / (ROW_H + GAP));
        if (i < 0 || i >= list.size()) return false;
        int y = listTop + i * (ROW_H + GAP);
        if (!inside(mx, cy, panelX, y, panelW, ROW_H)) return false;

        String name = list.get(i).getKey();
        if (inside(mx, cy, panelX + panelW - 20, y + 4, 16, 20)) {
            RedConfig.INSTANCE.teamPlayers.remove(name);
            RedConfig.save();
            return true;
        }
        for (int c = 0; c < COLORS.length; c++) {
            int sx = swatchesX() + c * (SW + SW_GAP);
            if (inside(mx, cy, sx - 1, y + 6, SW + 2, SW + 4)) {
                RedConfig.INSTANCE.teamPlayers.put(name, COLORS[c]);
                RedConfig.save();
                return true;
            }
        }
        return true;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll.add(-verticalAmount * 40, maxScroll(RedConfig.INSTANCE.teamPlayers.size()));
        return true;
    }

    @Override
    public void removed() {
        RedConfig.save();
        super.removed();
    }
}
