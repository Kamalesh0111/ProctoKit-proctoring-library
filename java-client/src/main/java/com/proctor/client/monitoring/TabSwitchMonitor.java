package com.proctor.client.monitoring;

import com.sun.jna.Native;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import org.json.JSONObject;

import java.util.function.Consumer;

/**
 * Monitors the active window title to provide a BEST-EFFORT detection of browser tab switches.
 *
 * IMPORTANT NOTE: A native application CANNOT reliably detect tab switches inside a browser.
 * This is a fundamental security boundary. This monitor works by watching for changes in the
 * browser's WINDOW TITLE (e.g., "My Exam - Google Chrome" -> "Google Search - Google Chrome").
 * This is an indirect method and may not work for all websites or browsers. True tab switch
 * detection requires a browser extension.
 *
 * This implementation is functionally similar to WindowMonitor but is focused on changes
 * within the allowed browser process.
 */
public class TabSwitchMonitor implements Runnable {

    private static final int MAX_TITLE_LENGTH = 1024;
    private final Consumer<String> eventConsumer;
    private String lastBrowserTitle = "";
    // In a real application, the developer would provide this via the session
    private final String allowedBrowserIdentifier = "Google Chrome"; // Example

    public TabSwitchMonitor(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
        this.lastBrowserTitle = getActiveWindowTitle();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String currentWindowTitle = getActiveWindowTitle();

                // Only proceed if we are actually in the target browser
                if (currentWindowTitle != null && currentWindowTitle.contains(allowedBrowserIdentifier)) {
                    // Check if the title has changed since the last check
                    if (!currentWindowTitle.equals(lastBrowserTitle)) {
                        
                        JSONObject event = new JSONObject();
                        event.put("timestamp", System.currentTimeMillis());
                        event.put("activity", "tabSwitch");
                        event.put("status", "suspicious"); // A tab switch is always suspicious

                        JSONObject details = new JSONObject();
                        details.put("previousTitle", lastBrowserTitle);
                        details.put("newTitle", currentWindowTitle);
                        details.put("message", "Browser window title changed, indicating a possible tab switch.");
                        event.put("details", details);
                        
                        eventConsumer.accept(event.toString());
                        
                        // Update the last known title
                        lastBrowserTitle = currentWindowTitle;
                    }
                } else {
                    // If the active window isn't the browser, reset the last title.
                    // The WindowMonitor is responsible for flagging this as a violation.
                    lastBrowserTitle = "";
                }

                Thread.sleep(1000); // Check every second
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("TabSwitchMonitor was interrupted.");
                break;
            }
        }
    }

    private String getActiveWindowTitle() {
        // This is Windows-specific logic
        if (com.sun.jna.Platform.isWindows()) {
            char[] buffer = new char[MAX_TITLE_LENGTH * 2];
            WinDef.HWND hwnd = User32.INSTANCE.GetForegroundWindow();
            User32.INSTANCE.GetWindowText(hwnd, buffer, MAX_TITLE_LENGTH);
            return Native.toString(buffer);
        }
        // NOTE: Implementations for macOS and Linux would be needed here.
        return "Unsupported OS";
    }
}
