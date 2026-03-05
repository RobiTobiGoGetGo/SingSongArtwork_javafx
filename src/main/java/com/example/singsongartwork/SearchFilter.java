package com.example.singsongartwork;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public final class SearchFilter {
    private SearchFilter() {
    }

    public static List<TrackEntry> filter(List<TrackEntry> tracks, String query) {
        if (query == null || query.isBlank()) {
            return tracks;
        }

        List<String> terms = Arrays.stream(query.trim().split("\\s+"))
                .map(term -> term.toLowerCase(Locale.ROOT))
                .filter(term -> !term.isBlank())
                .collect(Collectors.toList());

        return tracks.stream()
                .filter(track -> terms.stream().allMatch(track::containsIgnoreCase))
                .collect(Collectors.toList());
    }
}

