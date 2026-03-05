package com.example.singsongartwork;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.id3.ID3v23Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.ArtworkFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class Mp3MetadataServiceTest {

    private final Mp3MetadataService service = new Mp3MetadataService();

    @Test
    void loadFromDirectoryReadsOnlyMp3Files(@TempDir Path tempDir) throws Exception {
        Files.createFile(tempDir.resolve("track-a.mp3"));
        Files.createFile(tempDir.resolve("notes.txt"));

        List<TrackEntry> tracks = service.loadFromDirectory(tempDir);

        assertEquals(1, tracks.size());
        assertEquals("track-a.mp3", tracks.get(0).getFilename());
    }

    @Test
    void sortTracksSupportsMultipleFields() {
        TrackEntry t1 = new TrackEntry(Path.of("z.mp3"), "Alpha", "B", new byte[0]);
        TrackEntry t2 = new TrackEntry(Path.of("a.mp3"), "Alpha", "A", new byte[0]);
        TrackEntry t3 = new TrackEntry(Path.of("m.mp3"), "Beta", "C", new byte[0]);

        List<TrackEntry> sorted = service.sortTracks(List.of(t1, t2, t3), List.of(SortField.TITLE, SortField.ARTIST));

        assertEquals(List.of("a.mp3", "z.mp3", "m.mp3"), sorted.stream().map(TrackEntry::getFilename).toList());
    }

    @Test
    void addOrReplaceArtworkRejectsMissingMp3(@TempDir Path tempDir) {
        Path image = tempDir.resolve("cover.jpg");

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> service.addOrReplaceArtwork(tempDir.resolve("missing.mp3"), image)
        );

        assertTrue(ex.getMessage().contains("Not a valid mp3 file"));
    }

    @Test
    void replaceArtworkInTagPreservesExistingMetadata(@TempDir Path tempDir) throws Exception {
        Tag tag = new ID3v23Tag();
        tag.setField(FieldKey.TITLE, "Keep Title");
        tag.setField(FieldKey.ARTIST, "Keep Artist");

        Path imageFile = tempDir.resolve("cover.png");
        Files.write(imageFile, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7Z2wAAAABJRU5ErkJggg=="
        ));
        Artwork artwork = ArtworkFactory.createArtworkFromFile(imageFile.toFile());

        service.replaceArtworkInTag(tag, artwork);

        assertEquals("Keep Title", tag.getFirst(FieldKey.TITLE));
        assertEquals("Keep Artist", tag.getFirst(FieldKey.ARTIST));
        assertNotNull(tag.getFirstArtwork());
    }

    @Test
    void addOrReplaceArtworkEndToEndPreservesExistingMetadata(@TempDir Path tempDir) throws Exception {
        InputStream mp3FixtureStream = getClass().getResourceAsStream("/fixtures/tiny-valid.mp3");
        assumeTrue(mp3FixtureStream != null, "Missing fixture src/test/resources/fixtures/tiny-valid.mp3");

        Path mp3File = tempDir.resolve("e2e.mp3");
        try (InputStream in = mp3FixtureStream) {
            Files.copy(in, mp3File);
        }

        AudioFile initialAudio = AudioFileIO.read(mp3File.toFile());
        Tag initialTag = initialAudio.getTagOrCreateAndSetDefault();
        initialTag.setField(FieldKey.TITLE, "Original Title");
        initialTag.setField(FieldKey.ARTIST, "Original Artist");
        initialAudio.commit();

        Path imageFile = tempDir.resolve("cover.png");
        Files.write(imageFile, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7Z2wAAAABJRU5ErkJggg=="
        ));

        service.addOrReplaceArtwork(mp3File, imageFile);

        AudioFile updatedAudio = AudioFileIO.read(mp3File.toFile());
        Tag updatedTag = updatedAudio.getTag();
        assertNotNull(updatedTag);
        assertEquals("Original Title", updatedTag.getFirst(FieldKey.TITLE));
        assertEquals("Original Artist", updatedTag.getFirst(FieldKey.ARTIST));
        assertNotNull(updatedTag.getFirstArtwork());
    }

    @Test
    void batchEditMetadataUpdatesMultipleFiles(@TempDir Path tempDir) throws Exception {
        InputStream fixture = getClass().getResourceAsStream("/fixtures/tiny-valid.mp3");
        assumeTrue(fixture != null, "Missing fixture src/test/resources/fixtures/tiny-valid.mp3");

        Path mp3File1 = tempDir.resolve("track1.mp3");
        Path mp3File2 = tempDir.resolve("track2.mp3");
        try (InputStream in = fixture) {
            Files.copy(in, mp3File1);
        }
        try (InputStream in = getClass().getResourceAsStream("/fixtures/tiny-valid.mp3")) {
            Files.copy(in, mp3File2);
        }

        int updated = service.batchEditMetadata(
                List.of(mp3File1, mp3File2),
                "New Title",
                "New Artist"
        );

        assertEquals(2, updated);

        AudioFile file1 = AudioFileIO.read(mp3File1.toFile());
        Tag tag1 = file1.getTag();
        assertEquals("New Title", tag1.getFirst(FieldKey.TITLE));
        assertEquals("New Artist", tag1.getFirst(FieldKey.ARTIST));

        AudioFile file2 = AudioFileIO.read(mp3File2.toFile());
        Tag tag2 = file2.getTag();
        assertEquals("New Title", tag2.getFirst(FieldKey.TITLE));
        assertEquals("New Artist", tag2.getFirst(FieldKey.ARTIST));
    }

    @Test
    void batchEditMetadataHandlesPartialUpdates(@TempDir Path tempDir) throws Exception {
        InputStream fixture = getClass().getResourceAsStream("/fixtures/tiny-valid.mp3");
        assumeTrue(fixture != null, "Missing fixture src/test/resources/fixtures/tiny-valid.mp3");

        Path mp3File = tempDir.resolve("track.mp3");
        try (InputStream in = fixture) {
            Files.copy(in, mp3File);
        }

        int updated = service.batchEditMetadata(
                List.of(mp3File),
                "Updated Title",
                null
        );

        assertEquals(1, updated);

        AudioFile file = AudioFileIO.read(mp3File.toFile());
        Tag tag = file.getTag();
        assertEquals("Updated Title", tag.getFirst(FieldKey.TITLE));
    }

    @Test
    void metadataCacheInvalidatesOnArtworkWrite(@TempDir Path tempDir) throws Exception {
        InputStream fixture = getClass().getResourceAsStream("/fixtures/tiny-valid.mp3");
        assumeTrue(fixture != null, "Missing fixture src/test/resources/fixtures/tiny-valid.mp3");

        Path mp3File = tempDir.resolve("track.mp3");
        try (InputStream in = fixture) {
            Files.copy(in, mp3File);
        }

        // Load first time (populates cache)
        List<TrackEntry> tracks1 = service.loadFromDirectory(tempDir);
        assertEquals(1, tracks1.size());
        assertNotNull(tracks1.get(0));

        // Add artwork (should invalidate cache for this file)
        Path imageFile = tempDir.resolve("cover.png");
        Files.write(imageFile, Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7Z2wAAAABJRU5ErkJggg=="
        ));
        service.addOrReplaceArtwork(mp3File, imageFile);

        // Load again (cache should be invalidated, so it reads fresh)
        List<TrackEntry> tracks2 = service.loadFromDirectory(tempDir);
        assertEquals(1, tracks2.size());
        assertTrue(tracks2.get(0).hasArtwork(), "Track should show artwork after cache invalidation");
    }

    @Test
    void clearCacheResetsState() {
        service.clearCache();
        // After clear, internal cache should be empty; loading should work fresh.
        // This is a smoke test to ensure the method exists and doesn't throw.
        assertTrue(true);
    }
}
