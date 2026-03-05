package com.example.singsongartwork;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public enum SortField {
    FILENAME,
    TITLE,
    ARTIST;

    public static SortField fromString(String value) {
        String normalized = value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
        try {
            return SortField.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            String supported = Arrays.stream(SortField.values())
                    .map(field -> field.name().toLowerCase(Locale.ROOT))
                    .collect(Collectors.joining(","));
            throw new IllegalArgumentException("Invalid sort field: '" + value + "'. Supported values: " + supported);
        }
    }
}
