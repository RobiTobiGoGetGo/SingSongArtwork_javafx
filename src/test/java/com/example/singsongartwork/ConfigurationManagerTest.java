package com.example.singsongartwork;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DisplayName("ConfigurationManager Tests")
class ConfigurationManagerTest {

    @Test
    @DisplayName("Copy limit defaults include size and count")
    void testCopyLimitDefaults(@TempDir Path tempDir) {
        ConfigurationManager configManager = new ConfigurationManager(tempDir.resolve("config.properties"));

        assertEquals(ConfigurationManager.DEFAULT_MAX_COPY_SIZE_MB, configManager.getMaxCopySizeMb());
        assertEquals(ConfigurationManager.DEFAULT_MAX_COPY_COUNT, configManager.getMaxCopyCount());
    }

    @Test
    @DisplayName("Copy count limit persists and supports no limit")
    void testCopyCountPersistence(@TempDir Path tempDir) {
        Path configFile = tempDir.resolve("config.properties");
        ConfigurationManager configManager = new ConfigurationManager(configFile);

        configManager.saveMaxCopyCount(25);
        assertEquals(25, new ConfigurationManager(configFile).getMaxCopyCount());

        configManager.saveMaxCopyCount(ConfigurationManager.NO_LIMIT);
        assertEquals(ConfigurationManager.NO_LIMIT, new ConfigurationManager(configFile).getMaxCopyCount());
    }

    @Test
    @DisplayName("Zero copy limit values are treated as no limit")
    void testZeroLimitValuesAreNormalized(@TempDir Path tempDir) {
        Path configFile = tempDir.resolve("config.properties");
        ConfigurationManager configManager = new ConfigurationManager(configFile);

        configManager.saveMaxCopySizeMb(0);
        configManager.saveMaxCopyCount(0);

        ConfigurationManager reloaded = new ConfigurationManager(configFile);
        assertEquals(ConfigurationManager.NO_LIMIT, reloaded.getMaxCopySizeMb());
        assertEquals(ConfigurationManager.NO_LIMIT, reloaded.getMaxCopyCount());
    }
}

