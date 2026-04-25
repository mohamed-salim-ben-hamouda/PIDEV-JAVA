package com.pidev.utils;

/**
 * Google OAuth 2.0 configuration constants.
 *
 * SETUP INSTRUCTIONS:
 * 1. Go to https://console.cloud.google.com/
 * 2. Create/select a project → APIs & Services → Credentials
 * 3. Click "Create Credentials" → OAuth 2.0 Client ID → Desktop app
 * 4. Add authorized redirect URI: http://localhost:8080/callback
 * 5. Replace YOUR_CLIENT_ID and YOUR_CLIENT_SECRET below with your actual values.
 */
public class GoogleOAuthConfig {

    /** Your OAuth 2.0 Client ID from Google Cloud Console */
    public static final String CLIENT_ID =
            "1026633870387-23pdud4gmkicplg0djvqltt94esicsl6.apps.googleusercontent.com";

    /** Your OAuth 2.0 Client Secret from Google Cloud Console */
    public static final String CLIENT_SECRET =
            "YOUR_CLIENT_SECRET";

    /** Local redirect URI — must match what you registered in Cloud Console */
    public static final String REDIRECT_URI =
            "http://127.0.0.1:8888/callback";

    /** Google's Authorization endpoint */
    public static final String AUTH_ENDPOINT =
            "https://accounts.google.com/o/oauth2/v2/auth";

    /** Google's Token exchange endpoint */
    public static final String TOKEN_ENDPOINT =
            "https://oauth2.googleapis.com/token";

    /** Google's UserInfo endpoint */
    public static final String USERINFO_URL =
            "https://www.googleapis.com/oauth2/v3/userinfo";

    /** OAuth scopes: identity + email + profile */
    public static final String SCOPES =
            "openid email profile";

    /** Local callback server port */
    public static final int CALLBACK_PORT = 8888;
}
