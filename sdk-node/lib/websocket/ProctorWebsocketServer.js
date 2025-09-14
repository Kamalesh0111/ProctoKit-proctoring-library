const { WebSocketServer } = require('ws');
const url = require('url');

/**
 * The internal WebSocket server that listens for connections from student clients.
 */
class ProctorWebsocketServer {
    constructor(config, sessionManager) {
        this.config = config;
        this.sessionManager = sessionManager;
        this.wss = null;
    }

    start() {
        this.wss = new WebSocketServer({
            port: this.config.port,
            host: this.config.host
        });
        
        this.wss.on('listening', () => {
            console.log(`Proctoring WebSocket server started on ws://${this.config.host}:${this.config.port}`);
        });

        this.wss.on('connection', (ws, req) => {
            // The session ID is expected as the last part of the URL path.
            // e.g., ws://localhost:8080/exam123-student456
            const sessionId = url.parse(req.url).pathname.substring(1);

            this.sessionManager.startSession(ws, sessionId);

            ws.on('message', (message) => {
                const session = this.sessionManager.getSession(ws);
                if (session) {
                    session._handleEvent(message.toString());
                }
            });

            ws.on('close', () => {
                this.sessionManager.endSession(ws);
            });
            
            ws.on('error', (error) => {
                console.error('A WebSocket error occurred:', error);
                this.sessionManager.endSession(ws);
            });
        });
    }

    stop() {
        if (this.wss) {
            this.wss.close(() => {
                console.log('Proctoring WebSocket server stopped.');
            });
        }
    }
}

module.exports = ProctorWebsocketServer;
