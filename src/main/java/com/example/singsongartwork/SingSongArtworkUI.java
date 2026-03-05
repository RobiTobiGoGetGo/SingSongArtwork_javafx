package com.example.singsongartwork;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SingSongArtworkUI extends Application {
    private Mp3MetadataService service;
    private TableView<TrackEntry> trackTable;
    private TextField filterTextField;
    private Label statusLabel;
    private Label dirLabel;
    private Path currentDirectory;
    private List<TrackEntry> allTracksUnfiltered = new ArrayList<>();
    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".singsongartwork", "config.properties");

    @Override
    public void start(Stage primaryStage) {
        service = new Mp3MetadataService();

        // Main layout
        BorderPane root = new BorderPane();

        // Top: directory selection and controls
        VBox topPanel = createTopPanel();
        root.setTop(topPanel);

        // Center: table
        trackTable = createTrackTable();
        root.setCenter(trackTable);

        // Bottom: status
        statusLabel = new Label("Ready. Select a directory to begin.");
        root.setBottom(statusLabel);

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("SingSongArtwork");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Initialize dirLabel with the last used directory path (but don't load it)
        initializeLastDirectoryPath();
    }

    private VBox createTopPanel() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // Directory selection row
        HBox dirBox = new HBox(10);
        Button browseBtn = new Button("Browse Directory");
        dirLabel = new Label("No directory selected");
        browseBtn.setOnAction(e -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setTitle("Select MP3 Directory");
            if (currentDirectory != null) {
                chooser.setInitialDirectory(currentDirectory.toFile());
            }
            File selected = chooser.showDialog(null);
            if (selected != null) {
                currentDirectory = selected.toPath();
                dirLabel.setText(selected.getAbsolutePath());
                saveLastDirectory(selected.toPath());
                loadTracks(selected.toPath());
            }
        });
        dirBox.getChildren().addAll(browseBtn, dirLabel);

        // Filter row
        HBox filterBox = new HBox(10);
        Label filterLabel = new Label("Filter:");
        filterTextField = new TextField();
        filterTextField.setPromptText("Enter search terms (space-separated)");
        filterTextField.setOnKeyReleased(e -> applyFilter());
        filterBox.getChildren().addAll(filterLabel, filterTextField);

        vbox.getChildren().addAll(dirBox, filterBox);
        return vbox;
    }

    private TableView<TrackEntry> createTrackTable() {
        TableView<TrackEntry> table = new TableView<>();

        // Enable multi-select
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        TableColumn<TrackEntry, String> filenameCol = new TableColumn<>("Filename");
        filenameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilename()));
        filenameCol.setPrefWidth(200);

        TableColumn<TrackEntry, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
        titleCol.setPrefWidth(200);

        TableColumn<TrackEntry, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArtist()));
        artistCol.setPrefWidth(200);

        TableColumn<TrackEntry, String> artworkCol = new TableColumn<>("Artwork");
        artworkCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().artworkDisplayValue()));
        artworkCol.setPrefWidth(100);

        table.getColumns().addAll(filenameCol, titleCol, artistCol, artworkCol);

        // Add right-click context menu
        ContextMenu contextMenu = createTableContextMenu();
        table.setContextMenu(contextMenu);

        return table;
    }

    private ContextMenu createTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem replaceArtworkItem = new MenuItem("Replace Artwork...");
        replaceArtworkItem.setOnAction(e -> replaceArtworkForSelectedTracks());

        contextMenu.getItems().add(replaceArtworkItem);
        return contextMenu;
    }

    private void loadTracks(Path directory) {
        try {
            List<TrackEntry> tracks = service.loadFromDirectory(directory);
            allTracksUnfiltered = new ArrayList<>(tracks);
            applyFilterInternal(tracks);
            statusLabel.setText("Loaded " + tracks.size() + " MP3 files from: " + directory);
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void applyFilter() {
        if (allTracksUnfiltered.isEmpty()) {
            return;
        }

        String filterText = filterTextField.getText();

        try {
            List<TrackEntry> filtered = SearchFilter.filter(allTracksUnfiltered, filterText);

            trackTable.setItems(FXCollections.observableArrayList(filtered));
            statusLabel.setText("Showing " + filtered.size() + " tracks");
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void applyFilterInternal(List<TrackEntry> allTracks) {
        String filterText = filterTextField.getText();

        try {
            List<TrackEntry> filtered = SearchFilter.filter(allTracks, filterText);

            trackTable.setItems(FXCollections.observableArrayList(filtered));
            statusLabel.setText("Showing " + filtered.size() + " tracks");
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void replaceArtworkForSelectedTracks() {
        ObservableList<TrackEntry> selectedTracks = trackTable.getSelectionModel().getSelectedItems();
        if (selectedTracks == null || selectedTracks.isEmpty()) {
            statusLabel.setText("Error: Please select at least one track");
            return;
        }

        if (currentDirectory == null) {
            statusLabel.setText("Error: No directory selected");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Image File for Artwork");
        chooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*")
        );
        File imageFile = chooser.showOpenDialog(null);

        if (imageFile != null) {
            int successCount = 0;
            int failureCount = 0;

            for (TrackEntry track : selectedTracks) {
                try {
                    Path mp3Path = currentDirectory.resolve(track.getFilename());
                    service.addOrReplaceArtwork(mp3Path, imageFile.toPath());
                    successCount++;
                } catch (Exception ex) {
                    failureCount++;
                }
            }

            String message = String.format("Artwork updated: %d succeeded, %d failed", successCount, failureCount);
            statusLabel.setText(message);

            // Reload the tracks to show the updated artwork
            loadTracks(currentDirectory);
        }
    }

    private void saveLastDirectory(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = new Properties();
            props.setProperty("last.directory", directory.toString());
            try (var out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "SingSongArtwork Configuration");
            }
        } catch (IOException ex) {
            statusLabel.setText("Warning: Could not save directory preference: " + ex.getMessage());
        }
    }

    private void initializeLastDirectoryPath() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties props = new Properties();
                try (var in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
                String lastDir = props.getProperty("last.directory");
                if (lastDir != null && !lastDir.isBlank()) {
                    Path lastPath = Paths.get(lastDir);
                    if (Files.isDirectory(lastPath)) {
                        currentDirectory = lastPath;
                        dirLabel.setText(lastPath.toString());
                        // Note: We only set the path, but don't load the tracks
                    }
                }
            }
        } catch (IOException ex) {
            // Silently ignore - it's okay if there's no config file yet
        }
    }

    private void ensureConfigDirectory() throws IOException {
        Path configDir = CONFIG_FILE.getParent();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

