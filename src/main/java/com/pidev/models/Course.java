package com.pidev.models;

import java.util.ArrayList;
import java.util.List;

public class Course {
    public static final String DIFFICULTY_BEGINNER = "BEGINNER";
    public static final String DIFFICULTY_INTERMEDIATE = "INTERMEDIATE";
    public static final String DIFFICULTY_ADVANCED = "ADVANCED";

    private Integer id;
    private String title;
    private String description;
    private int duration;
    private String difficulty = DIFFICULTY_BEGINNER;
    private boolean isActive = true;
    private float validationScore;
    private String content;
    private String material;
    private Quiz prerequisiteQuiz;
    private List<String> sectionsToReview;

    private User creator;

    List<Challenge> challenges;

    // Constructor
    public Course() {
        this.challenges = new ArrayList<>();
        this.sectionsToReview = new ArrayList<>();
        this.isActive = true;
        this.difficulty = DIFFICULTY_BEGINNER;
    }

    // Constructor for Foreign Key usage
    public Course(int id) {
        this();
        this.id = id;
    }


    public int getDifficultyLevel() {
        return switch (this.difficulty) {
            case DIFFICULTY_INTERMEDIATE -> 2;
            case DIFFICULTY_ADVANCED -> 3;
            default -> 1;
        };
    }

    @Override
    public String toString() {
        return title != null ? title : "Course #" + id;
    }

    // --- Getters and Setters ---
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getDuration() { return duration; }
    public void setDuration(int duration) { this.duration = duration; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public float getValidationScore() { return validationScore; }
    public void setValidationScore(float validationScore) { this.validationScore = validationScore; }

    public User getCreator() { return creator; }
    public void setCreator(User creator) { this.creator = creator; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getMaterial() { return material; }
    public void setMaterial(String material) { this.material = material; }

    public List<Challenge> getChallenges() { return challenges; }
    public void setChallenges(List<Challenge> challenges) { this.challenges = challenges; }

    public Quiz getPrerequisiteQuiz() { return prerequisiteQuiz; }
    public void setPrerequisiteQuiz(Quiz prerequisiteQuiz) { this.prerequisiteQuiz = prerequisiteQuiz; }

    public List<String> getSectionsToReview() { return sectionsToReview; }
    public void setSectionsToReview(List<String> sectionsToReview) { this.sectionsToReview = sectionsToReview; }
}