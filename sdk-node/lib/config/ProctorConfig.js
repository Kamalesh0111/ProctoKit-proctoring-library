/**
 * A simple configuration class for the ProctorSDK.
 * It holds the host and port for the WebSocket server.
 */
class ProctorConfig {
    /**
     * @param {number} port The port to listen on.
     * @param {string} host The host to bind to (e.g., '0.0.0.0' to listen on all interfaces).
     */
    constructor(port, host = '0.0.0.0') {
        if (!port) {
            throw new Error('Port is a required configuration property.');
        }
        this.port = port;
        this.host = host;
    }
}

module.exports = ProctorConfig;
