@echo off
if exist build rmdir /s /q build
mkdir build

dir /s /b src\*.java > sources.txt

javac -d build -cp lib/jmapviewer-2.24.jar;lib/json-20240303.jar @sources.txt

if %errorlevel% equ 0 (
    echo Compilation successful.
    java -cp build;lib/jmapviewer-2.24.jar;lib/json-20240303.jar MapViewer
) else (
    echo Compilation failed.
)
