package com.pidev.models;

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

    public Quiz() {
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

    public User getSupervisor() { return supervisor; }
    public void setSupervisor(User supervisor) { this.supervisor = supervisor; }

    @Override
    public String toString() {
        return title != null ? title : "Quiz #" + id;
    }
}
