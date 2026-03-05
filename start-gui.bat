@echo off
REM SingSongArtwork - GUI Launcher
REM This script runs the JavaFX application with proper module configuration

cd /d "%~dp0"
echo Starting SingSongArtwork GUI...
echo Please wait...

REM Run with Maven which handles all dependencies and modules correctly
mvn clean javafx:run

pause

