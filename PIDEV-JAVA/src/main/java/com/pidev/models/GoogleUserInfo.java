package com.pidev.models;

/**
 * POJO representing user data returned by Google's userinfo endpoint.
 * Field names match the JSON keys returned by:
 * GET https://www.googleapis.com/oauth2/v3/userinfo
 */
public class GoogleUserInfo {

    /** Unique Google user identifier (subject) */
    public String sub;

    /** Verified email address */
    public String email;

    /** Whether Google has verified the email */
    public Boolean email_verified;

    /** Full display name */
    public String name;

    /** First name */
    public String given_name;

    /** Last name / family name */
    public String family_name;

    /** URL to Google profile picture */
    public String picture;

    /** Locale (e.g. "fr", "en") */
    public String locale;

    @Override
    public String toString() {
        return "GoogleUserInfo{" +
                "sub='" + sub + '\'' +
                ", email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", given_name='" + given_name + '\'' +
                ", family_name='" + family_name + '\'' +
                '}';
    }
}
