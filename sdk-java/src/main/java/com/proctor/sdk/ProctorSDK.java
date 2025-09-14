package com.proctor.sdk;

import com.proctor.sdk.config.ProctorConfig;
import com.proctor.sdk.session.ProctorSession;
import com.proctor.sdk.session.SessionManager;
import com.proctor.sdk.websocket.ProctorWebsocketServer;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * The final, production-ready entry point for the Proctoring SDK for Java.
 * This version allows the developer to provide installer links during configuration
 * and retrieve them to facilitate OS-aware downloads on their frontend.
 */
public class ProctorSDK {

    private final ProctorConfig config;
    private final SessionManager sessionManager;
    private final ProctorWebsocketServer server;
    private final List<Consumer<ProctorSession>> newSessionHandlers = new ArrayList<>();

    public ProctorSDK(ProctorConfig config) {
        this.config = config;
        this.sessionManager = new SessionManager(this::handleNewSession);
        InetSocketAddress address = new InetSocketAddress(config.getHost(), config.getPort());
        this.server = new ProctorWebsocketServer(address, this.sessionManager);
    }

    /**
     * Starts the internal WebSocket server.
     */
    public void start() {
        server.start();
    }

    /**
     * Stops the internal WebSocket server.
     * @throws InterruptedException if the thread is interrupted while stopping.
     */
    public void stop() throws InterruptedException {
        server.stop();
    }

    /**
     * Registers a handler to be called when a new student client connects.
     * @param handler A consumer that accepts the new ProctorSession.
     */
    public void onSession(Consumer<ProctorSession> handler) {
        newSessionHandlers.add(handler);
    }

    /**
     * (NEW) Returns the map of installer URLs provided during configuration.
     * The developer's backend will expose this data via an API endpoint for their
     * frontend to use for OS-specific download links.
     *
     * @return A map where the key is the OS ('windows', 'macos') and the value is the download URL.
     */
    public Map<String, String> getInstallerLinks() {
        return config.getInstallerUrls();
    }
    
    /**
     * The developer's frontend JavaScript is now responsible for initiating the session
     * by communicating with the installed client.
     * This SDK provides the necessary information via getInstallerLinks().
     */

    private void handleNewSession(ProctorSession session) {
        for (Consumer<ProctorSession> handler : newSessionHandlers) {
            try {
                handler.accept(session);
            } catch (RuntimeException e) {
                // Provides a more helpful error message for the developer using the SDK.
                System.err.println("ERROR: Unhandled exception in developer-provided onSession handler for session " 
                    + session.getSessionId() + ": " + e.getMessage());
            }
        }
    }
}

