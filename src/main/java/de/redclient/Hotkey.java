package de.redclient;

import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

import java.util.Objects;

/** A key plus exact modifier state (Ctrl / Shift / Alt), like in the vanilla controls menu. */
public class Hotkey {
    public int key;
    public boolean ctrl, shift, alt;

    public Hotkey() {}

    public Hotkey(int key, int mods) {
        this.key = key;
        this.ctrl = (mods & 1) != 0;
        this.shift = (mods & 2) != 0;
        this.alt = (mods & 4) != 0;
    }

    /** Bitmask of the currently held modifiers: 1 = Ctrl, 2 = Shift, 4 = Alt. */
    public static int currentMods(long window) {
        int m = 0;
        if (down(window, GLFW.GLFW_KEY_LEFT_CONTROL) || down(window, GLFW.GLFW_KEY_RIGHT_CONTROL)) m |= 1;
        if (down(window, GLFW.GLFW_KEY_LEFT_SHIFT) || down(window, GLFW.GLFW_KEY_RIGHT_SHIFT)) m |= 2;
        if (down(window, GLFW.GLFW_KEY_LEFT_ALT) || down(window, GLFW.GLFW_KEY_RIGHT_ALT)) m |= 4;
        return m;
    }

    private static boolean down(long window, int key) {
        return GLFW.glfwGetKey(window, key) == GLFW.GLFW_PRESS;
    }

    public int mask() {
        return (ctrl ? 1 : 0) | (shift ? 2 : 0) | (alt ? 4 : 0);
    }

    public boolean isDown(long window) {
        return key > 0 && down(window, key) && currentMods(window) == mask();
    }

    public static String modsLabel(int mods) {
        StringBuilder sb = new StringBuilder();
        if ((mods & 1) != 0) sb.append("Ctrl + ");
        if ((mods & 2) != 0) sb.append("Shift + ");
        if ((mods & 4) != 0) sb.append("Alt + ");
        return sb.toString();
    }

    public String label() {
        String k;
        try {
            k = InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString();
        } catch (Throwable t) {
            k = "Key " + key;
        }
        return modsLabel(mask()) + k;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Hotkey h && h.key == key && h.mask() == mask();
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, mask());
    }
}
