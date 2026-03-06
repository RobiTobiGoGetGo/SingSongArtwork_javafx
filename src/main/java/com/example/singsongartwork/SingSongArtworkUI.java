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
import java.net.URI;
import java.net.URISyntaxException;
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

public class SingSongArtworkUI extends Application {
    private Mp3MetadataService service;
    private TableView<TrackEntry> trackTable;
    private TextField filterTextField;
    private Label statusLabel;
    private Label selectionLabel;
    private Label dirLabel;
    private Label destLabel;
    private ProgressIndicator loadingIndicator;
    private Path currentDirectory;
    private List<TrackEntry> allTracksUnfiltered = new ArrayList<>();
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
    private static final String KEY_LAST_DIRECTORY = "last.directory";
    private static final String KEY_LAST_ARTWORK_DIRECTORY = "last.artwork.directory";
    private static final String KEY_LAST_COPY_DESTINATION = "last.copy.destination";
    private static final String KEY_UI_COLUMN_MODE = "ui.column.mode";
    private static final String KEY_UI_ROLE = "ui.role";

    // Phase 3: Artwork cache and in-flight tracking for lazy loading
    private final Map<Path, byte[]> artworkBytesCache = new ConcurrentHashMap<>();
    private final Set<Path> artworkLoadsInFlight = ConcurrentHashMap.newKeySet();
    private static final PseudoClass PLAYING_ROW_PSEUDO_CLASS = PseudoClass.getPseudoClass("playing");

    // Choices feature
    private TableColumn<TrackEntry, Boolean> choicesColumn;
    private final Set<Path> choicesTrackPaths = ConcurrentHashMap.newKeySet();
    private boolean showChoicesOnly = false;
    private String retainedFilterText = ""; // Retained when "Show choices" is active

    @Override
    public void start(Stage primaryStage) {
        service = new Mp3MetadataService();
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

        // Hamburger menu (☰)
        String topIconStyle = "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: normal; -fx-padding: 8px 12px; -fx-background-radius: 0; -fx-border-width: 0;";
        String menuItemStyle = "-fx-font-size: 11px; -fx-padding: 4px 12px;";

        MenuButton helpMenu = new MenuButton("☰");
        helpMenu.setStyle(topIconStyle);
        helpMenu.getStyleClass().add("icon-menu-button");
        MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts...");
        shortcutsItem.setStyle(menuItemStyle);
        shortcutsItem.setOnAction(e -> showKeyboardShortcuts());
        // Only show shortcuts in Admin mode
        if (adminMode) {
            helpMenu.getItems().add(shortcutsItem);
        }

        // Spacer to push three-dot menu to the right
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Three-dot menu (⋮) on far right
        MenuButton optionsMenu = new MenuButton("⋮");
        optionsMenu.setStyle(topIconStyle);
        optionsMenu.getStyleClass().add("icon-menu-button");

        // File source directory info as menu label (non-clickable)
        CustomMenuItem sourceMenuItem = new CustomMenuItem();
        VBox sourceInfo = new VBox(3);
        sourceInfo.setPadding(new Insets(6, 12, 6, 12));
        Label sourceTitleLabel = new Label("File source:");
        sourceTitleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");
        dirLabel = new Label("No directory selected");
        dirLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff;");
        dirLabel.setWrapText(true);
        dirLabel.setMaxWidth(300);
        sourceInfo.getChildren().addAll(sourceTitleLabel, dirLabel);
        sourceMenuItem.setContent(sourceInfo);
        sourceMenuItem.setHideOnClick(false);

        // File destination directory info as menu label (non-clickable)
        CustomMenuItem destMenuItem = new CustomMenuItem();
        VBox destInfo = new VBox(3);
        destInfo.setPadding(new Insets(6, 12, 6, 12));
        Label destTitleLabel = new Label("File destination:");
        destTitleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");
        destLabel = new Label("Not set");
        destLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff;");
        destLabel.setWrapText(true);
        destLabel.setMaxWidth(300);
        destInfo.getChildren().addAll(destTitleLabel, destLabel);
        destMenuItem.setContent(destInfo);
        destMenuItem.setHideOnClick(false);

        SeparatorMenuItem separator1 = new SeparatorMenuItem();

        MenuItem browseItem = new MenuItem("Choose file source...");
        browseItem.setStyle(menuItemStyle);
        browseItem.setOnAction(e -> openDirectoryChooser());

        MenuItem reloadItem = new MenuItem("Reload files");
        reloadItem.setStyle(menuItemStyle);
        reloadItem.setOnAction(e -> {
            if (currentDirectory != null) {
                // Show directory preview, then load
                if (showDirectoryPreview(currentDirectory)) {
                    loadTracksAsync(currentDirectory);
                }
            } else {
                statusLabel.setText("Error: No file source set. Please choose a file source first.");
            }
        });

        SeparatorMenuItem separator2 = new SeparatorMenuItem();

        CheckMenuItem showChoicesOnlyItem = new CheckMenuItem("Show choices");
        showChoicesOnlyItem.setStyle(menuItemStyle);
        showChoicesOnlyItem.setSelected(showChoicesOnly);
        showChoicesOnlyItem.setOnAction(e -> {
            showChoicesOnly = showChoicesOnlyItem.isSelected();
            if (showChoicesOnly) {
                // Save current filter text and disable filter
                retainedFilterText = filterTextField.getText();
                filterTextField.setText("");
                filterTextField.setDisable(true);
            } else {
                // Re-enable filter and restore previous filter text
                filterTextField.setDisable(false);
                filterTextField.setText(retainedFilterText);
            }
            applyFilter();
        });

        MenuItem copyChoicesItem = new MenuItem("Copy choices to...");
        copyChoicesItem.setStyle(menuItemStyle);
        copyChoicesItem.setOnAction(e -> copyChoicesTracksToDirectory());

        MenuItem clearChoicesItem = new MenuItem("Clear choices");
        clearChoicesItem.setStyle(menuItemStyle);
        clearChoicesItem.setOnAction(e -> clearChoicesTracks());

        SeparatorMenuItem separator3 = new SeparatorMenuItem();

        // Column Mode toggle (default: Less)
        Menu columnModeMenu = new Menu("Column Mode");
        columnModeMenu.setStyle(menuItemStyle);
        ToggleGroup columnModeGroup = new ToggleGroup();
        RadioMenuItem lessColumnsItem = new RadioMenuItem("Less");
        lessColumnsItem.setStyle(menuItemStyle);
        lessColumnsItem.setToggleGroup(columnModeGroup);
        lessColumnsItem.setSelected(!moreColumnsMode);
        lessColumnsItem.setOnAction(e -> {
            moreColumnsMode = false;
            applyColumnMode();
            saveUiPreferences();
        });
        RadioMenuItem moreColumnsItem = new RadioMenuItem("More");
        moreColumnsItem.setStyle(menuItemStyle);
        moreColumnsItem.setToggleGroup(columnModeGroup);
        moreColumnsItem.setSelected(moreColumnsMode);
        moreColumnsItem.setOnAction(e -> {
            moreColumnsMode = true;
            applyColumnMode();
            saveUiPreferences();
        });
        columnModeMenu.getItems().addAll(lessColumnsItem, moreColumnsItem);

        // Admin-only menu item - declare before role menu
        MenuItem chooseDestinationItem = new MenuItem("Choose file destination...");
        chooseDestinationItem.setStyle(menuItemStyle);
        chooseDestinationItem.setOnAction(e -> chooseFileDestination());

        // Role toggle (default: User) - currently no behavioral effect.
        Menu roleMenu = new Menu("Role");
        roleMenu.setStyle(menuItemStyle);
        ToggleGroup roleGroup = new ToggleGroup();
        RadioMenuItem userRoleItem = new RadioMenuItem("User");
        userRoleItem.setStyle(menuItemStyle);
        userRoleItem.setToggleGroup(roleGroup);
        userRoleItem.setSelected(!adminMode);
        userRoleItem.setOnAction(e -> {
            adminMode = false;
            refreshContextMenuForRole();
            // Rebuild options menu based on new admin mode
            rebuildOptionsMenu(optionsMenu, sourceMenuItem, destMenuItem, separator1, reloadItem, separator2, showChoicesOnlyItem, copyChoicesItem, clearChoicesItem, chooseDestinationItem, columnModeMenu, roleMenu);
            saveUiPreferences();
            if (statusLabel != null) {
                statusLabel.setText("Role switched to User mode");
            }
        });
        RadioMenuItem adminRoleItem = new RadioMenuItem("Admin");
        adminRoleItem.setStyle(menuItemStyle);
        adminRoleItem.setToggleGroup(roleGroup);
        adminRoleItem.setSelected(adminMode);
        adminRoleItem.setOnAction(e -> {
            adminMode = true;
            refreshContextMenuForRole();
            // Rebuild options menu based on new admin mode
            rebuildOptionsMenu(optionsMenu, sourceMenuItem, destMenuItem, separator1, reloadItem, separator2, showChoicesOnlyItem, copyChoicesItem, clearChoicesItem, chooseDestinationItem, columnModeMenu, roleMenu);
            saveUiPreferences();
            if (statusLabel != null) {
                statusLabel.setText("Role switched to Admin mode");
            }
        });
        roleMenu.getItems().addAll(userRoleItem, adminRoleItem);


        // Build initial optionsMenu
        rebuildOptionsMenu(optionsMenu, sourceMenuItem, destMenuItem, separator1, reloadItem, separator2, showChoicesOnlyItem, copyChoicesItem, clearChoicesItem, chooseDestinationItem, columnModeMenu, roleMenu);

        titleBar.getChildren().addAll(titleLabel, helpMenu, spacer, optionsMenu);

        // Top section: title bar + controls
        VBox topSection = new VBox();
        topSection.getChildren().add(titleBar);
        VBox topPanel = createTopPanel();
        topSection.getChildren().add(topPanel);
        root.setTop(topSection);

        // Center: table
        trackTable = createTrackTable();
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

        // Initialize dirLabel with the last used directory path (but don't load it)
        initializeLastDirectoryPath();

        // Initialize destLabel with the last used destination path (but don't load it)
        initializeLastDestinationPath();

        // Properly terminate the application when the window is closed
        primaryStage.setOnCloseRequest(e -> {
            e.consume();
            disposeMediaPlayer();
            System.exit(0);
        });

        primaryStage.show();

        // Step 12: If file source is set, automatically show preview and load files
        // If not set, user must manually choose via "Choose file source" menu
        if (currentDirectory != null && Files.isDirectory(currentDirectory)) {
            Platform.runLater(() -> {
                if (showDirectoryPreview(currentDirectory)) {
                    loadTracksAsync(currentDirectory);
                }
            });
        }
    }


    private void showKeyboardShortcuts() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Keyboard Shortcuts");
        alert.setHeaderText("SingSongArtwork - Keyboard Shortcuts");

        String shortcuts = """
                File Operations:
                  Ctrl+O              Open/Browse Directory
                  Ctrl+R              Reload Current Directory
                
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

    private VBox createTopPanel() {
        VBox vbox = new VBox(12);
        vbox.setPadding(new Insets(16));
        vbox.getStyleClass().add("top-panel");

        // Filter row with loading indicator
        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("filter-box");

        Label filterLabel = new Label("Filter:");

        // Create ComboBox with default filter terms
        ComboBox<String> filterComboBox = new ComboBox<>();
        filterComboBox.setEditable(true);
        filterComboBox.setPrefWidth(400);
        HBox.setHgrow(filterComboBox, Priority.ALWAYS);
        filterComboBox.setPromptText("Search tracks or select a default term...");

        // Load default terms into ComboBox
        Set<String> defaultTerms = SearchFilter.getDefaultFilterTerms();
        if (!defaultTerms.isEmpty()) {
            filterComboBox.setItems(FXCollections.observableArrayList(defaultTerms.stream().sorted().toList()));
        }

        // testing if this lands in changes
        // Get reference to the editor (TextBox inside ComboBox)
        filterTextField = filterComboBox.getEditor();
        filterTextField.setOnKeyReleased(e -> applyFilter());

        // Trigger filter when user selects a term from dropdown
        filterComboBox.setOnAction(e -> {
            String selectedValue = filterComboBox.getValue();
            if (selectedValue != null && !selectedValue.isBlank()) {
                // Make sure the editor text is set to the selected value
                filterTextField.setText(selectedValue);
                applyFilter();
            }
        });

        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.setOnAction(e -> {
            filterComboBox.setValue(null);
            filterTextField.clear();
            clearFilter();
        });

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        loadingIndicator.setPrefSize(24, 24);

        filterBox.getChildren().addAll(filterLabel, filterComboBox, clearFilterBtn, loadingIndicator);

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
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.M), () -> setChoicesForSelected(true));
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.M, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), () -> {
            showChoicesOnly = !showChoicesOnly;
            applyFilter();
            statusLabel.setText(showChoicesOnly ? "Showing choices only" : "Showing all tracks");
        });
        scene.getAccelerators().put(new KeyCodeCombination(KeyCode.U, KeyCombination.CONTROL_DOWN, KeyCombination.SHIFT_DOWN), this::clearChoicesTracks);
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
            // Verify directory exists and is accessible
            if (directory == null || !Files.isDirectory(directory)) {
                statusLabel.setText("Error: Invalid directory path");
                return false;
            }

            // Get both MP3 and other files in one pass to avoid multiple stream issues
            List<String> mp3Files = new ArrayList<>();
            List<String> otherFiles = new ArrayList<>();

            try (var stream = Files.list(directory)) {
                stream.filter(p -> !Files.isDirectory(p))
                      .forEach(p -> {
                          String fileName = p.getFileName().toString();
                          String lowerName = fileName.toLowerCase();
                          if (lowerName.endsWith(".mp3")) {
                              mp3Files.add(fileName);
                          } else {
                              otherFiles.add(fileName);
                          }
                      });
            }

            // Sort and limit
            mp3Files.sort(String::compareTo);
            mp3Files = mp3Files.stream().limit(10).toList();

            otherFiles.sort(String::compareTo);
            otherFiles = otherFiles.stream().limit(10).toList();

            // Show preview dialog for both empty and non-empty directories (consistent fail-safe approach)
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

            content.getChildren().add(dirPathLabel);

            if (mp3Files.isEmpty()) {
                // Step 15: Check for other file types when no MP3 files are found
                String emptyMessageText = "There are currently no MP3 files in this directory";
                if (!otherFiles.isEmpty()) {
                    emptyMessageText += ", but there are other types of files there";
                }

                Label emptyMessage = new Label(emptyMessageText);
                emptyMessage.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
                emptyMessage.setWrapText(true);
                content.getChildren().add(emptyMessage);

                // Show other file types if they exist
                if (!otherFiles.isEmpty()) {
                    Label otherFilesLabel = new Label("Other files found (" + otherFiles.size() + " shown):");
                    otherFilesLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");
                    content.getChildren().add(otherFilesLabel);

                    TextArea otherFilesList = new TextArea(String.join("\n", otherFiles));
                    otherFilesList.setEditable(false);
                    otherFilesList.setWrapText(false);
                    otherFilesList.setPrefRowCount(8);
                    otherFilesList.setPrefColumnCount(60);
                    otherFilesList.setStyle("-fx-font-family: 'Consolas', 'Monaco', 'Courier New', monospace; -fx-font-size: 12px;");
                    content.getChildren().add(otherFilesList);
                }
            } else {
                // Info label with count
                Label info = new Label("📁 MP3 files found (" + mp3Files.size() + " shown):");
                info.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
                content.getChildren().add(info);

                // File list with better styling
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

            // Set preferred size for dialog
            previewDialog.getDialogPane().setPrefWidth(700);

            return previewDialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
        } catch (Exception ex) {
            statusLabel.setText("Error reading directory: " + ex.getMessage());
            ex.printStackTrace();
            return false;
        }
    }

    private TableView<TrackEntry> createTrackTable() {
        TableView<TrackEntry> table = new TableView<>();

        // Enable multi-select
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        filenameColumn = new TableColumn<>("Filename");
        filenameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilename()));
        filenameColumn.setPrefWidth(320);
        filenameColumn.setSortable(true);
        filenameColumn.setResizable(true);

        titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setPrefWidth(260);
        titleColumn.setSortable(true);
        titleColumn.setResizable(true);

        artistColumn = new TableColumn<>("Artist");
        artistColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArtist()));
        artistColumn.setPrefWidth(240);
        artistColumn.setSortable(true);
        artistColumn.setResizable(true);

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

                if (!item.hasArtwork()) {
                    setText("-");
                    setGraphic(null);
                    return;
                }

                byte[] artworkBytes = getArtworkBytesForItem(item);
                if (artworkBytes.length == 0) {
                    setText("-");
                    setGraphic(null);
                    triggerArtworkLazyLoad(item);
                    return;
                }

                try {
                    Image image = new Image(new ByteArrayInputStream(artworkBytes), 48, 48, true, true);
                    if (image.isError()) {
                        setText("-");
                        setGraphic(null);
                        return;
                    }
                    ImageView imageView = new ImageView(image);
                    imageView.setFitWidth(48);
                    imageView.setFitHeight(48);
                    imageView.setPreserveRatio(true);
                    setText(null);
                    setGraphic(imageView);
                } catch (Exception ex) {
                    setText("-");
                    setGraphic(null);
                }
            }
        });
        artworkColumn.setComparator((a, b) -> Boolean.compare(a.hasArtwork(), b.hasArtwork()));
        artworkColumn.setPrefWidth(120);
        artworkColumn.setSortable(true);
        artworkColumn.setResizable(true);

        transportColumn = new TableColumn<>("Play");
        transportColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        transportColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TrackEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                    return;
                }

                String symbol = "▶";
                if (isCurrentTrack(item) && isMediaPlaying()) {
                    symbol = "⏸";
                }

                Button button = new Button(symbol);
                button.setFocusTraversable(false);
                button.setStyle("-fx-padding: 4px 8px; -fx-font-size: 12px;");
                button.setOnAction(e -> onTransportClicked(item));
                setText(null);
                setGraphic(button);
            }
        });
        transportColumn.setPrefWidth(72);
        transportColumn.setSortable(false);
        transportColumn.setResizable(false);

        choicesColumn = new TableColumn<>("Choices");
        choicesColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(choicesTrackPaths.contains(cellData.getValue().getFilePath())));
        choicesColumn.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    TrackEntry track = getTableRow() == null ? null : (TrackEntry) getTableRow().getItem();
                    if (track == null) {
                        return;
                    }
                    if (checkBox.isSelected()) {
                        choicesTrackPaths.add(track.getFilePath());
                    } else {
                        choicesTrackPaths.remove(track.getFilePath());
                    }
                    if (showChoicesOnly) {
                        applyFilter();
                    }
                    updateSelectionStatus();
                    if (trackTable != null) {
                        trackTable.refresh();
                    }
                });
            }

            @Override
            protected void updateItem(Boolean marked, boolean empty) {
                super.updateItem(marked, empty);
                if (empty || getTableRow() == null || getTableRow().getItem() == null) {
                    setGraphic(null);
                    return;
                }
                checkBox.setSelected(Boolean.TRUE.equals(marked));
                setGraphic(checkBox);
            }
        });
        choicesColumn.setComparator((a, b) -> Boolean.compare(b, a)); // Sort choices (true) before non-choices (false)
        choicesColumn.setPrefWidth(70);
        choicesColumn.setSortable(true);
        choicesColumn.setResizable(false);

        // Required order: choices, transport, artwork, filename, then optional metadata columns.
        table.getColumns().add(choicesColumn);
        table.getColumns().add(transportColumn);
        table.getColumns().add(artworkColumn);
        table.getColumns().add(filenameColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(artistColumn);

        applyColumnMode();

        // Add right-click context menu
        ContextMenu contextMenu = createTableContextMenu();
        table.setContextMenu(contextMenu);

        // Keep bottom status bar in sync with selection/data changes.
        table.getSelectionModel().getSelectedItems().addListener((ListChangeListener<TrackEntry>) change -> updateSelectionStatus());
        table.itemsProperty().addListener((obs, oldItems, newItems) -> updateSelectionStatus());

        // Enable drag-and-drop artwork replacement.
        configureArtworkDragAndDrop(table);

        table.setRowFactory(tv -> new TableRow<>() {
            @Override
            protected void updateItem(TrackEntry item, boolean empty) {
                super.updateItem(item, empty);
                boolean isPlayingRow = !empty && item != null && isCurrentTrack(item);
                pseudoClassStateChanged(PLAYING_ROW_PSEUDO_CLASS, isPlayingRow);
            }
        });

        return table;
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
            // Diagnostic output: keep URI visible in UI and console for troubleshooting path issues.
            statusLabel.setText("Media URI: " + mediaSource);
            System.out.println("[SingSongArtwork] Media URI: " + mediaSource);

            mediaPlayer = createMediaPlayer(mediaSource);

            invokeMediaPlayerVoid("setOnReady", (Runnable) () -> {
                Duration total = safeToDuration(invokeMediaPlayer("getTotalDuration"));
                playbackSlider.setMax(Math.max(total.toSeconds(), 0));
                updatePlaybackUi();
            });

            Object currentTimeProperty = invokeMediaPlayer("currentTimeProperty");
            if (currentTimeProperty instanceof javafx.beans.value.ObservableValue<?> observable) {
                observable.addListener((obs, oldTime, newTime) -> {
                    Duration current = safeToDuration(newTime);
                    Duration total = safeToDuration(invokeMediaPlayer("getTotalDuration"));
                    if (!scrubbingPlayback) {
                        playbackSlider.setValue(current.toSeconds());
                    }
                    playbackTimeLabel.setText(formatDuration(current) + " / " + formatDuration(total));
                });
            }

            invokeMediaPlayerVoid("setOnEndOfMedia", (Runnable) () -> {
                invokeMediaPlayerVoid("stop");
                updatePlaybackUi();
            });

            invokeMediaPlayerVoid("play");
            nowPlayingLabel.setText("Now Playing: " + track.getFilename());
            statusLabel.setText("Playing: " + track.getFilename() + " | URI: " + mediaSource);
            updatePlaybackUi();
        } catch (Exception ex) {
            statusLabel.setText("Playback error for " + track.getFilename() + ": " + ex.getMessage());
            disposeMediaPlayer();
        }
    }

    private String toJavaFxMediaUri(Path path) {
        Path absolute = path.toAbsolutePath().normalize();

        // Use Path.toUri() which properly encodes special characters (spaces, umlauts, etc.)
        URI fileUri = absolute.toUri();
        String uriString = fileUri.toString();

        // JavaFX Media rejects URIs with authority (file://host/share/...)
        // UNC paths from Windows come as file://host/share/...
        // We need to convert to file:////host/share/... (no authority)
        if (uriString.startsWith("file://") && !uriString.startsWith("file:///")) {
            // Has authority: file://host/...
            // Remove "file://" and prepend "file:////" to make it authority-free
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
            ex.printStackTrace();
            throw new RuntimeException(msg, ex);
        } catch (java.lang.reflect.InvocationTargetException ex) {
            String msg = "JavaFX Media failed to initialize: " + ex.getCause();
            System.err.println("[ERROR] " + msg);
            ex.getCause().printStackTrace();
            throw new RuntimeException(msg, ex.getCause());
        } catch (Exception ex) {
            String msg = "JavaFX media module error: " + ex.getClass().getSimpleName() + ": " + ex.getMessage();
            System.err.println("[ERROR] " + msg);
            ex.printStackTrace();
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
            playbackSlider.setValue(0);
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
        if (playbackPlayPauseButton == null) {
            return;
        }

        if (mediaPlayer == null) {
            playbackPlayPauseButton.setText("▶");
            playbackStopButton.setDisable(true);
            if (nowPlayingLabel != null && (nowPlayingLabel.getText() == null || nowPlayingLabel.getText().isBlank())) {
                nowPlayingLabel.setText("Now Playing: -");
            }
            if (playbackTimeLabel != null) {
                playbackTimeLabel.setText("00:00 / 00:00");
            }
        } else {
            playbackPlayPauseButton.setText(isMediaPlaying() ? "⏸" : "▶");
            playbackStopButton.setDisable(false);
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
        nowPlayingLabel = new Label("Now Playing: -");
        playbackTimeLabel = new Label("00:00 / 00:00");

        playbackPlayPauseButton = new Button("▶");
        playbackPlayPauseButton.setOnAction(e -> toggleGlobalPlayback());

        playbackStopButton = new Button("■");
        playbackStopButton.setOnAction(e -> stopPlayback());
        playbackStopButton.setDisable(true);

        playbackSlider = new Slider(0, 0, 0);
        playbackSlider.setPrefWidth(320);
        playbackSlider.setOnMousePressed(e -> scrubbingPlayback = true);
        playbackSlider.setOnMouseReleased(e -> {
            if (mediaPlayer != null) {
                invokeMediaPlayerVoid("seek", Duration.seconds(playbackSlider.getValue()));
            }
            scrubbingPlayback = false;
        });

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox playbackBar = new HBox(10);
        playbackBar.getStyleClass().add("status-bar");
        playbackBar.setPadding(new Insets(8, 14, 8, 14));
        playbackBar.setAlignment(Pos.CENTER_LEFT);
        playbackBar.getChildren().addAll(nowPlayingLabel, spacer, playbackSlider, playbackTimeLabel, playbackPlayPauseButton, playbackStopButton);
        return playbackBar;
    }

    private VBox createBottomPanel() {
        VBox bottom = new VBox();
        bottom.getChildren().add(createPlaybackBar());
        bottom.getChildren().add(createStatusBar());
        return bottom;
    }

    private void applyColumnMode() {
        if (filenameColumn == null || titleColumn == null || artistColumn == null || artworkColumn == null) {
            return;
        }
        boolean showMore = moreColumnsMode;
        titleColumn.setVisible(showMore);
        artistColumn.setVisible(showMore);
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
        int choices = choicesTrackPaths.size();
        selectionLabel.setText("Selected: " + selected + " | Choices: " + choices + " | Visible: " + visible + " | Total: " + total);
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
            copyChoicesItem.setOnAction(e -> copyChoicesTracksToDirectory());

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

    private void copyChoicesTracksToDirectory() {
        if (choicesTrackPaths.isEmpty()) {
            statusLabel.setText("No choices to copy");
            return;
        }

        // Step 13: Use saved file destination path instead of showing chooser
        Path destinationDir = getLastCopyDestination();
        if (destinationDir == null || !Files.isDirectory(destinationDir)) {
            statusLabel.setText("Error: No file destination set. Please choose a file destination first.");
            return;
        }

        // Show directory preview for confirmation before copying
        if (!showDirectoryPreview(destinationDir)) {
            statusLabel.setText("Copy operation cancelled");
            return;
        }

        // Perform the copy operation
        int successCount = 0;
        int failureCount = 0;

        for (Path sourcePath : choicesTrackPaths) {
            try {
                Path targetPath = destinationDir.resolve(sourcePath.getFileName());
                Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
                successCount++;
            } catch (Exception ex) {
                failureCount++;
            }
        }


        statusLabel.setText("Copied choices: " + successCount + " succeeded, " + failureCount + " failed");
    }

    private void refreshContextMenuForRole() {
        if (trackTable != null) {
            trackTable.setContextMenu(createTableContextMenu());
        }
    }

    private void rebuildOptionsMenu(MenuButton optionsMenu,
                                    CustomMenuItem sourceMenuItem,
                                    CustomMenuItem destMenuItem,
                                    SeparatorMenuItem separator1,
                                    MenuItem reloadItem,
                                    SeparatorMenuItem separator2,
                                    CheckMenuItem showChoicesOnlyItem,
                                    MenuItem copyChoicesItem,
                                    MenuItem clearChoicesItem,
                                    MenuItem chooseDestinationItem,
                                    Menu columnModeMenu,
                                    Menu roleMenu) {
        optionsMenu.getItems().clear();

        // Always visible items
        optionsMenu.getItems().addAll(
                sourceMenuItem,
                destMenuItem,
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
            optionsMenu.getItems().add(new MenuItem("Choose file source...") {
                {
                    setStyle("-fx-font-size: 11px; -fx-padding: 4px 12px;");
                    setOnAction(e -> openDirectoryChooser());
                }
            });
            optionsMenu.getItems().add(new SeparatorMenuItem());
            optionsMenu.getItems().add(showChoicesOnlyItem);
            optionsMenu.getItems().add(copyChoicesItem);
            optionsMenu.getItems().add(clearChoicesItem);
            optionsMenu.getItems().add(new SeparatorMenuItem());
            optionsMenu.getItems().add(chooseDestinationItem);
        }
    }

    private void chooseFileDestination() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Choose File Destination Directory");

        // Use last copy destination if available, otherwise fall back to current directory
        Path lastCopyDest = getLastCopyDestination();
        if (lastCopyDest != null && Files.isDirectory(lastCopyDest)) {
            chooser.setInitialDirectory(lastCopyDest.toFile());
        } else if (currentDirectory != null && Files.isDirectory(currentDirectory)) {
            chooser.setInitialDirectory(currentDirectory.toFile());
        }

        File selected = chooser.showDialog(null);
        if (selected == null) {
            statusLabel.setText("Destination selection cancelled");
            return;
        }

        Path destinationDir = selected.toPath();
        saveLastCopyDestination(destinationDir);

        // Update the destination label in the three-dot menu with full path
        if (destLabel != null) {
            destLabel.setText(destinationDir.toAbsolutePath().toString());
        }

        statusLabel.setText("File destination set to: " + destinationDir.toAbsolutePath().toString());
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

    private void saveLastDirectory(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = loadConfigProperties();
            props.setProperty(KEY_LAST_DIRECTORY, directory.toString());

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
            Properties props = loadConfigProperties();

            if (directory != null) {
                props.setProperty(KEY_LAST_ARTWORK_DIRECTORY, directory.toString());
            }

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
                Properties props = loadConfigProperties();
                String lastArtworkDir = props.getProperty(KEY_LAST_ARTWORK_DIRECTORY);
                if (lastArtworkDir != null && !lastArtworkDir.isBlank()) {
                    Path lastPath = Paths.get(lastArtworkDir);
                    if (Files.isDirectory(lastPath)) {
                        return lastPath;
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
        return null;
    }

    private void saveLastCopyDestination(Path directory) {
        try {
            ensureConfigDirectory();
            Properties props = loadConfigProperties();

            if (directory != null) {
                props.setProperty(KEY_LAST_COPY_DESTINATION, directory.toString());
            }

            try (var out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "SingSongArtwork Configuration");
            }
        } catch (IOException ex) {
            statusLabel.setText("Warning: Could not save copy destination preference: " + ex.getMessage());
        }
    }

    private Path getLastCopyDestination() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties props = loadConfigProperties();
                String lastCopyDest = props.getProperty(KEY_LAST_COPY_DESTINATION);
                if (lastCopyDest != null && !lastCopyDest.isBlank()) {
                    Path lastPath = Paths.get(lastCopyDest);
                    if (Files.isDirectory(lastPath)) {
                        return lastPath;
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
        return null;
    }

    private void initializeLastDirectoryPath() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties props = loadConfigProperties();
                String lastDir = props.getProperty(KEY_LAST_DIRECTORY);
                if (lastDir != null && !lastDir.isBlank()) {
                    Path lastPath = Paths.get(lastDir);
                    if (Files.isDirectory(lastPath)) {
                        currentDirectory = lastPath;
                        dirLabel.setText(lastPath.toString());
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
    }

    private void initializeLastDestinationPath() {
        try {
            if (Files.exists(CONFIG_FILE)) {
                Properties props = loadConfigProperties();
                String lastDest = props.getProperty(KEY_LAST_COPY_DESTINATION);
                if (lastDest != null && !lastDest.isBlank()) {
                    Path lastPath = Paths.get(lastDest);
                    if (Files.isDirectory(lastPath)) {
                        if (destLabel != null) {
                            destLabel.setText(lastPath.toAbsolutePath().toString());
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // Silently ignore
        }
    }

    private void ensureConfigDirectory() throws IOException {
        Path configDir = CONFIG_FILE.getParent();
        if (!Files.exists(configDir)) {
            Files.createDirectories(configDir);
        }
    }

    private void saveUiPreferences() {
        try {
            ensureConfigDirectory();
            Properties props = loadConfigProperties();
            props.setProperty(KEY_UI_COLUMN_MODE, moreColumnsMode ? "more" : "less");
            props.setProperty(KEY_UI_ROLE, adminMode ? "admin" : "user");
            try (var out = Files.newOutputStream(CONFIG_FILE)) {
                props.store(out, "SingSongArtwork Configuration");
            }
        } catch (IOException ex) {
            if (statusLabel != null) {
                statusLabel.setText("Warning: Could not save UI preferences: " + ex.getMessage());
            }
        }
    }

    private void initializeUiPreferences() {
        Properties props = loadConfigProperties();
        String columnMode = props.getProperty(KEY_UI_COLUMN_MODE, "less").trim().toLowerCase();
        moreColumnsMode = "more".equals(columnMode);

        // FAIL-SAFE: Always start in User mode, regardless of saved preference
        adminMode = false;
    }

    private Properties loadConfigProperties() {
        Properties props = new Properties();
        try {
            if (Files.exists(CONFIG_FILE)) {
                try (var in = Files.newInputStream(CONFIG_FILE)) {
                    props.load(in);
                }
            }
        } catch (IOException ex) {
            // Silently ignore
        }
        return props;
    }

    public static void main(String[] args) {
        launch(args);
    }
}
