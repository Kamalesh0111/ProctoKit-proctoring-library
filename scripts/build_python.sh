#!/bin/bash

# build_python.sh
# This script builds the python-agent into a single executable file using PyInstaller.

echo "--- Building Python Agent Executable ---"

# Navigate to the python agent directory relative to the script's location
cd "$(dirname "$0")/../python-agent"

# Check if PyInstaller is installed
if ! command -v pyinstaller &> /dev/null
then
    echo "PyInstaller could not be found. Please install it with 'pip install pyinstaller'"
    exit 1
fi

# Check for the Haar Cascade model file
if [ ! -f "haarcascade_frontalface_default.xml" ]; then
    echo "ERROR: haarcascade_frontalface_default.xml not found in python-agent directory."
    exit 1
fi

pyinstaller main.py \
  --name proctor_agent \
  --onefile \
  --windowed \
  --add-data "haarcascade_frontalface_default.xml:." \
  --clean

if [ $? -eq 0 ]; then
    echo "--- Python agent built successfully. Executable is in the 'dist' folder. ---"
else
    echo "--- ERROR: Python agent build failed. ---"
    exit 1
fi

# Return to the root directory
cd ../
