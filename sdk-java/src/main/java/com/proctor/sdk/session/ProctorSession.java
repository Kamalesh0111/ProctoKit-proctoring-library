package com.proctor.sdk.session;

import org.java_websocket.WebSocket;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Represents a single, active proctoring session for one student.
 * This class acts as an event emitter, allowing developers to listen for
 * specific events from an individual student's client.
 */
public class ProctorSession {

    private final String sessionId;
    private final WebSocket connection;

    // Listeners for various event types
    private final List<Consumer<JSONObject>> activityListeners = new ArrayList<>();
    private final List<Consumer<JSONObject>> violationListeners = new ArrayList<>();
    private final List<Runnable> disconnectListeners = new ArrayList<>();

    public ProctorSession(String sessionId, WebSocket connection) {
        this.sessionId = sessionId;
        this.connection = connection;
    }

    public String getSessionId() {
        return sessionId;
    }
    
    /**
     * Adds a listener that will be called for any event from the client.
     * @param listener The callback to execute.
     */
    public void onActivity(Consumer<JSONObject> listener) {
        activityListeners.add(listener);
    }

    /**
     * Adds a listener that will be called only for events marked as 'violation'.
     * @param listener The callback to execute.
     */
    public void onViolation(Consumer<JSONObject> listener) {
        violationListeners.add(listener);
    }
    
    /**
     * Adds a listener that will be called when the student's client disconnects.
     * @param listener The callback to execute.
     */
    public void onDisconnect(Runnable listener) {
        disconnectListeners.add(listener);
    }

    /**
     * Called by the server when a message is received for this session.
     * It parses the event and notifies the appropriate listeners.
     * @param message The raw JSON string from the client.
     */
    public void handleMessage(String message) {
        try {
            JSONObject event = new JSONObject(message);
            
            // Notify all general activity listeners
            activityListeners.forEach(listener -> listener.accept(event));
            
            // If the event is a violation, notify the specific violation listeners
            if ("violation".equals(event.optString("status"))) {
                violationListeners.forEach(listener -> listener.accept(event));
            }

        } catch (Exception e) {
            System.err.println("Error parsing event JSON for session " + sessionId + ": " + message);
        }
    }

    /**
     * Called by the server when the connection for this session is closed.
     */
    public void handleDisconnect() {
        disconnectListeners.forEach(Runnable::run);
    }
    
    /**
     * Closes the connection to the student's client.
     */
    public void disconnect() {
        if (connection != null && connection.isOpen()) {
            connection.close();
        }
    }
}
