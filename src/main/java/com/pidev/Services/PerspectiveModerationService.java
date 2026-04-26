package com.pidev.Services;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PerspectiveModerationService {
    private static final String BASE_URL = "https://commentanalyzer.googleapis.com/v1alpha1/comments:analyze";
    private static final String DEFAULT_API_KEY = "PASTE_YOUR_PERSPECTIVE_API_KEY_HERE";
    private static final String[] DEFAULT_ATTRIBUTES = new String[]{
            "TOXICITY",
            "SEVERE_TOXICITY",
            "INSULT",
            "THREAT",
            "IDENTITY_ATTACK",
            "PROFANITY"
    };
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(10);

    private final HttpClient httpClient;
    private final boolean enabled;
    private final boolean failOpen;
    private final boolean debug;
    private final String apiKey;
    private final List<String> attributes;
    private final double blockThreshold;
    private final Duration requestTimeout;

    public PerspectiveModerationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.enabled = parseBoolean(readFirstNonBlankEnv("true", "PERSPECTIVE_ENABLED"));
        this.failOpen = parseBoolean(readFirstNonBlankEnv("false", "PERSPECTIVE_FAIL_OPEN"));
        this.debug = parseBoolean(readFirstNonBlankEnv("false", "PERSPECTIVE_DEBUG"));
        this.apiKey = readFirstNonBlankEnv(DEFAULT_API_KEY, "PERSPECTIVE_API_KEY", "GOOGLE_PERSPECTIVE_API_KEY");
        this.attributes = parseAttributes(readFirstNonBlankEnv("", "PERSPECTIVE_ATTRIBUTES"));
        this.blockThreshold = parseDouble(readFirstNonBlankEnv("0.20", "PERSPECTIVE_THRESHOLD"), 0.20);
        this.requestTimeout = Duration.ofSeconds(parseLong(readFirstNonBlankEnv("10", "PERSPECTIVE_TIMEOUT"), DEFAULT_REQUEST_TIMEOUT.getSeconds()));
    }

    public ModerationDecision moderateText(String title, String description) throws ModerationException {
        if (!enabled) {
            return ModerationDecision.allowed("Perspective moderation disabled.");
        }

        String text = normalize(title) + "\n\n" + normalize(description);
        if (text.isBlank()) {
            return ModerationDecision.allowed("No text to moderate.");
        }
        if (containsSexualHint(text)) {
            return ModerationDecision.blocked("Blocked by strict sexual-content policy.");
        }

        if (apiKey == null || apiKey.isBlank() || "PASTE_YOUR_PERSPECTIVE_API_KEY_HERE".equals(apiKey)) {
            if (failOpen) {
                return ModerationDecision.allowed("Perspective API key missing; fail-open enabled.");
            }
            throw new ModerationException("Perspective API key missing. Edit DEFAULT_API_KEY in PerspectiveModerationService or set PERSPECTIVE_API_KEY.");
        }

        try {
            String url = BASE_URL + "?key=" + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
            HttpResponse<String> response = sendPerspectiveRequest(url, text, attributes);
            ModerationDecision decision = interpretResponse(response.body());
            debug("Perspective decision allowed=" + decision.allowed() + " | reason=" + decision.reason());
            return decision;
        } catch (ModerationException e) {
            if (failOpen) {
                return ModerationDecision.allowed("Perspective unavailable; fail-open enabled: " + e.getMessage());
            }
            throw e;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (failOpen) {
                return ModerationDecision.allowed("Perspective interrupted; fail-open enabled.");
            }
            throw new ModerationException("Perspective request interrupted.", e);
        } catch (IOException e) {
            if (failOpen) {
                return ModerationDecision.allowed("Perspective unreachable; fail-open enabled.");
            }
            throw new ModerationException("Perspective unreachable: " + e.getMessage(), e);
        } catch (Exception e) {
            if (failOpen) {
                return ModerationDecision.allowed("Perspective error; fail-open enabled.");
            }
            throw new ModerationException("Perspective processing error: " + e.getMessage(), e);
        }
    }

    private ModerationDecision interpretResponse(String body) {
        if (body == null || body.isBlank()) {
            return failOpen
                    ? ModerationDecision.allowed("Empty Perspective response; fail-open enabled.")
                    : ModerationDecision.blocked("Empty Perspective response; blocked for safety.");
        }

        StringBuilder blockedReasons = new StringBuilder();
        StringJoiner scoreSummary = new StringJoiner(" | ");
        int parsedCount = 0;

        for (String attribute : attributes) {
            Double score = extractAttributeScore(body, attribute);
            if (score == null) {
                continue;
            }
            parsedCount++;
            scoreSummary.add(attribute + "=" + score);
            if (score >= blockThreshold) {
                if (!blockedReasons.isEmpty()) {
                    blockedReasons.append(" ");
                }
                blockedReasons.append(attribute)
                        .append(" score ")
                        .append(score)
                        .append(" >= ")
                        .append(blockThreshold)
                        .append(".");
            }
        }

        if (parsedCount == 0) {
            return failOpen
                    ? ModerationDecision.allowed("Perspective scores not found; fail-open enabled.")
                    : ModerationDecision.blocked("Perspective scores missing; blocked for safety.");
        }

        if (!blockedReasons.isEmpty()) {
            return ModerationDecision.blocked("Blocked by strict policy. " + blockedReasons);
        }

        return ModerationDecision.allowed("Scores below strict threshold " + blockThreshold + ". " + scoreSummary);
    }

    private HttpResponse<String> sendPerspectiveRequest(String url, String text, List<String> requestedAttributes)
            throws IOException, InterruptedException, ModerationException {
        List<String> active = new ArrayList<>(requestedAttributes);
        ModerationException lastError = null;

        while (!active.isEmpty()) {
            String payload = buildRequestPayload(text, active);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return response;
            }

            String responseBody = response.body();
            if (response.statusCode() == 400) {
                String unsupportedAttribute = extractUnsupportedAttribute(responseBody);
                if (unsupportedAttribute != null && active.removeIf(a -> a.equalsIgnoreCase(unsupportedAttribute))) {
                    debug("Perspective rejected attribute " + unsupportedAttribute + ", retrying without it.");
                    lastError = new ModerationException("Perspective rejected attribute: " + unsupportedAttribute);
                    continue;
                }
            }

            throw new ModerationException("Perspective API returned HTTP " + response.statusCode() + bodySnippet(responseBody));
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new ModerationException("No supported Perspective attributes left.");
    }

    private String buildRequestPayload(String text, List<String> requestedAttributes) {
        String escaped = jsonEscape(text);
        String requestedAttributesJson = buildRequestedAttributesJson(requestedAttributes);
        return "{"
                + "\"comment\":{\"text\":\"" + escaped + "\"},"
                + "\"languages\":[\"en\",\"fr\",\"ar\"],"
                + "\"requestedAttributes\":{" + requestedAttributesJson + "},"
                + "\"doNotStore\":true"
                + "}";
    }

    private String buildRequestedAttributesJson(List<String> requestedAttributes) {
        StringJoiner joiner = new StringJoiner(",");
        for (String attribute : requestedAttributes) {
            joiner.add("\"" + attribute + "\":{}");
        }
        return joiner.toString();
    }

    private String extractUnsupportedAttribute(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return null;
        }
        Pattern p = Pattern.compile("Attribute\\s+([A-Z_]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(responseBody);
        if (!m.find()) {
            return null;
        }
        return m.group(1).toUpperCase(Locale.ROOT);
    }

    private boolean containsSexualHint(String text) {
        String lower = text.toLowerCase(Locale.ROOT);
        String[] sexualTerms = new String[]{
                "sex", "sexual", "nude", "naked", "porn", "xxx", "fuck", "dick", "pussy", "boobs",
                "suck", "blowjob", "anal", "cum", "horny", "rape", "molest", "incest"
        };
        for (String term : sexualTerms) {
            if (lower.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private Double extractAttributeScore(String body, String attribute) {
        Pattern scorePattern = Pattern.compile(
                "\"" + Pattern.quote(attribute) + "\"\\s*:\\s*\\{[\\s\\S]*?\"summaryScore\"\\s*:\\s*\\{[\\s\\S]*?\"value\"\\s*:\\s*([0-9]*\\.?[0-9]+)",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = scorePattern.matcher(body);
        if (!matcher.find()) {
            return null;
        }
        return parseDouble(matcher.group(1), 0.0);
    }

    private List<String> parseAttributes(String csv) {
        List<String> parsed = new ArrayList<>();
        if (csv != null && !csv.isBlank()) {
            String[] parts = csv.split(",");
            for (String part : parts) {
                String value = part == null ? "" : part.trim().toUpperCase(Locale.ROOT);
                if (!value.isBlank() && !parsed.contains(value)) {
                    parsed.add(value);
                }
            }
        }
        if (parsed.isEmpty()) {
            for (String attribute : DEFAULT_ATTRIBUTES) {
                parsed.add(attribute);
            }
        }
        return parsed;
    }

    private String jsonEscape(String value) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\b' -> sb.append("\\b");
                case '\f' -> sb.append("\\f");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) {
                        sb.append(String.format("\\u%04x", (int) c));
                    } else {
                        sb.append(c);
                    }
                }
            }
        }
        return sb.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String bodySnippet(String body) {
        if (body == null) {
            return "";
        }
        String normalized = body.trim().replace('\n', ' ').replace('\r', ' ');
        if (normalized.length() <= 220) {
            return " | body: " + normalized;
        }
        return " | body: " + normalized.substring(0, 220) + "...";
    }

    private String readFirstNonBlankEnv(String defaultValue, String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return defaultValue;
    }

    private double parseDouble(String raw, double fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Double.parseDouble(raw.trim());
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private long parseLong(String raw, long fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            long value = Long.parseLong(raw.trim());
            return value > 0 ? value : fallback;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    private boolean parseBoolean(String raw) {
        if (raw == null) {
            return false;
        }
        String normalized = raw.trim().toLowerCase(Locale.ROOT);
        return "1".equals(normalized) || "true".equals(normalized) || "yes".equals(normalized) || "on".equals(normalized);
    }

    private void debug(String message) {
        if (debug) {
            System.out.println("[PerspectiveModerationService] " + message);
        }
    }

    public record ModerationDecision(boolean allowed, String reason) {
        public static ModerationDecision allowed(String reason) {
            return new ModerationDecision(true, reason);
        }

        public static ModerationDecision blocked(String reason) {
            return new ModerationDecision(false, reason);
        }
    }

    public static class ModerationException extends Exception {
        public ModerationException(String message) {
            super(message);
        }

        public ModerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
