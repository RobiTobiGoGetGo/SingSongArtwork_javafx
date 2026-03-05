#!/usr/bin/env pwsh

Write-Host "Starting SingSongArtwork GUI..." -ForegroundColor Green
Write-Host "This will launch the JavaFX application with proper module configuration..." -ForegroundColor Cyan

Set-Location (Split-Path $PSCommandPath -Parent)

# Run with Maven - javafx:run handles JavaFX modules automatically
mvn javafx:run

if ($LASTEXITCODE -ne 0) {
    Write-Host "Application failed to start. Check console output above for errors." -ForegroundColor Red
} else {
    Write-Host "Application closed." -ForegroundColor Yellow
}
