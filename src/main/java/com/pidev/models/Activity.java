package com.pidev.models;

import java.time.LocalDateTime;

public class Activity {

    private Integer id;
    private Challenge challenge;
    private Group group;
    String submissionFile;
    private LocalDateTime submissionDate;
    private String status;
    private LocalDateTime activity_start_time;


    public Activity() {
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Challenge getChallenge() {
        return challenge;
    }

    public void setChallenge(Challenge challenge) {
        this.challenge = challenge;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getSubmissionFile() {
        return submissionFile;
    }

    public void setSubmissionFile(String submissionFile) {
        this.submissionFile = submissionFile;
    }

    public LocalDateTime getSubmissionDate() {
        return submissionDate;
    }

    public LocalDateTime getActivity_start_time() {
        return activity_start_time;
    }


    public void setSubmissionDate(LocalDateTime submissionDate) {
        this.submissionDate = submissionDate;
    }

    public void setActivity_start_time(LocalDateTime activityStartTime) {
        this.activity_start_time = activityStartTime;
    }


    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
