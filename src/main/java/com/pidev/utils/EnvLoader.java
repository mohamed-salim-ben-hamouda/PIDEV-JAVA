package com.pidev.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class EnvLoader {

    private static final Map<String, String> DOTENV = loadDotEnv();

    private EnvLoader() {
    }

    public static String get(String key) {
        return get(key, "");
    }

    public static String get(String key, String defaultValue) {
        String systemValue = System.getenv(key);
        if (systemValue != null && !systemValue.isBlank()) {
            return systemValue.trim();
        }

        String dotenvValue = DOTENV.get(key);
        if (dotenvValue != null && !dotenvValue.isBlank()) {
            return dotenvValue.trim();
        }

        return defaultValue;
    }

    private static Map<String, String> loadDotEnv() {
        Map<String, String> values = new HashMap<>();
        Path envPath = Path.of(".env");

        if (!Files.isRegularFile(envPath)) {
            return values;
        }

        try (BufferedReader reader = Files.newBufferedReader(envPath, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                parseLine(line, values);
            }
        } catch (IOException e) {
            System.err.println("Unable to read .env file: " + e.getMessage());
        }

        return values;
    }

    private static void parseLine(String line, Map<String, String> values) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
            return;
        }

        int separator = trimmed.indexOf('=');
        if (separator <= 0) {
            return;
        }

        String key = trimmed.substring(0, separator).trim();
        String value = trimmed.substring(separator + 1).trim();

        if ((value.startsWith("\"") && value.endsWith("\""))
                || (value.startsWith("'") && value.endsWith("'"))) {
            value = value.substring(1, value.length() - 1);
        }

        values.put(key, value);
    }
}
