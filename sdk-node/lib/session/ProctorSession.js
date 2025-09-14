const EventEmitter = require('events');

/**
 * Represents a single, active proctoring session for one student.
 * It emits events that the developer can listen to.
 *
 * @fires ProctorSession#violation
 * @fires ProctorSession#event
 * @fires ProctorSession#disconnect
 */
class ProctorSession extends EventEmitter {
    /**
     * @param {string} sessionId The unique ID for this session.
     * @param {WebSocket} webSocket The underlying WebSocket connection.
     */
    constructor(sessionId, webSocket) {
        super();
        this._sessionId = sessionId;
        this._webSocket = webSocket;
        this._startTime = new Date();
    }

    /**
     * The unique identifier for this session.
     * @returns {string}
     */
    get sessionId() {
        return this._sessionId;
    }

    /**
     * The time this session was started.
     * @returns {Date}
     */
    get startTime() {
        return this._startTime;
    }

    /**
     * The underlying WebSocket connection object.
     * @returns {WebSocket}
     */
    get connection() {
        return this._webSocket;
    }

    /**
     * Internal method to handle an incoming event from the client.
     * It parses the event and emits it for the developer to handle.
     * @param {string} eventJson The raw JSON string from the client.
     * @private
     */
    _handleEvent(eventJson) {
        try {
            const event = JSON.parse(eventJson);
            
            // Emit a generic 'event' for all messages.
            this.emit('event', event);

            // Emit a specific 'violation' event if the status is 'violation'.
            if (event.status === 'violation') {
                this.emit('violation', event);
            }
        } catch (error) {
            console.error(`[Session ${this.sessionId}] Error parsing event JSON:`, error);
        }
    }

    /**
     * Internal method to signal that the client has disconnected.
     * @private
     */
    _handleDisconnect() {
        this.emit('disconnect');
    }
}

module.exports = ProctorSession;
