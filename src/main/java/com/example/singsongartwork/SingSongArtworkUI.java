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
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
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
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SingSongArtworkUI extends Application {
    private Mp3MetadataService service;
    private TableView<TrackEntry> trackTable;
    private TextField filterTextField;
    private Label statusLabel;
    private Label selectionLabel;
    private Label dirLabel;
    private ProgressIndicator loadingIndicator;
    private Path currentDirectory;
    private List<TrackEntry> allTracksUnfiltered = new ArrayList<>();
    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".singsongartwork", "config.properties");

    // Phase 3: Artwork cache and in-flight tracking for lazy loading
    private final Map<Path, byte[]> artworkBytesCache = new ConcurrentHashMap<>();
    private final Set<Path> artworkLoadsInFlight = ConcurrentHashMap.newKeySet();

    @Override
    public void start(Stage primaryStage) {
        service = new Mp3MetadataService();

        // Main layout
        BorderPane root = new BorderPane();

        // Menu bar
        MenuBar menuBar = createMenuBar();

        // Top: menu bar and directory selection/controls
        VBox topSection = new VBox();
        topSection.getChildren().add(menuBar);
        VBox topPanel = createTopPanel();
        topSection.getChildren().add(topPanel);
        root.setTop(topSection);

        // Center: table
        trackTable = createTrackTable();
        root.setCenter(trackTable);

        // Bottom: status
        root.setBottom(createStatusBar());

        Scene scene = new Scene(root, 1200, 750);

        // Apply modern dark theme CSS
        String css = getClass().getResource("/styles/modern-dark.css").toExternalForm();
        scene.getStylesheets().add(css);

        primaryStage.setTitle("SingSongArtwork");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
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

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();

        // Help menu
        Menu helpMenu = new Menu("Help");
        MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts...");
        shortcutsItem.setOnAction(e -> showKeyboardShortcuts());
        helpMenu.getItems().add(shortcutsItem);

        // Three-dot menu (⋮) on the right side
        Menu optionsMenu = new Menu("⋮");
        optionsMenu.setStyle("-fx-font-size: 18px;");

        // Directory info as menu label (non-clickable)
        CustomMenuItem dirMenuItem = new CustomMenuItem();
        VBox dirInfo = new VBox(4);
        dirInfo.setPadding(new Insets(8, 12, 8, 12));
        Label dirTitleLabel = new Label("Current Directory:");
        dirTitleLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");
        dirLabel = new Label("No directory selected");
        dirLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #ffffff;");
        dirLabel.setWrapText(true);
        dirLabel.setMaxWidth(300);
        dirInfo.getChildren().addAll(dirTitleLabel, dirLabel);
        dirMenuItem.setContent(dirInfo);
        dirMenuItem.setHideOnClick(false);

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem browseItem = new MenuItem("Browse Directory...");
        browseItem.setOnAction(e -> openDirectoryChooser());

        MenuItem reloadItem = new MenuItem("Reload Directory");
        reloadItem.setOnAction(e -> {
            if (currentDirectory != null) {
                loadTracksAsync(currentDirectory);
            }
        });

        optionsMenu.getItems().addAll(dirMenuItem, separator1, browseItem, reloadItem);

        menuBar.getMenus().addAll(helpMenu, optionsMenu);
        return menuBar;
    }

    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("SingSongArtwork - Keyboard Shortcuts");

        String shortcuts = """
                File Operations:
                  Ctrl+O          Open/Browse Directory
                  Ctrl+R          Reload Current Directory
                
                Filter & Search:
                  Ctrl+F          Focus Filter Field
                  Ctrl+L          Clear Filter
                
                Selection & Editing:
                  Ctrl+C          Copy Filename(s) to Clipboard
                  Right-click     Context Menu (Replace Artwork, Batch Edit, Copy Filename)
                
                Table Navigation:
                  Ctrl+Click      Multi-select tracks
                  Shift+Click     Select range
                  Ctrl+A          Select all tracks
                
                Drag & Drop:
                  Drag image onto selected tracks to replace artwork
                """;

        TextArea textArea = new TextArea(shortcuts);
        textArea.setEditable(false);
        textArea.setWrapText(false);
        textArea.setPrefRowCount(18);
        textArea.setPrefColumnCount(50);

        alert.getDialogPane().setContent(textArea);
        alert.showAndWait();
    }

    private VBox createTopPanel() {
        VBox vbox = new VBox(12);
        vbox.setPadding(new Insets(16));
        vbox.getStyleClass().add("top-panel");

        // Filter row with loading indicator
        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("filter-box");

        Label filterLabel = new Label("Filter:");

        filterTextField = new TextField();
        filterTextField.setPromptText("Search tracks by filename, title, or artist...");
        filterTextField.setPrefWidth(400);
        HBox.setHgrow(filterTextField, Priority.ALWAYS);
        filterTextField.setOnKeyReleased(e -> applyFilter());

        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.setOnAction(e -> clearFilter());

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        loadingIndicator.setPrefSize(24, 24);

        filterBox.getChildren().addAll(filterLabel, filterTextField, clearFilterBtn, loadingIndicator);

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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.C, KeyCombination.CONTROL_DOWN), this::copyFilenameToClipboard);
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
                alert.setHeaderText("⚠️ No MP3 files in selected directory");

                // Apply dark theme CSS to alert
                alert.getDialogPane().getStylesheets().add(
                    getClass().getResource("/styles/modern-dark.css").toExternalForm()
                );

                VBox alertContent = new VBox(12);
                alertContent.setPadding(new Insets(10));
                Label pathLabel = new Label("Directory:");
                pathLabel.setStyle("-fx-font-weight: bold;");
                Label pathValue = new Label(directory.toString());
                pathValue.setWrapText(true);
                pathValue.setStyle("-fx-text-fill: #b3b3b3;");
                Label question = new Label("\nThis directory does not contain any MP3 files.\nDo you want to continue anyway?");
                alertContent.getChildren().addAll(pathLabel, pathValue, question);

                alert.getDialogPane().setContent(alertContent);
                return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
            }

            // Show preview dialog with sample files
            Dialog<ButtonType> previewDialog = new Dialog<>();
            previewDialog.setTitle("Directory Preview");
            previewDialog.setHeaderText("Selected Directory");

            // Apply dark theme CSS to dialog
            previewDialog.getDialogPane().getStylesheets().add(
                getClass().getResource("/styles/modern-dark.css").toExternalForm()
            );

            VBox content = new VBox(16);
            content.setPadding(new Insets(20));

            // Directory path label
            Label dirPathLabel = new Label(directory.toString());
            dirPathLabel.setWrapText(true);
            dirPathLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #b3b3b3;");

            // Info label with count
            Label info = new Label("📁 MP3 files found (" + mp3Files.size() + " shown):");
            info.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

            // File list with better styling
            TextArea fileList = new TextArea(String.join("\n", mp3Files));
            fileList.setEditable(false);
            fileList.setWrapText(false);
            fileList.setPrefRowCount(10);
            fileList.setPrefColumnCount(60);
            fileList.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 13px;");

            content.getChildren().add(dirPathLabel);
            content.getChildren().add(info);
            content.getChildren().add(fileList);
            previewDialog.getDialogPane().setContent(content);
            previewDialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
            previewDialog.getDialogPane().getButtonTypes().add(ButtonType.CANCEL);

            // Set preferred size for dialog
            previewDialog.getDialogPane().setPrefWidth(700);

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

                byte[] artworkBytes = getArtworkBytesForItem(item);
                if (artworkBytes.length == 0) {
                    setText("yes");
                    setGraphic(null);
                    triggerArtworkLazyLoad(item);
                    return;
                }

                try {
                    Image image = new Image(new ByteArrayInputStream(artworkBytes), 48, 48, true, true);
                    if (image.isError()) {
                        setText("yes");
                        setGraphic(null);
                        return;
                    }
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(48);
                    imageView.setFitHeight(48);
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

        // Enable drag-and-drop artwork replacement.
        configureArtworkDragAndDrop(table);

        return table;
    }

    private byte[] getArtworkBytesForItem(TrackEntry item) {
        byte[] embedded = item.getArtwork();
        if (embedded.length > 0) {
            return embedded;
        }
        return artworkBytesCache.getOrDefault(item.getFilePath(), new byte[0]);
    }

    private void triggerArtworkLazyLoad(TrackEntry item) {
        Path path = item.getFilePath();
        if (!item.hasArtwork() || artworkBytesCache.containsKey(path) || !artworkLoadsInFlight.add(path)) {
            return;
        }

        Task<byte[]> loadArtworkTask = new Task<>() {
            @Override
            protected byte[] call() {
                return service.loadArtworkBytes(path);
            }
        };

        loadArtworkTask.setOnSucceeded(e -> {
            artworkLoadsInFlight.remove(path);
            byte[] bytes = loadArtworkTask.getValue();
            if (bytes != null && bytes.length > 0) {
                artworkBytesCache.put(path, bytes);
                trackTable.refresh();
            }
        });

        loadArtworkTask.setOnFailed(e -> artworkLoadsInFlight.remove(path));

        Thread worker = new Thread(loadArtworkTask, "artwork-lazy-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void configureArtworkDragAndDrop(TableView<TrackEntry> table) {
        table.setOnDragOver(event -> {
            if (event.getGestureSource() != table && event.getDragboard().hasFiles()) {
                if (event.getDragboard().getFiles().stream().anyMatch(this::isSupportedImageFile)) {
                    event.acceptTransferModes(TransferMode.COPY);
                }
            }
            event.consume();
        });

        table.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            boolean success = false;
            if (dragboard.hasFiles()) {
                File image = dragboard.getFiles().stream().filter(this::isSupportedImageFile).findFirst().orElse(null);
                if (image != null) {
                    ObservableList<TrackEntry> selected = trackTable.getSelectionModel().getSelectedItems();
                    if (selected != null && !selected.isEmpty()) {
                        applyArtworkToTracks(new ArrayList<>(selected), image.toPath(), "drag-and-drop");
                        success = true;
                    } else {
                        statusLabel.setText("Select at least one track before dropping artwork.");
                    }
                }
            }
            event.setDropCompleted(success);
            event.consume();
        });
    }

    private boolean isSupportedImageFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".gif");
    }

    private HBox createStatusBar() {
        statusLabel = new Label("Ready. Select a directory to begin.");
        selectionLabel = new Label("Selected: 0 | Visible: 0");

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox statusBar = new HBox(10);
        statusBar.getStyleClass().add("status-bar");
        statusBar.setPadding(new Insets(10, 14, 10, 14));
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

        // Clear existing data
        allTracksUnfiltered.clear();
        artworkBytesCache.clear();
        artworkLoadsInFlight.clear();
        trackTable.setItems(FXCollections.observableArrayList());

        Task<Void> loadTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                try (var stream = java.nio.file.Files.list(directory)) {
                    stream.filter(path -> !java.nio.file.Files.isDirectory(path))
                          .filter(path -> path.toString().toLowerCase().endsWith(".mp3"))
                          .forEach(mp3Path -> {
                              try {
                                  TrackEntry track = service.loadSingleTrack(mp3Path);
                                  if (track != null) {
                                      // Update UI on JavaFX thread
                                      Platform.runLater(() -> {
                                          allTracksUnfiltered.add(track);
                                          applyFilterInternal(new ArrayList<>(allTracksUnfiltered));
                                          updateSelectionStatus();
                                      });
                                  }
                              } catch (Exception ex) {
                                  // Skip problematic files but continue loading
                              }
                          });
                }
                return null;
            }
        };

        loadTask.setOnSucceeded(e -> {
            statusLabel.setText("Loaded " + allTracksUnfiltered.size() + " MP3 files from: " + directory);
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

        MenuItem batchEditItem = new MenuItem("Batch Edit Metadata...");
        batchEditItem.setOnAction(e -> openBatchEditDialog());

        MenuItem copyFilenameItem = new MenuItem("Copy Filename(s)");
        copyFilenameItem.setOnAction(e -> copyFilenameToClipboard());

        contextMenu.getItems().add(replaceArtworkItem);
        contextMenu.getItems().add(batchEditItem);
        contextMenu.getItems().add(new SeparatorMenuItem());
        contextMenu.getItems().add(copyFilenameItem);
        return contextMenu;
    }

    private void copyFilenameToClipboard() {
        ObservableList<TrackEntry> selectedTracks = trackTable.getSelectionModel().getSelectedItems();
        if (selectedTracks == null || selectedTracks.isEmpty()) {
            statusLabel.setText("No tracks selected to copy");
            return;
        }

        // Collect filenames
        String filenames = selectedTracks.stream()
                .map(TrackEntry::getFilename)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

        // Copy to clipboard
        ClipboardContent content = new ClipboardContent();
        content.putString(filenames);
        Clipboard.getSystemClipboard().setContent(content);

        statusLabel.setText("Copied " + selectedTracks.size() + " filename(s) to clipboard");
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
            saveLastArtworkDirectory(imageFile.toPath().getParent());
            applyArtworkToTracks(new ArrayList<>(selectedTracks), imageFile.toPath(), "picker");
        }
    }

    private void applyArtworkToTracks(List<TrackEntry> selectedTracks, Path imagePath, String source) {
        int successCount = 0;
        int failureCount = 0;
        List<Path> modifiedPaths = new ArrayList<>();

        for (TrackEntry track : selectedTracks) {
            try {
                Path mp3Path = currentDirectory.resolve(track.getFilename());
                service.addOrReplaceArtwork(mp3Path, imagePath);
                successCount++;
                modifiedPaths.add(mp3Path);
            } catch (Exception ex) {
                failureCount++;
            }
        }

        statusLabel.setText(String.format("Artwork updated via %s: %d succeeded, %d failed", source, successCount, failureCount));

        // Reload modified tracks from disk to get updated metadata
        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (Path modifiedPath : modifiedPaths) {
                    // Invalidate cache and reload
                    service.invalidateCache(modifiedPath);
                    artworkBytesCache.remove(modifiedPath);
                    artworkLoadsInFlight.remove(modifiedPath);

                    // Load fresh track entry from disk
                    TrackEntry reloadedTrack = service.loadSingleTrack(modifiedPath);
                    if (reloadedTrack != null) {
                        // Find and update the entry in allTracksUnfiltered
                        for (int i = 0; i < allTracksUnfiltered.size(); i++) {
                            if (allTracksUnfiltered.get(i).getFilePath().equals(modifiedPath)) {
                                allTracksUnfiltered.set(i, reloadedTrack);
                                break;
                            }
                        }
                    }
                }
                return null;
            }
        };

        refreshTask.setOnSucceeded(e -> {
            applyFilterInternal(allTracksUnfiltered);
            trackTable.refresh();
        });

        Thread worker = new Thread(refreshTask, "artwork-refresh-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void openBatchEditDialog() {
        ObservableList<TrackEntry> selectedTracks = trackTable.getSelectionModel().getSelectedItems();
        if (selectedTracks == null || selectedTracks.isEmpty()) {
            statusLabel.setText("Error: Please select at least one track");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Batch Edit Metadata");
        dialog.setHeaderText("Update metadata for " + selectedTracks.size() + " selected tracks");

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

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        String newTitle = titleField.getText();
        String newArtist = artistField.getText();
        if ((newTitle == null || newTitle.isBlank()) && (newArtist == null || newArtist.isBlank())) {
            statusLabel.setText("Batch edit cancelled: no metadata values provided.");
            return;
        }

        List<Path> paths = selectedTracks.stream()
                .map(track -> currentDirectory.resolve(track.getFilename()))
                .toList();
        int updated = service.batchEditMetadata(paths, newTitle, newArtist);
        statusLabel.setText("Batch metadata edit updated " + updated + " tracks.");

        // Invalidate cache for modified files and refresh them
        for (Path path : paths) {
            service.invalidateCache(path);
        }

        // Reload only the modified tracks by reloading them from disk
        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (TrackEntry track : selectedTracks) {
                    Path mp3Path = currentDirectory.resolve(track.getFilename());
                    TrackEntry reloadedTrack = service.loadSingleTrack(mp3Path);
                    if (reloadedTrack != null) {
                        // Update the entry in allTracksUnfiltered
                        int idx = allTracksUnfiltered.indexOf(track);
                        if (idx >= 0) {
                            allTracksUnfiltered.set(idx, reloadedTrack);
                        }
                    }
                }
                return null;
            }
        };

        refreshTask.setOnSucceeded(e -> {
            applyFilterInternal(allTracksUnfiltered);
            trackTable.refresh();
        });

        Thread worker = new Thread(refreshTask, "batch-metadata-refresher");
        worker.setDaemon(true);
        worker.start();
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

