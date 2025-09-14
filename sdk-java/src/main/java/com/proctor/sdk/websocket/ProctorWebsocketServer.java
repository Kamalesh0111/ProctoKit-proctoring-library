package com.proctor.sdk.websocket;

import com.proctor.sdk.session.ProctorSession;
import com.proctor.sdk.session.SessionManager;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.net.URI;

/**
 * The internal WebSocket server that listens for connections from student clients.
 * It delegates all connection and message handling to the SessionManager.
 */
public class ProctorWebsocketServer extends WebSocketServer {

    private final SessionManager sessionManager;

    public ProctorWebsocketServer(InetSocketAddress address, SessionManager sessionManager) {
        super(address);
        this.sessionManager = sessionManager;
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        // The client connects with a URL like "ws://host:port/sessionId"
        // We extract the sessionId from the resource descriptor.
        try {
            String path = new URI(handshake.getResourceDescriptor()).getPath();
            // Remove the leading slash to get the ID
            String sessionId = path.startsWith("/") ? path.substring(1) : path;
            
            if (sessionId == null || sessionId.trim().isEmpty()) {
                System.err.println("Connection rejected: No session ID provided in URL.");
                conn.close(1003, "Session ID is required.");
                return;
            }
            
            System.out.println("Client connected with session ID: " + sessionId);
            sessionManager.startSession(conn, sessionId);

        } catch (Exception e) {
            System.err.println("Connection rejected: Invalid session URL.");
            conn.close(1003, "Invalid session URL format.");
        }
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        System.out.println("Client disconnected: " + conn.getRemoteSocketAddress());
        sessionManager.endSession(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        ProctorSession session = sessionManager.getSession(conn);
        if (session != null) {
            session.handleMessage(message);
        }
    }

    @Override
    public void onError(WebSocket conn, Exception ex) {
        System.err.println("An error occurred on connection " + (conn != null ? conn.getRemoteSocketAddress() : "UNKNOWN"));
        ex.printStackTrace();
        if (conn != null) {
            // Ensure the session is cleaned up on error
            sessionManager.endSession(conn);
        }
    }
    
    @Override
    public void onStart() {
        System.out.println("Proctoring WebSocket server started successfully on port " + getPort());
    }
}
