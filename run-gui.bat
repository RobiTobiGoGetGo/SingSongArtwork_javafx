@echo off
cd /d "%~dp0"
echo Starting SingSongArtwork GUI...
mvn exec:java "-Dexec.mainClass=com.example.singsongartwork.SingSongArtworkUI"

