package de.redclient;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Files;
import java.nio.file.Path;

public class RedConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("redclient.json");

    public static RedConfig INSTANCE = new RedConfig();

    public boolean customCrosshair = true;
    public boolean musicEnabled = true;
    public float musicVolume = 0.5f;
    public CrosshairProfile normal = CrosshairProfile.plus(0xFFFFFF);
    public CrosshairProfile friendly = CrosshairProfile.plus(0x55FF55);
    public CrosshairProfile enemy = CrosshairProfile.plus(0xFF3333);

    public CrosshairProfile profile(TargetType t) {
        return switch (t) {
            case NORMAL -> normal;
            case FRIENDLY -> friendly;
            case ENEMY -> enemy;
        };
    }

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                RedConfig c = GSON.fromJson(Files.readString(FILE), RedConfig.class);
                if (c != null) {
                    if (c.normal == null) c.normal = CrosshairProfile.plus(0xFFFFFF);
                    if (c.friendly == null) c.friendly = CrosshairProfile.plus(0x55FF55);
                    if (c.enemy == null) c.enemy = CrosshairProfile.plus(0xFF3333);
                    INSTANCE = c;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            Files.writeString(FILE, GSON.toJson(INSTANCE));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
