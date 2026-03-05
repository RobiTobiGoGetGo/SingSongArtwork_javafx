package com.example.singsongartwork;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mp3MetadataService {
    private final Map<Path, CachedTrackMetadata> metadataCache = new ConcurrentHashMap<>();

    public List<TrackEntry> loadFromDirectory(Path directory) throws IOException {
        if (!Files.exists(directory) || !Files.isDirectory(directory)) {
            throw new IllegalArgumentException("Directory does not exist: " + directory);
        }

        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(path -> !Files.isDirectory(path))
                    .filter(this::isMp3)
                    .map(this::loadTrackSafely)
                    .collect(Collectors.toList());
        }
    }

    public List<TrackEntry> sortTracks(List<TrackEntry> tracks, List<SortField> sortFields) {
        if (sortFields == null || sortFields.isEmpty()) {
            return new ArrayList<>(tracks);
        }

        Comparator<TrackEntry> comparator = null;
        for (SortField field : sortFields) {
            Comparator<TrackEntry> current = switch (field) {
                case FILENAME -> Comparator.comparing(t -> t.getFilename().toLowerCase(Locale.ROOT));
                case TITLE -> Comparator.comparing(t -> t.getTitle().toLowerCase(Locale.ROOT));
                case ARTIST -> Comparator.comparing(t -> t.getArtist().toLowerCase(Locale.ROOT));
            };
            comparator = comparator == null ? current : comparator.thenComparing(current);
        }

        return tracks.stream().sorted(comparator).collect(Collectors.toList());
    }

    public void addOrReplaceArtwork(Path mp3File, Path artworkFile) throws Exception {
        if (!Files.exists(mp3File) || !isMp3(mp3File)) {
            throw new IllegalArgumentException("Not a valid mp3 file: " + mp3File);
        }
        if (!Files.exists(artworkFile) || Files.isDirectory(artworkFile)) {
            throw new IllegalArgumentException("Not a valid artwork file: " + artworkFile);
        }

        AudioFile audioFile = AudioFileIO.read(mp3File.toFile());
        Tag tag = audioFile.getTagOrCreateAndSetDefault();

        Artwork artwork = ArtworkFactory.createArtworkFromFile(artworkFile.toFile());
        replaceArtworkInTag(tag, artwork);
        audioFile.commit();
        invalidateCache(mp3File);
    }

    public byte[] loadArtworkBytes(Path mp3Path) {
        try {
            AudioFile audioFile = AudioFileIO.read(mp3Path.toFile());
            Tag tag = audioFile.getTag();
            if (tag == null) {
                return new byte[0];
            }
            Artwork firstArtwork = tag.getFirstArtwork();
            if (firstArtwork == null || firstArtwork.getBinaryData() == null) {
                return new byte[0];
            }
            return firstArtwork.getBinaryData();
        } catch (Exception ex) {
            return new byte[0];
        }
    }

    public int batchEditMetadata(List<Path> mp3Paths, String newTitle, String newArtist) {
        int updated = 0;
        for (Path path : mp3Paths) {
            try {
                AudioFile audioFile = AudioFileIO.read(path.toFile());
                Tag tag = audioFile.getTagOrCreateAndSetDefault();
                if (newTitle != null && !newTitle.isBlank()) {
                    tag.setField(FieldKey.TITLE, newTitle.trim());
                }
                if (newArtist != null && !newArtist.isBlank()) {
                    tag.setField(FieldKey.ARTIST, newArtist.trim());
                }
                audioFile.commit();
                invalidateCache(path);
                updated++;
            } catch (Exception ignored) {
                // Keep best-effort batch behavior for mixed-quality libraries.
            }
        }
        return updated;
    }

    public void clearCache() {
        metadataCache.clear();
    }

    public TrackEntry loadSingleTrack(Path mp3Path) {
        return loadTrackSafely(mp3Path);
    }

    public void invalidateCache(Path mp3Path) {
        metadataCache.remove(mp3Path);
    }

    void replaceArtworkInTag(Tag tag, Artwork artwork) throws Exception {
        tag.deleteArtworkField();
        tag.setField(artwork);
    }

    private TrackEntry loadTrackSafely(Path path) {
        try {
            long lastModified = Files.getLastModifiedTime(path).toMillis();
            CachedTrackMetadata cached = metadataCache.get(path);
            if (cached != null && cached.lastModified == lastModified) {
                return cached.entry;
            }

            AudioFile audioFile = AudioFileIO.read(path.toFile());
            Tag tag = audioFile.getTag();

            String title = "";
            String artist = "";
            boolean hasArtwork = false;

            if (tag != null) {
                title = sanitizeTagValue(tag.getFirst(FieldKey.TITLE));
                artist = sanitizeTagValue(tag.getFirst(FieldKey.ARTIST));
                hasArtwork = tag.getFirstArtwork() != null;
            }

            // Keep artwork bytes empty in directory scans and lazy-load on demand.
            TrackEntry entry = new TrackEntry(path, title, artist, new byte[0], hasArtwork);
            metadataCache.put(path, new CachedTrackMetadata(lastModified, entry));
            return entry;
        } catch (Exception ex) {
            return new TrackEntry(path, "", "", new byte[0]);
        }
    }


    private String sanitizeTagValue(String value) {
        return value == null ? "" : value.trim();
    }

    private boolean isMp3(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".mp3");
    }

    private static final class CachedTrackMetadata {
        private final long lastModified;
        private final TrackEntry entry;

        private CachedTrackMetadata(long lastModified, TrackEntry entry) {
            this.lastModified = lastModified;
            this.entry = entry;
        }
    }
}
