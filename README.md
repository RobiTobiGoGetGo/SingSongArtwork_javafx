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

## Requirements

- **Java 17** or higher
- **Maven 3.6+**
- **JavaFX 17.0.2** (automatically managed by Maven)
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
- Or: Automatically loads last-used directory on startup

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
- Unlocks: Replace Artwork, Batch Edit, Keyboard Shortcuts, Advanced menus

**Replace Artwork** (Admin only)
- Select one or more tracks
- Right-click → **Replace Artwork...** → Choose image file
- Or: Drag and drop an image onto selected tracks

**Batch Edit Metadata** (Admin only)
- Select multiple tracks
- Right-click → **Batch Edit Metadata...**
- Enter new title/artist (leave blank to keep existing)

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
Reports are generated in `target/surefire-reports/`

### IntelliJ IDEA Configuration
⚠️ **Important**: IntelliJ's Run button doesn't properly configure JavaFX modules.

**Solution**: Use the provided scripts (`start-gui.bat`) or create a Maven run configuration:
1. Run → Edit Configurations... → `+` → Maven
2. Name: `SingSongArtwork [javafx:run]`
3. Command line: `javafx:run`
4. Click OK and Run

## Project Structure

```
SingSongArtwork/
├── src/main/java/com/example/singsongartwork/
│   ├── SingSongArtworkUI.java         # Main JavaFX GUI application
│   ├── SingSongArtworkApp.java        # Legacy CLI application
│   ├── Mp3MetadataService.java        # MP3 metadata reading/writing
│   ├── TrackEntry.java                # Data model for MP3 tracks
│   ├── SearchFilter.java              # Multi-word search logic
│   └── SortField.java                 # Sorting field enum
├── src/main/resources/
│   ├── defaultFilterTerms.txt         # Pre-configured filter terms
│   └── styles/modern-dark.css         # Dark theme stylesheet
├── src/test/java/                     # Comprehensive unit tests
├── pom.xml                            # Maven project configuration
├── start-gui.bat                      # Windows launcher (recommended)
├── start-gui.ps1                      # PowerShell launcher
└── README.md                          # This file
```

## Dependencies

- **JavaFX 17.0.2**: UI framework (controls, media)
- **JAudioTagger 3.0.1**: MP3 metadata reading/writing
- **JUnit 5.10.0**: Testing framework
- **Maven Compiler Plugin**: Java 17 compilation
- **JavaFX Maven Plugin**: JavaFX application execution

## Troubleshooting

### Application Won't Start
- Ensure Java 17+ is installed: `java -version`
- Rebuild: `mvn clean install`
- Use the provided scripts instead of IDE Run button

### Playback Issues
- Ensure MP3 files are valid and not corrupted
- Check console for JavaFX Media error messages
- UNC network paths may require special URI encoding (handled automatically)

### Artwork Not Displaying
- Artwork loads lazily on-demand (scroll to trigger loading)
- Check file permissions on MP3 files
- Some MP3s may not have embedded artwork (shows `-`)

### Admin Mode Not Working
- Password is: `pwd`
- Role always resets to User mode on restart (fail-safe)

### Changes Not Saved
- Application must have write permissions to MP3 directory
- Configuration saved to `~/.singsongartwork/config.properties`

## License

See [LICENSE.md](LICENSE.md) for details.

## Version History

- **v1.0** - Initial release with modern UI, playback, and advanced features
- Core MP3 metadata management
- Dark theme with role-based access control
- Comprehensive keyboard shortcuts and batch operations
