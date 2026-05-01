package com.pidev.models;

public class QuizStatisticsSummary {
    private final Quiz quiz;
    private final int totalAttempts;
    private final int uniqueStudents;
    private final int passedCount;
    private final int failedCount;
    private final double averageScore;
    private final double passRate;

    public QuizStatisticsSummary(Quiz quiz, int totalAttempts, int uniqueStudents, int passedCount, int failedCount, double averageScore, double passRate) {
        this.quiz = quiz;
        this.totalAttempts = totalAttempts;
        this.uniqueStudents = uniqueStudents;
        this.passedCount = passedCount;
        this.failedCount = failedCount;
        this.averageScore = averageScore;
        this.passRate = passRate;
    }

    public Quiz getQuiz() {
        return quiz;
    }

    public int getTotalAttempts() {
        return totalAttempts;
    }

    public int getUniqueStudents() {
        return uniqueStudents;
    }

    public int getPassedCount() {
        return passedCount;
    }

    public int getFailedCount() {
        return failedCount;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public double getPassRate() {
        return passRate;
    }

    @Override
    public String toString() {
        return quiz == null ? "Quiz" : quiz.toString();
    }
}