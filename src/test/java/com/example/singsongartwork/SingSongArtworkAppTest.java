package com.example.singsongartwork;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SingSongArtworkAppTest {

    @Test
    void parseSortFieldsDefaultsToFilename() {
        assertEquals(List.of(SortField.FILENAME), SingSongArtworkApp.parseSortFields("   "));
    }

    @Test
    void parseSortFieldsParsesMultipleValues() {
        List<SortField> fields = SingSongArtworkApp.parseSortFields("artist,title");

        assertEquals(List.of(SortField.ARTIST, SortField.TITLE), fields);
    }

    @Test
    void mainPrintsUsageWhenDirIsMissing() {
        RunOutput output = runAppAndCapture(new String[]{});

        assertEquals(2, output.exitCode());
        assertTrue(output.stdout().contains("Usage:"));
        assertTrue(output.stdout().contains("--dir <path>"));
        assertTrue(output.stderr().isBlank());
    }

    @Test
    void mainAppliesFilterOnFilenameWords(@TempDir Path tempDir) throws Exception {
        Files.createFile(tempDir.resolve("queen-live.mp3"));
        Files.createFile(tempDir.resolve("queen-studio.mp3"));
        Files.createFile(tempDir.resolve("readme.txt"));

        RunOutput output = runAppAndCapture(new String[]{
                "--dir", tempDir.toString(),
                "--filter", "queen live",
                "--sort", "filename"
        });

        assertEquals(0, output.exitCode());
        assertTrue(output.stdout().contains("filename"));
        assertTrue(output.stdout().contains("queen-live.mp3"));
        assertFalse(output.stdout().contains("queen-studio.mp3"));
        assertTrue(output.stderr().isBlank());
    }

    @Test
    void mainShowsErrorForMissingArtworkFile(@TempDir Path tempDir) {
        RunOutput output = runAppAndCapture(new String[]{
                "--dir", tempDir.toString(),
                "--replace-artwork", "song.mp3"
        });

        assertEquals(2, output.exitCode());
        assertTrue(output.stderr().contains("Error: --artwork-file is required"));
        assertTrue(output.stderr().contains("Usage:"));
    }

    @Test
    void mainShowsErrorForInvalidSortField(@TempDir Path tempDir) {
        RunOutput output = runAppAndCapture(new String[]{
                "--dir", tempDir.toString(),
                "--sort", "foo"
        });

        assertEquals(2, output.exitCode());
        assertTrue(output.stderr().contains("Error: Invalid sort field"));
        assertTrue(output.stderr().contains("filename,title,artist"));
        assertTrue(output.stderr().contains("Usage:"));
    }

    @Test
    void mainShowsErrorForMissingDirectory(@TempDir Path tempDir) {
        Path missingDir = tempDir.resolve("does-not-exist");

        RunOutput output = runAppAndCapture(new String[]{
                "--dir", missingDir.toString()
        });

        assertEquals(2, output.exitCode());
        assertTrue(output.stderr().contains("Error: Directory does not exist"));
        assertTrue(output.stderr().contains("Usage:"));
        assertTrue(output.stdout().isBlank());
    }

    private static RunOutput runAppAndCapture(String[] args) {
        ByteArrayOutputStream outBuffer = new ByteArrayOutputStream();
        ByteArrayOutputStream errBuffer = new ByteArrayOutputStream();

        int exitCode;
        try (PrintStream captureOut = new PrintStream(outBuffer, true, StandardCharsets.UTF_8);
             PrintStream captureErr = new PrintStream(errBuffer, true, StandardCharsets.UTF_8)) {
            exitCode = SingSongArtworkApp.run(args, captureOut, captureErr);
        }

        return new RunOutput(
                exitCode,
                outBuffer.toString(StandardCharsets.UTF_8),
                errBuffer.toString(StandardCharsets.UTF_8)
        );
    }

    private record RunOutput(int exitCode, String stdout, String stderr) {
    }
}
