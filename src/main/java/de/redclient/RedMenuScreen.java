package de.redclient;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/** Hauptmenue (Rechts-Shift): Suchleiste + scrollbare Modul-Liste. */
public class RedMenuScreen extends RedScreen {
    private record Module(String name, String desc, Supplier<Screen> opener) {}

    private static final List<Module> MODULES = List.of(
            new Module("Crosshair", "Eigenes Crosshair: Pixel, Outline, Dicke, eigenes Bild",
                    CrosshairScreen::new)
    );

    private static final int CARD_H = 44;
    private static final int GAP = 6;

    private static String lastQuery = "";

    private TextFieldWidget search;
    private double scroll;
    private int panelX, panelW, listTop, listBottom;

    public RedMenuScreen() {
        super(Text.literal("Menü"));
    }

    @Override
    protected void init() {
        panelW = Math.min(380, width - 40);
        panelX = (width - panelW) / 2;
        listTop = 66;
        listBottom = height - 24;

        search = new TextFieldWidget(textRenderer, panelX, 38, panelW, 18, Text.literal("Suche"));
        search.setPlaceholder(Text.literal("Suche..."));
        search.setText(lastQuery);
        search.setChangedListener(s -> {
            lastQuery = s;
            scroll = 0;
        });
        addDrawableChild(search);
        setInitialFocus(search);
    }

    private List<Module> filtered() {
        String q = lastQuery.trim().toLowerCase(Locale.ROOT);
        List<Module> out = new ArrayList<>();
        for (Module m : MODULES) {
            if (q.isEmpty() || m.name().toLowerCase(Locale.ROOT).contains(q)
                    || m.desc().toLowerCase(Locale.ROOT).contains(q)) {
                out.add(m);
            }
        }
        return out;
    }

    private double maxScroll(int count) {
        int total = count * (CARD_H + GAP);
        return Math.max(0, total - (listBottom - listTop));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        drawHeader(ctx, "Menü");
        border(ctx, panelX - 1, 37, panelX + panelW + 1, 57, RED);

        List<Module> list = filtered();
        scroll = Math.max(0, Math.min(scroll, maxScroll(list.size())));

        ctx.enableScissor(panelX, listTop, panelX + panelW, listBottom);
        for (int i = 0; i < list.size(); i++) {
            Module m = list.get(i);
            int y = listTop + i * (CARD_H + GAP) - (int) scroll;
            boolean hover = inside(mouseX, mouseY, panelX, y, panelW, CARD_H)
                    && mouseY >= listTop && mouseY < listBottom;
            ctx.fill(panelX, y, panelX + panelW, y + CARD_H, hover ? PANEL_HOVER : PANEL);
            ctx.fill(panelX, y, panelX + 4, y + CARD_H, hover ? RED_BRIGHT : RED);
            if (hover) border(ctx, panelX, y, panelX + panelW, y + CARD_H, RED);

            // Icon
            ctx.fill(panelX + 14, y + 8, panelX + 42, y + 36, 0xFF0A0A0A);
            border(ctx, panelX + 14, y + 8, panelX + 42, y + 36, RED_DARK);
            String initial = m.name().substring(0, 1);
            ctx.drawTextWithShadow(textRenderer, Text.literal(initial),
                    panelX + 28 - textRenderer.getWidth(initial) / 2, y + 18, RED_BRIGHT);

            ctx.drawTextWithShadow(textRenderer, Text.literal(m.name()), panelX + 52, y + 10, 0xFFFFFFFF);
            ctx.drawTextWithShadow(textRenderer,
                    Text.literal(textRenderer.trimToWidth(m.desc(), panelW - 90)), panelX + 52, y + 25, 0xFF999999);
            ctx.drawTextWithShadow(textRenderer, Text.literal(">"), panelX + panelW - 16, y + 18, hover ? 0xFFFFFFFF : RED);
        }
        ctx.disableScissor();

        if (list.isEmpty()) {
            String s = "Keine Treffer";
            ctx.drawTextWithShadow(textRenderer, Text.literal(s), width / 2 - textRenderer.getWidth(s) / 2, listTop + 20, 0xFF888888);
        }

        // Scrollbar
        double max = maxScroll(list.size());
        if (max > 0) {
            int trackH = listBottom - listTop;
            int barH = Math.max(16, (int) (trackH * (trackH / (trackH + max))));
            int barY = listTop + (int) ((trackH - barH) * (scroll / max));
            ctx.fill(panelX + panelW + 4, listTop, panelX + panelW + 7, listBottom, 0xFF1A1A1A);
            ctx.fill(panelX + panelW + 4, barY, panelX + panelW + 7, barY + barH, RED);
        }

        String hint = "Linksklick = Modul öffnen   |   Mausrad = scrollen   |   ESC = schließen";
        ctx.drawTextWithShadow(textRenderer, Text.literal(hint), width / 2 - textRenderer.getWidth(hint) / 2, height - 14, 0xFF777777);
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        if (click.button() != 0) return false;
        double mx = click.x(), my = click.y();
        if (my < listTop || my >= listBottom) return false;

        List<Module> list = filtered();
        for (int i = 0; i < list.size(); i++) {
            int y = listTop + i * (CARD_H + GAP) - (int) scroll;
            if (inside(mx, my, panelX, y, panelW, CARD_H)) {
                client.setScreen(list.get(i).opener().get());
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        scroll = Math.max(0, Math.min(maxScroll(filtered().size()), scroll - verticalAmount * 20));
        return true;
    }
}
