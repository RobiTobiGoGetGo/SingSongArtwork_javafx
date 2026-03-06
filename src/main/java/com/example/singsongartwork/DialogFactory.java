package com.example.singsongartwork;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

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
     * Show password authentication dialog for Admin mode.
     */
    public static boolean showPasswordPrompt(String adminPassword) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Admin Authentication");
        dialog.setHeaderText("Enter password to switch to Admin mode");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Password");

        VBox content = new VBox(8);
        content.setPadding(new Insets(10));
        content.getChildren().add(passwordField);
        dialog.getDialogPane().setContent(content);

        ButtonType okButton = new ButtonType("Unlock", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(okButton, ButtonType.CANCEL);

        Platform.runLater(passwordField::requestFocus);

        ButtonType result = dialog.showAndWait().orElse(ButtonType.CANCEL);
        return result == okButton && adminPassword.equals(passwordField.getText());
    }

    /**
     * Show batch metadata edit dialog.
     */
    public static Dialog<ButtonType> showBatchEditDialog(int trackCount) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Batch Edit Metadata");
        dialog.setHeaderText("Update metadata for " + trackCount + " selected tracks");

        TextField titleField = new TextField();
        titleField.setPromptText("New title (leave blank to keep existing)");

        TextField artistField = new TextField();
        artistField.setPromptText("New artist (leave blank to keep existing)");

        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(new Label("Title:"));
        content.getChildren().add(titleField);
        content.getChildren().add(new Label("Artist:"));
        content.getChildren().add(artistField);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

        return dialog;
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
}

