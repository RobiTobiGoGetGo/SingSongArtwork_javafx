package com.example.singsongartwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/**
 * Centralized configuration management for SingSongArtwork.
 * Handles all file I/O and preferences persistence.
 */
public class ConfigurationManager {
    private static final String KEY_LAST_MUSIC_DIRECTORY = "last.music.directory";
    private static final String KEY_LAST_ARTWORK_DIRECTORY = "last.artwork.directory";
    private static final String KEY_LAST_COPY_DIRECTORY = "last.copy.directory";
    private static final String KEY_UI_COLUMN_MODE = "ui.column.mode";
    private static final String KEY_UI_ROLE = "ui.role";

    private final Path configFile;

    public ConfigurationManager(Path configFile) {
        this.configFile = configFile;
    }

    /**
     * Get the last used music directory.
     */
    public Path getLastMusicDirectory() {
        try {
            if (Files.exists(configFile)) {
                Properties props = loadProperties();
                String lastDir = props.getProperty(KEY_LAST_MUSIC_DIRECTORY);
                if (lastDir != null && !lastDir.isBlank()) {
                    Path lastPath = Path.of(lastDir);
                    if (Files.isDirectory(lastPath)) {
                        return lastPath;
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
        return null;
    }

    /**
     * Save the music directory.
     */
    public void saveLastMusicDirectory(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = loadProperties();
            props.setProperty(KEY_LAST_MUSIC_DIRECTORY, directory.toString());
            saveProperties(props);
        } catch (IOException ex) {
            System.err.println("Warning: Could not save music directory preference: " + ex.getMessage());
        }
    }

    /**
     * Get the last used artwork directory.
     */
    public Path getLastArtworkDirectory() {
        try {
            if (Files.exists(configFile)) {
                Properties props = loadProperties();
                String lastArtworkDir = props.getProperty(KEY_LAST_ARTWORK_DIRECTORY);
                if (lastArtworkDir != null && !lastArtworkDir.isBlank()) {
                    Path lastPath = Path.of(lastArtworkDir);
                    if (Files.isDirectory(lastPath)) {
                        return lastPath;
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
        return null;
    }

    /**
     * Save the artwork directory.
     */
    public void saveLastArtworkDirectory(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = loadProperties();
            if (directory != null) {
                props.setProperty(KEY_LAST_ARTWORK_DIRECTORY, directory.toString());
            }
            saveProperties(props);
        } catch (IOException ex) {
            System.err.println("Warning: Could not save artwork directory preference: " + ex.getMessage());
        }
    }

    /**
     * Get the last used copy directory.
     */
    public Path getLastCopyDirectory() {
        try {
            if (Files.exists(configFile)) {
                Properties props = loadProperties();
                String lastCopyDir = props.getProperty(KEY_LAST_COPY_DIRECTORY);
                if (lastCopyDir != null && !lastCopyDir.isBlank()) {
                    Path lastPath = Path.of(lastCopyDir);
                    if (Files.isDirectory(lastPath)) {
                        return lastPath;
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
        return null;
    }

    /**
     * Save the copy directory.
     */
    public void saveLastCopyDirectory(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = loadProperties();
            if (directory != null) {
                props.setProperty(KEY_LAST_COPY_DIRECTORY, directory.toString());
            }
            saveProperties(props);
        } catch (IOException ex) {
            System.err.println("Warning: Could not save copy destination preference: " + ex.getMessage());
        }
    }

    /**
     * Get the UI column mode preference.
     * @return true for "More" mode, false for "Less" mode (default)
     */
    public boolean getColumnMode() {
        try {
            if (Files.exists(configFile)) {
                Properties props = loadProperties();
                String columnMode = props.getProperty(KEY_UI_COLUMN_MODE, "less").trim().toLowerCase();
                return "more".equals(columnMode);
            }
        } catch (Exception ex) {
            // Silently ignore
        }
        return false;
    }

    /**
     * Save the UI column mode preference.
     */
    public void saveColumnMode(boolean moreMode) {
        try {
            ensureConfigDirectory();
            Properties props = loadProperties();
            props.setProperty(KEY_UI_COLUMN_MODE, moreMode ? "more" : "less");
            saveProperties(props);
        } catch (IOException ex) {
            System.err.println("Warning: Could not save column mode preference: " + ex.getMessage());
        }
    }

    /**
     * Get the UI role preference.
     * Always returns false (User mode) at startup for fail-safe behavior.
     * The actual role is only stored for reference, not used on startup.
     */
    public boolean getAdminMode() {
        // FAIL-SAFE: Always start in User mode
        return false;
    }

    /**
     * Save the UI role preference.
     */
    public void saveAdminMode(boolean adminMode) {
        try {
            ensureConfigDirectory();
            Properties props = loadProperties();
            props.setProperty(KEY_UI_ROLE, adminMode ? "admin" : "user");
            saveProperties(props);
        } catch (IOException ex) {
            System.err.println("Warning: Could not save role preference: " + ex.getMessage());
        }
    }

    /**
     * Load properties from config file.
     */
    private Properties loadProperties() throws IOException {
        Properties props = new Properties();
        if (Files.exists(configFile)) {
            try (var in = Files.newInputStream(configFile)) {
                props.load(in);
            }
        }
        return props;
    }

    /**
     * Save properties to config file.
     */
    private void saveProperties(Properties props) throws IOException {
        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "SingSongArtwork Configuration");
        }
    }

    /**
     * Ensure config directory exists.
     */
    private void ensureConfigDirectory() throws IOException {
        Path configDir = configFile.getParent();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }
}

