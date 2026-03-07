package com.example.singsongartwork;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.css.PseudoClass;
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
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.awt.Desktop;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SingSongArtworkUI extends Application {
    private static final int ARTWORK_THUMB_SMALL = 48;
    private static final int ARTWORK_THUMB_LARGE = 96;
    private boolean largeArtworkMode = false;

    private Mp3MetadataService service;
    private ConfigurationManager configManager;
    private TableView<TrackEntry> trackTable;
    private TextField filterTextField;
    private Label statusLabel;
    private Label selectionLabel;
    private Label musicDirectoryLabel;
    private Label copyDirectoryLabel;
    private ProgressIndicator loadingIndicator;
    private Path currentDirectory;
    private final List<TrackEntry> allTracksUnfiltered = new ArrayList<>();
    private TableColumn<TrackEntry, TrackEntry> artworkColumn;
    private TableColumn<TrackEntry, String> filenameColumn;
    private TableColumn<TrackEntry, String> titleColumn;
    private TableColumn<TrackEntry, String> artistColumn;
    private TableColumn<TrackEntry, TrackEntry> transportColumn;
    private Label nowPlayingLabel;
    private Label playbackTimeLabel;
    private Slider playbackSlider;
    private Button playbackPlayPauseButton;
    private Button playbackStopButton;
    private Object mediaPlayer;
    private Path playingTrackPath;
    private boolean scrubbingPlayback;
    private boolean moreColumnsMode = false; // default: Less mode
    private boolean adminMode = false; // default: User mode
    private static final Path CONFIG_FILE = Paths.get(System.getProperty("user.home"), ".singsongartwork", "config.properties");
    private static final String KEY_LAST_MUSIC_DIRECTORY = "last.music.directory";
    private static final String KEY_LAST_ARTWORK_DIRECTORY = "last.artwork.directory";
    private static final String KEY_LAST_COPY_DIRECTORY = "last.copy.directory";
    private static final String KEY_UI_COLUMN_MODE = "ui.column.mode";
    private static final String KEY_UI_ROLE = "ui.role";
    private static final String ADMIN_PASSWORD = "pwd";
    private static final DateTimeFormatter LOG_TS_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // Phase 3: Artwork cache and in-flight tracking for lazy loading
    private final Map<Path, byte[]> artworkBytesCache = new ConcurrentHashMap<>();
    private final Set<Path> artworkLoadsInFlight = ConcurrentHashMap.newKeySet();
    private static final PseudoClass PLAYING_ROW_PSEUDO_CLASS = PseudoClass.getPseudoClass("playing");

    private TableColumn<TrackEntry, Boolean> choicesColumn;
    private final Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();
    private boolean showChoicesOnly = false;
    private String retainedFilterText = ""; // Retained when "Show choices" is active

    // Role menu items - made class-level for keyboard shortcut access
    private RadioMenuItem userRoleItem;
    private RadioMenuItem adminRoleItem;
    private Menu roleMenu;
    private MenuButton optionsMenu;
    private MenuButton helpMenu; // Class-level to allow dynamic updates
    private MenuBarBuilder menuBarBuilder;
    private TrackTableBuilder tableBuilder;
    private FilterPanelBuilder filterPanelBuilder;
    private CheckMenuItem showChoicesOnlyMenuItem;
    private PlaybackBarBuilder playbackBarBuilder;
    private final StringBuilder appLogBuffer = new StringBuilder();

    @Override
    public void start(Stage primaryStage) {
        service = new Mp3MetadataService();
        configManager = new ConfigurationManager(CONFIG_FILE);
        initializeUiPreferences();

        // Main layout
        BorderPane root = new BorderPane();

        // Custom title bar area + menu bar
        HBox titleBar = new HBox();
        titleBar.setStyle("-fx-background-color: #000000; -fx-padding: 0;");
        titleBar.setAlignment(Pos.CENTER_LEFT);

        // App title on left
        Label titleLabel = new Label("SingSongArtwork");
        titleLabel.setStyle("-fx-text-fill: #00d9ff; -fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 16px;");

        // Hamburger and options menus are now composed via MenuBarBuilder.
        menuBarBuilder = new MenuBarBuilder(
                this::showAppLogDialog,
                () -> showMarkdownFile("README.md", "README"),
                () -> showMarkdownFile("LICENSE.md", "LICENSE"),
                this::showKeyboardShortcuts,
                this::openDirectoryChooser,
                () -> {
                    if (currentDirectory != null) {
                        if (showDirectoryPreview(currentDirectory)) {
                            loadTracksAsync(currentDirectory);
                        }
                    } else if (statusLabel != null) {
                        statusLabel.setText("Error: No music directory set. Please choose a music directory first.");
                    }
                },
                this::openDirectoryChooser,
                this::chooseCopyDirectory,
                selected -> {
                    if (filterPanelBuilder != null) {
                        filterPanelBuilder.setShowChoicesOnly(selected);
                        syncShowChoicesStateFromBuilder();
                        applyFilter();
                    }
                },
                this::copyChoicesTracksToCopyDirectory,
                this::clearChoicesTracks,
                mode -> {
                    moreColumnsMode = mode == 1;
                    applyColumnMode();
                    saveUiPreferences();
                },
                this::handleRoleChangeRequested
        );

        var menus = menuBarBuilder.buildMenus();
        helpMenu = menus.getKey();
        optionsMenu = menus.getValue();
        userRoleItem = menuBarBuilder.getUserRoleItem();
        adminRoleItem = menuBarBuilder.getAdminRoleItem();

        // Keep startup state synced with current role.
        menuBarBuilder.updateForAdminMode(adminMode);
        menuBarBuilder.rebuildOptionsMenu(adminMode);

        // Spacer to push three-dot menu to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        titleBar.getChildren().addAll(titleLabel, spacer, helpMenu, optionsMenu);

        // Top section: title bar + controls
        VBox topSection = new VBox();
        topSection.getChildren().add(titleBar);
        VBox topPanel = createTopPanel();
        topSection.getChildren().add(topPanel);
        root.setTop(topSection);

        // Center: table
        tableBuilder = new TrackTableBuilder(
                () -> new byte[0],  // Artwork bytes will be handled per-item in cell factory
                this::onTransportClicked,
                ignored -> {
                    if (showChoicesOnly) {
                        applyFilter();
                    }
                    updateSelectionStatus();
                },
                this::isMediaPlaying,
                choicesTrackPaths
        );
        trackTable = tableBuilder.buildTable();
        // Replace artwork column with UI version that handles caching and lazy-loading
        replaceArtworkColumn(trackTable);
        configureFilenameDoubleClick();
        tableBuilder.addSelectionListener((ListChangeListener<TrackEntry>) change -> updateSelectionStatus());
        tableBuilder.addItemsListener((obs, oldItems, newItems) -> updateSelectionStatus());
        configureTableRowFactory(trackTable);
        configureArtworkDragAndDrop(trackTable);
        ContextMenu contextMenu = createTableContextMenu();
        tableBuilder.setContextMenu(contextMenu);
        root.setCenter(trackTable);

        // Bottom: playback bar + status
        root.setBottom(createBottomPanel());

        Scene scene = new Scene(root, 1200, 750);

        // Apply modern dark theme CSS
        String css = getClass().getResource("/styles/modern-dark.css").toExternalForm();
        scene.getStylesheets().add(css);

        // Set window background to dark
        scene.setFill(javafx.scene.paint.Color.rgb(24, 24, 24));

        primaryStage.setTitle("SingSongArtwork");
        primaryStage.setMinWidth(900);
        primaryStage.setMinHeight(600);
        primaryStage.setScene(scene);

        // Try to get dark title bar on Windows 11
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                // This will make the title bar dark on Windows 11
                primaryStage.initStyle(javafx.stage.StageStyle.DECORATED);
            }
        } catch (Exception e) {
            // Ignore if not supported
        }

        configureKeyboardShortcuts(scene);

        // Initialize dirLabel with the last used music directory path (but don't load it)
        initializeLastMusicDirectory();

        // Initialize copyDirectoryLabel with the last used copy directory path (but don't load it)
        initializeLastCopyDirectory();

        // Properly terminate the application when the window is closed
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            disposeMediaPlayer();
            System.exit(0);
        });

        primaryStage.show();

        // Step 12: If music directory is set, automatically show preview and load files
        // If not set, user must manually choose via "Choose music directory" menu
        if (currentDirectory != null && Files.isDirectory(currentDirectory)) {
            Platform.runLater(() -> {
                if (showDirectoryPreview(currentDirectory)) {
                    loadTracksAsync(currentDirectory);
                }
            });
        }
    }


    private void showKeyboardShortcuts() {
        DialogFactory.showKeyboardShortcuts();
    }

    private VBox createTopPanel() {
        filterPanelBuilder = new FilterPanelBuilder(
                SearchFilter.getDefaultFilterTerms(),
                ignored -> applyFilter(),
                () -> {
                    syncShowChoicesStateFromBuilder();
                    applyFilter();
                    if (statusLabel != null) {
                        statusLabel.setText(showChoicesOnly ? "Showing choices only" : "Showing all tracks");
                    }
                },
                this::copyChoicesTracksToCopyDirectory,
                enabled -> {
                    largeArtworkMode = enabled;
                    if (trackTable != null) {
                        if (artworkColumn != null) {
                            artworkColumn.setPrefWidth((largeArtworkMode ? ARTWORK_THUMB_LARGE : ARTWORK_THUMB_SMALL) + 2);
                        }
                        trackTable.refresh();
                    }
                }
        );
        VBox panel = filterPanelBuilder.buildPanel();
        // Keep legacy field for existing shortcuts/focus logic.
        filterTextField = filterPanelBuilder.getFilterTextField();
        return panel;
    }

    private void showFullArtworkForTrack(TrackEntry track) {
        if (track == null) {
            if (statusLabel != null) {
                statusLabel.setText("Select a row to preview artwork.");
            }
            return;
        }

        byte[] bytes = artworkBytesCache.getOrDefault(track.getFilePath(), new byte[0]);
        if (bytes.length == 0 && track.hasArtwork()) {
            bytes = service.loadArtworkBytes(track.getFilePath());
            if (bytes != null && bytes.length > 0) {
                artworkBytesCache.put(track.getFilePath(), bytes);
                if (trackTable != null) {
                    trackTable.refresh();
                }
            }
        }

        DialogFactory.showArtworkCard(track.getFilename(), bytes);
    }

    private void configureFilenameDoubleClick() {
        if (tableBuilder == null || tableBuilder.getFilenameColumn() == null) {
            return;
        }

        tableBuilder.getFilenameColumn().setCellFactory(col -> new TableCell<TrackEntry, String>() {
            @Override
            public void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                setGraphic(null);
            }

            {
                // Handle double-click on filename cell
                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        TableRow<TrackEntry> row = getTableRow();
                        if (row != null) {
                            TrackEntry rowTrack = row.getItem();
                            if (rowTrack != null) {
                                ClipboardContent content = new ClipboardContent();
                                content.putString(rowTrack.getFilename());
                                Clipboard.getSystemClipboard().setContent(content);
                                if (statusLabel != null) {
                                    statusLabel.setText("Copied filename to clipboard: " + rowTrack.getFilename());
                                }
                                event.consume();
                            }
                        }
                    }
                });
            }
        });
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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.M), () -> setChoicesForSelected(true));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), () -> {
            if (filterPanelBuilder != null) {
                filterPanelBuilder.setShowChoicesOnly(!showChoicesOnly);
                syncShowChoicesStateFromBuilder();
            } else {
                showChoicesOnly = !showChoicesOnly;
            }
            applyFilter();
            statusLabel.setText(showChoicesOnly ? "Showing choices only" : "Showing all tracks");
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::clearChoicesTracks);
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.A, KeyCombination.CONTROL_DOWN, KeyCombination.ALT_DOWN), this::toggleAdminRole);
    }

    private void toggleAdminRole() {
        if (adminMode) {
            // Switch to User mode
            if (userRoleItem != null) {
                userRoleItem.fire();
            }
        } else {
            // Switch to Admin mode
            if (adminRoleItem != null) {
                adminRoleItem.fire();
            }
        }
    }

    private boolean promptForAdminPassword() {
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
        return result == okButton && ADMIN_PASSWORD.equals(passwordField.getText());
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
                if (menuBarBuilder != null) {
                    menuBarBuilder.setMusicDirectory(selected.getAbsolutePath());
                }
                saveLastMusicDirectory(selected.toPath());
                loadTracks(selected.toPath());
            }
        }
    }

    private boolean showDirectoryPreview(Path directory) {
        return showDirectoryPreview(directory, null);
    }

    private boolean showDirectoryPreview(Path directory, String warningMessage) {
        return DialogFactory.showDirectoryPreview(directory, warningMessage);
    }

    private TableView<TrackEntry> createTrackTable() {
        if (tableBuilder != null) {
            // Table is already built by builder in start().
            return trackTable;
        }

        // Fallback if called without builder initialization.
        TableView<TrackEntry> table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        return table;
    }

    private void configureTableRowFactory(TableView<TrackEntry> table) {
        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TrackEntry item, boolean empty) {
                super.updateItem(item, empty);
                boolean isPlayingRow = !empty && item != null && isCurrentTrack(item);
                pseudoClassStateChanged(PLAYING_ROW_PSEUDO_CLASS, isPlayingRow);
            }
        });

        // Step 19: Add space key handler to toggle choice checkbox
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                event.consume();
                ObservableList<TrackEntry> selectedItems = table.getSelectionModel().getSelectedItems();
                if (selectedItems != null && !selectedItems.isEmpty()) {
                    // Toggle choices for all selected rows
                    for (TrackEntry track : new ArrayList<>(selectedItems)) {
                        boolean isCurrentlyChosen = choicesTrackPaths.contains(track.getFilePath());
                        if (isCurrentlyChosen) {
                            choicesTrackPaths.remove(track.getFilePath());
                        } else {
                            choicesTrackPaths.add(track.getFilePath());
                        }
                    }
                    if (showChoicesOnly) {
                        applyFilter();
                    }
                    table.refresh();
                    updateSelectionStatus();
                }
            }
        });
    }

    private void applyColumnMode() {
        if (tableBuilder != null) {
            tableBuilder.updateColumnMode(moreColumnsMode);
        } else {
            // Fallback: manual column mode application
            if (filenameColumn == null || titleColumn == null || artistColumn == null || artworkColumn == null) {
                return;
            }
            boolean showMore = moreColumnsMode;
            titleColumn.setVisible(showMore);
            artistColumn.setVisible(showMore);
        }
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
        int total = allTracksUnfiltered.size();
        int choices = choicesTrackPaths.size();
        selectionLabel.setText("Selected: " + selected + " | Choices: " + choices + " | Visible: " + visible + " | Total: " + total);
    }

    private void clearFilter() {
        filterTextField.clear();
        applyFilter();
    }

    private void setLoadingState(boolean loading, String message) {
        if (filterPanelBuilder != null) {
            filterPanelBuilder.setLoading(loading);
        } else if (filterTextField != null) {
            filterTextField.setDisable(loading);
        }
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
            loadingIndicator.setManaged(loading);
        }
        if (trackTable != null) {
            trackTable.setDisable(loading);
        }
        if (message != null && statusLabel != null) {
            statusLabel.setText(message);
            appendAppLog(message);
        }
    }

    private void syncShowChoicesStateFromBuilder() {
        if (filterPanelBuilder == null) {
            return;
        }
        showChoicesOnly = filterPanelBuilder.isShowChoicesOnly();
        retainedFilterText = filterPanelBuilder.getRetainedFilterText();
        if (showChoicesOnlyMenuItem != null && showChoicesOnlyMenuItem.isSelected() != showChoicesOnly) {
            showChoicesOnlyMenuItem.setSelected(showChoicesOnly);
        }
    }

    private void loadTracksAsync(Path directory) {
        setLoadingState(true, "Loading MP3 files from: " + directory + " ...");

        // Clear existing data
        allTracksUnfiltered.clear();
        choicesTrackPaths.clear();
        showChoicesOnly = false;
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
        String filterText = filterTextField == null ? "" : filterTextField.getText();

        try {
            List<TrackEntry> filtered = applyActiveFilters(allTracksUnfiltered, filterText);
            trackTable.setItems(FXCollections.observableArrayList(filtered));
            statusLabel.setText("Showing " + filtered.size() + " tracks");
            updateSelectionStatus();
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private void applyFilterInternal(List<TrackEntry> allTracks) {
        String filterText = filterTextField == null ? "" : filterTextField.getText();

        try {
            List<TrackEntry> filtered = applyActiveFilters(allTracks, filterText);
            trackTable.setItems(FXCollections.observableArrayList(filtered));
            statusLabel.setText("Showing " + filtered.size() + " tracks");
            updateSelectionStatus();
        } catch (Exception ex) {
            statusLabel.setText("Error: " + ex.getMessage());
        }
    }

    private List<TrackEntry> applyActiveFilters(List<TrackEntry> source, String filterText) {
        List<TrackEntry> textFiltered = SearchFilter.filter(source, filterText);
        if (!showChoicesOnly) {
            return textFiltered;
        }
        return textFiltered.stream()
                .filter(track -> choicesTrackPaths.contains(track.getFilePath()))
                .toList();
    }

    private ContextMenu createTableContextMenu() {
        ContextMenu contextMenu = new ContextMenu();
        String contextMenuItemStyle = "-fx-font-size: 11px; -fx-padding: 4px 12px;";

        if (adminMode) {
            MenuItem replaceArtworkItem = new MenuItem("Replace Artwork...");
            replaceArtworkItem.setStyle(contextMenuItemStyle);
            replaceArtworkItem.setOnAction(e -> replaceArtworkForSelectedTracks());

            MenuItem batchEditItem = new MenuItem("Batch Edit Metadata...");
            batchEditItem.setStyle(contextMenuItemStyle);
            batchEditItem.setOnAction(e -> openBatchEditDialog());

            contextMenu.getItems().add(replaceArtworkItem);
            contextMenu.getItems().add(batchEditItem);
            contextMenu.getItems().add(new SeparatorMenuItem());

            MenuItem choicesSelectedItem = new MenuItem("Mark choices");
            choicesSelectedItem.setStyle(contextMenuItemStyle);
            choicesSelectedItem.setOnAction(e -> setChoicesForSelected(true));

            MenuItem unchoicesSelectedItem = new MenuItem("Clear choices");
            unchoicesSelectedItem.setStyle(contextMenuItemStyle);
            unchoicesSelectedItem.setOnAction(e -> setChoicesForSelected(false));

            MenuItem copyChoicesItem = new MenuItem("Copy choices to...");
            copyChoicesItem.setStyle(contextMenuItemStyle);
            copyChoicesItem.setOnAction(e -> copyChoicesTracksToCopyDirectory());

            MenuItem clearChoicesItem = new MenuItem("Clear all choices");
            clearChoicesItem.setStyle(contextMenuItemStyle);
            clearChoicesItem.setOnAction(e -> clearChoicesTracks());

            MenuItem copyFilenameItem = new MenuItem("Copy filename(s)");
            copyFilenameItem.setStyle(contextMenuItemStyle);
            copyFilenameItem.setOnAction(e -> copyFilenameToClipboard());

            contextMenu.getItems().add(choicesSelectedItem);
            contextMenu.getItems().add(unchoicesSelectedItem);
            contextMenu.getItems().add(copyChoicesItem);
            contextMenu.getItems().add(clearChoicesItem);
            contextMenu.getItems().add(new SeparatorMenuItem());
            contextMenu.getItems().add(copyFilenameItem);
        } else {
            // User mode: only copy filename is available
            MenuItem copyFilenameItem = new MenuItem("Copy filename(s)");
            copyFilenameItem.setStyle(contextMenuItemStyle);
            copyFilenameItem.setOnAction(e -> copyFilenameToClipboard());
            contextMenu.getItems().add(copyFilenameItem);
        }

        return contextMenu;
    }

    private void setChoicesForSelected(boolean chosen) {
        ObservableList<TrackEntry> selectedTracks = trackTable.getSelectionModel().getSelectedItems();
        if (selectedTracks == null || selectedTracks.isEmpty()) {
            statusLabel.setText("No selected tracks to " + (chosen ? "mark" : "clear"));
            return;
        }

        for (TrackEntry track : selectedTracks) {
            if (chosen) {
                choicesTrackPaths.add(track.getFilePath());
            } else {
                choicesTrackPaths.remove(track.getFilePath());
            }
        }

        if (showChoicesOnly) {
            applyFilter();
        }
        trackTable.refresh();
        updateSelectionStatus();
        statusLabel.setText((chosen ? "Marked " : "Unmarked ") + selectedTracks.size() + " tracks");
    }

    private void clearChoicesTracks() {
        int count = choicesTrackPaths.size();
        choicesTrackPaths.clear();
        applyFilter();
        if (trackTable != null) {
            trackTable.refresh();
        }
        updateSelectionStatus();
        statusLabel.setText("Cleared " + count + " choices");
    }

    private void copyChoicesTracksToCopyDirectory() {
        if (choicesTrackPaths.isEmpty()) {
            statusLabel.setText("No choices to copy");
            return;
        }

        // Step 13: Use saved copy directory path instead of showing chooser
        Path copyDirectory = getLastCopyDirectory();

        // DEBUG: Log the destination being used
        System.out.println("[DEBUG] copyChoicesTracksToCopyDirectory - copyDirectory: " +
            (copyDirectory != null ? copyDirectory.toAbsolutePath() : "NULL"));

        if (copyDirectory == null || !Files.isDirectory(copyDirectory)) {
            statusLabel.setText("Error: No copy directory set. Please choose a copy directory first.");
            return;
        }

        String overwriteWarning = buildOverwriteWarning(copyDirectory, choicesTrackPaths);

        // Show directory preview for confirmation before copying
        if (!showDirectoryPreview(copyDirectory, overwriteWarning)) {
            statusLabel.setText("Copy operation cancelled");
            return;
        }

        // Perform the copy operation
        int successCount = 0;
        int failureCount = 0;

        for (Path sourcePath : choicesTrackPaths) {
            try {
                Path targetPath = copyDirectory.resolve(sourcePath.getFileName());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                successCount++;
            } catch (Exception ex) {
                failureCount++;
            }
        }

        statusLabel.setText("Copied choices: " + successCount + " succeeded, " + failureCount + " failed");

        // Step 18: Open the copy directory in Windows Explorer if copy was successful
        if (successCount > 0) {
            openDirectoryInExplorer(copyDirectory);
        }
    }

    private String buildOverwriteWarning(Path copyDirectory, Set<Path> sourcePaths) {
        try {
            File[] destinationFiles = copyDirectory.toFile().listFiles();
            if (destinationFiles == null) {
                return null;
            }

            Set<String> destinationNamesLower = ConcurrentHashMap.newKeySet();
            for (File file : destinationFiles) {
                if (file.isFile()) {
                    destinationNamesLower.add(file.getName().toLowerCase());
                }
            }

            List<String> overwriteNames = new ArrayList<>();
            for (Path sourcePath : sourcePaths) {
                String sourceName = sourcePath.getFileName().toString();
                if (destinationNamesLower.contains(sourceName.toLowerCase())) {
                    overwriteNames.add(sourceName);
                }
            }

            if (overwriteNames.isEmpty()) {
                return null;
            }

            overwriteNames.sort(String::compareToIgnoreCase);
            int shownCount = Math.min(10, overwriteNames.size());
            List<String> shown = overwriteNames.subList(0, shownCount);

            StringBuilder warning = new StringBuilder();
            warning.append("Warning: ")
                   .append(overwriteNames.size())
                   .append(" file(s) will be overwritten if you continue.\n");
            for (String name : shown) {
                warning.append("- ").append(name).append("\n");
            }
            if (overwriteNames.size() > shownCount) {
                warning.append("... and ")
                       .append(overwriteNames.size() - shownCount)
                       .append(" more");
            }
            return warning.toString().trim();
        } catch (Exception ex) {
            System.err.println("[WARN] Could not build overwrite warning: " + ex.getMessage());
            return null;
        }
    }

    private void openDirectoryInExplorer(Path directory) {
        try {
            if (System.getProperty("os.name").toLowerCase().contains("windows")) {
                // Use Windows Explorer to open the directory
                ProcessBuilder pb = new ProcessBuilder("explorer.exe", directory.toAbsolutePath().toString());
                pb.start();
                System.out.println("[DEBUG] Opened directory in Windows Explorer: " + directory.toAbsolutePath());
            } else {
                // For other operating systems, try to open with default file manager
                Desktop.getDesktop().open(directory.toFile());
                System.out.println("[DEBUG] Opened directory in default file manager: " + directory.toAbsolutePath());
            }
        } catch (Exception ex) {
            System.err.println("[ERROR] Failed to open directory: " + ex.getMessage());
            // Don't show error to user - the copy was already successful
        }
    }

    private void refreshContextMenuForRole() {
        if (trackTable != null) {
            trackTable.setContextMenu(createTableContextMenu());
        }
    }

    private void updateHelpMenuForRole() {
        if (menuBarBuilder != null) {
            menuBarBuilder.updateForAdminMode(adminMode);
            helpMenu = menuBarBuilder.getHelpMenu();
            return;
        }
        if (helpMenu != null) {
            helpMenu.getItems().clear();

            MenuItem appLogItem = new MenuItem("Show app log...");
            appLogItem.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
            appLogItem.setOnAction(e -> showAppLogDialog());
            helpMenu.getItems().add(appLogItem);

            MenuItem readmeItem = new MenuItem("README...");
            readmeItem.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
            readmeItem.setOnAction(e -> showMarkdownFile("README.md", "README"));
            helpMenu.getItems().add(readmeItem);

            MenuItem licenseItem = new MenuItem("LICENSE...");
            licenseItem.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
            licenseItem.setOnAction(e -> showMarkdownFile("LICENSE.md", "LICENSE"));
            helpMenu.getItems().add(licenseItem);

            if (adminMode) {
                // Add shortcuts item in Admin mode
                MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts...");
                shortcutsItem.setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
                shortcutsItem.setOnAction(e -> showKeyboardShortcuts());
                helpMenu.getItems().add(new SeparatorMenuItem());
                helpMenu.getItems().add(shortcutsItem);
            }
        }
    }

    private void rebuildOptionsMenu(MenuButton optionsMenu,
                                    CustomMenuItem musicDirectoryMenuItem,
                                    CustomMenuItem copyDirectoryMenuItem,
                                    SeparatorMenuItem separator1,
                                    MenuItem reloadItem,
                                    SeparatorMenuItem separator2,
                                    CheckMenuItem showChoicesOnlyItem,
                                    MenuItem copyChoicesItem,
                                    MenuItem clearChoicesItem,
                                    MenuItem chooseCopyDirectoryItem,
                                    Menu columnModeMenu,
                                    Menu roleMenu) {
        if (menuBarBuilder != null) {
            menuBarBuilder.rebuildOptionsMenu(adminMode);
            this.optionsMenu = menuBarBuilder.getOptionsMenu();
            this.userRoleItem = menuBarBuilder.getUserRoleItem();
            this.adminRoleItem = menuBarBuilder.getAdminRoleItem();
            return;
        }
        optionsMenu.getItems().clear();

        // Always visible items
        optionsMenu.getItems().addAll(
                musicDirectoryMenuItem,
                copyDirectoryMenuItem,
                separator1,
                reloadItem,
                separator2,
                columnModeMenu,
                new SeparatorMenuItem(),
                roleMenu
        );

        // Admin-only items
        if (adminMode) {
            optionsMenu.getItems().add(new SeparatorMenuItem());
            optionsMenu.getItems().add(new MenuItem("Choose music directory...") {
                {
                    setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
                    setOnAction(e -> openDirectoryChooser());
                }
            });
            optionsMenu.getItems().add(chooseCopyDirectoryItem);
            optionsMenu.getItems().add(new SeparatorMenuItem());
            optionsMenu.getItems().add(showChoicesOnlyItem);
            optionsMenu.getItems().add(copyChoicesItem);
            optionsMenu.getItems().add(clearChoicesItem);
        }
    }

    private void chooseCopyDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose copy directory");

        // Use last copy directory if available, otherwise fall back to current directory
        Path lastCopyDir = getLastCopyDirectory();
        if (lastCopyDir != null && Files.isDirectory(lastCopyDir)) {
            chooser.setInitialDirectory(lastCopyDir.toFile());
        } else if (currentDirectory != null && Files.isDirectory(currentDirectory)) {
            chooser.setInitialDirectory(currentDirectory.toFile());
        }

        File selected = chooser.showDialog(null);
        if (selected == null) {
            statusLabel.setText("Copy directory selection cancelled");
            return;
        }

        Path copyDirectory = selected.toPath();
        saveLastCopyDirectory(copyDirectory);

        // Update the copy directory label in the three-dot menu with full path
        if (menuBarBuilder != null) {
            menuBarBuilder.setCopyDirectory(copyDirectory.toAbsolutePath().toString());
        }
        if (copyDirectoryLabel != null) {
            copyDirectoryLabel.setText(copyDirectory.toAbsolutePath().toString());
        }

        statusLabel.setText("Copy directory set to: " + copyDirectory.toAbsolutePath());
    }

    private void copyFilenameToClipboard() {
        ObservableList<TrackEntry> selectedTracks = trackTable.getSelectionModel().getSelectedItems();
        if (selectedTracks == null || selectedTracks.isEmpty()) {
            statusLabel.setText("No tracks selected to copy");
            return;
        }

        String filenames = selectedTracks.stream()
                .map(TrackEntry::getFilename)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");

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

        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() {
                for (Path modifiedPath : modifiedPaths) {
                    service.invalidateCache(modifiedPath);
                    artworkBytesCache.remove(modifiedPath);
                    artworkLoadsInFlight.remove(modifiedPath);

                    TrackEntry reloadedTrack = service.loadSingleTrack(modifiedPath);
                    if (reloadedTrack != null) {
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

        List<Path> paths = selectedTracks.stream().map(track -> currentDirectory.resolve(track.getFilename())).toList();
        int updated = service.batchEditMetadata(paths, newTitle, newArtist);
        statusLabel.setText("Batch metadata edit updated " + updated + " tracks.");

        for (Path path : paths) {
            service.invalidateCache(path);
        }

        Task<Void> refreshTask = new Task<>() {
            @Override
            protected Void call() {
                for (TrackEntry track : selectedTracks) {
                    Path mp3Path = currentDirectory.resolve(track.getFilename());
                    TrackEntry reloadedTrack = service.loadSingleTrack(mp3Path);
                    if (reloadedTrack != null) {
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

    private void saveLastMusicDirectory(Path directory) {
        configManager.saveLastMusicDirectory(directory);
    }

    private void saveLastArtworkDirectory(Path directory) {
        configManager.saveLastArtworkDirectory(directory);
    }

    private Path getLastArtworkDirectory() {
        return configManager.getLastArtworkDirectory();
    }

    private void saveLastCopyDirectory(Path directory) {
        configManager.saveLastCopyDirectory(directory);
    }

    private Path getLastCopyDirectory() {
        return configManager.getLastCopyDirectory();
    }

    private void initializeLastMusicDirectory() {
        Path lastPath = configManager.getLastMusicDirectory();
        if (lastPath != null && Files.isDirectory(lastPath)) {
            currentDirectory = lastPath;
            if (menuBarBuilder != null) {
                menuBarBuilder.setMusicDirectory(lastPath.toString());
            }
            if (musicDirectoryLabel != null) {
                musicDirectoryLabel.setText(lastPath.toString());
            }
        }
    }

    private void initializeLastCopyDirectory() {
        Path lastPath = configManager.getLastCopyDirectory();
        if (lastPath != null && Files.isDirectory(lastPath)) {
            if (menuBarBuilder != null) {
                menuBarBuilder.setCopyDirectory(lastPath.toAbsolutePath().toString());
            }
            if (copyDirectoryLabel != null) {
                copyDirectoryLabel.setText(lastPath.toAbsolutePath().toString());
            }
        }
    }


    private void saveUiPreferences() {
        configManager.saveColumnMode(moreColumnsMode);
        configManager.saveAdminMode(adminMode);
    }

    private void initializeUiPreferences() {
        moreColumnsMode = configManager.getColumnMode();
        // FAIL-SAFE: Always start in User mode, regardless of saved preference
        adminMode = false;
    }


    private void showAppLogDialog() {
        DialogFactory.showAppLog(appLogBuffer.toString());
    }

    private void appendAppLog(String message) {
        if (message == null || message.isBlank()) {
            return;
        }
        appLogBuffer
            .append("[")
            .append(LocalDateTime.now().format(LOG_TS_FORMAT))
            .append("] ")
            .append(message)
            .append(System.lineSeparator());
    }

    private void showMarkdownFile(String filename, String title) {
        try {
            DialogFactory.showMarkdownFile(filename, title);
        } catch (IOException ex) {
            statusLabel.setText("Error reading " + filename + ": " + ex.getMessage());
        }
    }

    private void handleRoleChangeRequested(boolean requestAdmin) {
        if (requestAdmin == adminMode) {
            return;
        }

        if (requestAdmin) {
            if (!promptForAdminPassword()) {
                if (userRoleItem != null) {
                    userRoleItem.setSelected(true);
                }
                if (adminRoleItem != null) {
                    adminRoleItem.setSelected(false);
                }
                if (statusLabel != null) {
                    statusLabel.setText("Admin mode access denied");
                }
                return;
            }
            adminMode = true;
            if (statusLabel != null) {
                statusLabel.setText("Role switched to Admin mode");
            }
        } else {
            adminMode = false;
            if (statusLabel != null) {
                statusLabel.setText("Role switched to User mode");
            }
        }

        refreshContextMenuForRole();
        updateHelpMenuForRole();
        if (menuBarBuilder != null) {
            menuBarBuilder.rebuildOptionsMenu(adminMode);
            optionsMenu = menuBarBuilder.getOptionsMenu();
            userRoleItem = menuBarBuilder.getUserRoleItem();
            adminRoleItem = menuBarBuilder.getAdminRoleItem();
        }
        saveUiPreferences();
    }

    private boolean isCurrentTrack(TrackEntry track) {
        return track != null && playingTrackPath != null && playingTrackPath.equals(track.getFilePath());
    }

    private void onTransportClicked(TrackEntry track) {
        if (track == null) {
            return;
        }

        if (isCurrentTrack(track) && mediaPlayer != null) {
            if (isMediaPlaying()) {
                invokeMediaPlayerVoid("pause");
            } else {
                invokeMediaPlayerVoid("play");
            }
            updatePlaybackUi();
            return;
        }

        startPlayback(track);
    }

    private void startPlayback(TrackEntry track) {
        disposeMediaPlayer();
        playingTrackPath = track.getFilePath();

        try {
            String mediaSource = toJavaFxMediaUri(playingTrackPath);
            statusLabel.setText("Media URI: " + mediaSource);
            System.out.println("[SingSongArtwork] Media URI: " + mediaSource);

            mediaPlayer = createMediaPlayer(mediaSource);

            invokeMediaPlayerVoid("setOnReady", (Runnable) () -> {
                Duration total = safeToDuration(invokeMediaPlayer("getTotalDuration"));
                if (playbackBarBuilder != null) {
                    playbackBarBuilder.setMaxDuration(Math.max(total.toSeconds(), 0));
                } else {
                    playbackSlider.setMax(Math.max(total.toSeconds(), 0));
                }
                updatePlaybackUi();
            });

            Object currentTimeProperty = invokeMediaPlayer("currentTimeProperty");
            if (currentTimeProperty instanceof javafx.beans.value.ObservableValue<?> observable) {
                observable.addListener((obs, oldTime, newTime) -> {
                    Duration current = safeToDuration(newTime);
                    Duration total = safeToDuration(invokeMediaPlayer("getTotalDuration"));
                    if (playbackBarBuilder != null) {
                        playbackBarBuilder.updateSliderPosition(current.toSeconds());
                        playbackBarBuilder.setTime(formatDuration(current), formatDuration(total));
                    } else {
                        if (!scrubbingPlayback) {
                            playbackSlider.setValue(current.toSeconds());
                        }
                        playbackTimeLabel.setText(formatDuration(current) + " / " + formatDuration(total));
                    }
                });
            }

            invokeMediaPlayerVoid("setOnEndOfMedia", (Runnable) () -> {
                invokeMediaPlayerVoid("stop");
                updatePlaybackUi();
            });

            invokeMediaPlayerVoid("play");
            if (playbackBarBuilder != null) {
                playbackBarBuilder.setNowPlaying("Now Playing: " + track.getFilename());
            } else {
                nowPlayingLabel.setText("Now Playing: " + track.getFilename());
            }
            statusLabel.setText("Playing: " + track.getFilename() + " | URI: " + mediaSource);
            updatePlaybackUi();
        } catch (Exception ex) {
            statusLabel.setText("Playback error for " + track.getFilename() + ": " + ex.getMessage());
            disposeMediaPlayer();
        }
    }

    private String toJavaFxMediaUri(Path path) {
        Path absolute = path.toAbsolutePath().normalize();
        URI fileUri = absolute.toUri();
        String uriString = fileUri.toString();

        if (uriString.startsWith("file://") && !uriString.startsWith("file:///")) {
            uriString = "file:////" + uriString.substring("file://".length());
        }

        return uriString;
    }

    private boolean isMediaPlaying() {
        if (mediaPlayer == null) {
            return false;
        }
        Object status = invokeMediaPlayer("getStatus");
        return status != null && "PLAYING".equals(String.valueOf(status));
    }

    private Object createMediaPlayer(String mediaSource) {
        try {
            Class<?> mediaClass = Class.forName("javafx.scene.media.Media");
            Object media = mediaClass.getConstructor(String.class).newInstance(mediaSource);
            Class<?> mediaPlayerClass = Class.forName("javafx.scene.media.MediaPlayer");
            return mediaPlayerClass.getConstructor(mediaClass).newInstance(media);
        } catch (ClassNotFoundException ex) {
            String msg = "JavaFX media module not found. The javafx.media module is not on the module path.";
            System.err.println("[ERROR] " + msg);
            throw new RuntimeException(msg, ex);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            String msg = "JavaFX Media failed to initialize: " + ex.getCause();
            System.err.println("[ERROR] " + msg);
            throw new RuntimeException(msg, ex.getCause());
        } catch (Exception ex) {
            String msg = "JavaFX media module error: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            System.err.println("[ERROR] " + msg);
            throw new RuntimeException(msg, ex);
        }
    }

    private Object invokeMediaPlayer(String methodName, Object... args) {
        if (mediaPlayer == null) {
            return null;
        }
        try {
            java.lang.reflect.Method method = findCompatibleMethod(mediaPlayer.getClass(), methodName, args);
            if (method == null) {
                return null;
            }
            return method.invoke(mediaPlayer, args);
        } catch (Exception ex) {
            return null;
        }
    }

    private java.lang.reflect.Method findCompatibleMethod(Class<?> type, String methodName, Object[] args) {
        for (java.lang.reflect.Method method : type.getMethods()) {
            if (!method.getName().equals(methodName)) {
                continue;
            }
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != args.length) {
                continue;
            }
            boolean matches = true;
            for (int i = 0; i < paramTypes.length; i++) {
                Object arg = args[i];
                if (arg == null) {
                    continue;
                }
                if (!paramTypes[i].isAssignableFrom(arg.getClass())) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return method;
            }
        }
        return null;
    }

    private void invokeMediaPlayerVoid(String methodName, Object... args) {
        invokeMediaPlayer(methodName, args);
    }

    private Duration safeToDuration(Object value) {
        if (value instanceof Duration d) {
            return d;
        }
        return Duration.ZERO;
    }

    private void toggleGlobalPlayback() {
        if (mediaPlayer == null) {
            TrackEntry selected = trackTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                startPlayback(selected);
            } else {
                statusLabel.setText("Select a track to play.");
            }
            return;
        }

        if (isMediaPlaying()) {
            invokeMediaPlayerVoid("pause");
        } else {
            invokeMediaPlayerVoid("play");
        }
        updatePlaybackUi();
    }

    private void stopPlayback() {
        if (mediaPlayer != null) {
            invokeMediaPlayerVoid("stop");
            if (playbackBarBuilder != null) {
                playbackBarBuilder.resetSlider();
            } else {
                playbackSlider.setValue(0);
            }
            updatePlaybackUi();
        }
    }

    private void disposeMediaPlayer() {
        if (mediaPlayer != null) {
            try {
                invokeMediaPlayerVoid("stop");
                invokeMediaPlayerVoid("dispose");
            } catch (Exception ignored) {
                // Ignore dispose edge cases from platform media backends.
            }
            mediaPlayer = null;
        }
    }

    private void updatePlaybackUi() {
        if (playbackPlayPauseButton == null && playbackBarBuilder == null) {
            return;
        }

        if (mediaPlayer == null) {
            if (playbackBarBuilder != null) {
                playbackBarBuilder.setPlayingState(false);
                playbackBarBuilder.setStopEnabled(false);
                playbackBarBuilder.setTime("00:00", "00:00");
            } else {
                playbackPlayPauseButton.setText("▶");
                playbackStopButton.setDisable(true);
                if (nowPlayingLabel != null && (nowPlayingLabel.getText() == null || nowPlayingLabel.getText().isBlank())) {
                    nowPlayingLabel.setText("Now Playing: -");
                }
                if (playbackTimeLabel != null) {
                    playbackTimeLabel.setText("00:00 / 00:00");
                }
            }
        } else {
            if (playbackBarBuilder != null) {
                playbackBarBuilder.setPlayingState(isMediaPlaying());
                playbackBarBuilder.setStopEnabled(true);
            } else {
                playbackPlayPauseButton.setText(isMediaPlaying() ? "⏸" : "▶");
                playbackStopButton.setDisable(false);
            }
        }

        if (trackTable != null) {
            trackTable.refresh();
        }
    }

    private String formatDuration(Duration duration) {
        if (duration == null || duration.isUnknown() || duration.lessThanOrEqualTo(Duration.ZERO)) {
            return "00:00";
        }
        int totalSeconds = (int) Math.floor(duration.toSeconds());
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private HBox createPlaybackBar() {
        playbackBarBuilder = new PlaybackBarBuilder(
                this::toggleGlobalPlayback,
                this::stopPlayback,
                duration -> {
                    if (mediaPlayer != null) {
                        invokeMediaPlayerVoid("seek", duration);
                    }
                }
        );

        HBox bar = playbackBarBuilder.buildBar();
        playbackSlider = playbackBarBuilder.getSeekSlider();
        return bar;
    }

    private VBox createBottomPanel() {
        VBox bottom = new VBox();
        bottom.getChildren().add(createPlaybackBar());
        bottom.getChildren().add(createStatusBar());
        return bottom;
    }

    private void replaceArtworkColumn(TableView<TrackEntry> table) {
        // Find and remove the artwork column built by TableBuilder
        TableColumn<TrackEntry, ?> artworkCol = null;
        for (TableColumn<TrackEntry, ?> col : table.getColumns()) {
            if ("Artwork".equals(col.getText())) {
                artworkCol = col;
                break;
            }
        }

        if (artworkCol != null) {
            table.getColumns().remove(artworkCol);
        }

        // Create new artwork column with proper caching and lazy-loading
        artworkColumn = new TableColumn<>("Artwork");
        artworkColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        artworkColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TrackEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                // Check if track has artwork (using the hint)
                if (!item.hasArtwork()) {
                    setText("-");
                    setGraphic(null);
                    return;
                }

                // Try to get cached artwork first
                byte[] artworkBytes = artworkBytesCache.getOrDefault(item.getFilePath(), null);

                if (artworkBytes == null || artworkBytes.length == 0) {
                    // Artwork not in cache yet - display "-" and trigger lazy load
                    setText("-");
                    setGraphic(null);
                    triggerArtworkLazyLoad(item);
                    return;
                }

                // Display cached artwork
                try {
                    int thumbSize = largeArtworkMode ? ARTWORK_THUMB_LARGE : ARTWORK_THUMB_SMALL;
                    Image image = new Image(new ByteArrayInputStream(artworkBytes), thumbSize, thumbSize, true, true);
                    if (image.isError()) {
                        setText("-");
                        setGraphic(null);
                        return;
                    }
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(thumbSize);
                    imageView.setFitHeight(thumbSize);
                    imageView.setPreserveRatio(true);
                    setText(null);
                    setGraphic(imageView);
                } catch (Exception ex) {
                    setText("-");
                    setGraphic(null);
                }
            }

            {
                // Handle double-click on artwork cell at cell level
                setOnMouseClicked(event -> {
                    if (event.getClickCount() == 2 && !isEmpty()) {
                        TrackEntry rowItem = getItem();
                        if (rowItem != null) {
                            showFullArtworkForTrack(rowItem);
                            event.consume();
                        }
                    }
                });
            }
        });
        artworkColumn.setComparator((a, b) -> Boolean.compare(a.hasArtwork(), b.hasArtwork()));
        artworkColumn.setPrefWidth((largeArtworkMode ? ARTWORK_THUMB_LARGE : ARTWORK_THUMB_SMALL) + 2);
        artworkColumn.setSortable(true);
        artworkColumn.setResizable(true);

        // Insert artwork column back at position 2 (after choices and transport)
        table.getColumns().add(2, artworkColumn);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
