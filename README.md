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
- **Copy Limits**: Default copy limits are **700 MB** total size and **31 files**, with support for no-limit settings
- **Directory Safety Rules**: Music, copy, and artwork directories must all be different from each other

### Advanced Features
- **Persistent Settings**: Remembers last used directories, column mode, copy limits, and UI preferences
- **Lazy Artwork Loading**: Efficient on-demand loading of large artwork files
- **Default Filter Terms**: Pre-configured search terms loaded from `defaultFilterTerms.txt`
- **Keyboard Shortcuts**: Comprehensive keyboard navigation (Admin mode)
- **Safer Copy Workflow**:
  - In **User mode**, copying is only allowed to an **empty** copy directory
  - In **Admin mode**, copying into a non-empty copy directory is allowed after preview/confirmation
  - Copying is blocked if the selection exceeds the configured file-count or total-size limits
- **Artwork Interactions**:
  - Double-click **filename** cell to copy that filename
  - Double-click **artwork** cell to preview full artwork (if available)
  - In **Admin mode**, missing artwork actions remain available from contextual UI actions

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
2. The app always starts in **User mode** for safety
3. Click the **three-dot menu (⋮)** → **Choose music directory...**
4. Select your MP3 folder and confirm the preview
5. If you later set copy and artwork directories, they must be different from the music directory and from each other

## Usage Guide

### Basic Operations

**Loading Music Files**
- Three-dot menu → **Choose music directory...** → Select folder
- App can reuse the last selected source directory
- The selected music directory must be different from the configured copy and artwork directories

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
- The app always returns to **User mode** on the next start (fail-safe)

**Replace Artwork** (Admin only)
- Select one or more tracks
- Right-click → **Replace artwork** → Choose image file
- Or: Drag and drop an image onto selected tracks
- The configured artwork directory must be different from the music and copy directories

**Batch Edit Metadata** (Admin only)
- Select multiple tracks
- Right-click → **Batch Edit Metadata...**
- Enter new title/artist (leave blank to keep existing)

**Artwork Preview / Missing Artwork Search**
- Double-click artwork cell to open full-size preview (if artwork exists)
- Search and replace actions for missing artwork are available from the contextual artwork UI in Admin mode
- Search terms are built from filename (without `.mp3`) + artist

**Filename Copy**
- Double-click a **filename cell** to copy only that row's filename
- `Ctrl+C` or context menu copies selected filename(s)

**Mark and Copy Tracks**
- Check boxes in **Choices** column to mark tracks
- Use the copy action to send marked tracks to the configured copy directory
- The configured copy directory must be different from the music and artwork directories
- In **User mode**, the copy directory must also be empty before copying can begin
- In **Admin mode**, a non-empty copy directory is allowed, and overwrite warnings are shown before copying

**Copy Limits**
- Default limits are **700 MB** total size and **31 files**
- The lower applicable limit always wins
- Limits can be changed by an Admin, including a **No limit** option for either setting
- Copying is blocked when a choice set exceeds the configured limits

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
- Copy limits (max total MB and max file count)
- UI column mode (Less/More)
- UI role preference (always starts in User mode for safety)

### Directory Guardrails
- **Music**, **Copy**, and **Artwork** directories are mutually exclusive
- If you try to set one directory to the same path as another, the app blocks the change and explains why
- Copying is also re-validated at copy time, so previously saved invalid combinations are still blocked

### Default Filter Terms
Edit `src/main/resources/defaultFilterTerms.txt` to customize pre-loaded filter options.

## Development

### Run Tests
```powershell
mvn test
```

### Full Clean Test Run
```powershell
mvn clean test
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

- **JavaFX 21.0.3**: UI framework (`javafx-controls`, `javafx.fxml`, `javafx-media`)
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

### Directory Selection Is Rejected
- The **music**, **copy**, and **artwork** directories must all be different
- If two directory roles point to the same folder, pick a separate folder for each role
- If a previously saved copy directory is now invalid, choose a new one before copying

### Copy Is Blocked
- In **User mode**, the copy directory must be empty
- The copy directory must be different from the music and artwork directories
- The selected choices must stay within the configured copy size/count limits

### Playback Issues
- Ensure MP3 files are valid and not corrupted
- Check console for JavaFX Media error messages
- UNC network paths may require special URI encoding (handled automatically)

### Artwork Not Displaying
- Artwork loads lazily on-demand (first render may show `-` briefly)
- Check file permissions on MP3 files
- Some MP3s may not have embedded artwork (shows `-`)

### Admin Mode Not Working
- Password is: `pwd`
- Role always resets to User mode on restart (fail-safe)

## License

See [LICENSE.md](LICENSE.md) for details.

## Version History

- **v0.40.3** - Enforced mutually exclusive music, copy, and artwork directories
- **v0.40.2** - Blocked User-mode copy into non-empty destination directories
- **v0.40.1** - Enforced copy choice limits and updated selection behavior
