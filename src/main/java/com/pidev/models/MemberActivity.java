package com.pidev.models;


public class MemberActivity {

    private Integer id;
    private Activity activity; // Maps from $id_activity
    private User user;         // Maps from $user_id
    private String activityDescription = "";
    private Double indivScore;

    public MemberActivity() {
    }

    public MemberActivity(Activity activity, User user, String activityDescription, Double indivScore) {
        this.activity = activity;
        this.user = user;
        this.activityDescription = activityDescription;
        this.indivScore = indivScore;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getActivityDescription() {
        return activityDescription;
    }

    public void setActivityDescription(String activityDescription) {
        this.activityDescription = activityDescription;
    }

    public Double getIndivScore() {
        return indivScore;
    }

    public void setIndivScore(Double indivScore) {
        this.indivScore = indivScore;
    }
}
