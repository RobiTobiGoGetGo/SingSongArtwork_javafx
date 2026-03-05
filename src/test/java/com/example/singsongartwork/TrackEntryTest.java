package com.example.singsongartwork;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TrackEntryTest {

    @Test
    void containsIgnoreCaseChecksFilenameTitleAndArtist() {
        TrackEntry entry = new TrackEntry(Path.of("queen-live.mp3"), "Live Aid", "Queen", new byte[0]);

        assertTrue(entry.containsIgnoreCase("queen"));
        assertTrue(entry.containsIgnoreCase("live"));
        assertTrue(entry.containsIgnoreCase("aid"));
        assertFalse(entry.containsIgnoreCase("beatles"));
    }

    @Test
    void artworkDisplayValueReflectsArtworkPresence() {
        TrackEntry withArtwork = new TrackEntry(Path.of("a.mp3"), "", "", new byte[]{1, 2});
        TrackEntry withoutArtwork = new TrackEntry(Path.of("b.mp3"), "", "", new byte[0]);

        assertEquals("yes", withArtwork.artworkDisplayValue());
        assertEquals("no", withoutArtwork.artworkDisplayValue());
    }
}

