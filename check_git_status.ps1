# Git Status Diagnostic Script
Write-Host "=== GIT STATUS DIAGNOSTIC ===" -ForegroundColor Cyan
Write-Host ""

# Check if we're in a git repo
if (Test-Path ".git") {
    Write-Host "✓ Git repository found" -ForegroundColor Green
} else {
    Write-Host "✗ Git repository NOT found" -ForegroundColor Red
    exit 1
}

Write-Host ""
Write-Host "=== MODIFIED FILES (unstaged) ===" -ForegroundColor Yellow
$unstaged = git diff --name-only 2>$null
if ($unstaged) {
    Write-Host $unstaged -ForegroundColor Green
} else {
    Write-Host "(none)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== STAGED FILES ===" -ForegroundColor Yellow
$staged = git diff --cached --name-only 2>$null
if ($staged) {
    Write-Host $staged -ForegroundColor Green
} else {
    Write-Host "(none)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== UNTRACKED FILES ===" -ForegroundColor Yellow
$untracked = git ls-files --others --exclude-standard 2>$null
if ($untracked) {
    Write-Host $untracked -ForegroundColor Green
} else {
    Write-Host "(none)" -ForegroundColor Gray
}

Write-Host ""
Write-Host "=== SHORT STATUS ===" -ForegroundColor Yellow
git status --short 2>$null

Write-Host ""
Write-Host "=== FULL STATUS ===" -ForegroundColor Yellow
git status 2>$null

