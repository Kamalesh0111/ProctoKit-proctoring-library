#!/bin/bash

# build_all.sh
# This is the main build script. It orchestrates the entire build process:
# 1. Builds the Python executable.
# 2. Builds the Java "fat JAR" (bundling the Python exe).
# 3. Builds the native installer for the current OS.

echo "====== STARTING FULL PROJECT BUILD ======"

# Get the directory of the script
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

# --- Step 1: Build the Python Agent ---
echo ""
echo ">>> STEP 1: Building Python Agent..."
bash "${SCRIPT_DIR}/build_python.sh"
if [ $? -ne 0 ]; then exit 1; fi

# --- Step 2: Build the Java Client JAR ---
echo ""
echo ">>> STEP 2: Building Java Client JAR..."
bash "${SCRIPT_DIR}/build_java_client.sh"
if [ $? -ne 0 ]; then exit 1; fi

# --- Step 3: Build the Native Installer ---
echo ""
echo ">>> STEP 3: Building Native Installer..."
# Navigate to the root of the project
cd "${SCRIPT_DIR}/.."

# Detect the operating system
OS="$(uname -s)"
case "${OS}" in
    Linux*)     
        echo "Installer for Linux is not yet configured. Skipping."
        # You would add Linux installer logic here (e.g., creating a .deb package)
        ;;
    Darwin*)    
        echo "Detected macOS. Running macOS installer script..."
        bash "installer/macos_installer.sh"
        if [ $? -ne 0 ]; then echo "macOS installer build failed."; exit 1; fi
        ;;
    CYGWIN*|MINGW*|MSYS*) 
        echo "Detected Windows. Running Inno Setup compiler..."
        # This requires Inno Setup to be in your system's PATH.
        # The command is typically 'iscc'.
        if ! command -v iscc &> /dev/null
        then
            echo "ERROR: Inno Setup Compiler (iscc.exe) not found in your PATH."
            exit 1
        fi
        iscc "installer/windows_setup.iss"
        if [ $? -ne 0 ]; then echo "Windows installer build failed."; exit 1; fi
        ;;
    *)          
        echo "Unsupported OS: ${OS}. Cannot build installer." 
        ;;
esac

echo ""
echo "====== FULL PROJECT BUILD COMPLETED SUCCESSFULLY! ======"
