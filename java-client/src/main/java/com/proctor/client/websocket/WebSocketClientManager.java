package com.proctor.client.websocket;

import com.proctor.client.ui.ConsoleUI;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Manages the WebSocket connection to the server.
 * This version includes the missing 'awaitOpen' method required by ProctorApp.
 */
public class WebSocketClientManager {

    private final WebSocketClient client;
    // A latch is a concurrency tool that allows one thread to wait for another to finish.
    private final CountDownLatch connectLatch = new CountDownLatch(1);

    public WebSocketClientManager(URI serverUri) {
        this.client = new WebSocketClient(serverUri) {
            @Override
            public void onOpen(ServerHandshake handshakedata) {
                ConsoleUI.showMessage("Successfully connected to the proctoring server.");
                connectLatch.countDown(); // Connection is open, release the latch.
            }

            @Override
            public void onMessage(String message) {
                ConsoleUI.showMessage("Received message from server: " + message);
            }

            @Override
            public void onClose(int code, String reason, boolean remote) {
                ConsoleUI.showWarning("Disconnected from server. Reason: " + reason);
            }

            @Override
            public void onError(Exception ex) {
                ConsoleUI.showError("WebSocket error: " + ex.getMessage());
            }
        };
    }

    public void connect() {
        client.connect();
    }

    public void close() {
        client.close();
    }

    public boolean isOpen() {
        return client.isOpen();
    }

    public void sendEvent(String eventJson) {
        if (isOpen()) {
            client.send(eventJson);
        } else {
            ConsoleUI.showWarning("Attempted to send event while disconnected.");
        }
    }

    /**
     * (FIX) This is the missing method. It waits for the WebSocket connection to be established.
     * ProctorApp will call this to ensure a connection exists before starting monitors.
     * @param timeout The maximum time to wait.
     * @param unit The time unit of the timeout argument.
     * @return {@code true} if the connection was established within the time limit, {@code false} otherwise.
     * @throws InterruptedException if the current thread is interrupted while waiting.
     */
    public boolean awaitOpen(long timeout, TimeUnit unit) throws InterruptedException {
        // This will pause the thread until connectLatch.countDown() is called in onOpen(), or the timeout expires.
        return connectLatch.await(timeout, unit);
    }
}

