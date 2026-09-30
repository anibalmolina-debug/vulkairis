package com.vulkairis.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Manages loading, saving, and accessing Vulkairis configuration.
 */
public class ConfigManager {
    private static final Logger LOGGER = LoggerFactory.getLogger("Vulkairis/Config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
            .getConfigDir()
            .resolve("vulkairis.json");

    private static VulkairisConfig currentConfig = new VulkairisConfig();

    public static synchronized VulkairisConfig loadConfig() {
        try {
            if (Files.exists(CONFIG_PATH)) {
                try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                    VulkairisConfig loaded = GSON.fromJson(reader, VulkairisConfig.class);
                    if (loaded != null) {
                        currentConfig = loaded;
                        LOGGER.info("[Vulkairis] Configuration loaded successfully from {}", CONFIG_PATH);
                        return currentConfig;
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.error("[Vulkairis] Failed to load configuration from {}. Using defaults. Error: {}",
                    CONFIG_PATH, e.getMessage());
        }

        // Save default config if not found or corrupted
        currentConfig = new VulkairisConfig();
        saveConfig();
        return currentConfig;
    }

    public static synchronized void saveConfig() {
        try {
            if (!Files.exists(CONFIG_PATH.getParent())) {
                Files.createDirectories(CONFIG_PATH.getParent());
            }
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(currentConfig, writer);
                LOGGER.info("[Vulkairis] Configuration saved to {}", CONFIG_PATH);
            }
        } catch (Exception e) {
            LOGGER.error("[Vulkairis] Failed to save configuration to {}: {}", CONFIG_PATH, e.getMessage());
        }
    }

    public static VulkairisConfig getConfig() {
        return currentConfig;
    }
}
