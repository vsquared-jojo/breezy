package com.example.windfx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings, saved to config/windfx.json */
public class WindFxConfig {

    public enum ParticleMode implements dev.isxander.yacl3.api.NameableEnum {
        STREAKS("Wind lines (recommended)"),
        LEAVES("Leaves (vanilla flutter)"),
        DUST("Dust flecks");

        private final String label;
        ParticleMode(String label) { this.label = label; }

        @Override
        public Component getDisplayName() { return Component.literal(label); }
    }

    public boolean enabled = true;
    public int particlesPerSecond = 40;   // 0..200
    public float windSpeed = 1.0f;        // multiplier
    public int radius = 14;               // blocks around the player
    public float gustStrength = 1.0f;     // how strongly gusts vary things
    public ParticleMode mode = ParticleMode.STREAKS;
    public float lineLength = 1.0f;       // wind line length multiplier

    // ---- persistence ----
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE = FabricLoader.getInstance().getConfigDir().resolve("windfx.json");
    private static WindFxConfig instance = new WindFxConfig();

    public static WindFxConfig get() { return instance; }

    public static void load() {
        if (Files.exists(FILE)) {
            try (Reader r = Files.newBufferedReader(FILE)) {
                WindFxConfig c = GSON.fromJson(r, WindFxConfig.class);
                if (c != null) instance = c;
            } catch (Exception e) {
                System.err.println("[windfx] Could not read config, using defaults: " + e);
            }
        }
        instance.clamp();
        save();
    }

    public static void save() {
        instance.clamp();
        try (Writer w = Files.newBufferedWriter(FILE)) {
            GSON.toJson(instance, w);
        } catch (IOException e) {
            System.err.println("[windfx] Could not save config: " + e);
        }
    }

    private void clamp() {
        particlesPerSecond = Math.max(0, Math.min(200, particlesPerSecond));
        windSpeed = Math.max(0.1f, Math.min(4f, windSpeed));
        radius = Math.max(4, Math.min(32, radius));
        gustStrength = Math.max(0f, Math.min(2f, gustStrength));
        if (mode == null) mode = ParticleMode.STREAKS;
        lineLength = Math.max(0.3f, Math.min(3f, lineLength));
    }
}
