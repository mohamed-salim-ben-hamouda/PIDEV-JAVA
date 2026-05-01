package com.pidev.models;

import java.util.ArrayList;
import java.util.List;

public class Quiz {
    private Integer id;
    private Course course;
    private Chapter chapter;
    private String title;
    private float passingScore;
    private int maxAttempts;
    private Integer questionsPerAttempt;
    private int timeLimit;
    private User supervisor;
    private List<Question> questions;
    private List<Object> quizAttempts;
    private String categoryDistribution;

    public Quiz() {
        this.questions = new ArrayList<>();
        this.quizAttempts = new ArrayList<>();
    }

    public Quiz(int id) {
        this.id = id;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Course getCourse() { return course; }
    public void setCourse(Course course) { this.course = course; }

    public Chapter getChapter() { return chapter; }
    public void setChapter(Chapter chapter) { this.chapter = chapter; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public float getPassingScore() { return passingScore; }
    public void setPassingScore(float passingScore) { this.passingScore = passingScore; }

    public int getMaxAttempts() { return maxAttempts; }
    public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }

    public Integer getQuestionsPerAttempt() { return questionsPerAttempt; }
    public void setQuestionsPerAttempt(Integer questionsPerAttempt) { this.questionsPerAttempt = questionsPerAttempt; }

    public int getTimeLimit() { return timeLimit; }
    public void setTimeLimit(int timeLimit) { this.timeLimit = timeLimit; }

    public int getTimeLimitSeconds() { return timeLimit * 60; }

    public User getSupervisor() { return supervisor; }
    public void setSupervisor(User supervisor) { this.supervisor = supervisor; }

    public List<Question> getQuestions() { return questions; }
    public void setQuestions(List<Question> questions) { this.questions = questions; }

    public List<Object> getQuizAttempts() { return quizAttempts; }
    public void setQuizAttempts(List<Object> quizAttempts) { this.quizAttempts = quizAttempts; }

    public String getCategoryDistribution() { return categoryDistribution; }
    public void setCategoryDistribution(String categoryDistribution) { this.categoryDistribution = categoryDistribution; }

    @Override
    public String toString() {
        String chapterTitle = chapter != null ? chapter.getTitle() : null;
        String localTitle = title != null ? title : "";
        if (chapterTitle != null && !chapterTitle.isBlank()) {
            return chapterTitle + " - " + (localTitle.isBlank() ? "Quiz #" + id : localTitle);
        }
        return localTitle.isBlank() ? "Quiz #" + id : localTitle;
    }
}
