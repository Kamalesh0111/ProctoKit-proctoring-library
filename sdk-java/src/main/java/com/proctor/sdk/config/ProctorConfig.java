package com.proctor.sdk.config;

import java.util.Map;

/**
 * Configuration class for the ProctorSDK.
 * This updated version includes a map to hold the download URLs for the client installers.
 */
public class ProctorConfig {
    private final int port;
    private final String host;
    private final Map<String, String> installerUrls;

    /**
     * @param port The port for the WebSocket server to listen on.
     * @param host The host to bind to (e.g., '0.0.0.0').
     * @param installerUrls A map where the key is the OS ('windows', 'macos') and the value is the public download URL.
     *
     * @example
     * Map<String, String> urls = new HashMap<>()
     * urls.put("windows", "https://github.com/your-repo/releases/download/v1.0/proctor-client-setup.exe");
     * urls.put("macos", "https://github.com/your-repo/releases/download/v1.0/ProctorClient-1.0.dmg");
     * new ProctorConfig(8080, "0.0.0.0", urls);
     */
    public ProctorConfig(int port, String host, Map<String, String> installerUrls) {
        if (port <= 0) {
            throw new IllegalArgumentException("Port must be a positive number.");
        }
        if (installerUrls == null || installerUrls.isEmpty()) {
            throw new IllegalArgumentException("Installer URLs must be provided.");
        }
        this.port = port;
        this.host = host;
        this.installerUrls = installerUrls;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public Map<String, String> getInstallerUrls() {
        return installerUrls;
    }
}

