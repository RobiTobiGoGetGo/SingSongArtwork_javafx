package com.example.singsongartwork;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.control.ScrollPane;
import javafx.stage.Screen;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Factory for creating common dialogs in SingSongArtwork.
 * Centralizes dialog creation, styling, and error handling.
 */
public class DialogFactory {

    /**
     * Show directory preview dialog with MP3 file listing.
     */
    public static boolean showDirectoryPreview(Path directory, String warningMessage) {
        try {
            if (directory == null || !Files.isDirectory(directory)) {
                return false;
            }

            File dirFile = directory.toFile();
            final List<String> mp3FilesList = new ArrayList<>();
            final List<String> otherFilesList = new ArrayList<>();

            File[] files = dirFile.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        String fileName = file.getName();
                        String lowerName = fileName.toLowerCase();
                        if (lowerName.endsWith(".mp3")) {
                            mp3FilesList.add(fileName);
                        } else {
                            otherFilesList.add(fileName);
                        }
                    }
                }
            } else {
                return false;
            }

            mp3FilesList.sort(String::compareTo);
            List<String> mp3Files = mp3FilesList.stream().limit(10).toList();

            otherFilesList.sort(String::compareTo);
            List<String> otherFiles = otherFilesList.stream().limit(10).toList();

            Dialog<ButtonType> previewDialog = new Dialog<>();
            previewDialog.setTitle("Directory Preview");
            previewDialog.setHeaderText("Selected Directory");

            try {
                previewDialog.getDialogPane().getStylesheets().add(
                    DialogFactory.class.getResource("/styles/modern-dark.css").toExternalForm()
                );
            } catch (Exception ex) {
                // CSS not found, continue with default styling
            }

            VBox content = new VBox(16);
            content.setPadding(new Insets(20));

            Label dirPathLabel = new Label(directory.toString());
            dirPathLabel.setWrapText(true);
            dirPathLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b3b3b3;");
            content.getChildren().add(dirPathLabel);

            if (warningMessage != null && !warningMessage.isBlank()) {
                Label overwriteWarningLabel = new Label(warningMessage);
                overwriteWarningLabel.setWrapText(true);
                overwriteWarningLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #ff6b6b;");
                content.getChildren().add(overwriteWarningLabel);
            }

            if (mp3Files.isEmpty()) {
                String emptyMessageText = "There are currently no MP3 files in this directory";
                if (!otherFiles.isEmpty()) {
                    emptyMessageText += ", but there are other types of files there";
                }

                Label emptyMessage = new Label(emptyMessageText);
                emptyMessage.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
                emptyMessage.setWrapText(true);
                content.getChildren().add(emptyMessage);

                if (!otherFiles.isEmpty()) {
                    Label otherFilesLabel = new Label("Other files found (" + otherFiles.size() + " shown):");
                    otherFilesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");
                    content.getChildren().add(otherFilesLabel);

                    TextArea otherFilesArea = new TextArea(String.join("\n", otherFiles));
                    otherFilesArea.setEditable(false);
                    otherFilesArea.setWrapText(false);
                    otherFilesArea.setPrefRowCount(8);
                    otherFilesArea.setPrefColumnCount(60);
                    otherFilesArea.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 12px;");
                    content.getChildren().add(otherFilesArea);
                }
            } else {
                Label info = new Label("📁 MP3 files found (" + mp3Files.size() + " shown):");
                info.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                content.getChildren().add(info);

                TextArea fileList = new TextArea(String.join("\n", mp3Files));
                fileList.setEditable(false);
                fileList.setWrapText(false);
                fileList.setPrefRowCount(10);
                fileList.setPrefColumnCount(60);
                fileList.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 13px;");
                content.getChildren().add(fileList);
            }

            previewDialog.getDialogPane().setContent(content);
            previewDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            previewDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);
            previewDialog.getDialogPane().setPrefWidth(700);

            return previewDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        } catch (Exception ex) {
            System.err.println("Directory preview error: " + ex.getMessage());
            return false;
        }
    }


    /**
     * Show markdown file content in a dialog.
     */
    public static void showMarkdownFile(String filename, String title) throws IOException {
        Path mdPath = Path.of(filename);
        if (!Files.exists(mdPath)) {
            throw new IOException("File not found: " + filename);
        }

        String content = Files.readString(mdPath);

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle(title);
        dialog.setHeaderText(filename);

        TextArea textArea = new TextArea(content);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setPrefRowCount(30);
        textArea.setPrefColumnCount(100);

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setPrefWidth(900);
        dialog.showAndWait();
    }

    /**
     * Show application log in a dialog.
     */
    public static void showAppLog(String logContent) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Application Log");
        dialog.setHeaderText("SingSongArtwork - Runtime Log");

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(24);
        textArea.setPrefColumnCount(90);
        textArea.setText(logContent.isEmpty() ? "No log entries yet." : logContent);

        dialog.getDialogPane().setContent(textArea);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    /**
     * Show keyboard shortcuts in an alert dialog.
     */
    public static void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("SingSongArtwork - Keyboard Shortcuts");

        String shortcuts = """
                File Operations:
                  Ctrl+O              Open Music Directory
                  Ctrl+R              Reload Current Music Directory
                
                Filter & Search:
                  Ctrl+F              Focus Filter Field
                  Ctrl+L              Clear Filter
                
                Selection & Marking:
                  M                   Mark Selected Rows
                  Ctrl+Shift+M        Toggle Show Marked Only
                  Ctrl+Shift+U        Unmark All Files
                
                Clipboard:
                  Ctrl+C              Copy Filename(s) to Clipboard
                
                Table Navigation:
                  Ctrl+Click          Multi-select tracks
                  Shift+Click         Select range
                  Ctrl+A              Select all tracks
                
                Context Menu:
                  Right-click         View options for selected tracks
                
                Admin & User Modes:
                  Ctrl+Alt+A          Toggle Admin/User Role
                
                Drag & Drop:
                  Drag image onto selected tracks to replace artwork
                """;

        TextArea textArea = new TextArea(shortcuts);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(22);
        textArea.setPrefColumnCount(50);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    /**
     * Show a popup card with full-size artwork.
     */
    public static void showArtworkCard(String title, byte[] artworkBytes) {
        if (artworkBytes == null || artworkBytes.length == 0) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Artwork Preview");
            alert.setHeaderText(title);
            alert.setContentText("No artwork available for this track.");
            alert.showAndWait();
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Artwork Preview");
        dialog.setHeaderText(title);

        try {
            Image image = new Image(new ByteArrayInputStream(artworkBytes));
            if (image.isError()) {
                throw new IllegalStateException("Invalid artwork image data");
            }

            Rectangle2D bounds = Screen.getPrimary().getVisualBounds();
            // About a quarter of total screen area: half width x half height.
            double initialWidth = Math.max(420, bounds.getWidth() * 0.5);
            double initialHeight = Math.max(320, bounds.getHeight() * 0.5);

            ImageView imageView = new ImageView(image);
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(initialWidth - 48);
            imageView.setFitHeight(initialHeight - 96);

            ScrollPane scrollPane = new ScrollPane(new StackPane(imageView));
            scrollPane.setFitToWidth(true);
            scrollPane.setFitToHeight(true);
            scrollPane.setPannable(true);
            scrollPane.setPrefViewportWidth(initialWidth);
            scrollPane.setPrefViewportHeight(initialHeight);

            dialog.getDialogPane().setContent(scrollPane);
            dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
            dialog.getDialogPane().setPrefWidth(initialWidth);
            dialog.getDialogPane().setPrefHeight(initialHeight);
            dialog.showAndWait();
        } catch (Exception ex) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Artwork Preview");
            alert.setHeaderText(title);
            alert.setContentText("Could not display artwork: " + ex.getMessage());
            alert.showAndWait();
        }
    }
}
