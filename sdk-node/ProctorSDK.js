const EventEmitter = require('events');
const ProctorConfig = require('./config/ProctorConfig');
const SessionManager = require('./session/SessionManager');
const ProctorWebsocketServer = require('./websocket/ProctorWebsocketServer');

/**
 * The main public entry point for the Proctoring SDK for Node.js.
 * This version is designed for the automated handshake workflow.
 *
 * @fires ProctorSDK#session
 *
 * @example
 * // server.js
 * const { ProctorSDK, ProctorConfig } = require('proctor-sdk');
 *
 * // 1. Configure and start the SDK on your backend server.
 * const config = new ProctorConfig(8080, '0.0.0.0');
 * const sdk = new ProctorSDK(config);
 *
 * // 2. Set up your session handler.
 * sdk.on('session', (session) => {
 * console.log(`[SDK] New session started: ${session.sessionId}`);
 * session.on('violation', (event) => {
 * // Handle violations...
 * });
 * });
 *
 * sdk.start();
 *
 * // 3. On your frontend, use the provided JavaScript snippet from the documentation.
 * // The snippet will automatically handle the installation prompt and session handshake
 * // with the installed client. The generateClientLaunchUrl() method is no longer used.
 */
class ProctorSDK extends EventEmitter {
    /**
     * @param {ProctorConfig} config The configuration object for the SDK.
     */
    constructor(config) {
        super();
        this.config = config;
        this.sessionManager = new SessionManager((session) => this.emit('session', session));
        this.server = new ProctorWebsocketServer(this.config, this.sessionManager);
    }

    /**
     * Starts the internal WebSocket server to listen for client connections.
     */
    start() {
        this.server.start();
    }

    /**
     * Stops the WebSocket server.
     */
    stop() {
        this.server.stop();
    }
    
    /**
     * NOTE: This method is now obsolete in the automated handshake workflow.
     * The developer should now use the provided frontend JavaScript snippet
     * which communicates with the installer directly.
     * This method is removed to enforce the correct integration pattern.
     */
    // generateClientLaunchUrl(sessionId) { ... }
}

module.exports = { ProctorSDK, ProctorConfig };

