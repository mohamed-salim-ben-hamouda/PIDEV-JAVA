package com.pidev.models;

import java.time.LocalDateTime;

public class QuizAttemptDetail {
    private final int attemptNumber;
    private final double score;
    private final boolean passed;
    private final LocalDateTime submittedAt;
    private final String studentName;
    private final String studentEmail;

    public QuizAttemptDetail(int attemptNumber, double score, boolean passed, LocalDateTime submittedAt, String studentName, String studentEmail) {
        this.attemptNumber = attemptNumber;
        this.score = score;
        this.passed = passed;
        this.submittedAt = submittedAt;
        this.studentName = studentName;
        this.studentEmail = studentEmail;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public double getScore() {
        return score;
    }

    public boolean isPassed() {
        return passed;
    }

    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }

    public String getStudentName() {
        return studentName;
    }

    public String getStudentEmail() {
        return studentEmail;
    }

    public String getInitials() {
        String name = studentName == null ? "" : studentName.trim();
        if (!name.isEmpty()) {
            String[] parts = name.split("\\s+");
            String first = parts[0].isEmpty() ? "" : parts[0].substring(0, 1).toUpperCase();
            String second = parts.length > 1 && !parts[1].isEmpty() ? parts[1].substring(0, 1).toUpperCase() : "";
            String initials = first + second;
            if (!initials.isEmpty()) {
                return initials;
            }
        }

        if (studentEmail != null && !studentEmail.isBlank()) {
            return studentEmail.substring(0, 1).toUpperCase();
        }
        return "?";
    }
}