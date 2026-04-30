package com.pidev.utils;

public class EmailConfig {
    // IMPORTANT: Replace these in your .env file
    public static final String SMTP_USERNAME = EnvConfig.get("SMTP_USERNAME", "tas.sam.se@gmail.com");
    public static final String SMTP_PASSWORD = EnvConfig.get("SMTP_PASSWORD", "");
    
    public static final String SMTP_HOST = "smtp.gmail.com";
    public static final String SMTP_PORT = "587";
}
