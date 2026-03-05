package com.example.singsongartwork;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public final class SearchFilter {
    private SearchFilter() {
    }

    private static Set<String> defaultFilterTerms;

    static {
        defaultFilterTerms = loadDefaultFilterTerms();
    }

    public static Set<String> getDefaultFilterTerms() {
        return defaultFilterTerms;
    }

    private static Set<String> loadDefaultFilterTerms() {
        Set<String> terms = new HashSet<>();
        try {
            // Look for defaultFilterTerms.txt in the same package/classpath
            Path resourcePath = Paths.get(SearchFilter.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                    .getParent()
                    .resolve("com/example/singsongartwork/defaultFilterTerms.txt");

            // Also try classpath resource approach
            try (var stream = SearchFilter.class.getResourceAsStream("/defaultFilterTerms.txt")) {
                if (stream != null) {
                    String content = new String(stream.readAllBytes());
                    terms = Arrays.stream(content.trim().split("\\n"))
                            .map(String::trim)
                            .filter(line -> !line.isBlank())
                            .map(term -> term.toLowerCase(Locale.ROOT))
                            .collect(Collectors.toSet());
                    return terms;
                }
            } catch (Exception e) {
                // Fall through to file-based approach
            }

            // File-based fallback
            if (Files.exists(resourcePath)) {
                terms = Files.readAllLines(resourcePath).stream()
                        .map(String::trim)
                        .filter(line -> !line.isBlank())
                        .map(term -> term.toLowerCase(Locale.ROOT))
                        .collect(Collectors.toSet());
            }
        } catch (Exception ex) {
            // Silently fail - default terms simply won't be loaded
        }
        return terms;
    }

    public static List<TrackEntry> filter(List<TrackEntry> tracks, String query) {
        // If filter is empty, show all rows
        if (query == null || query.isBlank()) {
            return tracks;
        }

        // Break text into words (separated by spaces)
        final List<String> words = Arrays.stream(query.trim().split("\\s+"))
                .map(word -> word.toLowerCase(Locale.ROOT))
                .filter(word -> !word.isBlank())
                .collect(Collectors.toList());

        // If no words after parsing, show all
        if (words.isEmpty()) {
            return tracks;
        }

        // Show rows where ALL words match in at least one column (filename, title, or artist)
        return tracks.stream()
                .filter(track -> words.stream().allMatch(word ->
                    track.getFilename().toLowerCase(Locale.ROOT).contains(word) ||
                    track.getTitle().toLowerCase(Locale.ROOT).contains(word) ||
                    track.getArtist().toLowerCase(Locale.ROOT).contains(word)
                ))
                .collect(Collectors.toList());
    }
}



