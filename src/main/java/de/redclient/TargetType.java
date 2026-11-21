package de.redclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public enum TargetType {
    NORMAL("Normal"),
    FRIENDLY("Freundlich"),
    HOSTILE("Unfreundlich");

    public final String label;

    TargetType(String label) {
        this.label = label;
    }

    /** Spieler und Monster = unfreundlich, andere Mobs (Tiere, Dorfbewohner, ...) = freundlich. */
    public static TargetType current(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof PlayerEntity || e instanceof Monster) return HOSTILE;
            if (e instanceof MobEntity) return FRIENDLY;
        }
        return NORMAL;
    }
}
