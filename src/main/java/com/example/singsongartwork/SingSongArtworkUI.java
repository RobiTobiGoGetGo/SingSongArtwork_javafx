package com.example.singsongartwork;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.ByteArrayInputStream;
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
    private Label selectionLabel;
    private Label dirLabel;
    private Button browseBtn;
    private ProgressIndicator loadingIndicator;
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
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 900, 600);
        primaryStage.setTitle("SingSongArtwork");
        primaryStage.setScene(scene);
        configureKeyboardShortcuts(scene);

        // Initialize dirLabel with the last used directory path (but don't load it)
        initializeLastDirectoryPath();

        // Properly terminate the application when the window is closed
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            System.exit(0);
        });

        primaryStage.show();

        // ALWAYS auto-open directory chooser on startup (user must explicitly choose directory)
        Platform.runLater(this::openDirectoryChooser);
    }

    private VBox createTopPanel() {
        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // Directory selection row
        HBox dirBox = new HBox(10);
        browseBtn = new Button("Browse Directory");
        dirLabel = new Label("No directory selected");
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        loadingIndicator.setPrefSize(18, 18);
        browseBtn.setOnAction(e -> openDirectoryChooser());
        dirBox.getChildren().add(browseBtn);
        dirBox.getChildren().add(dirLabel);
        dirBox.getChildren().add(loadingIndicator);

        // Filter row
        HBox filterBox = new HBox(10);
        Label filterLabel = new Label("Filter:");
        filterTextField = new TextField();
        filterTextField.setPromptText("Enter search terms (space-separated)");
        filterTextField.setOnKeyReleased(e -> applyFilter());
        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.setOnAction(e -> clearFilter());
        filterBox.getChildren().add(filterLabel);
        filterBox.getChildren().add(filterTextField);
        filterBox.getChildren().add(clearFilterBtn);

        vbox.getChildren().add(dirBox);
        vbox.getChildren().add(filterBox);
        return vbox;
    }

    private void configureKeyboardShortcuts(Scene scene) {
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.O, KeyCombination.CONTROL_DOWN), this::openDirectoryChooser);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.L, KeyCombination.CONTROL_DOWN), this::clearFilter);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.F, KeyCombination.CONTROL_DOWN), () -> {
            if (filterTextField != null) {
                filterTextField.requestFocus();
                filterTextField.selectAll();
            }
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.R, KeyCombination.CONTROL_DOWN), () -> {
            if (currentDirectory != null) {
                loadTracksAsync(currentDirectory);
            }
        });
    }

    private void openDirectoryChooser() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Select MP3 Directory");
        if (currentDirectory != null) {
            chooser.setInitialDirectory(currentDirectory.toFile());
        }
        File selected = chooser.showDialog(null);
        if (selected != null) {
            // Show preview of MP3 files in the directory
            if (showDirectoryPreview(selected.toPath())) {
                currentDirectory = selected.toPath();
                dirLabel.setText(selected.getAbsolutePath());
                saveLastDirectory(selected.toPath());
                loadTracks(selected.toPath());
            }
        }
    }

    private boolean showDirectoryPreview(Path directory) {
        try {
            List<String> mp3Files = Files.list(directory)
                    .filter(p -> !Files.isDirectory(p) && p.toString().toLowerCase().endsWith(".mp3"))
                    .map(p -> p.getFileName().toString())
                    .sorted()
                    .limit(10)
                    .toList();

            if (mp3Files.isEmpty()) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("No MP3 Files Found");
                alert.setHeaderText("No MP3 files in selected directory");
                alert.setContentText("The directory:\n" + directory + "\n\ndoes not contain any MP3 files. Continue anyway?");
                return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
            }

            // Show preview dialog with sample files
            Dialog<ButtonType> previewDialog = new Dialog<>();
            previewDialog.setTitle("Directory Preview");
            previewDialog.setHeaderText("Selected Directory: " + directory);

            VBox content = new VBox(10);
            content.setPadding(new Insets(10));

            Label info = new Label("MP3 files found (" + mp3Files.size() + " shown):");
            TextArea fileList = new TextArea(String.join("\n", mp3Files));
            fileList.setEditable(false);
            fileList.setWrapText(true);
            fileList.setPrefRowCount(8);

            content.getChildren().add(info);
            content.getChildren().add(fileList);
            previewDialog.getDialogPane().setContent(content);
            previewDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            previewDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

            return previewDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        } catch (Exception ex) {
            statusLabel.setText("Error reading directory: " + ex.getMessage());
            return false;
        }
    }

    private TableView<TrackEntry> createTrackTable() {
        TableView<TrackEntry> table = new TableView<>();

        // Enable multi-select
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        TableColumn<TrackEntry, String> filenameCol = new TableColumn<>("Filename");
        filenameCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilename()));
        filenameCol.setPrefWidth(260);
        filenameCol.setSortable(true);
        filenameCol.setResizable(true);

        TableColumn<TrackEntry, String> titleCol = new TableColumn<>("Title");
        titleCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
        titleCol.setPrefWidth(260);
        titleCol.setSortable(true);
        titleCol.setResizable(true);

        TableColumn<TrackEntry, String> artistCol = new TableColumn<>("Artist");
        artistCol.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArtist()));
        artistCol.setPrefWidth(240);
        artistCol.setSortable(true);
        artistCol.setResizable(true);

        TableColumn<TrackEntry, TrackEntry> artworkCol = new TableColumn<>("Artwork");
        artworkCol.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        artworkCol.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TrackEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                if (!item.hasArtwork()) {
                    setText("no");
                    setGraphic(null);
                    return;
                }

                try {
                    Image image = new Image(new ByteArrayInputStream(item.getArtwork()), 32, 32, true, true);
                    if (image.isError()) {
                        setText("yes");
                        setGraphic(null);
                        return;
                    }
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(32);
                    imageView.setFitHeight(32);
                    imageView.setPreserveRatio(true);
                    setText("yes");
                    setGraphic(imageView);
                } catch (Exception ex) {
                    setText("yes");
                    setGraphic(null);
                }
            }
        });
        artworkCol.setComparator((a, b) -> Boolean.compare(a.hasArtwork(), b.hasArtwork()));
        artworkCol.setPrefWidth(140);
        artworkCol.setSortable(true);
        artworkCol.setResizable(true);

        table.getColumns().add(filenameCol);
        table.getColumns().add(titleCol);
        table.getColumns().add(artistCol);
        table.getColumns().add(artworkCol);

        // Add right-click context menu
        ContextMenu contextMenu = createTableContextMenu();
        table.setContextMenu(contextMenu);

        // Keep bottom status bar in sync with selection/data changes.
        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TrackEntry>) change -> updateSelectionStatus());
        table.itemsProperty().addListener((obs, oldItems, newItems) -> updateSelectionStatus());

        return table;
    }

    private HBox createStatusBar() {
        statusLabel = new Label("Ready. Select a directory to begin.");
        selectionLabel = new Label("Selected: 0 | Visible: 0");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(8, 10, 8, 10));
        statusBar.setAlignment(Pos.CENTER_LEFT);
        statusBar.getChildren().add(statusLabel);
        statusBar.getChildren().add(spacer);
        statusBar.getChildren().add(selectionLabel);
        return statusBar;
    }

    private void updateSelectionStatus() {
        if (selectionLabel == null || trackTable == null) {
            return;
        }
        int selected = trackTable.getSelectionModel().getSelectedItems().size();
        int visible = trackTable.getItems() == null ? 0 : trackTable.getItems().size();
        int total = allTracksUnfiltered == null ? 0 : allTracksUnfiltered.size();
        selectionLabel.setText("Selected: " + selected + " | Visible: " + visible + " | Total: " + total);
    }

    private void clearFilter() {
        filterTextField.clear();
        applyFilter();
    }

    private void setLoadingState(boolean loading, String message) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
            loadingIndicator.setManaged(loading);
        }
        if (browseBtn != null) {
            browseBtn.setDisable(loading);
        }
        if (filterTextField != null) {
            filterTextField.setDisable(loading);
        }
        if (trackTable != null) {
            trackTable.setDisable(loading);
        }
        if (message != null && statusLabel != null) {
            statusLabel.setText(message);
        }
    }

    private void loadTracksAsync(Path directory) {
        setLoadingState(true, "Loading MP3 files from: " + directory + " ...");

        Task<List<TrackEntry>> loadTask = new Task<>() {
            @Override
            protected List<TrackEntry> call() throws Exception {
                return service.loadFromDirectory(directory);
            }
        };

        loadTask.setOnSucceeded(e -> {
            List<TrackEntry> tracks = loadTask.getValue();
            allTracksUnfiltered = new ArrayList<>(tracks);
            applyFilterInternal(tracks);
            statusLabel.setText("Loaded " + tracks.size() + " MP3 files from: " + directory);
            updateSelectionStatus();
            setLoadingState(false, null);
        });

        loadTask.setOnFailed(e -> {
            Throwable error = loadTask.getException();
            statusLabel.setText("Error: " + (error == null ? "Unknown error" : error.getMessage()));
            setLoadingState(false, null);
        });

        Thread worker = new Thread(loadTask, "mp3-directory-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void loadTracks(Path directory) {
        loadTracksAsync(directory);
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
            updateSelectionStatus();
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
            updateSelectionStatus();
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private ContextMenu createTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();

        MenuItem replaceArtworkItem = new MenuItem("Replace Artwork...");
        replaceArtworkItem.setOnAction(e -> replaceArtworkForSelectedTracks());

        contextMenu.getItems().add(replaceArtworkItem);
        return contextMenu;
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

        // Set initial directory to last used artwork directory
        Path lastArtworkDir = getLastArtworkDirectory();
        if (lastArtworkDir != null && Files.isDirectory(lastArtworkDir)) {
            chooser.setInitialDirectory(lastArtworkDir.toFile());
        }

        File imageFile = chooser.showOpenDialog(null);

        if (imageFile != null) {
            // Save the artwork directory for next time
            saveLastArtworkDirectory(imageFile.toPath().getParent());

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

            // Also load and preserve last artwork directory if it exists
            Path lastArtworkDir = getLastArtworkDirectory();
            if (lastArtworkDir != null) {
                props.setProperty("last.artwork.directory", lastArtworkDir.toString());
            }

            try (var out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "SingSongArtwork Configuration");
            }
        } catch (IOException ex) {
            statusLabel.setText("Warning: Could not save directory preference: " + ex.getMessage());
        }
    }

    private void saveLastArtworkDirectory(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = new Properties();

            // Load existing properties
            if (Files.exists(CONFIG_FILE)) {
                try (var in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }

            // Update artwork directory
            if (directory != null) {
                props.setProperty("last.artwork.directory", directory.toString());
            }

            // Save
            try (var out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "SingSongArtwork Configuration");
            }
        } catch (IOException ex) {
            statusLabel.setText("Warning: Could not save artwork directory preference: " + ex.getMessage());
        }
    }

    private Path getLastArtworkDirectory() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties props = new Properties();
                try (var in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
                String lastArtworkDir = props.getProperty("last.artwork.directory");
                if (lastArtworkDir != null && !lastArtworkDir.isBlank()) {
                    Path lastPath = Paths.get(lastArtworkDir);
                    if (Files.isDirectory(lastPath)) {
                        return lastPath;
                    }
                }
            }
        } catch (IOException ex) {
            // Silently ignore
        }
        return null;
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

