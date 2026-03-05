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
}
