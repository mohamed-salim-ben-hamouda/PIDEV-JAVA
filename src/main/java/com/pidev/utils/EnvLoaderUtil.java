package com.pidev.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public final class EnvLoaderUtil {
    private EnvLoaderUtil() {
    }

    public static void loadDotEnvFromProjectRoot() {
        Path envPath = Paths.get(System.getProperty("user.dir"), ".env");
        if (!Files.isRegularFile(envPath)) {
            return;
        }

        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String rawLine : lines) {
                String line = rawLine == null ? "" : rawLine.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("export ")) {
                    line = line.substring("export ".length()).trim();
                }

                int separatorIndex = line.indexOf('=');
                if (separatorIndex <= 0) {
                    continue;
                }

                String key = line.substring(0, separatorIndex).trim();
                String value = stripQuotes(line.substring(separatorIndex + 1).trim());
                if (key.isEmpty() || value.isEmpty()) {
                    continue;
                }

                String existing = System.getProperty(key);
                if (existing == null || existing.isBlank()) {
                    System.setProperty(key, value);
                }
            }
        } catch (IOException e) {
            System.err.println("Could not load .env: " + e.getMessage());
        }
    }

    private static String stripQuotes(String value) {
        if (value.length() >= 2) {
            boolean doubleQuoted = value.startsWith("\"") && value.endsWith("\"");
            boolean singleQuoted = value.startsWith("'") && value.endsWith("'");
            if (doubleQuoted || singleQuoted) {
                return value.substring(1, value.length() - 1);
            }
        }
        return value;
    }
}
