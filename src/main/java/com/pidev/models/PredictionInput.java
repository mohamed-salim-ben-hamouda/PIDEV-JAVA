package com.pidev.models;

public class PredictionInput {
    private final double avgGroupScore;
    private final double avgCompletionTime;
    private final int difficulty;
    private final int deadlineDays;
    private final double groupSkillVariance;
    private final int groupSize;

    public PredictionInput(double avgGroupScore, double avgCompletionTime, int difficulty, int deadlineDays, double groupSkillVariance, int groupSize) {
        this.avgGroupScore = avgGroupScore;
        this.avgCompletionTime = avgCompletionTime;
        this.difficulty = difficulty;
        this.deadlineDays = deadlineDays;
        this.groupSkillVariance = groupSkillVariance;
        this.groupSize = groupSize;
    }

    public double getAvgGroupScore() {
        return avgGroupScore;
    }

    public double getAvgCompletionTime() {
        return avgCompletionTime;
    }

    public int getDifficulty() {
        return difficulty;
    }

    public int getDeadlineDays() {
        return deadlineDays;
    }

    public double getGroupSkillVariance() {
        return groupSkillVariance;
    }

    public int getGroupSize() {
        return groupSize;
    }
}
