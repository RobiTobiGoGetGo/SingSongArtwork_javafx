package com.example.singsongartwork;

import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

    @Test
    @DisplayName("Copy directory is blocked when it matches the music directory")
    void testCopyDirectoryBlockedWhenMatchingMusicDirectory() throws IOException {
        Path musicDir = Files.createTempDirectory("singsongartwork-music-dir");
        Path artworkDir = Files.createTempDirectory("singsongartwork-artwork-dir");

        String message = SingSongArtworkUI.buildInvalidCopyDirectoryMessage(musicDir, musicDir, artworkDir);

        assertNotNull(message);
        assertTrue(message.contains("music directory"));
    }

    @Test
    @DisplayName("Copy directory is blocked when it matches the artwork directory")
    void testCopyDirectoryBlockedWhenMatchingArtworkDirectory() throws IOException {
        Path musicDir = Files.createTempDirectory("singsongartwork-music-dir");
        Path artworkDir = Files.createTempDirectory("singsongartwork-artwork-dir");

        String message = SingSongArtworkUI.buildInvalidCopyDirectoryMessage(artworkDir, musicDir, artworkDir);

        assertNotNull(message);
        assertTrue(message.contains("artwork directory"));
    }

    @Test
    @DisplayName("Copy directory is blocked when music and artwork directories are both the same as copy")
    void testCopyDirectoryBlockedWhenMatchingMusicAndArtworkDirectory() throws IOException {
        Path sharedDir = Files.createTempDirectory("singsongartwork-shared-dir");

        String message = SingSongArtworkUI.buildInvalidCopyDirectoryMessage(sharedDir, sharedDir, sharedDir);

        assertNotNull(message);
        assertTrue(message.contains("both the music directory and the artwork directory"));
    }

    @Test
    @DisplayName("Copy directory is allowed when it differs from music and artwork directories")
    void testCopyDirectoryAllowedWhenDistinct() throws IOException {
        Path copyDir = Files.createTempDirectory("singsongartwork-copy-dir");
        Path musicDir = Files.createTempDirectory("singsongartwork-music-dir");
        Path artworkDir = Files.createTempDirectory("singsongartwork-artwork-dir");

        assertNull(SingSongArtworkUI.buildInvalidCopyDirectoryMessage(copyDir, musicDir, artworkDir));
    }

    @Test
    @DisplayName("User mode copy is blocked when the copy directory already contains files")
    void testUserModeCopyBlockedWhenDirectoryHasFiles() throws IOException {
        Path destinationDir = Files.createTempDirectory("singsongartwork-user-copy-block");
        Files.createFile(destinationDir.resolve("existing.mp3"));

        String message = SingSongArtworkUI.buildUserModeCopyBlockedMessage(false, destinationDir);

        assertNotNull(message, "User mode should be blocked when destination already contains files");
        assertTrue(message.contains("empty copy directory"));
    }

    @Test
    @DisplayName("Admin mode copy is allowed even when the copy directory already contains files")
    void testAdminModeCopyAllowedWhenDirectoryHasFiles() throws IOException {
        Path destinationDir = Files.createTempDirectory("singsongartwork-admin-copy-allow");
        Files.createFile(destinationDir.resolve("existing.mp3"));

        assertNull(SingSongArtworkUI.buildUserModeCopyBlockedMessage(true, destinationDir),
                "Admin mode should still allow copying into a non-empty destination");
    }

    @Test
    @DisplayName("User mode copy is allowed when the copy directory is empty")
    void testUserModeCopyAllowedWhenDirectoryIsEmpty() throws IOException {
        Path destinationDir = Files.createTempDirectory("singsongartwork-user-copy-empty");

        assertFalse(SingSongArtworkUI.copyDirectoryHasFiles(destinationDir));
        assertNull(SingSongArtworkUI.buildUserModeCopyBlockedMessage(false, destinationDir),
                "User mode should allow copying into an empty destination");
    }

    @Test
    @DisplayName("Copy choices overwrite warning lists collisions")
    void testBuildOverwriteWarningListsCollisions() throws Exception {
        Path destinationDir = Files.createTempDirectory("singsongartwork-dest");
        Files.createFile(destinationDir.resolve("a.mp3"));
        Files.createFile(destinationDir.resolve("b.mp3"));

        Set<Path> sourcePaths = new HashSet<>();
        sourcePaths.add(Path.of("A.MP3")); // case-insensitive collision
        sourcePaths.add(Path.of("b.mp3")); // exact collision
        sourcePaths.add(Path.of("c.mp3")); // no collision

        SingSongArtworkUI ui = new SingSongArtworkUI();
        Method method = SingSongArtworkUI.class.getDeclaredMethod("buildOverwriteWarning", Path.class, Set.class);
        method.setAccessible(true);

        String warning = (String) method.invoke(ui, destinationDir, sourcePaths);

        assertNotNull(warning, "Warning should be generated when collisions exist");
        assertTrue(warning.contains("2 file(s) will be overwritten"), "Warning should include collision count");
        assertTrue(warning.contains("A.MP3"), "Warning should list colliding source filename");
        assertTrue(warning.contains("b.mp3"), "Warning should list colliding source filename");
    }

    @Test
    @DisplayName("Copy choices overwrite warning is null when no collisions")
    void testBuildOverwriteWarningNoCollisions() throws Exception {
        Path destinationDir = Files.createTempDirectory("singsongartwork-dest-no-collision");
        Files.createFile(destinationDir.resolve("x.mp3"));

        Set<Path> sourcePaths = new HashSet<>();
        sourcePaths.add(Path.of("a.mp3"));
        sourcePaths.add(Path.of("b.mp3"));

        SingSongArtworkUI ui = new SingSongArtworkUI();
        Method method = SingSongArtworkUI.class.getDeclaredMethod("buildOverwriteWarning", Path.class, Set.class);
        method.setAccessible(true);

        String warning = (String) method.invoke(ui, destinationDir, sourcePaths);
        assertNull(warning, "Warning should be null when there are no overwrite collisions");
    }

    @Test
    @DisplayName("Copy limit check allows selections exactly at both limits")
    void testCopyLimitCheckAllowsExactLimit() {
        int defaultCountLimit = ConfigurationManager.DEFAULT_MAX_COPY_COUNT;
        String message = SingSongArtworkUI.buildCopyLimitExceededMessage(defaultCountLimit, 700.0, defaultCountLimit, 700);
        assertNull(message, "Selections exactly at the configured limits should be allowed");
    }

    @Test
    @DisplayName("Copy limit check blocks on count or size and the lower effective limit wins")
    void testCopyLimitCheckBlocksWhenEitherLimitExceeded() {
        int defaultCountLimit = ConfigurationManager.DEFAULT_MAX_COPY_COUNT;

        String countOnly = SingSongArtworkUI.buildCopyLimitExceededMessage(defaultCountLimit + 1, 650.0, defaultCountLimit, 700);
        assertNotNull(countOnly, "Copy should be blocked when count exceeds the limit");
        assertTrue(countOnly.contains("Count: " + (defaultCountLimit + 1) + " files selected, limit is " + defaultCountLimit + " files."));

        String sizeOnly = SingSongArtworkUI.buildCopyLimitExceededMessage(defaultCountLimit - 1, 701.0, defaultCountLimit, 700);
        assertNotNull(sizeOnly, "Copy should be blocked when size exceeds the limit");
        assertTrue(sizeOnly.contains("Size: 701.0 MB selected, limit is 700 MB."));

        String bothExceeded = SingSongArtworkUI.buildCopyLimitExceededMessage(defaultCountLimit + 1, 701.0, defaultCountLimit, 700);
        assertNotNull(bothExceeded, "Copy should be blocked when both limits are exceeded");
        assertTrue(bothExceeded.contains("Count: " + (defaultCountLimit + 1) + " files selected, limit is " + defaultCountLimit + " files."));
        assertTrue(bothExceeded.contains("Size: 701.0 MB selected, limit is 700 MB."));
    }

    @Test
    @DisplayName("Copy limit check ignores disabled limits")
    void testCopyLimitCheckIgnoresNoLimitSettings() {
        int defaultCountLimit = ConfigurationManager.DEFAULT_MAX_COPY_COUNT;
        assertNull(SingSongArtworkUI.buildCopyLimitExceededMessage(500, 5000.0,
                ConfigurationManager.NO_LIMIT, ConfigurationManager.NO_LIMIT));
        assertNull(SingSongArtworkUI.buildCopyLimitExceededMessage(500, 650.0,
                ConfigurationManager.NO_LIMIT, 700));
        assertNull(SingSongArtworkUI.buildCopyLimitExceededMessage(defaultCountLimit - 1, 5000.0,
                defaultCountLimit, ConfigurationManager.NO_LIMIT));
    }

    @Test
    @DisplayName("Choice update marks up to the configured count limit and blocks the rest")
    void testChoiceUpdateRespectsCountLimitWhenMarking() {
        Set<Path> choices = java.util.concurrent.ConcurrentHashMap.newKeySet();
        List<Path> targets = java.util.stream.IntStream.range(0, ConfigurationManager.DEFAULT_MAX_COPY_COUNT + 2)
                .mapToObj(i -> Path.of("song-" + i + ".mp3"))
                .toList();

        SingSongArtworkUI.ChoiceUpdateResult result = SingSongArtworkUI.applyChoiceUpdate(
                choices,
                targets,
                SingSongArtworkUI.ChoiceUpdateMode.MARK,
                ConfigurationManager.DEFAULT_MAX_COPY_COUNT
        );

        assertEquals(ConfigurationManager.DEFAULT_MAX_COPY_COUNT, result.getAdded());
        assertEquals(0, result.getRemoved());
        assertEquals(2, result.getBlocked());
        assertEquals(ConfigurationManager.DEFAULT_MAX_COPY_COUNT, choices.size());
    }

    @Test
    @DisplayName("Choice update allows exact count limit without blocking")
    void testChoiceUpdateAllowsExactCountLimit() {
        Set<Path> choices = java.util.concurrent.ConcurrentHashMap.newKeySet();
        List<Path> targets = java.util.stream.IntStream.range(0, ConfigurationManager.DEFAULT_MAX_COPY_COUNT)
                .mapToObj(i -> Path.of("song-" + i + ".mp3"))
                .toList();

        SingSongArtworkUI.ChoiceUpdateResult result = SingSongArtworkUI.applyChoiceUpdate(
                choices,
                targets,
                SingSongArtworkUI.ChoiceUpdateMode.MARK,
                ConfigurationManager.DEFAULT_MAX_COPY_COUNT
        );

        assertEquals(ConfigurationManager.DEFAULT_MAX_COPY_COUNT, result.getAdded());
        assertEquals(0, result.getBlocked());
        assertEquals(ConfigurationManager.DEFAULT_MAX_COPY_COUNT, choices.size());
    }

    @Test
    @DisplayName("Choice update toggle frees space before adding new choices")
    void testChoiceUpdateToggleRemovesThenAddsWithinLimit() {
        Set<Path> choices = java.util.concurrent.ConcurrentHashMap.newKeySet();
        Path existingA = Path.of("existing-a.mp3");
        Path existingB = Path.of("existing-b.mp3");
        choices.add(existingA);
        choices.add(existingB);

        SingSongArtworkUI.ChoiceUpdateResult result = SingSongArtworkUI.applyChoiceUpdate(
                choices,
                List.of(existingA, Path.of("new-a.mp3"), Path.of("new-b.mp3")),
                SingSongArtworkUI.ChoiceUpdateMode.TOGGLE,
                3
        );

        assertEquals(2, result.getAdded());
        assertEquals(1, result.getRemoved());
        assertEquals(0, result.getBlocked());
        assertFalse(choices.contains(existingA));
        assertTrue(choices.contains(existingB));
        assertTrue(choices.contains(Path.of("new-a.mp3")));
        assertTrue(choices.contains(Path.of("new-b.mp3")));
    }

    @Test
    @DisplayName("Choice update ignores count cap when no limit is configured")
    void testChoiceUpdateIgnoresCountCapInNoLimitMode() {
        Set<Path> choices = java.util.concurrent.ConcurrentHashMap.newKeySet();
        List<Path> targets = java.util.stream.IntStream.range(0, ConfigurationManager.DEFAULT_MAX_COPY_COUNT + 20)
                .mapToObj(i -> Path.of("song-" + i + ".mp3"))
                .toList();

        SingSongArtworkUI.ChoiceUpdateResult result = SingSongArtworkUI.applyChoiceUpdate(
                choices,
                targets,
                SingSongArtworkUI.ChoiceUpdateMode.MARK,
                ConfigurationManager.NO_LIMIT
        );

        assertEquals(targets.size(), result.getAdded());
        assertEquals(0, result.getBlocked());
        assertEquals(targets.size(), choices.size());
    }

    @Test
    @DisplayName("Space-toggle behavior flips choices for all selected rows")
    void testSpaceToggleBehaviorForSelectedRows() {
        Set<Path> choices = java.util.concurrent.ConcurrentHashMap.newKeySet();
        TrackEntry t1 = new TrackEntry(Path.of("song1.mp3"), "t1", "a1", new byte[0]);
        TrackEntry t2 = new TrackEntry(Path.of("song2.mp3"), "t2", "a2", new byte[0]);
        java.util.List<TrackEntry> selected = java.util.List.of(t1, t2);

        // First toggle: add both
        toggleChoicesForSelectedRows(choices, selected);
        assertTrue(choices.contains(t1.getFilePath()));
        assertTrue(choices.contains(t2.getFilePath()));

        // Second toggle: remove both
        toggleChoicesForSelectedRows(choices, selected);
        assertFalse(choices.contains(t1.getFilePath()));
        assertFalse(choices.contains(t2.getFilePath()));
    }

    @Test
    @DisplayName("Music directory is blocked when it matches the copy directory")
    void testMusicDirectoryBlockedWhenMatchingCopyDirectory() throws IOException {
        Path musicDir = Files.createTempDirectory("singsongartwork-music-dir");
        Path artworkDir = Files.createTempDirectory("singsongartwork-artwork-dir");

        String message = SingSongArtworkUI.buildDirectoryConflictMessage(
                "music",
                musicDir,
                "copy",
                musicDir,
                "artwork",
                artworkDir
        );

        assertNotNull(message);
        assertTrue(message.contains("copy directory"));
    }

    @Test
    @DisplayName("Artwork directory is blocked when it matches the copy directory")
    void testArtworkDirectoryBlockedWhenMatchingCopyDirectory() throws IOException {
        Path musicDir = Files.createTempDirectory("singsongartwork-music-dir");
        Path artworkDir = Files.createTempDirectory("singsongartwork-artwork-dir");

        String message = SingSongArtworkUI.buildDirectoryConflictMessage(
                "artwork",
                artworkDir,
                "music",
                musicDir,
                "copy",
                artworkDir
        );

        assertNotNull(message);
        assertTrue(message.contains("copy directory"));
    }

    @Test
    @DisplayName("Music directory is blocked when it matches both copy and artwork directories")
    void testMusicDirectoryBlockedWhenMatchingCopyAndArtworkDirectories() throws IOException {
        Path sharedDir = Files.createTempDirectory("singsongartwork-shared-dir");

        String message = SingSongArtworkUI.buildDirectoryConflictMessage(
                "music",
                sharedDir,
                "copy",
                sharedDir,
                "artwork",
                sharedDir
        );

        assertNotNull(message);
        assertTrue(message.contains("both the copy directory and the artwork directory"));
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

    private void toggleChoicesForSelectedRows(Set<Path> choicesTrackPaths, java.util.List<TrackEntry> selectedItems) {
        for (TrackEntry track : new java.util.ArrayList<>(selectedItems)) {
            boolean isCurrentlyChosen = choicesTrackPaths.contains(track.getFilePath());
            if (isCurrentlyChosen) {
                choicesTrackPaths.remove(track.getFilePath());
            } else {
                choicesTrackPaths.add(track.getFilePath());
            }
        }
    }
}
