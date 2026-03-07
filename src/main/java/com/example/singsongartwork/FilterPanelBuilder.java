package com.example.singsongartwork;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Set;

/**
 * Builder for the filter and search control panel.
 * Encapsulates filter UI creation and interaction logic.
 */
public class FilterPanelBuilder {
    private TextField filterTextField;
    private Button showChoicesToggleBtn;
    private ComboBox<String> filterComboBox;
    private ProgressIndicator loadingIndicator;
    private boolean showChoicesOnly = false;
    private String retainedFilterText = "";

    private final Set<String> defaultTerms;
    private final java.util.function.Consumer<String> onFilterChanged;
    private final Runnable onToggleShowChoices;
    private final Runnable onCopyChoices;
    private Button largeArtworkToggleBtn;
    private boolean largeArtworkMode = false;

    private final java.util.function.Consumer<Boolean> onToggleLargeArtwork;

    public FilterPanelBuilder(
            Set<String> defaultTerms,
            java.util.function.Consumer<String> onFilterChanged,
            Runnable onToggleShowChoices,
            Runnable onCopyChoices,
            java.util.function.Consumer<Boolean> onToggleLargeArtwork) {
        this.defaultTerms = defaultTerms;
        this.onFilterChanged = onFilterChanged;
        this.onToggleShowChoices = onToggleShowChoices;
        this.onCopyChoices = onCopyChoices;
        this.onToggleLargeArtwork = onToggleLargeArtwork;
    }

    /**
     * Build and return the complete filter panel.
     */
    public VBox buildPanel() {
        VBox vbox = new VBox(12);
        vbox.setPadding(new Insets(16));
        vbox.getStyleClass().add("top-panel");

        HBox filterBox = new HBox(12);
        filterBox.setAlignment(Pos.CENTER_LEFT);
        filterBox.getStyleClass().add("filter-box");

        Label filterLabel = new Label("Filter:");

        // Create ComboBox with default filter terms
        filterComboBox = new ComboBox<>();
        filterComboBox.setEditable(true);
        filterComboBox.setPrefWidth(400);
        filterComboBox.setPrefHeight(34);
        filterComboBox.setStyle("-fx-font-size: 13px;");
        HBox.setHgrow(filterComboBox, Priority.ALWAYS);
        filterComboBox.setPromptText("Search tracks or select a default term...");

        if (!defaultTerms.isEmpty()) {
            filterComboBox.setItems(FXCollections.observableArrayList(
                    defaultTerms.stream().sorted().toList()
            ));
        }

        filterTextField = filterComboBox.getEditor();
        filterTextField.setOnKeyReleased(e -> onFilterChanged.accept(filterTextField.getText()));

        // Trigger filter when user selects from dropdown
        filterComboBox.setOnAction(e -> {
            String selectedValue = filterComboBox.getValue();
            if (selectedValue != null && !selectedValue.isBlank()) {
                filterTextField.setText(selectedValue);
                onFilterChanged.accept(selectedValue);
            }
        });

        String barButtonBaseStyle = "-fx-font-size: 13px; -fx-padding: 6px 10px; -fx-min-height: 34px; -fx-pref-height: 34px;";
        String barIconButtonStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0; -fx-min-width: 34px; -fx-pref-width: 34px; -fx-min-height: 34px; -fx-pref-height: 34px;";

        Button clearFilterBtn = new Button("Clear");
        clearFilterBtn.setStyle(barButtonBaseStyle);
        clearFilterBtn.setOnAction(e -> clearFilter());

        // Toggle for "Show choices" mode
        showChoicesToggleBtn = new Button("☑");
        showChoicesToggleBtn.setStyle(barIconButtonStyle + " -fx-opacity: 0.6;");
        showChoicesToggleBtn.setTooltip(new Tooltip("Toggle show choices only"));
        showChoicesToggleBtn.setOnAction(e -> toggleShowChoices());

        // Copy choices button
        Button copyChoicesBtn = new Button("💿");
        copyChoicesBtn.setStyle(barButtonBaseStyle);
        copyChoicesBtn.setTooltip(new Tooltip("Copy choices to copy directory"));
        copyChoicesBtn.setOnAction(e -> onCopyChoices.run());

        // Toggle for larger artwork icons
        largeArtworkToggleBtn = new Button("Art+");
        largeArtworkToggleBtn.setStyle(barButtonBaseStyle + " -fx-opacity: 0.75;");
        largeArtworkToggleBtn.setTooltip(new Tooltip("Toggle larger artwork icons"));
        largeArtworkToggleBtn.setOnAction(e -> toggleLargeArtworkMode());

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setVisible(false);
        loadingIndicator.setManaged(false);
        loadingIndicator.setPrefSize(24, 24);

        Region rightSpacer = new Region();
        HBox.setHgrow(rightSpacer, Priority.ALWAYS);

        filterBox.getChildren().addAll(
                showChoicesToggleBtn,
                filterLabel,
                filterComboBox,
                clearFilterBtn,
                loadingIndicator,
                largeArtworkToggleBtn,
                rightSpacer,
                copyChoicesBtn
        );

        vbox.getChildren().add(filterBox);
        return vbox;
    }

    /**
     * Get current filter text.
     */
    public String getFilterText() {
        return filterTextField != null ? filterTextField.getText() : "";
    }

    /**
     * Set filter text.
     */
    public void setFilterText(String text) {
        if (filterTextField != null) {
            filterTextField.setText(text);
        }
    }

    /**
     * Clear the filter.
     */
    public void clearFilter() {
        if (filterComboBox != null) {
            filterComboBox.setValue(null);
        }
        if (filterTextField != null) {
            filterTextField.clear();
        }
        onFilterChanged.accept("");
    }

    /**
     * Get whether "show choices only" is active.
     */
    public boolean isShowChoicesOnly() {
        return showChoicesOnly;
    }

    /**
     * Toggle the "show choices only" state.
     */
    public void toggleShowChoices() {
        showChoicesOnly = !showChoicesOnly;

        if (showChoicesOnly) {
            // Save filter and disable it
            retainedFilterText = filterTextField.getText();
            filterTextField.setText("");
            filterTextField.setDisable(true);
        } else {
            // Restore filter
            filterTextField.setDisable(false);
            filterTextField.setText(retainedFilterText);
        }

        updateShowChoicesButton();
        onToggleShowChoices.run();
    }

    /**
     * Update the show choices button appearance.
     */
    private void updateShowChoicesButton() {
        String barIconButtonStyle = "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 0; -fx-min-width: 34px; -fx-pref-width: 34px; -fx-min-height: 34px; -fx-pref-height: 34px;";
        if (showChoicesToggleBtn != null) {
            showChoicesToggleBtn.setStyle(showChoicesOnly
                    ? barIconButtonStyle + " -fx-opacity: 1.0;"
                    : barIconButtonStyle + " -fx-opacity: 0.6;");
        }
    }

    /**
     * Expose the underlying filter editor to preserve legacy keyboard focus wiring.
     */
    public TextField getFilterTextField() {
        return filterTextField;
    }

    /**
     * Set loading state for the embedded progress indicator.
     */
    public void setLoading(boolean loading) {
        if (loadingIndicator != null) {
            loadingIndicator.setVisible(loading);
            loadingIndicator.setManaged(loading);
        }
        setFilterDisabled(loading);
    }

    /**
     * Synchronize show-choices state from external controls (menu/shortcuts).
     */
    public void setShowChoicesOnly(boolean desired) {
        if (showChoicesOnly != desired) {
            toggleShowChoices();
        }
    }

    /**
     * Return the current show-choices retained filter value.
     */
    public String getRetainedFilterText() {
        return retainedFilterText;
    }

    /**
     * Set retained filter text.
     */
    public void setRetainedFilterText(String text) {
        this.retainedFilterText = text;
    }

    /**
     * Disable the filter text field during loading.
     */
    public void setFilterDisabled(boolean disabled) {
        if (filterTextField != null) {
            filterTextField.setDisable(disabled);
        }
    }

    private void toggleLargeArtworkMode() {
        largeArtworkMode = !largeArtworkMode;
        updateLargeArtworkButton();
        onToggleLargeArtwork.accept(largeArtworkMode);
    }

    private void updateLargeArtworkButton() {
        String barButtonBaseStyle = "-fx-font-size: 13px; -fx-padding: 6px 10px; -fx-min-height: 34px; -fx-pref-height: 34px;";
        if (largeArtworkToggleBtn != null) {
            largeArtworkToggleBtn.setStyle(largeArtworkMode
                    ? barButtonBaseStyle + " -fx-opacity: 1.0;"
                    : barButtonBaseStyle + " -fx-opacity: 0.75;");
        }
    }

    public boolean isLargeArtworkMode() {
        return largeArtworkMode;
    }

    public void setLargeArtworkMode(boolean desired) {
        if (largeArtworkMode != desired) {
            largeArtworkMode = desired;
            updateLargeArtworkButton();
            onToggleLargeArtwork.accept(largeArtworkMode);
        }
    }
}
