package com.pidev.Services.Challenge.Interfaces;

import com.pidev.models.Activity;
import com.pidev.models.Group;
import com.pidev.models.MemberActivity;

import java.util.List;

public interface IMemberActivity {
    void addMemberActivity(MemberActivity m, int a,int u);
    List<MemberActivity> display(int a,int u);
    void delete(int id);
    void update(MemberActivity m);
    boolean findDescription(int a,int u);
    List<MemberActivity> getAllGroupMembersForActivity(Group group, Activity activityId);
    void updateIndivScore(MemberActivity m,double score);
    boolean IsIndivScore(MemberActivity m);
    double SelectIndivScore(int u,int a);
}
