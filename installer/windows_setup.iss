; Inno Setup Script for Proctoring Client
; This script creates a Windows installer that handles the automated session handshake.
; It assumes the final "fat JAR" is located in the java-client/target/ directory.

[Setup]
; --- Basic Application Information ---
AppName=Proctoring Client
AppVersion=1.0
; {autopf} is the Program Files directory (e.g., C:\Program Files)
DefaultDirName={autopf}\ProctoringClient
DefaultGroupName=Proctoring Client
UninstallDisplayIcon={app}\ProctorApp.ico
SolidCompression=yes
WizardStyle=modern
; --- Output file name for the final installer ---
OutputBaseFilename=proctor-client-setup

[Files]
; 1. The main application "fat JAR". This is the most important file.
;    The source path is relative to the location of this .iss script.
Source: "..\java-client\target\java-client-1.0-SNAPSHOT.jar"; DestDir: "{app}"; DestName: "ProctorApp.jar"

; 2. The PowerShell helper script for the automated handshake.
Source: "handshake-server.ps1"; DestDir: "{app}"

; 3. (Optional) An icon for the application. You would need to create this .ico file.
; Source: "path\to\your\icon.ico"; DestDir: "{app}"; DestName: "ProctorApp.ico"

[Icons]
; Creates a shortcut in the Start Menu.
Name: "{group}\Proctoring Client"; Filename: "{app}\ProctorApp.jar"

[Run]
; This is the most critical section for the automated handshake.
; After all files are copied, the installer will run the PowerShell script.
; - 'runhidden': The student won't see the black PowerShell console window.
; - 'waituntilterminated': The installer will pause and wait for the handshake to complete before finishing.
Filename: "powershell.exe"; Parameters: "-ExecutionPolicy Bypass -File ""{app}\handshake-server.ps1"""; Flags: runhidden waituntilterminated