package com.pidev.models;

public class IndividualRankingEntry {
    private final int userId;
    private final String displayName;
    private final String photoPath;
    private final double finalScore;
    private int rank;

    public IndividualRankingEntry(int userId, String displayName, String photoPath, double finalScore) {
        this.userId = userId;
        this.displayName = displayName;
        this.photoPath = photoPath;
        this.finalScore = finalScore;
    }

    public int getUserId() {
        return userId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public double getFinalScore() {
        return finalScore;
    }

    public int getRank() {
        return rank;
    }

    public void setRank(int rank) {
        this.rank = rank;
    }

    public String getInitials() {
        if (displayName == null || displayName.isBlank()) {
            return "?";
        }

        String[] parts = displayName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, 1).toUpperCase();
        }

        String first = parts[0].substring(0, 1).toUpperCase();
        String second = parts[1].substring(0, 1).toUpperCase();
        return first + second;
    }
}
