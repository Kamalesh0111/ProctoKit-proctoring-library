package com.proctor.client.monitoring;

import org.json.JSONObject;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * Monitors the system for newly started processes.
 * It maintains a baseline of running processes and checks for any new additions
 * that are on a "forbidden" list.
 */
public class ProcessMonitor implements Runnable {

    private final Consumer<String> eventConsumer;
    private Set<Long> baselineProcessIds;

    // A simple, hardcoded list of forbidden process names (case-insensitive).
    // In a real application, this would be configurable from the server.
    private static final Set<String> FORBIDDEN_PROCESSES = new HashSet<>() {{
        add("discord");
        add("slack");
        add("teams");
        add("zoom");
        add("skype");
        add("obs");
        add("anydesk");
        add("teamviewer");
        add("virtualbox");
        add("vmware");
    }};

    public ProcessMonitor(Consumer<String> eventConsumer) {
        this.eventConsumer = eventConsumer;
        // Establish the initial set of running processes
        this.baselineProcessIds = getRunningProcessIds();
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Set<Long> currentProcessIds = getRunningProcessIds();
                
                // Find processes that are in the current set but not in the baseline set
                Set<Long> newProcessIds = new HashSet<>(currentProcessIds);
                newProcessIds.removeAll(baselineProcessIds);

                if (!newProcessIds.isEmpty()) {
                    checkForForbiddenProcesses(newProcessIds);
                    // Add the new processes to the baseline to avoid repeated alerts
                    baselineProcessIds.addAll(newProcessIds);
                }

                Thread.sleep(5000); // Check every 5 seconds
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.err.println("ProcessMonitor was interrupted.");
                break;
            }
        }
    }

    private void checkForForbiddenProcesses(Set<Long> newProcessIds) {
        for (long pid : newProcessIds) {
            ProcessHandle.of(pid).ifPresent(processHandle -> {
                String processName = processHandle.info().command()
                    .map(cmd -> cmd.substring(cmd.lastIndexOf('/') + 1))
                    .map(cmd -> cmd.substring(cmd.lastIndexOf('\\') + 1)) // for Windows paths
                    .map(String::toLowerCase)
                    .orElse("");

                // Check if the process name contains any of the forbidden keywords
                FORBIDDEN_PROCESSES.stream()
                    .filter(processName::contains)
                    .findFirst()
                    .ifPresent(forbiddenKeyword -> {
                        JSONObject event = new JSONObject();
                        event.put("timestamp", System.currentTimeMillis());
                        event.put("activity", "processStarted");
                        event.put("status", "violation");

                        JSONObject details = new JSONObject();
                        details.put("processName", processName);
                        details.put("pid", pid);
                        details.put("message", "A forbidden application was launched: " + forbiddenKeyword);
                        event.put("details", details);
                        
                        eventConsumer.accept(event.toString());
                    });
            });
        }
    }

    private Set<Long> getRunningProcessIds() {
        return ProcessHandle.allProcesses()
                .map(ProcessHandle::pid)
                .collect(Collectors.toSet());
    }
}
