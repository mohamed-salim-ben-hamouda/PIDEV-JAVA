package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.models.MemberActivity;
import com.pidev.models.ProblemSolution;
import com.pidev.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

public class MembersActivityModifCardsController {
    @FXML
    private Label userName;
    @FXML
    private TextArea activityDescModifier;
    @FXML
    private Button EditBtn;
    private User m;
    private MemberActivity ma;
    private Runnable onUpdateCallback;
    private ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    public void initData(MemberActivity member_activity,Runnable onUpdate){
        this.m=member_activity.getUser();
        this.ma=member_activity;
        this.onUpdateCallback=onUpdate;
        userName.setText(m.getPrenom() + " " + m.getNom());
        activityDescModifier.setText(ma.getActivityDescription());
    }
    @FXML
    public void onUpdateMA(){
        String m_activity=activityDescModifier.getText();
        ma.setActivityDescription(m_activity);
        try {
            serviceMA.update(ma);
            if (onUpdateCallback != null) {
                onUpdateCallback.run();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
}
