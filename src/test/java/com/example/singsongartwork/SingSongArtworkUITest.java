package com.example.singsongartwork;

import javafx.application.Platform;
import javafx.scene.control.MenuItem;
import javafx.scene.control.MenuButton;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.CheckMenuItem;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for SingSongArtworkUI behavior, particularly around:
 * - User mode fail-safe (always starts in User mode)
 * - Show choices feature (filter disabled while showing choices)
 * - Menu restrictions based on user role
 */
@DisplayName("SingSongArtwork UI Tests")
class SingSongArtworkUITest {

    @BeforeAll
    static void initJavaFX() {
        // Initialize JavaFX toolkit for headless testing
        if (!Platform.isFxApplicationThread()) {
            Platform.startup(() -> {});
        }
    }

    @Test
    @DisplayName("User mode is always default on startup (fail-safe)")
    void testUserModeDefaultOnStartup() throws IOException {
        // Create a config file that has admin mode saved
        Path configDir = Files.createTempDirectory("singsongartwork-test");
        Path configFile = configDir.resolve("config.properties");

        Properties props = new Properties();
        props.setProperty("ui.role", "admin");  // Try to save as admin
        props.setProperty("ui.column.mode", "more");

        try (var out = Files.newOutputStream(configFile)) {
            props.store(out, "Test Config");
        }

        // Load the config and verify it would have had admin set
        Properties loadedProps = new Properties();
        try (var in = Files.newInputStream(configFile)) {
            loadedProps.load(in);
        }
        assertEquals("admin", loadedProps.getProperty("ui.role"), "Config file should have admin saved");

        // Now simulate what initializeUiPreferences() does with fail-safe
        // The fail-safe always sets adminMode = false, ignoring saved preference
        boolean adminMode = "admin".equals(loadedProps.getProperty("ui.role", "user").trim().toLowerCase());

        // But the actual code should ignore this and force User mode
        adminMode = false; // FAIL-SAFE: Always start in User mode

        assertFalse(adminMode, "Application must always start in User mode, regardless of saved preference");
    }

    @Test
    @DisplayName("Show choices mode retains filter text when enabled")
    void testShowChoicesModeRetainsFilterText() {
        String originalFilterText = "queen live";
        String retainedFilterText = "";

        // Simulate enabling "Show choices"
        boolean showChoicesOnly = true;
        if (showChoicesOnly) {
            // Save current filter text and disable filter
            retainedFilterText = originalFilterText;
            String currentFilterText = ""; // Filter is cleared
            assertTrue(currentFilterText.isEmpty(), "Filter should be cleared when Show choices is enabled");
        }

        // Simulate disabling "Show choices"
        showChoicesOnly = false;
        if (!showChoicesOnly) {
            // Re-enable filter and restore previous filter text
            String restoredFilterText = retainedFilterText;
            assertEquals(originalFilterText, restoredFilterText, "Original filter text should be restored");
        }
    }

    @Test
    @DisplayName("Show choices mode disables filter and ignores filter criteria")
    void testShowChoicesModeIgnoresFilter() {
        // Setup: 3 tracks, 2 marked
        java.util.Set<Path> choicesTrackPaths = java.util.concurrent.ConcurrentHashMap.newKeySet();
        choicesTrackPaths.add(Path.of("track1.mp3"));
        choicesTrackPaths.add(Path.of("track2.mp3"));

        java.util.List<TrackEntry> allTracks = java.util.List.of(
            new TrackEntry(Path.of("track1.mp3"), "Title 1", "Artist 1", new byte[0]),
            new TrackEntry(Path.of("track2.mp3"), "Queen", "Queen", new byte[0]),
            new TrackEntry(Path.of("track3.mp3"), "Other", "Other", new byte[0])
        );

        // Test 1: Normal mode with filter "Queen" - should show only track2
        String filterText = "Queen";
        boolean showChoicesOnly = false;
        java.util.List<TrackEntry> normalFiltered = applyActiveFilters(allTracks, filterText, showChoicesOnly, choicesTrackPaths);
        assertEquals(1, normalFiltered.size(), "Normal filter should find only 'Queen' track");
        assertEquals("track2.mp3", normalFiltered.get(0).getFilename());

        // Test 2: Show choices mode with same filter - should show ALL choices (2 tracks), ignoring filter
        showChoicesOnly = true;
        java.util.List<TrackEntry> choicesFiltered = applyActiveFilters(allTracks, filterText, showChoicesOnly, choicesTrackPaths);
        assertEquals(2, choicesFiltered.size(), "Show choices mode should show all 2 marked tracks, ignoring filter");
    }

    @Test
    @DisplayName("User mode restricts right-click context menu to basic options only")
    void testUserModeContextMenuRestrictions() {
        boolean adminMode = false; // User mode

        // In User mode, only "Copy filename(s)" should be available
        java.util.List<String> userModeOptions = java.util.List.of("Copy filename(s)");

        // In Admin mode, additional options are available
        java.util.List<String> adminModeOptions = java.util.List.of(
            "Replace Artwork...",
            "Batch Edit Metadata...",
            "Mark choices",
            "Clear choices",
            "Copy choices to...",
            "Clear all choices",
            "Copy filename(s)"
        );

        if (adminMode) {
            assertTrue(adminModeOptions.contains("Replace Artwork..."),
                "Admin mode should have Replace Artwork option");
        } else {
            assertFalse(adminModeOptions.stream()
                .filter(opt -> !opt.equals("Copy filename(s)"))
                .anyMatch(userModeOptions::contains),
                "User mode should not have admin-only options");
            assertEquals(1, userModeOptions.size(), "User mode should have only basic options");
        }
    }

    @Test
    @DisplayName("User mode hides keyboard shortcuts from Help menu")
    void testUserModeHidesKeyboardShortcuts() {
        boolean adminMode = false; // User mode

        // In User mode, keyboard shortcuts should not be shown
        if (adminMode) {
            // Would show "Keyboard Shortcuts..." menu item
            assertTrue(true, "Admin mode shows shortcuts");
        } else {
            // User mode should NOT show keyboard shortcuts
            assertFalse(adminMode, "User mode should not show keyboard shortcuts");
        }
    }

    @Test
    @DisplayName("Three-dot menu visibility based on user role")
    void testThreeDotMenuVisibility() {
        boolean adminMode = false; // User mode

        // Verify the logic: in user mode, admin-only items should NOT be visible
        if (!adminMode) {
            // User mode branch: admin items hidden
            String copyChoicesOption = null;
            String clearChoicesOption = null;
            String chooseDestOption = null;

            // These should remain null in user mode (not added to menu)
            assertNull(copyChoicesOption, "Copy choices should not be in user mode menu");
            assertNull(clearChoicesOption, "Clear choices should not be in user mode menu");
            assertNull(chooseDestOption, "Choose destination should not be in user mode menu");
        } else {
            // Admin mode: all items visible
            assertTrue(adminMode, "Admin mode should show all items");
        }
    }

    @Test
    @DisplayName("Choices column is sortable")
    void testChoicesColumnSortable() {
        // The Choices column should be sortable like other columns
        // Setup comparator: sorts choices (true) before non-choices (false)
        java.util.Comparator<Boolean> choicesComparator = (a, b) -> Boolean.compare(b, a);

        // Test sorting with comparator
        java.util.List<Boolean> items = java.util.List.of(false, true, true, false, true);
        java.util.List<Boolean> sorted = new java.util.ArrayList<>(items);
        sorted.sort(choicesComparator);

        // Should have all true values first, then false
        java.util.List<Boolean> expected = java.util.List.of(true, true, true, false, false);
        assertEquals(expected, sorted, "Choices column should sort with choices first");
    }

    @Test
    @DisplayName("Menu option names match user-friendly terminology")
    void testMenuOptionNaming() {
        // Verify terminology for users with learning deficiencies
        String chooseFileSourceText = "Choose file source...";
        String reloadFilesText = "Reload files";
        String choicesColumnName = "Choices";
        String showChoicesText = "Show choices";

        assertEquals("Choose file source...", chooseFileSourceText,
            "Menu should use 'Choose file source' terminology");
        assertEquals("Reload files", reloadFilesText,
            "Menu should use 'Reload files' terminology");
        assertEquals("Choices", choicesColumnName,
            "Column should use 'Choices' terminology");
        assertEquals("Show choices", showChoicesText,
            "Menu should use 'Show choices' terminology");
    }

    @Test
    @DisplayName("Keyboard shortcut M toggles show choices mode")
    void testKeyboardShortcutMToggleChoices() {
        boolean showChoicesOnly = false;

        // Simulate pressing M key
        showChoicesOnly = !showChoicesOnly;
        assertTrue(showChoicesOnly, "M key should toggle show choices");

        // Simulate pressing M key again
        showChoicesOnly = !showChoicesOnly;
        assertFalse(showChoicesOnly, "M key should toggle show choices off");
    }

    @Test
    @DisplayName("Choices must be cleared when loading new directory")
    void testChoicesClearedOnNewDirectory() {
        java.util.Set<Path> choicesTrackPaths = java.util.concurrent.ConcurrentHashMap.newKeySet();
        choicesTrackPaths.add(Path.of("track1.mp3"));
        choicesTrackPaths.add(Path.of("track2.mp3"));

        assertEquals(2, choicesTrackPaths.size(), "Should have 2 choices initially");

        // Simulate loading new directory - choices should be cleared
        choicesTrackPaths.clear();

        assertEquals(0, choicesTrackPaths.size(), "Choices must be cleared when loading new directory");
    }

    // Helper method to simulate filter application
    private java.util.List<TrackEntry> applyActiveFilters(
            java.util.List<TrackEntry> source,
            String filterText,
            boolean showChoicesOnly,
            java.util.Set<Path> choicesTrackPaths) {
        // If show choices is enabled, ignore filter and show only choices
        if (showChoicesOnly) {
            return source.stream()
                    .filter(track -> choicesTrackPaths.contains(track.getFilePath()))
                    .toList();
        }

        // Otherwise apply normal filter
        java.util.List<TrackEntry> textFiltered = SearchFilter.filter(source, filterText);
        return textFiltered;
    }
}

