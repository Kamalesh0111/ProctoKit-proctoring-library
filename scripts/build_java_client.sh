#!/bin/bash

# build_java_client.sh
# This script compiles the java-client and bundles the Python executable into a single "fat JAR".
# IMPORTANT: build_python.sh must be run before this script.

echo "--- Building Java Client Fat JAR ---"

# Navigate to the root of the project
cd "$(dirname "$0")/.."

# Check if the Python executable exists
if [ ! -f "python-agent/dist/proctor_agent" ] && [ ! -f "python-agent/dist/proctor_agent.exe" ]; then
    echo "WARNING: Python executable not found. Did you run build_python.sh first?"
    echo "Attempting to build Java client anyway..."
fi

# Run Maven to clean and package the java-client module.
# The -pl flag specifies the module, and -am builds its dependencies too.
mvn clean package -pl java-client -am

if [ $? -eq 0 ]; then
    echo "--- Java client built successfully. JAR is in java-client/target/ ---"
else
    echo "--- ERROR: Java client build failed. ---"
    exit 1
fi
