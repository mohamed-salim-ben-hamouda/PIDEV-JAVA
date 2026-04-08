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

}
