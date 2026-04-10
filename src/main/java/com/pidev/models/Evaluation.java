package com.pidev.models;

public class Evaluation {
    private Long id;
    private Activity activity;
    private Double groupScore;
    private String feedback;
    private String preFeedback;
    private String status;
    public Evaluation() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public Double getGroupScore() {
        return groupScore;
    }

    public void setGroupScore(Double groupScore) {
        this.groupScore = groupScore;
    }

    public String getFeedback() {
        return feedback;
    }
    public String getStatus(){
        return status;
    }
    public void setStatus(String status){
        this.status=status;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public String getPreFeedback() {
        return preFeedback;
    }

    public void setPreFeedback(String preFeedback) {
        this.preFeedback = preFeedback;
    }

}
