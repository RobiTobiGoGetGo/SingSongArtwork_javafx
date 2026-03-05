package com.example.singsongartwork;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SingSongArtworkApp {
    private static final int EXIT_OK = 0;
    private static final int EXIT_USAGE_ERROR = 2;
    private static final int EXIT_RUNTIME_ERROR = 1;


    static int run(String[] args, PrintStream out, PrintStream err) {
        try {
            Map<String, String> options = parseArgs(args);
            String dirValue = options.get("--dir");

            if (dirValue == null || dirValue.isBlank()) {
                printUsage(out);
                return EXIT_USAGE_ERROR;
            }

            Path directory = Path.of(dirValue);
            Mp3MetadataService service = new Mp3MetadataService();

            String replaceArtworkFile = options.get("--replace-artwork");
            String artworkFile = options.get("--artwork-file");
            if (replaceArtworkFile != null) {
                if (artworkFile == null || artworkFile.isBlank()) {
                    throw new IllegalArgumentException("--artwork-file is required when using --replace-artwork");
                }

                Path mp3Path = Path.of(replaceArtworkFile);
                if (!mp3Path.isAbsolute()) {
                    mp3Path = directory.resolve(mp3Path);
                }

                service.addOrReplaceArtwork(mp3Path, Path.of(artworkFile));
                out.println("Artwork updated: " + mp3Path.getFileName());
            }

            List<TrackEntry> tracks = service.loadFromDirectory(directory);
            tracks = SearchFilter.filter(tracks, options.get("--filter"));
            tracks = service.sortTracks(tracks, parseSortFields(options.get("--sort")));

            printTable(tracks, out);
            return EXIT_OK;
        } catch (IllegalArgumentException ex) {
            printError(ex.getMessage(), err);
            printUsage(err);
            return EXIT_USAGE_ERROR;
        } catch (Exception ex) {
            printError(ex.getMessage(), err);
            return EXIT_RUNTIME_ERROR;
        }
    }

    static List<SortField> parseSortFields(String rawSortFields) {
        if (rawSortFields == null || rawSortFields.isBlank()) {
            return List.of(SortField.FILENAME);
        }

        List<SortField> sortFields = new ArrayList<>();
        for (String item : rawSortFields.split(",")) {
            if (!item.isBlank()) {
                sortFields.add(SortField.fromString(item));
            }
        }

        return sortFields.isEmpty() ? List.of(SortField.FILENAME) : sortFields;
    }

    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> parsed = new LinkedHashMap<>();
        for (int i = 0; i < args.length; i++) {
            String token = args[i];
            if (!token.startsWith("--")) {
                continue;
            }

            String value = i + 1 < args.length && !args[i + 1].startsWith("--") ? args[++i] : "";
            parsed.put(token, value);
        }
        return parsed;
    }

    private static void printTable(List<TrackEntry> tracks, PrintStream out) {
        int fileWidth = maxWidth("filename", tracks.stream().map(TrackEntry::getFilename).toList());
        int titleWidth = maxWidth("title", tracks.stream().map(TrackEntry::getTitle).toList());
        int artistWidth = maxWidth("artist", tracks.stream().map(TrackEntry::getArtist).toList());

        String format = "%-" + fileWidth + "s  %-" + titleWidth + "s  %-" + artistWidth + "s  %-7s%n";
        out.printf(format, "filename", "title", "artist", "artwork");
        out.printf(format, "-".repeat(fileWidth), "-".repeat(titleWidth), "-".repeat(artistWidth), "-------");

        for (TrackEntry track : tracks) {
            out.printf(format, track.getFilename(), track.getTitle(), track.getArtist(), track.artworkDisplayValue());
        }
    }

    private static int maxWidth(String header, List<String> values) {
        int max = header.length();
        for (String value : values) {
            if (value != null) {
                max = Math.max(max, value.length());
            }
        }
        return max;
    }

    private static void printError(String message, PrintStream err) {
        String safeMessage = (message == null || message.isBlank())
                ? "Unexpected runtime failure"
                : message;
        err.println("Error: " + safeMessage);
    }

    private static void printUsage(PrintStream out) {
        out.println("Usage:");
        out.println("  --dir <path> [--filter <query>] [--sort filename,title,artist]");
        out.println("  --dir <path> --replace-artwork <mp3FileNameOrPath> --artwork-file <imagePath>");
    }
}
