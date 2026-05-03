package com.pidev.models;

import java.util.Locale;

public enum ReactionType {
    LIKE("like", "Like", "\uD83D\uDC4D", "+1.png"),
    LOVE("love", "Love", "\u2764\uFE0F", "heart.png"),
    HAHA("haha", "Haha", "\uD83D\uDE02", "joy.png"),
    WOW("wow", "Wow", "\uD83D\uDE2E", "heart_eyes.png"),
    SAD("sad", "Sad", "\uD83D\uDE22", "pensive.png"),
    ANGRY("angry", "Angry", "\uD83D\uDE21", "angry.png");

    private final String code;
    private final String label;
    private final String emoji;
    private final String iconFile;

    ReactionType(String code, String label, String emoji, String iconFile) {
        this.code = code;
        this.label = label;
        this.emoji = emoji;
        this.iconFile = iconFile;
    }

    public String code() {
        return code;
    }

    public String label() {
        return label;
    }

    public String emoji() {
        return emoji;
    }

    public String iconFile() {
        return iconFile;
    }

    public static ReactionType fromCode(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (ReactionType type : values()) {
            if (type.code.equals(normalized)) {
                return type;
            }
        }
        return null;
    }
}
