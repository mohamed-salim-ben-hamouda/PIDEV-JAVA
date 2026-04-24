package com.pidev.models;

import java.time.LocalDateTime;

public class Challenge {
    private Integer id;
    private String title;
    private String description;
    private String targetSkill;
    private String difficulty;
    private int minGroupNbr;
    private int maxGroupNbr;
    private LocalDateTime deadLine;
    private LocalDateTime createdAt;
    private String content;
    private User creator;
    private Course course;
    private int github;

    public Challenge() {
        this.createdAt = LocalDateTime.now();
    }

    public Challenge(int id) {
        this.id = id;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getTargetSkill() {
        return targetSkill;
    }

    public void setTargetSkill(String targetSkill) {
        this.targetSkill = targetSkill;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public int getMinGroupNbr() {
        return minGroupNbr;
    }

    public void setMinGroupNbr(int minGroupNbr) {
        this.minGroupNbr = minGroupNbr;
    }

    public int getMaxGroupNbr() {
        return maxGroupNbr;
    }

    public void setMaxGroupNbr(int maxGroupNbr) {
        this.maxGroupNbr = maxGroupNbr;
    }

    public LocalDateTime getDeadLine() {
        return deadLine;
    }

    public void setDeadLine(LocalDateTime deadLine) {
        this.deadLine = deadLine;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public User getCreator() {
        return creator;
    }

    public void setCreator(User creator) {
        this.creator = creator;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
    }
    public int getGithub(){ return github; }
    public void setGithub(int github){ this.github = github; }
}