package com.pidev.utils;

import io.github.cdimascio.dotenv.Dotenv;

public class EnvConfig {
    private static final Dotenv dotenv;

    static {
        dotenv = Dotenv.configure()
                .directory("./") // Explicitly look in the current directory
                .ignoreIfMissing()
                .load();
        
        if (dotenv.get("GOOGLE_CLIENT_ID") == null) {
            System.err.println("CRITICAL ERROR: .env file NOT FOUND or GOOGLE_CLIENT_ID is missing!");
        } else {
            System.out.println("Environment variables loaded successfully from .env");
        }
    }

    public static String get(String key) {
        return dotenv.get(key);
    }

    public static String get(String key, String defaultValue) {
        String value = dotenv.get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }
}
