package com.pidev.models;

import java.util.Locale;

public enum ReactionType {
    LIKE("like", "Like", "👍"),
    LOVE("love", "Love", "❤️"),
    HAHA("haha", "Haha", "😂"),
    WOW("wow", "Wow", "😮"),
    SAD("sad", "Sad", "😢"),
    ANGRY("angry", "Angry", "😡");

    private final String code;
    private final String label;
    private final String emoji;

    ReactionType(String code, String label, String emoji) {
        this.code = code;
        this.label = label;
        this.emoji = emoji;
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
