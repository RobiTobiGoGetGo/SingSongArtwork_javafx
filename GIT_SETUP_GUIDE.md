# SingSongArtwork Git Configuration Guide

## STEP 1: Ensure Git detects all file changes

Open your terminal and run these commands:

```bash
cd C:\Users\robch\IdeaProjects\SingSongArtwork

# Refresh Git's file cache
git update-index --refresh

# Check if there are any differences
git status

# If you see "modified" files, they need to be staged:
git add -A

# Then commit
git commit -m "Your message here"

# Finally push to GitHub
git push origin main
```

## STEP 2: IntelliJ IDEA Configuration

To fix the IntelliJ-Git synchronization issue:

1. **Go to**: File → Settings → Version Control → Git
2. **Set**: Git executable to system Git (not bundled)
   - Path: `C:\Program Files\Git\cmd\git.exe` (or wherever Git is installed)
3. **Go to**: File → Settings → Version Control
4. **Enable**: "Show dirty markers" for files with uncommitted changes
5. **Click**: "Refresh" button to refresh Git status

## STEP 3: Verify Changes Appear

After making changes in IntelliJ:

1. **File should appear in "Changes" panel** under:
   - View → Tool Windows → Git → Commits (or)
   - View → Tool Windows → Version Control

2. **If it doesn't appear after 5 seconds**:
   - Right-click in Git panel → "Refresh"
   - Or press Ctrl+T (Sync with Remote)

## STEP 4: Regular Backup Workflow

**IMPORTANT**: Do this after every major change:

1. **Stage changes**: `git add -A`
2. **Check status**: `git status`
3. **Commit**: `git commit -m "descriptive message"`
4. **Push**: `git push origin main`
5. **Verify on GitHub**: Check https://github.com/RobiTobiGoGetGo/SingSongArtwork_javafx

## TROUBLESHOOTING

If "Changes" still don't appear:

1. **Force Git to reindex**:
   ```bash
   git update-index --really-refresh
   ```

2. **Clear Git cache**:
   ```bash
   git rm -r --cached .
   git add -A
   git commit -m "Fix: Refresh git tracking"
   git push origin main
   ```

3. **Restart IntelliJ**:
   - File → Invalidate Caches → Restart

4. **Last resort - clone fresh**:
   ```bash
   git clone https://github.com/RobiTobiGoGetGo/SingSongArtwork_javafx
   ```

