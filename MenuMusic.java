package de.redclient;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.MovingSoundInstance;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.random.Random;

/** Leise Hintergrundmusik, solange ein Red-Client-Menue offen ist (Fade-in / Fade-out, Loop). */
public final class MenuMusic {
    private static final Identifier ID = Identifier.of("redclient", "menu_music");
    private static Loop current;
    private static long startedAt;

    private MenuMusic() {}

    /** Startet die Musik (falls aktiv) bzw. bricht ein laufendes Ausblenden ab. */
    public static void start() {
        if (!RedConfig.INSTANCE.musicEnabled) return;
        if (current != null && !current.isDone()) {
            current.stopping = false;
            return;
        }
        current = new Loop();
        startedAt = System.currentTimeMillis();
        MinecraftClient.getInstance().getSoundManager().play(current);
    }

    /** Wird jeden Frame vom Menue aufgerufen: startet die Musik neu, falls sie (z.B. wegen Loop-Problemen) endete. */
    public static void ensure() {
        if (!RedConfig.INSTANCE.musicEnabled) return;
        if (current == null || current.isDone()) {
            start();
            return;
        }
        if (!current.stopping && System.currentTimeMillis() - startedAt > 1500
                && !MinecraftClient.getInstance().getSoundManager().isPlaying(current)) {
            current.stopping = true;
            current = null;
            start();
        }
    }

    /** Blendet aus, wenn danach kein Red-Screen mehr offen ist. */
    public static void requestStop() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.send(() -> {
            if (!(mc.currentScreen instanceof RedScreen)) fadeOut();
        });
    }

    public static void fadeOut() {
        if (current != null) current.stopping = true;
    }

    /** Nach Aenderung der Einstellungen (An/Aus). */
    public static void applySettings() {
        if (RedConfig.INSTANCE.musicEnabled) start();
        else fadeOut();
    }

    private static final class Loop extends MovingSoundInstance {
        boolean stopping;

        Loop() {
            super(SoundEvent.of(ID), SoundCategory.MUSIC, Random.create());
            this.repeat = true;
            this.repeatDelay = 0;
            this.relative = true;
            this.attenuationType = SoundInstance.AttenuationType.NONE;
            this.volume = 0.0001f;
        }

        @Override
        public void tick() {
            float target = stopping ? 0f : Math.max(0f, Math.min(1f, RedConfig.INSTANCE.musicVolume));
            float step = 0.02f;
            float v = this.volume;
            if (v < target) v = Math.min(target, v + step);
            else v = Math.max(target, v - step * 1.5f);
            this.volume = Math.max(0.0001f, v);
            if (stopping && v <= 0.001f) setDone();
        }
    }
}
