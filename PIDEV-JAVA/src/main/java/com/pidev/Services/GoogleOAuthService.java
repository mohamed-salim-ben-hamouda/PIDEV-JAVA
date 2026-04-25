package com.pidev.Services;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.pidev.models.GoogleUserInfo;
import com.pidev.utils.GoogleOAuthConfig;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Handles the complete Google OAuth 2.0 Authorization Code flow:
 *  1) Build the authorization URL and open it in the system browser.
 *  2) Start a local HTTP server to receive the redirect with the auth code.
 *  3) Exchange the auth code for an access token.
 *  4) Fetch the user's profile from Google's userinfo endpoint.
 */
public class GoogleOAuthService {

    private final Gson gson = new Gson();

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Initiates the full OAuth login flow.
     *
     * Opens the browser, waits up to 2 minutes for the user to log in,
     * then returns the user's Google profile — or null on failure/timeout.
     */
    public GoogleUserInfo signIn() {
        String state = generateState();
        String codeVerifier = generateCodeVerifier();
        String codeChallenge;
        try {
            codeChallenge = generateCodeChallenge(codeVerifier);
        } catch (Exception e) {
            System.err.println("Failed to generate PKCE challenge: " + e.getMessage());
            return null;
        }
        String authUrl = buildAuthUrl(state, codeChallenge);

        // Will hold the authorization code captured by the local server
        AtomicReference<String> codeRef = new AtomicReference<>();
        CountDownLatch latch = new CountDownLatch(1);

        HttpServer server = null;
        try {
            server = startCallbackServer(state, codeRef, latch);
            openBrowser(authUrl);

            // Wait up to 2 minutes for the user to complete sign-in
            boolean completed = latch.await(2, TimeUnit.MINUTES);
            if (!completed || codeRef.get() == null) {
                System.err.println("Google OAuth: timed out waiting for user login.");
                return null;
            }

            String accessToken = exchangeCodeForToken(codeRef.get(), codeVerifier);
            if (accessToken == null) return null;

            return fetchUserInfo(accessToken);

        } catch (Exception e) {
            System.err.println("Google OAuth error: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            if (server != null) {
                server.stop(1);
            }
        }
    }

    // -------------------------------------------------------------------------
    // Step 1: Build the Authorization URL
    // -------------------------------------------------------------------------

    private String buildAuthUrl(String state, String codeChallenge) {
        try {
            return GoogleOAuthConfig.AUTH_ENDPOINT
                    + "?client_id="     + encode(GoogleOAuthConfig.CLIENT_ID)
                    + "&redirect_uri="  + encode(GoogleOAuthConfig.REDIRECT_URI)
                    + "&response_type=code"
                    + "&scope="         + encode(GoogleOAuthConfig.SCOPES)
                    + "&state="         + encode(state)
                    + "&access_type=offline"
                    + "&prompt=select_account"
                    + "&code_challenge=" + encode(codeChallenge)
                    + "&code_challenge_method=S256";
        } catch (Exception e) {
            throw new RuntimeException("Failed to build auth URL", e);
        }
    }

    // -------------------------------------------------------------------------
    // Step 2: Local callback server
    // -------------------------------------------------------------------------

    private HttpServer startCallbackServer(String expectedState,
                                           AtomicReference<String> codeRef,
                                           CountDownLatch latch) throws IOException {
        HttpServer server = HttpServer.create(
                new InetSocketAddress(GoogleOAuthConfig.CALLBACK_PORT), 0);

        server.createContext("/callback", exchange -> {
            try {
                // Parse query parameters from the redirect URL
                String query = exchange.getRequestURI().getQuery();
                Map<String, String> params = parseQuery(query);

                String receivedState = params.get("state");
                String code = params.get("code");
                String error = params.get("error");

                // Build a small HTML response to show in the browser
                String html;
                if (error != null) {
                    html = buildHtmlPage("Login Cancelled",
                            "❌ Google sign-in was cancelled or denied.",
                            "#e53935");
                } else if (code != null && expectedState.equals(receivedState)) {
                    codeRef.set(code);
                    html = buildHtmlPage("Success!",
                            "✅ Sign-in successful! You can close this tab and return to the app.",
                            "#43a047");
                } else {
                    html = buildHtmlPage("Error",
                            "⚠️ Invalid state parameter. Possible CSRF attack. Please try again.",
                            "#fb8c00");
                }

                // Send HTTP response
                byte[] responseBytes = html.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, responseBytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(responseBytes);
                }
            } catch (Exception e) {
                System.err.println("Callback server error: " + e.getMessage());
            } finally {
                latch.countDown();
            }
        });

        server.setExecutor(null);
        server.start();
        System.out.println("Google OAuth callback server started on port " + GoogleOAuthConfig.CALLBACK_PORT);
        return server;
    }

    // -------------------------------------------------------------------------
    // Step 3: Exchange authorization code for access token
    // -------------------------------------------------------------------------

    private String exchangeCodeForToken(String code, String codeVerifier) throws Exception {
        String postBody = "code="          + encode(code)
                + "&client_id="     + encode(GoogleOAuthConfig.CLIENT_ID)
                + "&redirect_uri="   + encode(GoogleOAuthConfig.REDIRECT_URI)
                + "&grant_type=authorization_code"
                + "&code_verifier=" + encode(codeVerifier);

        String response = httpPost(GoogleOAuthConfig.TOKEN_ENDPOINT, postBody);
        if (response == null) return null;

        JsonObject json = gson.fromJson(response, JsonObject.class);
        if (json.has("error")) {
            System.err.println("Token exchange error: " + json.get("error").getAsString()
                    + " — " + (json.has("error_description") ? json.get("error_description").getAsString() : ""));
            return null;
        }

        return json.get("access_token").getAsString();
    }

    // -------------------------------------------------------------------------
    // Step 4: Fetch user profile from Google
    // -------------------------------------------------------------------------

    private GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        String response = httpGet(GoogleOAuthConfig.USERINFO_URL, accessToken);
        if (response == null) return null;
        return gson.fromJson(response, GoogleUserInfo.class);
    }

    // -------------------------------------------------------------------------
    // HTTP helpers
    // -------------------------------------------------------------------------

    private String httpPost(String urlStr, String body) throws Exception {
        HttpURLConnection conn = openConnection(urlStr, "POST");
        conn.setDoOutput(true);
        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        return readResponse(conn);
    }

    private String httpGet(String urlStr, String bearerToken) throws Exception {
        HttpURLConnection conn = openConnection(urlStr, "GET");
        conn.setRequestProperty("Authorization", "Bearer " + bearerToken);
        return readResponse(conn);
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws Exception {
        URI uri = URI.create(urlStr);
        HttpURLConnection conn = (HttpURLConnection) uri.toURL().openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        int status = conn.getResponseCode();
        InputStream is = (status >= 200 && status < 300)
                ? conn.getInputStream()
                : conn.getErrorStream();

        if (is == null) return null;
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (status < 200 || status >= 300) {
            System.err.println("HTTP " + status + " from " + conn.getURL() + ": " + body);
            return null;
        }
        return body;
    }

    // -------------------------------------------------------------------------
    // Utilities
    // -------------------------------------------------------------------------

    private void openBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(URI.create(url));
        } else {
            // Linux fallback
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    }

    private Map<String, String> parseQuery(String query) {
        Map<String, String> params = new HashMap<>();
        if (query == null || query.isBlank()) return params;
        for (String pair : query.split("&")) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                params.put(
                        URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                        URLDecoder.decode(kv[1], StandardCharsets.UTF_8)
                );
            }
        }
        return params;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String generateState() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeVerifier() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String generateCodeChallenge(String verifier) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(verifier.getBytes(StandardCharsets.US_ASCII));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
    }

    /** Returns a simple success/failure HTML page shown in the browser after redirect */
    private String buildHtmlPage(String title, String message, String color) {
        return "<!DOCTYPE html><html><head><meta charset='UTF-8'>"
                + "<title>" + title + "</title>"
                + "<style>body{font-family:sans-serif;display:flex;align-items:center;"
                + "justify-content:center;height:100vh;margin:0;background:#f5f5f5;}"
                + ".card{background:#fff;padding:40px 60px;border-radius:16px;"
                + "box-shadow:0 4px 24px rgba(0,0,0,.12);text-align:center;}"
                + "h2{color:" + color + ";margin-bottom:12px;}"
                + "p{color:#555;font-size:16px;}"
                + "</style></head><body>"
                + "<div class='card'><h2>" + title + "</h2><p>" + message + "</p></div>"
                + "</body></html>";
    }
}
