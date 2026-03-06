package com.example.singsongartwork;

import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Builder for the track table component.
 * Encapsulates table construction, column setup, and cell factories.
 */
public class TrackTableBuilder {
    private TableColumn<TrackEntry, TrackEntry> artworkColumn;
    private TableColumn<TrackEntry, String> filenameColumn;
    private TableColumn<TrackEntry, String> titleColumn;
    private TableColumn<TrackEntry, String> artistColumn;
    private TableColumn<TrackEntry, TrackEntry> transportColumn;
    private TableColumn<TrackEntry, Boolean> choicesColumn;
    private TableView<TrackEntry> table;

    private boolean moreColumnsMode = false;
    private final java.util.function.Supplier<byte[]> artworkLoader;
    private final java.util.function.Consumer<TrackEntry> onPlayClicked;
    private final java.util.function.Consumer<Set<Path>> onChoicesChanged;
    private final java.util.function.Supplier<Boolean> isPlayingSupplier;
    private final Set<Path> choicesTrackPaths;

    public TrackTableBuilder(
            java.util.function.Supplier<byte[]> artworkLoader,
            java.util.function.Consumer<TrackEntry> onPlayClicked,
            java.util.function.Consumer<Set<Path>> onChoicesChanged,
            java.util.function.Supplier<Boolean> isPlayingSupplier,
            Set<Path> choicesTrackPaths) {
        this.artworkLoader = artworkLoader;
        this.onPlayClicked = onPlayClicked;
        this.onChoicesChanged = onChoicesChanged;
        this.isPlayingSupplier = isPlayingSupplier;
        this.choicesTrackPaths = choicesTrackPaths;
    }

    /**
     * Build and return the complete track table.
     */
    public TableView<TrackEntry> buildTable() {
        table = new TableView<>();
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);

        // Create columns
        createArtworkColumn();
        createFilenameColumn();
        createTitleColumn();
        createArtistColumn();
        createTransportColumn();
        createChoicesColumn();

        // Add columns in required order
        table.getColumns().add(choicesColumn);
        table.getColumns().add(transportColumn);
        table.getColumns().add(artworkColumn);
        table.getColumns().add(filenameColumn);
        table.getColumns().add(titleColumn);
        table.getColumns().add(artistColumn);

        applyColumnMode();

        // Space key handler to toggle choices
        table.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.SPACE) {
                event.consume();
                ObservableList<TrackEntry> selectedItems = table.getSelectionModel().getSelectedItems();
                if (selectedItems != null && !selectedItems.isEmpty()) {
                    for (TrackEntry track : new ArrayList<>(selectedItems)) {
                        boolean isCurrentlyChosen = choicesTrackPaths.contains(track.getFilePath());
                        if (isCurrentlyChosen) {
                            choicesTrackPaths.remove(track.getFilePath());
                        } else {
                            choicesTrackPaths.add(track.getFilePath());
                        }
                    }
                    table.refresh();
                    onChoicesChanged.accept(choicesTrackPaths);
                }
            }
        });

        return table;
    }

    private void createArtworkColumn() {
        artworkColumn = new TableColumn<>("Artwork");
        artworkColumn.setCellValueFactory(cellData -> new ReadOnlyObjectWrapper<>(cellData.getValue()));
        artworkColumn.setCellFactory(col -> new TableCell<>() {
            @Override
            protected void updateItem(TrackEntry item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || !item.hasArtwork()) {
                    setText("-");
                    setGraphic(null);
                    return;
                }

                byte[] artworkBytes = artworkLoader.get();
                if (artworkBytes.length == 0) {
                    setText("-");
                    setGraphic(null);
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
    }

    private void createFilenameColumn() {
        filenameColumn = new TableColumn<>("Filename");
        filenameColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getFilename()));
        filenameColumn.setPrefWidth(320);
        filenameColumn.setSortable(true);
        filenameColumn.setResizable(true);
    }

    private void createTitleColumn() {
        titleColumn = new TableColumn<>("Title");
        titleColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getTitle()));
        titleColumn.setPrefWidth(260);
        titleColumn.setSortable(true);
        titleColumn.setResizable(true);
    }

    private void createArtistColumn() {
        artistColumn = new TableColumn<>("Artist");
        artistColumn.setCellValueFactory(cellData -> new javafx.beans.property.SimpleStringProperty(cellData.getValue().getArtist()));
        artistColumn.setPrefWidth(240);
        artistColumn.setSortable(true);
        artistColumn.setResizable(true);
    }

    private void createTransportColumn() {
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

                Button button = new Button("▶");
                button.setFocusTraversable(false);
                button.setStyle("-fx-padding: 4px 8px; -fx-font-size: 12px;");
                button.setOnAction(e -> onPlayClicked.accept(item));
                setText(null);
                setGraphic(button);
            }
        });
        transportColumn.setPrefWidth(72);
        transportColumn.setSortable(false);
        transportColumn.setResizable(false);
    }

    private void createChoicesColumn() {
        choicesColumn = new TableColumn<>("Choices");
        choicesColumn.setCellValueFactory(cellData ->
                new ReadOnlyObjectWrapper<>(choicesTrackPaths.contains(cellData.getValue().getFilePath())));
        choicesColumn.setCellFactory(col -> new TableCell<>() {
            private final CheckBox checkBox = new CheckBox();

            {
                checkBox.setOnAction(e -> {
                    TrackEntry track = getTableRow() == null ? null : getTableRow().getItem();
                    if (track == null) {
                        return;
                    }
                    if (checkBox.isSelected()) {
                        choicesTrackPaths.add(track.getFilePath());
                    } else {
                        choicesTrackPaths.remove(track.getFilePath());
                    }
                    onChoicesChanged.accept(choicesTrackPaths);
                    if (table != null) {
                        table.refresh();
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
        choicesColumn.setComparator((a, b) -> Boolean.compare(b, a));
        choicesColumn.setPrefWidth(70);
        choicesColumn.setSortable(true);
        choicesColumn.setResizable(false);
    }

    /**
     * Update column visibility based on mode.
     */
    public void updateColumnMode(boolean showMore) {
        moreColumnsMode = showMore;
        applyColumnMode();
    }

    private void applyColumnMode() {
        if (titleColumn == null || artistColumn == null) {
            return;
        }
        titleColumn.setVisible(moreColumnsMode);
        artistColumn.setVisible(moreColumnsMode);
    }

    /**
     * Refresh the table display.
     */
    public void refresh() {
        if (table != null) {
            table.refresh();
        }
    }

    /**
     * Set context menu on table.
     */
    public void setContextMenu(ContextMenu menu) {
        if (table != null) {
            table.setContextMenu(menu);
        }
    }

    /**
     * Get the table instance.
     */
    public TableView<TrackEntry> getTable() {
        return table;
    }

    /**
     * Get artwork column.
     */
    public TableColumn<TrackEntry, TrackEntry> getArtworkColumn() {
        return artworkColumn;
    }

    /**
     * Get filename column.
     */
    public TableColumn<TrackEntry, String> getFilenameColumn() {
        return filenameColumn;
    }

    /**
     * Get title column.
     */
    public TableColumn<TrackEntry, String> getTitleColumn() {
        return titleColumn;
    }

    /**
     * Get artist column.
     */
    public TableColumn<TrackEntry, String> getArtistColumn() {
        return artistColumn;
    }

    /**
     * Get choices column.
     */
    public TableColumn<TrackEntry, Boolean> getChoicesColumn() {
        return choicesColumn;
    }

    /**
     * Get current column mode state.
     */
    public boolean isMoreColumnsMode() {
        return moreColumnsMode;
    }

    /**
     * Add a selection change listener (useful for UI state sync).
     */
    public void addSelectionListener(javafx.collections.ListChangeListener<TrackEntry> listener) {
        if (table != null) {
            table.getSelectionModel().getSelectedItems().addListener(listener);
        }
    }

    /**
     * Add an items change listener (useful for tracking data updates).
     */
    public void addItemsListener(javafx.beans.value.ChangeListener<ObservableList<TrackEntry>> listener) {
        if (table != null) {
            table.itemsProperty().addListener(listener);
        }
    }
}
