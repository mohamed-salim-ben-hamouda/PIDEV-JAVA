package com.pidev.Services;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FightModerationService {
    private static final String DEFAULT_URL = "http://127.0.0.1:8010/moderate-image";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern SCORE_PATTERN = Pattern.compile("\"(?:score|confidence|fight_score|violence_score|fight_probability|violence_probability|probability|unsafe_score|nsfw_score)\"\\s*:\\s*([0-9]*\\.?[0-9]+)");
    private static final Pattern REASON_PATTERN = Pattern.compile("\"(?:reason|label|message)\"\\s*:\\s*\"([^\"]*)\"");
    private static final Pattern DECISION_PATTERN = Pattern.compile("\"(?:decision|verdict|result|status)\"\\s*:\\s*\"([^\"]+)\"");

    private final HttpClient httpClient;
    private final String moderationUrl;
    private final boolean enabled;
    private final boolean failOpen;
    private final boolean debug;
    private final double blockScoreThreshold;
    private final Duration requestTimeout;
    private final long maxFileBytes;

    public FightModerationService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.moderationUrl = readFirstNonBlankEnv(DEFAULT_URL, "APP_FIGHT_MODERATION_URL", "FIGHT_MODERATION_URL");
        this.enabled = parseBoolean(readFirstNonBlankEnv("true", "FIGHT_MODERATION_ENABLED"));
        this.failOpen = parseBoolean(readFirstNonBlankEnv("false", "FIGHT_MODERATION_FAIL_OPEN"));
        this.debug = parseBoolean(readFirstNonBlankEnv("false", "FIGHT_MODERATION_DEBUG"));
        this.blockScoreThreshold = parseDouble(readFirstNonBlankEnv("0.80", "FIGHT_MODERATION_CONFIDENCE", "FIGHT_MODERATION_BLOCK_THRESHOLD"), 0.80);
        this.requestTimeout = Duration.ofSeconds(parseLong(readFirstNonBlankEnv("8", "FIGHT_MODERATION_TIMEOUT"), DEFAULT_REQUEST_TIMEOUT.getSeconds()));
        this.maxFileBytes = parseLong(readFirstNonBlankEnv("10485760", "FIGHT_MODERATION_MAX_FILE_BYTES"), 10 * 1024 * 1024);
    }

    public ModerationDecision moderatePost(String title, String description, String attachmentPath) throws ModerationException {
        if (!enabled) {
            return ModerationDecision.allowed("Moderation disabled.");
        }

        Path imagePath = resolveLocalFile(attachmentPath);
        if (imagePath == null) {
            return ModerationDecision.allowed("No local image attachment; image moderation skipped.");
        }

        try {
            long fileSize = Files.size(imagePath);
            if (fileSize <= 0) {
                return ModerationDecision.allowed("Attachment is empty; image moderation skipped.");
            }
            if (fileSize > maxFileBytes) {
                if (failOpen) {
                    return ModerationDecision.allowed("Attachment too large for moderation; fail-open enabled.");
                }
                throw new ModerationException("Attachment too large for moderation.");
            }

            String responseBody = callModerationApi(imagePath);
            ModerationDecision decision = interpretResponse(responseBody);
            debug("Decision allowed=" + decision.allowed() + " | reason=" + decision.reason());
            return decision;
        } catch (ModerationException e) {
            if (failOpen) {
                return ModerationDecision.allowed("Moderation unavailable, fail-open enabled: " + e.getMessage());
            }
            throw e;
        } catch (Exception e) {
            if (failOpen) {
                return ModerationDecision.allowed("Moderation processing error; fail-open enabled.");
            }
            throw new ModerationException("Moderation processing error: " + e.getMessage(), e);
        }
    }

    private String callModerationApi(Path imagePath) throws ModerationException {
        MultipartBody multipart;
        try {
            multipart = buildMultipartBody(imagePath);
        } catch (IOException e) {
            throw new ModerationException("Could not read attachment for moderation: " + e.getMessage(), e);
        }

        ModerationException lastFailure = null;
        for (String candidateUrl : buildEndpointCandidates(moderationUrl)) {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(candidateUrl))
                        .timeout(requestTimeout)
                        .header("Content-Type", "multipart/form-data; boundary=" + multipart.boundary())
                        .POST(HttpRequest.BodyPublishers.ofByteArray(multipart.body()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                int status = response.statusCode();
                if (status == 404) {
                    lastFailure = new ModerationException("Moderation endpoint not found at " + candidateUrl);
                    continue;
                }
                if (status < 200 || status >= 300) {
                    throw new ModerationException("Moderation API returned HTTP " + status + " at " + candidateUrl + bodySnippet(response.body()));
                }
                debug("HTTP " + status + " from " + candidateUrl + bodySnippet(response.body()));
                return response.body();
            } catch (IllegalArgumentException e) {
                throw new ModerationException("Invalid moderation URL: " + candidateUrl, e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ModerationException("Moderation request interrupted.", e);
            } catch (IOException e) {
                lastFailure = new ModerationException("Moderation API unreachable at " + candidateUrl + ": " + e.getMessage(), e);
            }
        }

        if (lastFailure != null) {
            throw lastFailure;
        }
        throw new ModerationException("No moderation endpoint candidates available.");
    }

    private void debug(String message) {
        if (debug) {
            System.out.println("[FightModerationService] " + message);
        }
    }

    private List<String> buildEndpointCandidates(String baseUrl) {
        List<String> candidates = new ArrayList<>();
        if (baseUrl == null || baseUrl.isBlank()) {
            candidates.add(DEFAULT_URL);
            return candidates;
        }

        String trimmed = baseUrl.trim();
        candidates.add(trimmed);

        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (lower.endsWith("/moderate")) {
            candidates.add(trimmed.substring(0, trimmed.length() - "/moderate".length()) + "/moderate-image");
        } else if (lower.endsWith("/moderate-image")) {
            candidates.add(trimmed.substring(0, trimmed.length() - "/moderate-image".length()) + "/moderate");
        } else if (!lower.contains("/moderate")) {
            String withoutTrailingSlash = trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
            candidates.add(withoutTrailingSlash + "/moderate-image");
            candidates.add(withoutTrailingSlash + "/moderate");
        }

        return candidates.stream().distinct().toList();
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

    private ModerationDecision interpretResponse(String body) {
        if (body == null || body.isBlank()) {
            return failOpen
                    ? ModerationDecision.allowed("Empty moderation response.")
                    : ModerationDecision.blocked("Empty moderation response; blocked for safety.");
        }

        String lower = body.toLowerCase(Locale.ROOT);
        if (containsAny(lower,
                "\"blocked\":true", "\"block\":true",
                "\"is_allowed\":false", "\"allow\":false", "\"safe\":false",
                "\"unsafe\":true", "\"fight\":true", "\"violence\":true",
                "\"is_fight\":true", "\"contains_fight\":true")) {
            return ModerationDecision.blocked(extractReason(body, "Blocked by moderation model."));
        }
        if (containsAny(lower,
                "\"blocked\":false", "\"block\":false",
                "\"is_allowed\":true", "\"allow\":true", "\"safe\":true",
                "\"unsafe\":false", "\"fight\":false", "\"violence\":false",
                "\"is_fight\":false", "\"contains_fight\":false")) {
            return ModerationDecision.allowed(extractReason(body, "Allowed by moderation model."));
        }

        String decision = extractDecisionKeyword(lower);
        if (decision != null) {
            if (containsAny(decision, "block", "reject", "unsafe", "fight", "violence")) {
                return ModerationDecision.blocked(extractReason(body, "Blocked by moderation decision."));
            }
            if (containsAny(decision, "allow", "safe", "ok", "approved", "clean")) {
                return ModerationDecision.allowed(extractReason(body, "Allowed by moderation decision."));
            }
        }

        Matcher scoreMatcher = SCORE_PATTERN.matcher(lower);
        if (scoreMatcher.find()) {
            double score = parseDouble(scoreMatcher.group(1), 0.0);
            if (score >= blockScoreThreshold) {
                return ModerationDecision.blocked("Fight score " + score + " is above threshold " + blockScoreThreshold + ".");
            }
            return ModerationDecision.allowed("Fight score " + score + " is below threshold.");
        }

        return failOpen
                ? ModerationDecision.allowed("Unknown moderation format; fail-open enabled.")
                : ModerationDecision.blocked("Unknown moderation response format; blocked for safety.");
    }

    private String extractReason(String body, String fallback) {
        Matcher reasonMatcher = REASON_PATTERN.matcher(body);
        if (reasonMatcher.find()) {
            String reason = reasonMatcher.group(1);
            if (reason != null && !reason.isBlank()) {
                return reason;
            }
        }
        return fallback;
    }

    private String extractDecisionKeyword(String bodyLowerCase) {
        Matcher matcher = DECISION_PATTERN.matcher(bodyLowerCase);
        if (!matcher.find()) {
            return null;
        }
        String value = matcher.group(1);
        return value == null ? null : value.trim();
    }

    private boolean containsAny(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private MultipartBody buildMultipartBody(Path imagePath) throws IOException {
        String boundary = "----PidevBoundary" + UUID.randomUUID();
        String fileName = imagePath.getFileName().toString();
        String mimeType = Files.probeContentType(imagePath);
        if (mimeType == null || mimeType.isBlank()) {
            mimeType = "application/octet-stream";
        }

        byte[] fileBytes = Files.readAllBytes(imagePath);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        output.write(("--" + boundary + "\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + fileName + "\"\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
        output.write(fileBytes);
        output.write("\r\n".getBytes(StandardCharsets.UTF_8));
        output.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return new MultipartBody(boundary, output.toByteArray());
    }

    private Path resolveLocalFile(String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return null;
        }
        try {
            Path direct = Paths.get(rawPath.trim()).normalize();
            if (Files.exists(direct) && Files.isRegularFile(direct)) {
                return direct;
            }

            Path relative = Paths.get(System.getProperty("user.dir"), rawPath.trim()).normalize();
            if (Files.exists(relative) && Files.isRegularFile(relative)) {
                return relative;
            }
        } catch (InvalidPathException ignored) {
            return null;
        } catch (Exception ignored) {
            return null;
        }
        return null;
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

    private record MultipartBody(String boundary, byte[] body) {
    }
}
