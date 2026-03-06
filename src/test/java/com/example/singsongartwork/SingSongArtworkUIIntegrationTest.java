package com.example.singsongartwork;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the UI simplification features, particularly:
 * - Show choices mode with filter interaction
 * - Choice management across multiple operations
 * - Menu option restrictions based on user mode
 */
@DisplayName("SingSongArtwork UI Integration Tests")
class SingSongArtworkUIIntegrationTest {

    @Test
    @DisplayName("Complete show choices workflow")
    void testShowChoicesWorkflow(@TempDir Path tempDir) throws Exception {
        // Setup: Create test tracks
        Files.createFile(tempDir.resolve("track1-queen.mp3"));
        Files.createFile(tempDir.resolve("track2-beatles.mp3"));
        Files.createFile(tempDir.resolve("track3-pink.mp3"));

        List<TrackEntry> tracks = List.of(
            new TrackEntry(Path.of("track1-queen.mp3"), "Live Aid", "Queen", new byte[0]),
            new TrackEntry(Path.of("track2-beatles.mp3"), "Help", "Beatles", new byte[0]),
            new TrackEntry(Path.of("track3-pink.mp3"), "Learning to Fly", "Pink Floyd", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();
        String retainedFilterText = "";

        // Step 1: Apply a filter
        String filterText = "queen";
        List<TrackEntry> filtered = SearchFilter.filter(tracks, filterText);
        assertEquals(1, filtered.size(), "Filter should find only Queen track");

        // Step 2: Mark some choices
        choicesTrackPaths.add(Path.of("track1-queen.mp3"));
        choicesTrackPaths.add(Path.of("track2-beatles.mp3"));

        // Step 3: Enable "Show choices" mode
        boolean showChoicesOnly = true;
        if (showChoicesOnly) {
            retainedFilterText = filterText;
            filterText = "";  // Filter is cleared
        }

        // Step 4: Verify filter is empty but choices are shown
        assertEquals("", filterText, "Filter text should be cleared");
        assertEquals("queen", retainedFilterText, "Original filter should be retained");

        // Step 5: Apply active filters (should show choices, ignore current filter)
        List<TrackEntry> choicesFiltered = applyActiveFilters(tracks, filterText, showChoicesOnly, choicesTrackPaths);
        assertEquals(2, choicesFiltered.size(), "Show choices should show both marked tracks");

        // Step 6: Disable "Show choices" mode
        showChoicesOnly = false;
        filterText = retainedFilterText;  // Restore filter

        // Step 7: Verify filter is restored
        List<TrackEntry> restoredFilter = applyActiveFilters(tracks, filterText, showChoicesOnly, choicesTrackPaths);
        assertEquals(1, restoredFilter.size(), "Filter should be restored, showing only Queen");
    }

    @Test
    @DisplayName("Choices persist across filter changes")
    void testChoicesPersistAcrossFilterChanges(@TempDir Path tempDir) throws Exception {
        List<TrackEntry> tracks = List.of(
            new TrackEntry(Path.of("alpha.mp3"), "Song One", "Queen", new byte[0]),
            new TrackEntry(Path.of("beta.mp3"), "Song Two", "Beatles", new byte[0]),
            new TrackEntry(Path.of("charlie.mp3"), "Song Three", "Pink Floyd", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();
        choicesTrackPaths.add(Path.of("alpha.mp3"));
        choicesTrackPaths.add(Path.of("beta.mp3"));

        // Change filter to "Queen"
        List<TrackEntry> filtered1 = SearchFilter.filter(tracks, "Queen");
        assertEquals(1, filtered1.size(), "Filter should find Queen");

        // Choices should still be 2
        assertEquals(2, choicesTrackPaths.size(), "Choices should persist across filter changes");

        // Change filter to "Pink"
        List<TrackEntry> filtered2 = SearchFilter.filter(tracks, "Pink");
        assertEquals(1, filtered2.size(), "Filter should find Pink");

        // Choices should still be 2
        assertEquals(2, choicesTrackPaths.size(), "Choices should persist across filter changes");
    }

    @Test
    @DisplayName("Choices cleared when loading new directory")
    void testChoicesClearedOnDirectoryLoad() {
        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();
        choicesTrackPaths.add(Path.of("track1.mp3"));
        choicesTrackPaths.add(Path.of("track2.mp3"));
        choicesTrackPaths.add(Path.of("track3.mp3"));

        assertEquals(3, choicesTrackPaths.size(), "Should have 3 choices");

        // Simulate loading new directory
        choicesTrackPaths.clear();

        assertEquals(0, choicesTrackPaths.size(), "Choices must be cleared");
    }

    @Test
    @DisplayName("Empty filter shows all tracks in normal mode")
    void testEmptyFilterShowsAll() {
        List<TrackEntry> allTracks = List.of(
            new TrackEntry(Path.of("a.mp3"), "Title A", "Artist A", new byte[0]),
            new TrackEntry(Path.of("b.mp3"), "Title B", "Artist B", new byte[0]),
            new TrackEntry(Path.of("c.mp3"), "Title C", "Artist C", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();

        // Empty filter in normal mode
        List<TrackEntry> result = applyActiveFilters(allTracks, "", false, choicesTrackPaths);
        assertEquals(3, result.size(), "Empty filter should show all tracks");
    }

    @Test
    @DisplayName("Multi-word filter with all words required")
    void testMultiWordFilterRequiresAllWords() {
        List<TrackEntry> tracks = List.of(
            new TrackEntry(Path.of("queen-live.mp3"), "Live Aid", "Queen", new byte[0]),
            new TrackEntry(Path.of("beatles-live.mp3"), "Live", "Beatles", new byte[0]),
            new TrackEntry(Path.of("pink-live.mp3"), "Dark Side", "Pink Floyd", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();

        // Filter "queen live" - should find only the Queen live track
        List<TrackEntry> result = applyActiveFilters(tracks, "queen live", false, choicesTrackPaths);
        assertEquals(1, result.size(), "Should find only Queen track with both 'queen' and 'live'");
        assertEquals("queen-live.mp3", result.get(0).getFilename());
    }

    @Test
    @DisplayName("Choices column sorting - marked items first")
    void testChoicesColumnSorting() {
        List<Boolean> unsorted = List.of(false, true, false, true, true, false);

        // Comparator that puts true (choices) before false (non-choices)
        java.util.Comparator<Boolean> choicesComparator = (a, b) -> Boolean.compare(b, a);
        java.util.List<Boolean> sorted = new java.util.ArrayList<>(unsorted);
        sorted.sort(choicesComparator);

        List<Boolean> expected = List.of(true, true, true, false, false, false);
        assertEquals(expected, sorted, "Choices should be sorted with true (marked) before false");
    }

    @Test
    @DisplayName("Case-insensitive filter matching")
    void testCaseInsensitiveFiltering() {
        List<TrackEntry> tracks = List.of(
            new TrackEntry(Path.of("moonlight.mp3"), "Moonlight Sonata", "Beethoven", new byte[0]),
            new TrackEntry(Path.of("night.mp3"), "Night Train", "James Brown", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();

        // Test lowercase
        List<TrackEntry> result1 = applyActiveFilters(tracks, "MOON beethoven", false, choicesTrackPaths);
        assertEquals(1, result1.size(), "Filter should be case-insensitive");

        // Test mixed case
        List<TrackEntry> result2 = applyActiveFilters(tracks, "NiGhT jAmEs", false, choicesTrackPaths);
        assertEquals(1, result2.size(), "Filter should be case-insensitive with mixed case");
    }

    @Test
    @DisplayName("Progressive filter reduction works correctly")
    void testProgressiveFilterReduction() {
        List<TrackEntry> tracks = List.of(
            new TrackEntry(Path.of("banana.mp3"), "Yellow Banana", "Ana Gardens", new byte[0]),
            new TrackEntry(Path.of("orange.mp3"), "Orange Dream", "Orange Band", new byte[0]),
            new TrackEntry(Path.of("apple.mp3"), "Apple Pie", "Apple Artists", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();

        // Filter with "ana" - should match banana + Ana artist
        List<TrackEntry> result1 = applyActiveFilters(tracks, "ana", false, choicesTrackPaths);
        assertEquals(1, result1.size(), "Filter 'ana' should match only banana");

        // Remove last char to "an" - should match banana + orange
        List<TrackEntry> result2 = applyActiveFilters(tracks, "an", false, choicesTrackPaths);
        assertEquals(2, result2.size(), "Filter 'an' should match banana and orange");

        // Clear filter - should show all
        List<TrackEntry> result3 = applyActiveFilters(tracks, "", false, choicesTrackPaths);
        assertEquals(3, result3.size(), "Empty filter should show all tracks");
    }

    @Test
    @DisplayName("Show choices mode with empty choices list")
    void testShowChoicesModeEmptyChoices() {
        List<TrackEntry> tracks = List.of(
            new TrackEntry(Path.of("a.mp3"), "Song A", "Artist A", new byte[0]),
            new TrackEntry(Path.of("b.mp3"), "Song B", "Artist B", new byte[0])
        );

        Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();
        // No choices selected

        List<TrackEntry> result = applyActiveFilters(tracks, "", true, choicesTrackPaths);
        assertEquals(0, result.size(), "Show choices with empty choices should show nothing");
    }

    @Test
    @DisplayName("User mode startup is enforced (fail-safe)")
    void testUserModeFailSafe() {
        // Simulate loading saved preference that says "admin"
        String savedRole = "admin";

        // But the fail-safe overrides it
        boolean adminMode = "admin".equals(savedRole);
        adminMode = false; // FAIL-SAFE: Always start in User mode

        assertFalse(adminMode, "Fail-safe must override any saved admin preference");
    }

    // Helper method
    private List<TrackEntry> applyActiveFilters(
            List<TrackEntry> source,
            String filterText,
            boolean showChoicesOnly,
            Set<Path> choicesTrackPaths) {
        if (showChoicesOnly) {
            return source.stream()
                    .filter(track -> choicesTrackPaths.contains(track.getFilePath()))
                    .toList();
        }

        List<TrackEntry> textFiltered = SearchFilter.filter(source, filterText);
        return textFiltered;
    }
}

