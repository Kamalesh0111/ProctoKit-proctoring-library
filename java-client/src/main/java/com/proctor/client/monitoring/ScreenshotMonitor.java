package com.proctor.client.monitoring;

import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * Monitors common screenshot locations for newly created image files.
 * This is a REACTIVE approach: it detects the screenshot file just after it has been created.
 * It is more reliable and cross-platform than trying to intercept keyboard shortcuts.
 */
public class ScreenshotMonitor implements Runnable {

    private final Consumer<String> eventConsumer;
    private final WatchService watchService;

    // Common image file extensions to look for
    private static final Set<String> IMAGE_EXTENSIONS = new HashSet<>(Arrays.asList("png", "jpg", "jpeg", "bmp", "gif"));

    public ScreenshotMonitor(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
        try {
            this.watchService = FileSystems.getDefault().newWatchService();
            registerDirectories();
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize ScreenshotMonitor WatchService", e);
        }
    }

    private void registerDirectories() {
        String userHome = System.getProperty("user.home");
        // List of common directories where screenshots are saved
        List<Path> pathsToWatch = Arrays.asList(
                Paths.get(userHome, "Desktop"),
                Paths.get(userHome, "Pictures", "Screenshots"), // Windows default
                Paths.get(userHome, "OneDrive", "Pictures", "Screenshots"), // Windows + OneDrive
                Paths.get(System.getenv("USERPROFILE"), "Desktop") // More reliable for Windows Desktop
        );

        for (Path path : pathsToWatch) {
            if (Files.isDirectory(path)) {
                try {
                    path.register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
                    System.out.println("ScreenshotMonitor is now watching: " + path);
                } catch (IOException e) {
                    System.err.println("Could not register directory for screenshot monitoring: " + path);
                }
            }
        }
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            WatchKey key;
            try {
                key = watchService.take(); // This blocks until an event occurs
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ScreenshotMonitor was interrupted.");
                break;
            }

            for (WatchEvent<?> event : key.pollEvents()) {
                if (event.kind() == StandardWatchEventKinds.ENTRY_CREATE) {
                    Path newFilePath = (Path) event.context();
                    String fileName = newFilePath.toString().toLowerCase();
                    String fileExtension = getFileExtension(fileName);

                    if (IMAGE_EXTENSIONS.contains(fileExtension)) {
                        handleScreenshotDetected(newFilePath);
                    }
                }
            }

            // Reset the key to continue watching for new events
            boolean valid = key.reset();
            if (!valid) {
                System.err.println("WatchKey is no longer valid. Stopping ScreenshotMonitor for one directory.");
                // You might want to handle this more gracefully, e.g., by re-registering
            }
        }
    }

    private void handleScreenshotDetected(Path filePath) {
        JSONObject event = new JSONObject();
        event.put("timestamp", System.currentTimeMillis());
        event.put("activity", "screenshotTaken");
        event.put("status", "violation");

        JSONObject details = new JSONObject();
        details.put("fileName", filePath.toString());
        details.put("message", "A new image file was created in a monitored directory, indicating a likely screenshot.");
        event.put("details", details);

        eventConsumer.accept(event.toString());
        System.out.println("Screenshot detected: " + filePath);
    }

    private String getFileExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf(".");
        if (lastIndexOfDot == -1) {
            return ""; // No extension
        }
        return fileName.substring(lastIndexOfDot + 1);
    }
}
