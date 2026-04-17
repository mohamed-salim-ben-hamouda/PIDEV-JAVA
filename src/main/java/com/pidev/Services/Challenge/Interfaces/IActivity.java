package com.pidev.Services.Challenge.Interfaces;

import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.Group;

import java.util.List;

public interface IActivity {
    void StartActivity(Activity a, Challenge c, Group g);
    Activity findActivityInprogress(int id);
    boolean isUserLeader(int g,int u);
    void submissionfile(Activity a);
    boolean isActivityPassedByGrp(int g,int c);
    List<Group> findGroupsInActivity(int c);
    Activity findActivityByChallengeAndGrp(Challenge c,Group g);
    void updateActivityStatus(Activity a);
    List<Activity> getOldActivitiesForUser(int userId);
    void updateActivityFile(Activity a);
    void delete(int a);
    List<Activity> displayAll();
    //boolean isActivitySubmitted(int a_id);
}
