package com.example.singsongartwork;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.util.Pair;

/**
 * Builder for the menu bar components (hamburger and three-dot menus).
 * Encapsulates all menu creation and role-based visibility logic.
 */
public class MenuBarBuilder {
    private final String topIconStyle = "-fx-background-color: transparent; -fx-text-fill: #ffffff; -fx-font-size: 12px; -fx-font-weight: normal; -fx-padding: 8px 12px; -fx-background-radius: 0; -fx-border-width: 0;";
    private final String menuItemStyle = "-fx-font-size: 11px; -fx-padding: 4px 12px;";

    private MenuButton helpMenu;
    private MenuButton optionsMenu;
    private RadioMenuItem userRoleItem;
    private RadioMenuItem adminRoleItem;
    private Menu roleMenu;
    private Label musicDirectoryLabel;
    private Label copyDirectoryLabel;

    // Event handlers
    private final Runnable onShowAppLog;
    private final Runnable onShowReadme;
    private final Runnable onShowLicense;
    private final Runnable onShowShortcuts;
    private final Runnable onReloadMusicFiles;
    private final Runnable onChooseMusicDirectory;
    private final Runnable onChooseCopyDirectory;
    private final java.util.function.Consumer<Boolean> onToggleShowChoices;
    private final Runnable onCopyChoices;
    private final Runnable onClearChoices;
    private final java.util.function.Consumer<Integer> onColumnModeChanged;
    private final java.util.function.Consumer<Boolean> onRoleChanged;

    public MenuBarBuilder(
            Runnable onShowAppLog,
            Runnable onShowReadme,
            Runnable onShowLicense,
            Runnable onShowShortcuts,
            Runnable onReloadMusicFiles,
            Runnable onChooseMusicDirectory,
            Runnable onChooseCopyDirectory,
            java.util.function.Consumer<Boolean> onToggleShowChoices,
            Runnable onCopyChoices,
            Runnable onClearChoices,
            java.util.function.Consumer<Integer> onColumnModeChanged,
            java.util.function.Consumer<Boolean> onRoleChanged) {
        this.onShowAppLog = onShowAppLog;
        this.onShowReadme = onShowReadme;
        this.onShowLicense = onShowLicense;
        this.onShowShortcuts = onShowShortcuts;
        this.onReloadMusicFiles = onReloadMusicFiles;
        this.onChooseMusicDirectory = onChooseMusicDirectory;
        this.onChooseCopyDirectory = onChooseCopyDirectory;
        this.onToggleShowChoices = onToggleShowChoices;
        this.onCopyChoices = onCopyChoices;
        this.onClearChoices = onClearChoices;
        this.onColumnModeChanged = onColumnModeChanged;
        this.onRoleChanged = onRoleChanged;
    }

    /**
     * Build both hamburger and three-dot menus.
     */
    public Pair<MenuButton, MenuButton> buildMenus() {
        helpMenu = buildHamburgerMenu();
        optionsMenu = buildThreeDotMenu();
        return new Pair<>(helpMenu, optionsMenu);
    }

    private MenuButton buildHamburgerMenu() {
        MenuButton menu = new MenuButton("☰");
        menu.setStyle(topIconStyle);
        menu.getStyleClass().add("icon-menu-button");

        MenuItem appLogItem = new MenuItem("Show app log...");
        appLogItem.setStyle(menuItemStyle);
        appLogItem.setOnAction(e -> onShowAppLog.run());
        menu.getItems().add(appLogItem);

        MenuItem readmeItem = new MenuItem("README...");
        readmeItem.setStyle(menuItemStyle);
        readmeItem.setOnAction(e -> onShowReadme.run());
        menu.getItems().add(readmeItem);

        MenuItem licenseItem = new MenuItem("LICENSE...");
        licenseItem.setStyle(menuItemStyle);
        licenseItem.setOnAction(e -> onShowLicense.run());
        menu.getItems().add(licenseItem);

        return menu;
    }

    private MenuButton buildThreeDotMenu() {
        MenuButton menu = new MenuButton("⋮");
        menu.setStyle(topIconStyle);
        menu.getStyleClass().add("icon-menu-button");

        // Music directory info
        CustomMenuItem musicDirectoryMenuItem = createMusicDirectoryItem();
        menu.getItems().add(musicDirectoryMenuItem);

        // Copy directory info
        CustomMenuItem copyDirectoryMenuItem = createCopyDirectoryItem();
        menu.getItems().add(copyDirectoryMenuItem);

        menu.getItems().add(new SeparatorMenuItem());

        MenuItem reloadItem = new MenuItem("Reload music files");
        reloadItem.setStyle(menuItemStyle);
        reloadItem.setOnAction(e -> onReloadMusicFiles.run());
        menu.getItems().add(reloadItem);

        menu.getItems().add(new SeparatorMenuItem());

        // Column Mode
        Menu columnModeMenu = createColumnModeMenu();
        menu.getItems().add(columnModeMenu);

        menu.getItems().add(new SeparatorMenuItem());

        // Role menu
        roleMenu = createRoleMenu();
        menu.getItems().add(roleMenu);

        optionsMenu = menu;
        return menu;
    }

    private CustomMenuItem createMusicDirectoryItem() {
        CustomMenuItem item = new CustomMenuItem();
        VBox info = new VBox(3);
        info.setPadding(new Insets(6, 12, 6, 12));

        Label titleLabel = new Label("Music directory:");
        titleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");

        musicDirectoryLabel = new Label("No directory selected");
        musicDirectoryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff;");
        musicDirectoryLabel.setWrapText(true);
        musicDirectoryLabel.setMaxWidth(300);

        info.getChildren().addAll(titleLabel, musicDirectoryLabel);
        item.setContent(info);
        item.setHideOnClick(false);
        return item;
    }

    private CustomMenuItem createCopyDirectoryItem() {
        CustomMenuItem item = new CustomMenuItem();
        VBox info = new VBox(3);
        info.setPadding(new Insets(6, 12, 6, 12));

        Label titleLabel = new Label("File destination:");
        titleLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #b3b3b3; -fx-font-weight: 600;");

        copyDirectoryLabel = new Label("Not set");
        copyDirectoryLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #ffffff;");
        copyDirectoryLabel.setWrapText(true);
        copyDirectoryLabel.setMaxWidth(300);

        info.getChildren().addAll(titleLabel, copyDirectoryLabel);
        item.setContent(info);
        item.setHideOnClick(false);
        return item;
    }

    private Menu createColumnModeMenu() {
        Menu menu = new Menu("Column Mode");
        menu.setStyle(menuItemStyle);

        ToggleGroup group = new ToggleGroup();

        RadioMenuItem lessItem = new RadioMenuItem("Less");
        lessItem.setStyle(menuItemStyle);
        lessItem.setToggleGroup(group);
        lessItem.setSelected(true);
        lessItem.setOnAction(e -> onColumnModeChanged.accept(0)); // 0 = Less

        RadioMenuItem moreItem = new RadioMenuItem("More");
        moreItem.setStyle(menuItemStyle);
        moreItem.setToggleGroup(group);
        moreItem.setOnAction(e -> onColumnModeChanged.accept(1)); // 1 = More

        menu.getItems().addAll(lessItem, moreItem);
        return menu;
    }

    private Menu createRoleMenu() {
        Menu menu = new Menu("Role");
        menu.setStyle(menuItemStyle);

        ToggleGroup group = new ToggleGroup();

        userRoleItem = new RadioMenuItem("User");
        userRoleItem.setStyle(menuItemStyle);
        userRoleItem.setToggleGroup(group);
        userRoleItem.setSelected(true);
        userRoleItem.setOnAction(e -> onRoleChanged.accept(false)); // false = User

        adminRoleItem = new RadioMenuItem("Admin");
        adminRoleItem.setStyle(menuItemStyle);
        adminRoleItem.setToggleGroup(group);
        adminRoleItem.setOnAction(e -> onRoleChanged.accept(true)); // true = Admin

        menu.getItems().addAll(userRoleItem, adminRoleItem);
        return menu;
    }

    /**
     * Update menus for admin mode.
     * Adds admin-only items and keyboard shortcuts to hamburger menu.
     */
    public void updateForAdminMode(boolean isAdmin) {
        if (helpMenu == null) {
            return;
        }

        helpMenu.getItems().clear();

        MenuItem appLogItem = new MenuItem("Show app log...");
        appLogItem.setStyle(menuItemStyle);
        appLogItem.setOnAction(e -> onShowAppLog.run());
        helpMenu.getItems().add(appLogItem);

        MenuItem readmeItem = new MenuItem("README...");
        readmeItem.setStyle(menuItemStyle);
        readmeItem.setOnAction(e -> onShowReadme.run());
        helpMenu.getItems().add(readmeItem);

        MenuItem licenseItem = new MenuItem("LICENSE...");
        licenseItem.setStyle(menuItemStyle);
        licenseItem.setOnAction(e -> onShowLicense.run());
        helpMenu.getItems().add(licenseItem);

        if (isAdmin) {
            helpMenu.getItems().add(new SeparatorMenuItem());

            MenuItem shortcutsItem = new MenuItem("Keyboard Shortcuts...");
            shortcutsItem.setStyle(menuItemStyle);
            shortcutsItem.setOnAction(e -> onShowShortcuts.run());
            helpMenu.getItems().add(shortcutsItem);
        }
    }

    /**
     * Rebuild options menu for current admin mode.
     */
    public void rebuildOptionsMenu(boolean isAdmin) {
        if (optionsMenu == null) {
            return;
        }

        optionsMenu.getItems().clear();

        // Music directory
        CustomMenuItem musicDirectoryMenuItem = createMusicDirectoryItem();
        optionsMenu.getItems().add(musicDirectoryMenuItem);

        // Copy directory
        CustomMenuItem copyDirectoryMenuItem = createCopyDirectoryItem();
        optionsMenu.getItems().add(copyDirectoryMenuItem);

        optionsMenu.getItems().add(new SeparatorMenuItem());

        MenuItem reloadItem = new MenuItem("Reload music files");
        reloadItem.setStyle(menuItemStyle);
        reloadItem.setOnAction(e -> onReloadMusicFiles.run());
        optionsMenu.getItems().add(reloadItem);

        optionsMenu.getItems().add(new SeparatorMenuItem());

        Menu columnModeMenu = createColumnModeMenu();
        optionsMenu.getItems().add(columnModeMenu);

        optionsMenu.getItems().add(new SeparatorMenuItem());

        roleMenu = createRoleMenu();
        optionsMenu.getItems().add(roleMenu);

        // Admin-only items
        if (isAdmin) {
            optionsMenu.getItems().add(new SeparatorMenuItem());

            MenuItem chooseMusicDirItem = new MenuItem("Choose music directory...");
            chooseMusicDirItem.setStyle(menuItemStyle);
            chooseMusicDirItem.setOnAction(e -> onChooseMusicDirectory.run());
            optionsMenu.getItems().add(chooseMusicDirItem);

            MenuItem chooseCopyDirItem = new MenuItem("Choose file destination...");
            chooseCopyDirItem.setStyle(menuItemStyle);
            chooseCopyDirItem.setOnAction(e -> onChooseCopyDirectory.run());
            optionsMenu.getItems().add(chooseCopyDirItem);

            optionsMenu.getItems().add(new SeparatorMenuItem());

            CheckMenuItem showChoicesItem = new CheckMenuItem("Show choices");
            showChoicesItem.setStyle(menuItemStyle);
            showChoicesItem.setOnAction(e -> onToggleShowChoices.accept(showChoicesItem.isSelected()));
            optionsMenu.getItems().add(showChoicesItem);

            MenuItem copyChoicesItem = new MenuItem("Copy choices to...");
            copyChoicesItem.setStyle(menuItemStyle);
            copyChoicesItem.setOnAction(e -> onCopyChoices.run());
            optionsMenu.getItems().add(copyChoicesItem);

            MenuItem clearChoicesItem = new MenuItem("Clear choices");
            clearChoicesItem.setStyle(menuItemStyle);
            clearChoicesItem.setOnAction(e -> onClearChoices.run());
            optionsMenu.getItems().add(clearChoicesItem);
        }
    }

    /**
     * Update music directory label.
     */
    public void setMusicDirectory(String path) {
        if (musicDirectoryLabel != null) {
            musicDirectoryLabel.setText(path);
        }
    }

    /**
     * Update copy directory label.
     */
    public void setCopyDirectory(String path) {
        if (copyDirectoryLabel != null) {
            copyDirectoryLabel.setText(path);
        }
    }

    /**
     * Get hamburger menu.
     */
    public MenuButton getHelpMenu() {
        return helpMenu;
    }

    /**
     * Get three-dot menu.
     */
    public MenuButton getOptionsMenu() {
        return optionsMenu;
    }

    /**
     * Get user role menu item.
     */
    public RadioMenuItem getUserRoleItem() {
        return userRoleItem;
    }

    /**
     * Get admin role menu item.
     */
    public RadioMenuItem getAdminRoleItem() {
        return adminRoleItem;
    }
}
