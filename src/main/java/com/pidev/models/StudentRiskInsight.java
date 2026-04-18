package com.pidev.models;

import java.util.List;

public class StudentRiskInsight {
    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH
    }

    private final User student;
    private final int attempts;
    private final double averageScore;
    private final double passRate;
    private final int riskScore;
    private final RiskLevel riskLevel;
    private final String reason;
    private final List<String> recommendedActions;
    private final List<String> recommendedCourses;

    public StudentRiskInsight(
            User student,
            int attempts,
            double averageScore,
            double passRate,
            int riskScore,
            RiskLevel riskLevel,
            String reason,
            List<String> recommendedActions,
            List<String> recommendedCourses
    ) {
        this.student = student;
        this.attempts = attempts;
        this.averageScore = averageScore;
        this.passRate = passRate;
        this.riskScore = riskScore;
        this.riskLevel = riskLevel;
        this.reason = reason;
        this.recommendedActions = recommendedActions;
        this.recommendedCourses = recommendedCourses;
    }

    public User getStudent() {
        return student;
    }

    public int getAttempts() {
        return attempts;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getPassRate() {
        return passRate;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskLevel getRiskLevel() {
        return riskLevel;
    }

    public String getReason() {
        return reason;
    }

    public List<String> getRecommendedActions() {
        return recommendedActions;
    }

    public List<String> getRecommendedCourses() {
        return recommendedCourses;
    }
}