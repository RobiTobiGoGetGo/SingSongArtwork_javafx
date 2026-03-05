package com.example.singsongartwork;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class SearchFilterTest {

    @Test
    void returnsSameListWhenQueryIsBlank() {
        List<TrackEntry> input = List.of(track("a.mp3", "Song", "Artist"));

        List<TrackEntry> filtered = SearchFilter.filter(input, "   ");

        assertSame(input, filtered);
    }

    @Test
    void everySearchWordMustMatchAnyField() {
        TrackEntry queenLive = track("queen-live.mp3", "Live Aid", "Queen");
        TrackEntry beatlesLive = track("beatles.mp3", "Live", "The Beatles");

        List<TrackEntry> filtered = SearchFilter.filter(List.of(queenLive, beatlesLive), "queen live");

        assertEquals(1, filtered.size());
        assertEquals("queen-live.mp3", filtered.get(0).getFilename());
    }

    @Test
    void filteringIsCaseInsensitive() {
        TrackEntry track = track("mix.mp3", "Moonlight", "Beethoven");

        List<TrackEntry> filtered = SearchFilter.filter(List.of(track), "MOON beetho");

        assertEquals(1, filtered.size());
    }

    @Test
    void progressivelyRemovingCharactersRefreshesProperly() {
        // This test simulates the user typing "ana" then removing characters
        TrackEntry banana = track("banana.mp3", "Yellow Banana", "Ana Gardens");
        TrackEntry orange = track("orange.mp3", "Orange Dream", "Orange Band");
        TrackEntry apple = track("apple.mp3", "Apple Pie", "Apple Artists");
        List<TrackEntry> allTracks = List.of(banana, orange, apple);

        // First, filter with "ana" - should match "banana" and "Ana" in artist
        List<TrackEntry> filtered1 = SearchFilter.filter(allTracks, "ana");
        assertEquals(1, filtered1.size(), "Filter 'ana' should match only banana");
        assertEquals("banana.mp3", filtered1.get(0).getFilename());

        // Then, filter with "an" - should match "banana" and "Ana" and "orange"
        List<TrackEntry> filtered2 = SearchFilter.filter(allTracks, "an");
        assertEquals(2, filtered2.size(), "Filter 'an' should match banana and orange (contains 'an')");

        // Finally, clear filter - should return all
        List<TrackEntry> filtered3 = SearchFilter.filter(allTracks, "");
        assertEquals(3, filtered3.size(), "Empty filter should return all tracks");
        assertSame(allTracks, filtered3, "Empty filter should return the same list object");
    }

    @Test
    void clearingFilterReturnsAllTracks() {
        TrackEntry track1 = track("song1.mp3", "Title One", "Artist One");
        TrackEntry track2 = track("song2.mp3", "Title Two", "Artist Two");
        TrackEntry track3 = track("song3.mp3", "Title Three", "Artist Three");
        List<TrackEntry> allTracks = List.of(track1, track2, track3);

        // Filter with something specific
        List<TrackEntry> filtered1 = SearchFilter.filter(allTracks, "one");
        assertEquals(1, filtered1.size());

        // Clear filter
        List<TrackEntry> filtered2 = SearchFilter.filter(allTracks, "");
        assertEquals(3, filtered2.size(), "Clearing filter should return all tracks");
        assertSame(allTracks, filtered2, "Should return the original list when query is blank");
    }

    private static TrackEntry track(String filename, String title, String artist) {
        return new TrackEntry(Path.of(filename), title, artist, new byte[0]);
    }
}

