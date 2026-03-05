# SingSongArtwork

A Java application that reads MP3 metadata and artwork, displays them in a sortable/filterable table, and allows adding/replacing artwork in MP3 files.

## Requirements Coverage

1. ✅ **Java project setup**: Maven project `SingSongArtwork` with Java 17
2. ✅ **Load MP3 metadata + artwork**: Scans `.mp3` files and extracts title, artist, artwork
3. ✅ **4 columns**: GUI table displays `filename`, `title`, `artist`, `artwork`
4. ✅ **Sorting**: Click column headers to sort by filename, title, or artist
5. ✅ **Filtering**: Search bar filters by all words (AND logic) across filename/title/artist
6. ✅ **Add/replace artwork**: "Replace Artwork" button to update MP3 artwork

## Build and Test

```powershell
mvn -q -U test
```

## Run

### GUI Application (Recommended)

**Option 1: Run the Batch Script (Easiest - Windows)**
```powershell
.\start-gui.bat
```

**Option 2: Run the PowerShell Script (Windows)**
```powershell
.\start-gui.ps1
```

**Option 3: Maven Command Line**
```powershell
mvn javafx:run
```

**Option 4: Original Batch Script**
```powershell
.\run-gui.bat
```

⚠️ **Note:** Running directly from IntelliJ's "Run" button doesn't work because IntelliJ doesn't properly configure JavaFX modules. Use one of the scripts above instead.

### CLI Application (Legacy)

For command-line operation:

```powershell
mvn exec:java "-Dexec.mainClass=com.example.singsongartwork.SingSongArtworkApp" "-Dexec.args=--dir C:\Path\To\Mp3s"
```

Filter and sort:
```powershell
mvn exec:java "-Dexec.mainClass=com.example.singsongartwork.SingSongArtworkApp" "-Dexec.args=--dir C:\Path\To\Mp3s --filter queen live --sort artist,title"
```

Replace artwork:
```powershell
mvn exec:java "-Dexec.mainClass=com.example.singsongartwork.SingSongArtworkApp" "-Dexec.args=--dir C:\Path\To\Mp3s --replace-artwork song.mp3 --artwork-file C:\Path\cover.jpg"
```

## GUI Features

- **Browse Directory**: Select an MP3 directory to scan
- **Filter**: Real-time search (all words must match filename, title, or artist)
- **Sort**: Click any column header to sort (click again to reverse)
- **Replace Artwork**: Select a track and click "Replace Artwork" to update its cover image

## Troubleshooting

### IntelliJ Run Button Not Working

IntelliJ's direct application runner doesn't properly configure JavaFX modules. Use one of the provided scripts instead:

```powershell
.\start-gui.bat
```

If you want to use IntelliJ's Run button, follow these steps:
1. `File` → `Invalidate Caches / Restart...`
2. Choose `Invalidate and Restart`
3. Wait for IntelliJ to restart
4. `Run` → `Edit Configurations...`
5. Click `+` → `Maven`
6. Fill in:
   - Name: `SingSongArtwork`
   - Command line: `javafx:run`
7. Click OK and Run

## Notes

- The app displays all MP3s in the selected directory (non-recursive)
- Filter updates automatically as you type
- Sorting is done via standard JavaFX table column headers

## Exit Codes (CLI mode)

- `0`: Success
- `2`: Usage or validation error
- `1`: Runtime failure
