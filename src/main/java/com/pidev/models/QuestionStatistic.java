package com.pidev.models;

public class QuestionStatistic {
    private final Question question;
    private final int totalResponses;
    private final int correctCount;
    private final double successRate;

    public QuestionStatistic(Question question, int totalResponses, int correctCount, double successRate) {
        this.question = question;
        this.totalResponses = totalResponses;
        this.correctCount = correctCount;
        this.successRate = successRate;
    }

    public Question getQuestion() {
        return question;
    }

    public int getTotalResponses() {
        return totalResponses;
    }

    public int getCorrectCount() {
        return correctCount;
    }

    public double getSuccessRate() {
        return successRate;
    }
}