package com.vltir.replay.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;


public class KiwiAddonsConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("kiwi-server-replay-addons");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("ServerReplay").resolve("kiwi-addons.json");

    private static KiwiAddonsConfig instance = new KiwiAddonsConfig();
    public static KiwiAddonsConfig get() {
        return instance;
    }

    public static void load() {
        if(Files.exists(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                KiwiAddonsConfig loaded = GSON.fromJson(reader, KiwiAddonsConfig.class);
                instance = (loaded != null) ? loaded : new KiwiAddonsConfig();
            } catch (IOException e) {
                LOGGER.error("Failed to read config, using defaults", e);
                instance = new KiwiAddonsConfig();
            }
        }
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(instance, writer);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save config", e);
        }
    }
}
