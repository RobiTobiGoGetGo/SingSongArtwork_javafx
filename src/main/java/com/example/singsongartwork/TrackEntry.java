package com.example.singsongartwork;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Objects;

public final class TrackEntry {
    private final Path filePath;
    private final String filename;
    private final String title;
    private final String artist;
    private final byte[] artwork;
    private final Boolean hasArtworkHint;

    public TrackEntry(Path filePath, String title, String artist, byte[] artwork) {
        this(filePath, title, artist, artwork, null);
    }

    public TrackEntry(Path filePath, String title, String artist, byte[] artwork, Boolean hasArtworkHint) {
        this.filePath = Objects.requireNonNull(filePath, "filePath must not be null");
        this.filename = filePath.getFileName().toString();
        this.title = sanitize(title);
        this.artist = sanitize(artist);
        this.artwork = artwork == null ? new byte[0] : Arrays.copyOf(artwork, artwork.length);
        this.hasArtworkHint = hasArtworkHint;
    }

    public Path getFilePath() {
        return filePath;
    }

    public String getFilename() {
        return filename;
    }

    public String getTitle() {
        return title;
    }

    public String getArtist() {
        return artist;
    }

    public byte[] getArtwork() {
        return Arrays.copyOf(artwork, artwork.length);
    }

    public boolean hasArtwork() {
        if (artwork.length > 0) {
            return true;
        }
        return hasArtworkHint != null && hasArtworkHint;
    }

    public TrackEntry withArtwork(byte[] artworkBytes) {
        return new TrackEntry(filePath, title, artist, artworkBytes, artworkBytes != null && artworkBytes.length > 0);
    }

    public String artworkDisplayValue() {
        return hasArtwork() ? "yes" : "no";
    }

    public boolean containsIgnoreCase(String term) {
        String lowered = sanitize(term).toLowerCase(Locale.ROOT);
        if (lowered.isBlank()) {
            return true;
        }

        return filename.toLowerCase(Locale.ROOT).contains(lowered)
                || title.toLowerCase(Locale.ROOT).contains(lowered)
                || artist.toLowerCase(Locale.ROOT).contains(lowered);
    }

    private static String sanitize(String value) {
        return value == null ? "" : value.trim();
    }
}
