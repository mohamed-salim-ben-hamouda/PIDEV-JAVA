package com.pidev.utils;

import java.nio.file.Files;
import java.nio.file.Path;

public final class EnvConfig {

    private static final boolean ENV_FILE_PRESENT;

    static {
        ENV_FILE_PRESENT = Files.isRegularFile(Path.of(".env"));

        String googleClientId = get("GOOGLE_CLIENT_ID");
        if (!ENV_FILE_PRESENT) {
            System.err.println("Warning: .env file not found in project root. Falling back to system environment variables.");
        } else if (googleClientId == null || googleClientId.isBlank()) {
            System.err.println("Warning: .env loaded but GOOGLE_CLIENT_ID is missing.");
        } else {
            System.out.println("Environment variables loaded successfully from .env");
        }
    }

    private EnvConfig() {
    }

    public static String get(String key) {
        return EnvLoader.get(key, null);
    }

    public static String get(String key, String defaultValue) {
        String value = EnvLoader.get(key, defaultValue);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
