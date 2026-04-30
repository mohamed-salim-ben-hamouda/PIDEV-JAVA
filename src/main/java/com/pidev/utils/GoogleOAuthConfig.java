package com.pidev.utils;

/**
 * Google OAuth 2.0 configuration constants.
 */
public class GoogleOAuthConfig {

    /** Your OAuth 2.0 Client ID from Google Cloud Console */
    public static final String CLIENT_ID = EnvConfig.get("GOOGLE_CLIENT_ID", "");

    /** Your OAuth 2.0 Client Secret from Google Cloud Console */
    public static final String CLIENT_SECRET = EnvConfig.get("GOOGLE_CLIENT_SECRET", "");

    /** Local redirect URI — must match what you registered in Cloud Console */
    public static final String REDIRECT_URI = "http://localhost:8888/callback";

    /** Port for the local callback server */
    public static final int CALLBACK_PORT = 8888;

    /** Google OAuth Endpoints */
    public static final String AUTH_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    public static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    public static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";

    /** Scopes required for profile and email */
    public static final String SCOPES = "openid email profile";
}
