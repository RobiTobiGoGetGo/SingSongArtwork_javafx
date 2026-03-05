#!/usr/bin/env pwsh

Write-Host "Starting SingSongArtwork GUI..." -ForegroundColor Green
Write-Host "This will launch the JavaFX application with proper module configuration..." -ForegroundColor Cyan

Set-Location (Split-Path $PSCommandPath -Parent)

# Run with Maven which handles all dependencies and modules correctly
mvn clean javafx:run

Write-Host "Application closed." -ForegroundColor Yellow

