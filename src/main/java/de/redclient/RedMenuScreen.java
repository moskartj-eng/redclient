package de.redclient;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class RedMenuScreen extends Screen {
    private static final int CELL = 11;
    private static final int GX = 20, GY = 50;
    private static final int PX = GX + CrosshairProfile.SIZE * CELL + 20; // rechte Spalte

    private TargetType tab = TargetType.NORMAL;

    public RedMenuScreen() {
        super(Text.literal("Red Client"));
    }

    private CrosshairProfile profile() {
        return RedConfig.INSTANCE.profile(tab);
    }

    @Override
    protected void init() {
        // Tabs
        int tx = GX;
        for (TargetType t : TargetType.values()) {
            ButtonWidget b = ButtonWidget.builder(Text.literal(t.label), btn -> {
                tab = t;
                clearAndInit();
            }).dimensions(tx, 20, 80, 20).build();
            b.active = t != tab;
            addDrawableChild(b);
            tx += 84;
        }

        // Crosshair an/aus
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Eigenes Crosshair: " + (RedConfig.INSTANCE.customCrosshair ? "AN" : "AUS")),
                btn -> {
                    RedConfig.INSTANCE.customCrosshair = !RedConfig.INSTANCE.customCrosshair;
                    clearAndInit();
                }).dimensions(tx + 10, 20, 150, 20).build());

        // RGB-Slider
        addDrawableChild(new Channel(PX, 50, 150, "R", 16));
        addDrawableChild(new Channel(PX, 72, 150, "G", 8));
        addDrawableChild(new Channel(PX, 94, 150, "B", 0));

        // Farb-Presets
        String[] names = {"Weiß", "Rot", "Grün", "Blau"};
        int[] cols = {0xFFFFFF, 0xFF0000, 0x00FF00, 0x3366FF};
        for (int i = 0; i < 4; i++) {
            int col = cols[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(names[i]), btn -> {
                profile().color = col;
                clearAndInit();
            }).dimensions(PX + i * 38, 118, 36, 20).build());
        }

        // Blending
        addDrawableChild(ButtonWidget.builder(
                Text.literal("Vanilla-Blending: " + (profile().vanillaBlending ? "AN" : "AUS")),
                btn -> {
                    profile().vanillaBlending = !profile().vanillaBlending;
                    clearAndInit();
                }).dimensions(PX, 142, 150, 20).build());

        // Leeren / Standard
        addDrawableChild(ButtonWidget.builder(Text.literal("Alles leeren"), btn -> {
            profile().pixels = new boolean[CrosshairProfile.SIZE * CrosshairProfile.SIZE];
        }).dimensions(PX, 166, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Standard-Kreuz"), btn -> {
            CrosshairProfile fresh = CrosshairProfile.plus(profile().color);
            fresh.vanillaBlending = profile().vanillaBlending;
            RedConfig.INSTANCE.setProfile(tab, fresh);
        }).dimensions(PX, 190, 150, 20).build());

        addDrawableChild(ButtonWidget.builder(Text.literal("Fertig"), btn -> close())
                .dimensions(PX, 214, 150, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);

        ctx.drawTextWithShadow(textRenderer, Text.literal("Red Client – Crosshair: " + tab.label), GX, 6, 0xFFFF5555);

        // Raster
        CrosshairProfile p = profile();
        int size = CrosshairProfile.SIZE;
        ctx.fill(GX - 1, GY - 1, GX + size * CELL + 1, GY + size * CELL + 1, 0xFF000000);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                int cx = GX + x * CELL, cy = GY + y * CELL;
                int bg = ((x + y) % 2 == 0) ? 0xFF3A3A3A : 0xFF444444;
                ctx.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, bg);
                if (p.get(x, y)) ctx.fill(cx, cy, cx + CELL - 1, cy + CELL - 1, 0xFF000000 | p.color);
            }
        }
        ctx.drawTextWithShadow(textRenderer, Text.literal("Links = malen, Rechts = löschen"),
                GX, GY + size * CELL + 6, 0xFFAAAAAA);

        // 1:1-Vorschau
        ctx.drawTextWithShadow(textRenderer, Text.literal("Vorschau"), PX + 160, 50, 0xFFAAAAAA);
        ctx.fill(PX + 160, 62, PX + 160 + 39, 62 + 39, 0xFF6B8E5A);
        CrosshairRenderer.draw(ctx, p, PX + 160 + 12, 62 + 12);
    }

    private boolean paint(double mx, double my, int button) {
        int x = (int) Math.floor((mx - GX) / CELL);
        int y = (int) Math.floor((my - GY) / CELL);
        int size = CrosshairProfile.SIZE;
        if (x < 0 || y < 0 || x >= size || y >= size) return false;
        if (button == 0) profile().set(x, y, true);
        else if (button == 1) profile().set(x, y, false);
        else return false;
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) return true;
        return paint(click.x(), click.y(), click.button());
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        if (super.mouseDragged(click, offsetX, offsetY)) return true;
        return paint(click.x(), click.y(), click.button());
    }

    @Override
    public void removed() {
        RedConfig.save();
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private class Channel extends SliderWidget {
        private final String name;
        private final int shift;

        Channel(int x, int y, int w, String name, int shift) {
            super(x, y, w, 20, Text.empty(), ((profile().color >> shift) & 255) / 255.0);
            this.name = name;
            this.shift = shift;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            setMessage(Text.literal(name + ": " + (int) Math.round(value * 255)));
        }

        @Override
        protected void applyValue() {
            int v = (int) Math.round(value * 255);
            CrosshairProfile p = profile();
            p.color = (p.color & ~(255 << shift)) | (v << shift);
        }
    }
}
