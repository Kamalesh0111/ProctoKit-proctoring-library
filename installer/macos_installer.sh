#!/bin/bash

# macOS Installer Script for Proctoring Client
# This script bundles the Java application into a macOS .app and then creates a .dmg for distribution.
# It registers the 'proctor-session://' custom URL protocol.

# --- Configuration ---
APP_NAME="ProctorClient"
VERSION="1.0"
# Path to the "fat JAR" created by Maven, relative to the project root.
JAVA_CLIENT_JAR_PATH="../java-client/target/java-client-1.0-SNAPSHOT-jar-with-dependencies.jar"
# (Optional) You would need to create an Apple Icon Image file (.icns) for your app.
ICON_FILE="icon.icns" 

# --- Directory Setup ---
echo "--- Starting macOS package build ---"
BUILD_DIR="build_macos"
APP_BUNDLE_PATH="$BUILD_DIR/$APP_NAME.app"
CONTENTS_PATH="$APP_BUNDLE_PATH/Contents"
MACOS_PATH="$CONTENTS_PATH/MacOS"
RESOURCES_PATH="$CONTENTS_PATH/Resources"

# Clean up any previous builds to ensure a fresh start.
rm -rf "$BUILD_DIR"
mkdir -p "$MACOS_PATH"
mkdir -p "$RESOURCES_PATH"

# --- Create the Info.plist file ---
# This XML file is the heart of a macOS application. It tells the OS everything about the app.
# The 'CFBundleURLTypes' section is what registers our custom 'proctor-session://' protocol.
echo "--- Creating Info.plist ---"
cat > "$CONTENTS_PATH/Info.plist" <<EOL
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN" "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
<plist version="1.0">
<dict>
    <key>CFBundleExecutable</key>
    <string>launcher</string>
    <key>CFBundleIconFile</key>
    <string>icon.icns</string>
    <key>CFBundleIdentifier</key>
    <string>com.proctor.client</string>
    <key>CFBundleName</key>
    <string>$APP_NAME</string>
    <key>CFBundlePackageType</key>
    <string>APPL</string>
    <key>CFBundleVersion</key>
    <string>$VERSION</string>
    <key>CFBundleShortVersionString</key>
    <string>$VERSION</string>
    <key>CFBundleURLTypes</key>
    <array>
        <dict>
            <key>CFBundleURLName</key>
            <string>Proctoring Session URL</string>
            <key>CFBundleURLSchemes</key>
            <array>
                <string>proctor-session</string>
            </array>
        </dict>
    </array>
</dict>
</plist>
EOL

# --- Create the Launcher Script ---
# This script is the actual executable inside the .app bundle.
# When the OS opens the app to handle a URL, it runs this script and passes the URL as an argument.
echo "--- Creating launcher script ---"
cat > "$MACOS_PATH/launcher" <<EOL
#!/bin/bash
# Get the directory where this script is located.
DIR=\$(cd "\$(dirname "\$0")"; pwd)
# Launch the Java application, passing all command-line arguments (i.e., the proctor-session:// URL) to it.
java -jar "\$DIR/../Resources/ProctorApp.jar" "\$@"
EOL

# Make the launcher script executable.
chmod +x "$MACOS_PATH/launcher"

# --- Copy Application Files ---
echo "--- Copying application files ---"
# Copy the main JAR file into the app's Resources folder.
cp "$JAVA_CLIENT_JAR_PATH" "$RESOURCES_PATH/ProctorApp.jar"

# (Optional) Copy the icon file if it exists.
if [ -f "$ICON_FILE" ]; then
    cp "$ICON_FILE" "$RESOURCES_PATH/icon.icns"
else
    echo "Note: Icon file '$ICON_FILE' not found. App will have a default icon."
fi

# --- Create the .dmg Disk Image ---
# A .dmg is the standard way to distribute applications on macOS.
echo "--- Creating DMG file for distribution ---"
DMG_NAME="$BUILD_DIR/${APP_NAME}-${VERSION}.dmg"
hdiutil create -volname "$APP_NAME Installer" -srcfolder "$APP_BUNDLE_PATH" -ov -format UDZO "$DMG_NAME"

echo ""
echo "--- macOS build completed successfully! ---"
echo "Installer created at: $DMG_NAME"
echo "To install, open the DMG and drag '$APP_NAME.app' to the Applications folder."

