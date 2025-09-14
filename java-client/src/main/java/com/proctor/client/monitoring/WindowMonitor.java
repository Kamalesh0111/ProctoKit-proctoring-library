package com.proctor.client.monitoring;

import com.sun.jna.Native;
import com.sun.jna.Platform;
import com.sun.jna.Pointer;
import com.sun.jna.platform.mac.CoreFoundation;
import com.sun.jna.platform.mac.CoreFoundation.CFArrayRef;
import com.sun.jna.platform.mac.CoreFoundation.CFDictionaryRef;
import com.sun.jna.platform.mac.CoreFoundation.CFStringRef;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.function.Consumer;

/**
 * Monitors the currently active window on the user's desktop.
 * This version contains the final fix for the JNA macOS compilation error.
 */
public class WindowMonitor implements Runnable {

    private static final int MAX_TITLE_LENGTH = 1024;
    private final Consumer<String> eventConsumer;
    private String lastActiveWindowTitle = "";
    private final String allowedBrowserTitlePart = "Google Chrome";

    public WindowMonitor(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
        this.lastActiveWindowTitle = getActiveWindowTitle();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String currentWindowTitle = getActiveWindowTitle();
                if (currentWindowTitle != null && !currentWindowTitle.equals(lastActiveWindowTitle)) {
                    lastActiveWindowTitle = currentWindowTitle;
                    JSONObject event = new JSONObject();
                    event.put("timestamp", System.currentTimeMillis());
                    event.put("activity", "windowSwitch");
                    boolean isViolation = !currentWindowTitle.contains(allowedBrowserTitlePart);
                    event.put("status", isViolation ? "violation" : "suspicious");
                    JSONObject details = new JSONObject();
                    details.put("activeWindow", currentWindowTitle);
                    details.put("message", "User switched to a new active window.");
                    event.put("details", details);
                    eventConsumer.accept(event.toString());
                }
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("WindowMonitor was interrupted.");
                break;
            } catch (Exception e) {
                System.err.println("Error in WindowMonitor loop: " + e.getMessage());
            }
        }
    }

    private String getActiveWindowTitle() {
        if (Platform.isWindows()) {
            return getWindowsActiveWindowTitle();
        }
        if (Platform.isMac()) {
            return getMacActiveWindowTitle();
        }
        if (Platform.isLinux()) {
            return getLinuxActiveWindowTitle();
        }
        return "Unsupported OS";
    }

    private String getWindowsActiveWindowTitle() {
        char[] buffer = new char[MAX_TITLE_LENGTH * 2];
        WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
        User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
        return Native.toString(buffer);
    }

    private String getLinuxActiveWindowTitle() {
        try {
            Process p = Runtime.getRuntime().exec("xdotool getactivewindow getwindowname");
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String title = reader.readLine();
            p.waitFor();
            return title != null ? title : "";
        } catch (Exception e) {
            return "Linux Window - N/A (is xdotool installed?)";
        }
    }

    // --- macOS Specific JNA Interface and Logic ---
    private interface CoreGraphics extends CoreFoundation {
        CoreGraphics INSTANCE = Native.load("CoreGraphics", CoreGraphics.class);
        int kCGWindowListOptionOnScreenOnly = 1;
        int kCGNullWindowID = 0;
        CFArrayRef CGWindowListCopyWindowInfo(int option, int relativeToWindow);
    }

    private String getMacActiveWindowTitle() {
        CFStringRef kCGWindowOwnerNameKey = CFStringRef.createCFString("kCGWindowOwnerName");
        CFStringRef kCGWindowNameKey = CFStringRef.createCFString("kCGWindowName");

        CFArrayRef windowList = CoreGraphics.INSTANCE.CGWindowListCopyWindowInfo(CoreGraphics.kCGWindowListOptionOnScreenOnly, CoreGraphics.kCGNullWindowID);
        if (windowList == null) {
            return "macOS Window - Permission Denied?";
        }

        String activeWindowTitle = "macOS Window - Not Found";
        try {
            int count = windowList.getCount();
            for (int i = 0; i < count; i++) {
                Pointer windowRef = windowList.getValueAtIndex(i);
                CFDictionaryRef windowInfo = new CFDictionaryRef(windowRef);
                
                // FINAL FIX: Pass the CFStringRef object (which is a PointerType) directly as the key.
                Pointer ownerNamePtr = windowInfo.getValue(kCGWindowOwnerNameKey);
                Pointer windowNamePtr = windowInfo.getValue(kCGWindowNameKey);

                if (ownerNamePtr != null) {
                    CFStringRef ownerNameRef = new CFStringRef(ownerNamePtr);
                    String ownerName = ownerNameRef.toString();

                    String windowName = "";
                    if (windowNamePtr != null) {
                        CFStringRef windowNameRef = new CFStringRef(windowNamePtr);
                        windowName = windowNameRef.toString();
                    }
                    
                    if (windowName != null && !windowName.isEmpty()) {
                        activeWindowTitle = ownerName + " - " + windowName;
                        break;
                    }
                }
            }
        } finally {
            windowList.release();
            kCGWindowOwnerNameKey.release();
            kCGWindowNameKey.release();
        }
        return activeWindowTitle;
    }
}

