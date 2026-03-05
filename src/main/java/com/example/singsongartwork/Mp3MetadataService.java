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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Mp3MetadataService {
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
    }

    void replaceArtworkInTag(Tag tag, Artwork artwork) throws Exception {
        tag.deleteArtworkField();
        tag.setField(artwork);
    }

    private TrackEntry loadTrackSafely(Path path) {
        try {
            AudioFile audioFile = AudioFileIO.read(path.toFile());
            Tag tag = audioFile.getTag();

            String title = "";
            String artist = "";
            byte[] artwork = new byte[0];

            if (tag != null) {
                title = sanitizeTagValue(tag.getFirst(FieldKey.TITLE));
                artist = sanitizeTagValue(tag.getFirst(FieldKey.ARTIST));
                Artwork firstArtwork = tag.getFirstArtwork();
                if (firstArtwork != null && firstArtwork.getBinaryData() != null) {
                    artwork = firstArtwork.getBinaryData();
                }
            }

            return new TrackEntry(path, title, artist, artwork);
        } catch (Exception ex) {
            // Keep scanning robust even if an individual file has malformed tags.
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
}
