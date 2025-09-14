package com.proctor.sdk;

import com.proctor.sdk.config.ProctorConfig;
import java.util.HashMap;
import java.util.Map;

/**
 * A local server for performing real-time, end-to-end testing of the entire proctoring system.
 * This server will start and then print the command needed to launch the actual java-client.
 */
public class LocalTestServer {

    public static void main(String[] args) {
        System.out.println("--- Starting Local End-to-End Test Server ---");

        // 1. Configure the SDK.
        Map<String, String> dummyInstallerLinks = new HashMap<>();
        dummyInstallerLinks.put("windows", "http://localhost/dummy-installer.exe");
        ProctorConfig config = new ProctorConfig(8080, "localhost", dummyInstallerLinks);

        // 2. Initialize the SDK.
        ProctorSDK sdk = new ProctorSDK(config);

        // 3. Set up event handlers to display the results of your tests.
        sdk.onSession(session -> {
            System.out.println("\n[SERVER] ✅ Client connected! Session ID: " + session.getSessionId());
            System.out.println("[SERVER] Ready to receive real-time events. Start performing actions to test.");

            session.onActivity(event -> {
                System.out.println("[SERVER] 🔵 Received Activity: " + event.toString());
            });

            session.onViolation(violationEvent -> {
                System.err.println("[SERVER] ❗ VIOLATION DETECTED: " + violationEvent.toString());
            });

            session.onDisconnect(() -> {
                System.out.println("[SERVER] ❌ Client disconnected. Session ID: " + session.getSessionId());
            });
        });

        // 4. Start the server.
        sdk.start();
        System.out.println("[SERVER] Server is listening on ws://localhost:8080");

        // 5. Generate and print the exact command needed to launch the java-client.
        String testSessionId = "real-time-test-session";
        String websocketUrl = "ws://localhost:8080/" + testSessionId;
        // Adjust the path for your OS if necessary. This is for Windows cmd/powershell.
        String jarPath = "..\\java-client\\target\\java-client-1.0-SNAPSHOT-jar-with-dependencies.jar";
        
        System.out.println("\n--- TO START THE CLIENT, RUN THE FOLLOWING COMMAND IN A NEW TERMINAL: ---");
        System.out.println("java -jar " + jarPath + " " + websocketUrl);
        System.out.println("----------------------------------------------------------------------------\n");
    }
}