package com.proctor.client;

import com.proctor.client.monitoring.*;
import com.proctor.client.process.PythonProcessManager;
import com.proctor.client.ui.ConsoleUI;
import com.proctor.client.websocket.WebSocketClientManager;

import java.net.URI;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * The main entry point for the proctoring client application.
 * This updated version expects a standard WebSocket URL as a direct command-line argument,
 * passed to it by the "smart" installer after the automated handshake.
 */
public class ProctorApp {

    private final ExecutorService monitorExecutor = Executors.newCachedThreadPool();
    private final BlockingQueue<String> eventQueue = new LinkedBlockingQueue<>();
    private WebSocketClientManager webSocketClient;
    private PythonProcessManager pythonProcessManager;

    /**
     * The main method, the entry point of the Java application.
     * @param args Command-line arguments. Expects a single argument: the full "ws://..." or "wss://..." URL.
     */
    public static void main(String[] args) {
        ConsoleUI.printStartupBanner();

        // Validate the launch argument, which is now a direct WebSocket URL.
        if (args.length == 0 || args[0] == null || !args[0].startsWith("ws")) {
            ConsoleUI.showError("Invalid launch command.");
            ConsoleUI.showError("This application is designed to be started automatically by its installer.");
            try { Thread.sleep(5000); } catch (InterruptedException ignored) {}
            return;
        }

        try {
            // The argument is the WebSocket URL directly.
            String wssUrl = args[0];
            URI serverUri = new URI(wssUrl);

            ProctorApp app = new ProctorApp();
            app.start(serverUri);

        } catch (Exception e) {
            ConsoleUI.showError("Failed to start the application due to an invalid URL or an internal error.");
            e.printStackTrace();
        }
    }

    public void start(URI serverUri) {
        webSocketClient = new WebSocketClientManager(serverUri);
        pythonProcessManager = new PythonProcessManager(eventQueue::offer);
        addShutdownHook();

        ConsoleUI.showMessage("Attempting to connect to the proctoring server...");
        webSocketClient.connect();

        try {
            if (!webSocketClient.awaitOpen(10, TimeUnit.SECONDS)) {
                 ConsoleUI.showError("Could not connect to the server. Please check your internet connection.");
                 return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            ConsoleUI.showError("Connection attempt was interrupted.");
            return;
        }
        
        startMonitors();
        startEventDispatcher();
    }
    
    private void startMonitors() {
        ConsoleUI.showMessage("Starting all system monitors...");
        monitorExecutor.submit(pythonProcessManager);
        monitorExecutor.submit(new ClipboardMonitor(eventQueue::offer));
        monitorExecutor.submit(new WindowMonitor(eventQueue::offer));
        monitorExecutor.submit(new ProcessMonitor(eventQueue::offer));
        monitorExecutor.submit(new TabSwitchMonitor(eventQueue::offer));
        monitorExecutor.submit(new ScreenshotMonitor(eventQueue::offer));
    }

    private void startEventDispatcher() {
        Thread dispatcherThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    String event = eventQueue.take();
                    if (webSocketClient.isOpen()) {
                        webSocketClient.sendEvent(event);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        dispatcherThread.setName("Event-Dispatcher");
        dispatcherThread.setDaemon(true);
        dispatcherThread.start();
    }

    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ConsoleUI.showMessage("Shutdown signal received. Cleaning up resources...");
            monitorExecutor.shutdownNow();
            if (pythonProcessManager != null) {
                pythonProcessManager.stop();
            }
            if (webSocketClient != null && webSocketClient.isOpen()) {
                webSocketClient.close();
            }
            ConsoleUI.showMessage("Cleanup complete. Application will now exit.");
        }));
    }
}

