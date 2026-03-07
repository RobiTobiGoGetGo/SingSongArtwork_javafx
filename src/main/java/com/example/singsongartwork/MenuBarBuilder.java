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
    private MenuItem musicDirectoryMenuItem;
    private MenuItem copyDirectoryMenuItem;
    private MenuItem artworkDirectoryMenuItem;

    // Event handlers
    private final Runnable onShowAppLog;
    private final Runnable onShowReadme;
    private final Runnable onShowLicense;
    private final Runnable onShowShortcuts;
    private final Runnable onReloadMusicFiles;
    private final Runnable onChooseMusicDirectory;
    private final Runnable onChooseCopyDirectory;
    private final Runnable onChooseArtworkDirectory;
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
            Runnable onChooseArtworkDirectory,
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
        this.onChooseArtworkDirectory = onChooseArtworkDirectory;
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

        // Directory menu items at top (always visible, clickable only in Admin mode)
        musicDirectoryMenuItem = new MenuItem("Music: Not set");
        musicDirectoryMenuItem.setStyle(menuItemStyle);
        musicDirectoryMenuItem.setDisable(true); // Will be enabled in Admin mode
        musicDirectoryMenuItem.setOnAction(e -> onChooseMusicDirectory.run());
        menu.getItems().add(musicDirectoryMenuItem);

        copyDirectoryMenuItem = new MenuItem("Copy: Not set");
        copyDirectoryMenuItem.setStyle(menuItemStyle);
        copyDirectoryMenuItem.setDisable(true); // Will be enabled in Admin mode
        copyDirectoryMenuItem.setOnAction(e -> onChooseCopyDirectory.run());
        menu.getItems().add(copyDirectoryMenuItem);

        artworkDirectoryMenuItem = new MenuItem("Artwork: Not set");
        artworkDirectoryMenuItem.setStyle(menuItemStyle);
        artworkDirectoryMenuItem.setDisable(true); // Will be enabled in Admin mode
        artworkDirectoryMenuItem.setOnAction(e -> onChooseArtworkDirectory.run());
        menu.getItems().add(artworkDirectoryMenuItem);

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

        // Directory menu items at top
        musicDirectoryMenuItem = new MenuItem("Music: Not set");
        musicDirectoryMenuItem.setStyle(menuItemStyle);
        musicDirectoryMenuItem.setDisable(!isAdmin); // Enabled only in Admin mode
        musicDirectoryMenuItem.setOnAction(e -> onChooseMusicDirectory.run());
        optionsMenu.getItems().add(musicDirectoryMenuItem);

        copyDirectoryMenuItem = new MenuItem("Copy: Not set");
        copyDirectoryMenuItem.setStyle(menuItemStyle);
        copyDirectoryMenuItem.setDisable(!isAdmin); // Enabled only in Admin mode
        copyDirectoryMenuItem.setOnAction(e -> onChooseCopyDirectory.run());
        optionsMenu.getItems().add(copyDirectoryMenuItem);

        artworkDirectoryMenuItem = new MenuItem("Artwork: Not set");
        artworkDirectoryMenuItem.setStyle(menuItemStyle);
        artworkDirectoryMenuItem.setDisable(!isAdmin); // Enabled only in Admin mode
        artworkDirectoryMenuItem.setOnAction(e -> onChooseArtworkDirectory.run());
        optionsMenu.getItems().add(artworkDirectoryMenuItem);

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
        if (musicDirectoryMenuItem != null) {
            musicDirectoryMenuItem.setText("Music: " + (path == null || path.isEmpty() ? "Not set" : path));
        }
    }

    /**
     * Update copy directory label.
     */
    public void setCopyDirectory(String path) {
        if (copyDirectoryMenuItem != null) {
            copyDirectoryMenuItem.setText("Copy: " + (path == null || path.isEmpty() ? "Not set" : path));
        }
    }

    /**
     * Update artwork directory label.
     */
    public void setArtworkDirectory(String path) {
        if (artworkDirectoryMenuItem != null) {
            artworkDirectoryMenuItem.setText("Artwork: " + (path == null || path.isEmpty() ? "Not set" : path));
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
