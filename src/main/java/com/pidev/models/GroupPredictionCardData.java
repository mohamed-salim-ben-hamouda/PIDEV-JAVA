package com.pidev.models;

public class GroupPredictionCardData {
    private final Group group;
    private final String status;
    private final double successPercentage;
    private final double failPercentage;
    private final boolean available;
    private final String subtitle;
    private final String recommendation;

    public GroupPredictionCardData(
            Group group,
            String status,
            double successPercentage,
            double failPercentage,
            boolean available,
            String subtitle,
            String recommendation
    ) {
        this.group = group;
        this.status = status;
        this.successPercentage = successPercentage;
        this.failPercentage = failPercentage;
        this.available = available;
        this.subtitle = subtitle;
        this.recommendation = recommendation;
    }

    public Group getGroup() {
        return group;
    }

    public String getStatus() {
        return status;
    }

    public double getSuccessPercentage() {
        return successPercentage;
    }

    public double getFailPercentage() {
        return failPercentage;
    }

    public boolean isAvailable() {
        return available;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public String getRecommendation() {
        return recommendation;
    }
}
