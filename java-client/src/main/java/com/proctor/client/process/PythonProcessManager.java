package com.proctor.client.process;

import com.sun.jna.Platform;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Manages the lifecycle of the Python vision agent subprocess.
 * This version includes a more graceful shutdown procedure.
 */
public class PythonProcessManager implements Runnable {

    private final Consumer<String> eventConsumer;
    private Process pythonProcess;
    private static final String EXECUTABLE_NAME = "proctor_agent";

    public PythonProcessManager(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
    }

    @Override
    public void run() {
        try {
            String executablePath = extractExecutable();
            File executableFile = new File(executablePath);

            if (!executableFile.exists() || !executableFile.canExecute()) {
                throw new IOException("Python executable is not valid or cannot be executed.");
            }

            System.out.println("[INFO] Starting Python agent...");
            ProcessBuilder pb = new ProcessBuilder(executablePath);
            pythonProcess = pb.start();

            // Thread to read standard output
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        eventConsumer.accept(line);
                    }
                } catch (IOException e) { /* Process likely terminated */ }
            }).start();
            
            // Thread to read error output for debugging
            new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(pythonProcess.getErrorStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.err.println("[PYTHON-ERROR] " + line);
                    }
                } catch (IOException e) { /* Process likely terminated */ }
            }).start();

            System.out.println("[INFO] Python agent started successfully.");
            int exitCode = pythonProcess.waitFor();
            System.out.println("[INFO] Python agent exited with code: " + exitCode);

        } catch (IOException | InterruptedException e) {
            System.err.println("[ERROR] Failed to start or run Python agent: " + e.getMessage());
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Attempts to shut down the Python process gracefully, with a forceful fallback.
     */
    public void stop() {
        if (pythonProcess != null && pythonProcess.isAlive()) {
            System.out.println("[INFO] Attempting graceful shutdown of Python agent...");

            // 1. Destroy the process. This sends a standard termination signal.
            pythonProcess.destroy();
            
            try {
                // 2. Wait for a short period for the process to exit.
                // The Python script's 'finally' block should now have time to run.
                if (pythonProcess.waitFor(3, TimeUnit.SECONDS)) {
                    System.out.println("[INFO] Python agent shut down gracefully.");
                } else {
                    // 3. If it doesn't exit, force kill it. This is our safety net.
                    System.err.println("[WARN] Python agent did not shut down in time. Forcing termination.");
                    pythonProcess.destroyForcibly();
                }
            } catch (InterruptedException e) {
                System.err.println("[ERROR] Interrupted during shutdown. Forcing termination.");
                pythonProcess.destroyForcibly();
                Thread.currentThread().interrupt();
            }
        }
    }

    private String extractExecutable() throws IOException {
        String resourceName = Platform.isWindows() ? EXECUTABLE_NAME + ".exe" : EXECUTABLE_NAME;
        String resourcePath = "bin/" + resourceName;
        
        InputStream in = PythonProcessManager.class.getClassLoader().getResourceAsStream(resourcePath);
        
        if (in == null) {
            throw new RuntimeException("Cannot find " + resourceName + " in JAR resources. Path was: " + resourcePath);
        }

        File tempFile = File.createTempFile(EXECUTABLE_NAME, Platform.isWindows() ? ".exe" : "");
        tempFile.deleteOnExit();
        tempFile.setExecutable(true);

        try (FileOutputStream out = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        } finally {
            in.close();
        }

        return tempFile.getAbsolutePath();
    }
}

