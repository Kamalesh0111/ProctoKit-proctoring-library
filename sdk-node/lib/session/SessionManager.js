const ProctorSession = require('./ProctorSession');

/**
 * Manages the lifecycle of all active ProctorSession objects.
 */
class SessionManager {
    constructor(newSessionHandler) {
        this.activeSessions = new Map();
        this.newSessionHandler = newSessionHandler;
    }

    /**
     * Creates and starts a new session for a connecting client.
     * @param {WebSocket} ws The WebSocket connection.
     * @param {string} sessionId The unique ID for the session.
     */
    startSession(ws, sessionId) {
        if (!sessionId) {
            console.error('Attempted to start a session without a sessionId.');
            ws.close(1008, 'Session ID is required.');
            return;
        }

        const session = new ProctorSession(sessionId, ws);
        this.activeSessions.set(ws, session);
        
        // Notify the main SDK that a new session has been created.
        if (this.newSessionHandler) {
            this.newSessionHandler(session);
        }
    }

    /**
     * Ends a session when a client disconnects.
     * @param {WebSocket} ws The disconnected WebSocket.
     */
    endSession(ws) {
        const session = this.activeSessions.get(ws);
        if (session) {
            session._handleDisconnect();
            this.activeSessions.delete(ws);
        }
    }
    
    /**
     * Retrieves the session associated with a WebSocket connection.
     * @param {WebSocket} ws The WebSocket connection.
     * @returns {ProctorSession|undefined}
     */
    getSession(ws) {
        return this.activeSessions.get(ws);
    }
}

module.exports = SessionManager;
