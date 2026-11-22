package de.redclient;

import net.minecraft.client.MinecraftClient;

/** Drops the whole held stack in one go when one of the configured hotkeys is pressed in-game. */
public final class StackDropper {
    private static boolean wasDown = false;

    private StackDropper() {}

    public static void tick(MinecraftClient mc) {
        RedConfig cfg = RedConfig.INSTANCE;
        if (!cfg.stackDropEnabled || cfg.stackDropKeys.isEmpty() || mc.player == null || mc.currentScreen != null) {
            wasDown = false;
            return;
        }
        long window = mc.getWindow().getHandle();
        boolean any = false;
        for (Hotkey k : cfg.stackDropKeys) {
            if (k.isDown(window)) {
                any = true;
                break;
            }
        }
        if (any && !wasDown) mc.player.dropSelectedItem(true);
        wasDown = any;
    }
}
