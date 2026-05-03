package com.pidev.models;


public class Membership {

    private Integer id;
    private User user;          // Reference to the User object
    private Group group;        // Reference to the Group object
    private String role;
    private double contributionScore;
    private String achievementUnlocked;
    private boolean isActive = false;

    public Membership() {
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
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

    // Java naming convention for booleans is 'isActive' and 'setActive'
    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
}
