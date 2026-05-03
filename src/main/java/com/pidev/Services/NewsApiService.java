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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class NewsApiService {
    private static final String DEFAULT_API_KEY = "95d0a70673e94c2fbf93dd83708b581b";
    private static final String BASE_URL = "https://newsapi.org/v2/top-headlines";
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(4);
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);
    private static final Pattern TITLE_PATTERN = Pattern.compile("\"title\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
    private static final Pattern URL_PATTERN = Pattern.compile("\"url\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");

    private final HttpClient httpClient;
    private final String apiKey;
    private final String language;
    private final String category;
    private final String country;

    public NewsApiService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .build();
        this.apiKey = readFirstNonBlankEnv(DEFAULT_API_KEY, "NEWS_API_KEY");
        this.language = readFirstNonBlankEnv("en", "NEWS_API_LANGUAGE").toLowerCase(Locale.ROOT);
        this.category = readFirstNonBlankEnv("technology", "NEWS_API_CATEGORY").toLowerCase(Locale.ROOT);
        this.country = readFirstNonBlankEnv("", "NEWS_API_COUNTRY").toLowerCase(Locale.ROOT);
    }

    public List<String> fetchTopHeadlines(int limit) throws IOException, InterruptedException {
        return fetchTopHeadlinesWithLinks(limit).stream()
                .map(Headline::title)
                .collect(Collectors.toList());
    }

    public List<Headline> fetchTopHeadlinesWithLinks(int limit) throws IOException, InterruptedException {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IOException("NEWS_API_KEY is missing.");
        }

        int pageSize = Math.max(1, Math.min(limit, 10));
        StringBuilder url = new StringBuilder(BASE_URL)
                .append("?pageSize=").append(pageSize)
                .append("&category=").append(encode(category))
                .append("&apiKey=").append(encode(apiKey));

        if (!country.isBlank()) {
            url.append("&country=").append(encode(country));
        } else {
            url.append("&language=").append(encode(language));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .timeout(REQUEST_TIMEOUT)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException("News API returned HTTP " + response.statusCode() + ".");
        }

        List<Headline> headlines = parseHeadlines(response.body(), limit);
        if (headlines.isEmpty()) {
            throw new IOException("News API returned no headlines.");
        }
        return headlines;
    }

    private List<Headline> parseHeadlines(String json, int limit) {
        List<String> titles = parseTitles(json, limit);
        List<String> urls = parseUrls(json, limit);
        List<Headline> headlines = new ArrayList<>();

        int size = Math.min(titles.size(), urls.size());
        for (int i = 0; i < size; i++) {
            headlines.add(new Headline(titles.get(i), urls.get(i)));
        }
        return headlines;
    }

    private List<String> parseTitles(String json, int limit) {
        List<String> titles = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return titles;
        }

        Matcher matcher = TITLE_PATTERN.matcher(json);
        while (matcher.find() && titles.size() < limit) {
            String title = unescapeJsonString(matcher.group(1)).trim();
            if (title.isBlank() || "[Removed]".equalsIgnoreCase(title)) {
                continue;
            }
            titles.add(title);
        }
        return titles;
    }

    private List<String> parseUrls(String json, int limit) {
        List<String> urls = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return urls;
        }

        Matcher matcher = URL_PATTERN.matcher(json);
        while (matcher.find() && urls.size() < limit) {
            String value = unescapeJsonString(matcher.group(1)).trim();
            if (value.isBlank()) {
                continue;
            }
            urls.add(value);
        }
        return urls;
    }

    private String unescapeJsonString(String value) {
        return value
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
                .replace("\\n", " ")
                .replace("\\r", " ")
                .replace("\\t", " ")
                .trim();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
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

    public record Headline(String title, String url) {
    }
}
