package de.redclient;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Schneller Lookup: Item -> Groessenfaktor fuer gedroppte Items (wird vom Renderer-Mixin benutzt). */
public final class DroppedItemTweaks {
    public static final float MIN_SCALE = 0.1f;
    public static final float MAX_SCALE = 32f;
    public static final float DEFAULT_SCALE = 3f;

    private static Map<Item, Float> cache = new HashMap<>();

    private DroppedItemTweaks() {}

    /** Nach jeder Aenderung an RedConfig.INSTANCE.itemScales aufrufen. */
    public static void rebuild() {
        Map<Item, Float> next = new HashMap<>();
        for (Map.Entry<String, Float> e : RedConfig.INSTANCE.itemScales.entrySet()) {
            Identifier id = Identifier.tryParse(e.getKey());
            if (id == null || !Registries.ITEM.containsId(id)) continue;
            float s = Math.max(MIN_SCALE, Math.min(MAX_SCALE, e.getValue() == null ? 1f : e.getValue()));
            next.put(Registries.ITEM.get(id), s);
        }
        cache = next;
    }

    public static float scaleFor(ItemStack stack) {
        if (!RedConfig.INSTANCE.droppedTweaksEnabled || cache.isEmpty() || stack == null) return 1f;
        Float s = cache.get(stack.getItem());
        return s == null ? 1f : s;
    }

    public static boolean flat() {
        return RedConfig.INSTANCE.droppedTweaksEnabled && RedConfig.INSTANCE.flatItems;
    }

    private static Boolean hook;

    /** True if our render mixin was actually applied to the game's item entity render state. */
    public static boolean hookLoaded() {
        if (hook == null) hook = MixinMarker.class.isAssignableFrom(net.minecraft.client.render.entity.state.ItemEntityRenderState.class);
        return hook;
    }

    public static String idOf(Item item) {
        return Registries.ITEM.getId(item).toString();
    }
}
