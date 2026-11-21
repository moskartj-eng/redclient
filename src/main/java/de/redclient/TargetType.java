package de.redclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.Monster;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public enum TargetType {
    NORMAL("Normal", "normal"),
    FRIENDLY("Friendly", "friendly"),
    ENEMY("Enemy", "enemy");

    public final String label;
    public final String key;

    TargetType(String label, String key) {
        this.label = label;
        this.key = key;
    }

    /** Spieler und Monster = Enemy, andere Mobs (Tiere, Dorfbewohner, ...) = Friendly. */
    public static TargetType current(MinecraftClient mc) {
        HitResult hit = mc.crosshairTarget;
        if (hit instanceof EntityHitResult ehr) {
            Entity e = ehr.getEntity();
            if (e instanceof PlayerEntity || e instanceof Monster) return ENEMY;
            if (e instanceof MobEntity) return FRIENDLY;
        }
        return NORMAL;
    }
}
