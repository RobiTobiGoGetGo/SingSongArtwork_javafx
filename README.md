# SingSongArtwork

A modern JavaFX application for managing MP3 metadata and artwork with an intuitive dark-themed UI. Features include MP3 file browsing, sortable/filterable track lists, artwork management, audio playback, and batch operations.

## Features

### Core Functionality
- **MP3 Metadata Loading**: Automatically scans and loads MP3 metadata (filename, title, artist, artwork)
- **Sortable Table**: Click column headers to sort by any field (filename, title, artist, artwork presence)
- **Real-time Filtering**: Multi-word search across filename, title, and artist (AND logic)
- **Artwork Management**: Add/replace artwork via drag-and-drop or file picker
- **Audio Playback**: Built-in MP3 player with playback controls and progress slider
- **Batch Operations**: Batch edit metadata for multiple tracks at once

### User Interface
- **Modern Dark Theme**: Professional dark UI with cyan accents
- **Column Modes**: Toggle between "Less" (artwork + filename) and "More" (all columns)
- **Artwork Size Toggle**: `Art+` button in filter bar toggles small/large artwork thumbnails
- **User/Admin Roles**: Password-protected Admin mode unlocks advanced features
- **Choice Selection**: Mark tracks for bulk operations (copy, batch edit)
- **Directory Preview**: Preview MP3 files before loading
- **Application Log**: Built-in runtime log viewer for troubleshooting

### Advanced Features
- **Persistent Settings**: Remembers last used directories, column mode, and UI preferences
- **Lazy Artwork Loading**: Efficient on-demand loading of large artwork files
- **Default Filter Terms**: Pre-configured search terms loaded from `defaultFilterTerms.txt`
- **Keyboard Shortcuts**: Comprehensive keyboard navigation (Admin mode)
- **File Destination**: Set separate destination directory for copying chosen tracks
- **Artwork Interactions**:
  - Double-click **filename** cell to copy that filename
  - Double-click **artwork** cell to preview full artwork (if available)
  - In **Admin mode**, double-click missing artwork (`-`) to offer YouTube Music search (with confirmation)

## Requirements

- **Java 17** or higher
- **Maven 3.6+**
- **JavaFX 21.0.3** (managed by Maven)
- **Windows** (primary platform; scripts included for easy launching)

## Quick Start

### 1. Build the Project
```powershell
mvn clean install
```

### 2. Run the Application

**Recommended: Use the Batch Script**
```powershell
.\start-gui.bat
```

**Alternative: Maven Command**
```powershell
mvn javafx:run
```

**Alternative: PowerShell Script**
```powershell
.\start-gui.ps1
```

### 3. First-Time Setup
1. Launch the application
2. Click the **three-dot menu (⋮)** → **Choose music directory...**
3. Select your MP3 folder
4. The app will preview and load your MP3 files

## Usage Guide

### Basic Operations

**Loading Music Files**
- Three-dot menu → **Choose music directory...** → Select folder
- App can reuse the last selected source directory

**Filtering Tracks**
- Type in the filter box to search across filename, title, and artist
- All words must match (AND logic)
- Select from dropdown for default filter terms

**Sorting Tracks**
- Click any column header to sort
- Click again to reverse sort order

**Playing Music**
- Click the **▶** button in any row to play that track
- Use playback controls at the bottom: Play/Pause, Stop, Seek slider

### Advanced Operations

**Admin Mode** (Password: `pwd`)
- Three-dot menu → **Role** → **Admin**
- Unlocks: Replace Artwork, Batch Edit, Keyboard Shortcuts, advanced operations

**Replace Artwork** (Admin only)
- Select one or more tracks
- Right-click → **Replace Artwork...** → Choose image file
- Or: Drag and drop an image onto selected tracks

**Batch Edit Metadata** (Admin only)
- Select multiple tracks
- Right-click → **Batch Edit Metadata...**
- Enter new title/artist (leave blank to keep existing)

**Artwork Preview / Missing Artwork Search**
- Double-click artwork cell to open full-size preview (if artwork exists)
- In Admin mode, double-click missing artwork (`-`) to open a confirmation dialog for YouTube Music search
- Search terms are built from filename (without `.mp3`) + artist

**Filename Copy**
- Double-click a **filename cell** to copy only that row's filename
- `Ctrl+C` or context menu copies selected filename(s)

**Mark and Copy Tracks**
- Check boxes in **Choices** column to mark tracks
- Click **💿** button to copy marked tracks to destination directory
- Or: Three-dot menu → **Copy choices to...**

**Keyboard Shortcuts** (Admin only)
- Hamburger menu (☰) → **Keyboard Shortcuts...** for full list
- Quick: `Ctrl+O` (Open), `Ctrl+F` (Filter), `M` (Mark), `Space` (Toggle choice)

**Application Log**
- Hamburger menu (☰) → **Show app log...**
- View timestamped runtime events and status messages

## Configuration

### Persisted Settings
Configuration is saved to `~/.singsongartwork/config.properties`:
- Last used music directory
- Last used artwork directory
- Last used copy destination
- UI column mode (Less/More)
- UI role preference (always starts in User mode for safety)

### Default Filter Terms
Edit `src/main/resources/defaultFilterTerms.txt` to customize pre-loaded filter options.

## Development

### Run Tests
```powershell
mvn test
```

### Build Without Tests
```powershell
mvn clean install -DskipTests
```

### Generate Test Reports
```powershell
mvn surefire-report:report
```
Reports are generated in `target/surefire-reports/`.

### IntelliJ IDEA Configuration
If IntelliJ Run is not configured for JavaFX modules, prefer scripts or Maven run config.

## Project Structure

```text
SingSongArtwork/
├── src/main/java/com/example/singsongartwork/
│   ├── SingSongArtworkUI.java         # Main JavaFX GUI application
│   ├── SingSongArtworkApp.java        # Legacy CLI application
│   ├── Mp3MetadataService.java        # MP3 metadata reading/writing
│   ├── TrackEntry.java                # Data model for MP3 tracks
│   ├── SearchFilter.java              # Multi-word search logic
│   ├── SortField.java                 # Sorting field enum
│   ├── ConfigurationManager.java      # Config persistence abstraction
│   ├── DialogFactory.java             # Centralized dialog creation
│   ├── FilterPanelBuilder.java        # Filter/top-bar UI builder
│   ├── TrackTableBuilder.java         # Table/column builder
│   ├── MenuBarBuilder.java            # Help/options menu builder
│   └── PlaybackBarBuilder.java        # Playback controls builder
├── src/main/resources/
│   ├── defaultFilterTerms.txt         # Pre-configured filter terms
│   └── styles/modern-dark.css         # Dark theme stylesheet
├── src/test/java/                     # Unit/integration tests
├── pom.xml                            # Maven project configuration
├── start-gui.bat                      # Windows launcher (recommended)
├── start-gui.ps1                      # PowerShell launcher
└── README.md                          # This file
```

## Dependencies

- **JavaFX 21.0.3**: UI framework (`javafx-controls`, `javafx-fxml`, `javafx-media`)
- **JAudioTagger 3.0.1**: MP3 metadata reading/writing
- **JUnit Jupiter 5.11.3**: Testing framework
- **Maven Compiler Plugin 3.13.0**: Java 17 compilation
- **Maven Surefire Plugin 3.5.1**: Test execution
- **JavaFX Maven Plugin 0.0.8**: JavaFX app execution

## Troubleshooting

### Application Won't Start
- Ensure Java 17+ is installed: `java -version`
- Rebuild: `mvn clean install`
- Use the provided scripts instead of IDE Run button when needed

### Playback Issues
- Ensure MP3 files are valid and not corrupted
- Check console for JavaFX Media error messages
- UNC network paths may require special URI encoding (handled automatically)

### Artwork Not Displaying
- Artwork loads lazily on-demand (first render may show `-` briefly)
- Check file permissions on MP3 files
- Some MP3s may not have embedded artwork (shows `-`)

### Missing Artwork Search Not Triggering
- Feature is **Admin mode only**
- Double-click directly on the artwork cell showing `-`
- Confirm dialog must be accepted before browser opens

### Admin Mode Not Working
- Password is: `pwd`
- Role always resets to User mode on restart (fail-safe)

## License

See [LICENSE.md](LICENSE.md) for details.

## Version History

- **v1.0** - Initial release with modern UI, playback, and advanced features
