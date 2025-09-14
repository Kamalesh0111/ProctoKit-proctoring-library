package com.proctor.client.monitoring;

import org.json.JSONObject;

import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.function.Consumer;

/**
 * Monitors the system clipboard for any new text content.
 * It runs in a loop, checking the clipboard periodically. If new text is detected,
 * it fires a "suspicious" event with the copied content.
 */
public class ClipboardMonitor implements Runnable {

    private final Consumer<String> eventConsumer;
    private String lastClipboardContent = "";

    public ClipboardMonitor(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
        // Initialize with current clipboard content to avoid firing on startup
        this.lastClipboardContent = getClipboardText();
    }

    @Override
    public void run() {
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        while (!Thread.currentThread().isInterrupted()) {
            try {
                String currentContent = getClipboardText(clipboard);
                // Check if the content is new and not empty
                if (currentContent != null && !currentContent.isEmpty() && !currentContent.equals(lastClipboardContent)) {
                    lastClipboardContent = currentContent;
                    
                    JSONObject event = new JSONObject();
                    event.put("timestamp", System.currentTimeMillis());
                    event.put("activity", "clipboardCopy");
                    event.put("status", "suspicious"); // Any copy during an exam is suspicious

                    JSONObject details = new JSONObject();
                    // For privacy, you might want to truncate or hash this in a real product
                    details.put("copiedText", currentContent.substring(0, Math.min(currentContent.length(), 100)));
                    details.put("message", "New text content was copied to the clipboard.");
                    event.put("details", details);

                    eventConsumer.accept(event.toString());
                }
                // Check every second
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ClipboardMonitor was interrupted.");
                break;
            } catch (Exception e) {
                // Handle cases where clipboard is not available or contains non-text data
                // System.err.println("Could not read clipboard: " + e.getMessage());
            }
        }
    }

    private String getClipboardText() {
        return getClipboardText(Toolkit.getDefaultToolkit().getSystemClipboard());
    }

    private String getClipboardText(Clipboard clipboard) {
        try {
            Transferable contents = clipboard.getContents(null);
            if (contents != null && contents.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                return (String) contents.getTransferData(DataFlavor.stringFlavor);
            }
        } catch (Exception e) {
            // Ignore - clipboard might be busy or contain other data types
        }
        return null;
    }
}

