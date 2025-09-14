package com.proctor.client.ui;

/**
 * A simple utility class for displaying formatted messages to the user in the console.
 * This version includes the missing 'showWarning' method.
 */
public class ConsoleUI {

    // ANSI escape codes for coloring text in the console.
    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_YELLOW = "\u001B[33m"; // For warnings
    public static final String ANSI_CYAN = "\u001B[36m";

    public static void printStartupBanner() {
        System.out.println(ANSI_CYAN + "========================================" + ANSI_RESET);
        System.out.println(ANSI_CYAN + "   Proctoring Client Application v1.0   " + ANSI_RESET);
        System.out.println(ANSI_CYAN + "========================================" + ANSI_RESET);
        System.out.println();
    }

    public static void showMessage(String message) {
        System.out.println(ANSI_GREEN + "[INFO] " + message + ANSI_RESET);
    }

    public static void showError(String message) {
        System.err.println(ANSI_RED + "[ERROR] " + message + ANSI_RESET);
    }

    /**
     * (FIX) This was the missing method. It displays a warning message to the console.
     * Warnings are for non-critical issues that the user should be aware of.
     * @param message The warning message to display.
     */
    public static void showWarning(String message) {
        System.out.println(ANSI_YELLOW + "[WARNING] " + message + ANSI_RESET);
    }
}

