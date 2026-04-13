package com.pidev.models;

public class Membership {
    private Integer id;
    private Integer userId;
    private Integer groupId;
    private String role;
    private double contributionScore;
    private String achievementUnlocked;
    private boolean active;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Integer getGroupId() {
        return groupId;
    }

    public void setGroupId(Integer groupId) {
        this.groupId = groupId;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public double getContributionScore() {
        return contributionScore;
    }

    public void setContributionScore(double contributionScore) {
        this.contributionScore = contributionScore;
    }

    public String getAchievementUnlocked() {
        return achievementUnlocked;
    }

    public void setAchievementUnlocked(String achievementUnlocked) {
        this.achievementUnlocked = achievementUnlocked;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
