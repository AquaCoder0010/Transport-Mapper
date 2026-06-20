#!/bin/bash
rm -rf build && mkdir build

find src -name "*.java" > sources.txt

javac -d build -cp lib/jmapviewer-2.24.jar:lib/json-20240303.jar @sources.txt

if [ $? -eq 0 ]; then
    echo "Compilation successful."
    java -cp build:lib/jmapviewer-2.24.jar:lib/json-20240303.jar MapViewer
else
    echo "Compilation failed."
fi
