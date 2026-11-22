package de.redclient;

import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.Entity;

import java.util.Locale;

/** TeamView lookup: which players are marked, and in which color. Used by the entity mixin. */
public final class TeamView {
    public static final int DEFAULT_COLOR = 0xFF2020;

    private TeamView() {}

    private static Boolean hook;

    /** True if our name mixin was actually applied to the game's Entity class. */
    public static boolean hookLoaded() {
        if (hook == null) hook = MixinMarker.class.isAssignableFrom(Entity.class);
        return hook;
    }

    /** Returns the highlight color (RGB) if this entity is a marked player, otherwise null. */
    public static Integer colorOf(Entity e) {
        if (RedConfig.INSTANCE.teamPlayers.isEmpty() || !(e instanceof AbstractClientPlayerEntity)) return null;
        return RedConfig.INSTANCE.teamPlayers.get(e.getName().getString().toLowerCase(Locale.ROOT));
    }
}
