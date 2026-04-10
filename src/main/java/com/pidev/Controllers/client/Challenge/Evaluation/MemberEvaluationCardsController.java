package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.Activity;
import com.pidev.models.MemberActivity;
import com.pidev.models.User;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;


public class MemberEvaluationCardsController {
    @FXML
    private Label memberName;
    @FXML
    private Label activityDesc;
    @FXML
    private TextField scoreInput;
    @FXML
    private Button submitBtn;
    private User m;
    private MemberActivity ma;
    private ServiceMemberActivity serviceMA=new ServiceMemberActivity();
    private Runnable onUpdateCallback;
    public void setMemberActivityData(MemberActivity member_activity,Runnable onUpdate){
        this.m=member_activity.getUser();
        this.ma=member_activity;
        this.onUpdateCallback=onUpdate;
        memberName.setText(m.getPrenom() + " " + m.getNom());
        activityDesc.setText("Activity: " + member_activity.getActivityDescription());
        if (member_activity.getId() == -1) {
            activityDesc.setText("This user will be assigned a score of 0 for the individual evaluation, as the activity was not submitted");
            activityDesc.setStyle("-fx-text-fill: #e74c3c; -fx-font-style: italic;");

            scoreInput.setText("0.0");
            scoreInput.setDisable(true);
            submitBtn.setVisible(false);
            submitBtn.setManaged(false);
        } else {
            activityDesc.setText("Submission: " + member_activity.getActivityDescription());

            scoreInput.setDisable(false);
            submitBtn.setDisable(false);
            boolean condition=serviceMA.IsIndivScore(ma);
            if (condition){
                submitBtn.setText("Modify");
                scoreInput.setText(String.valueOf(ma.getIndivScore()));
            }
            scoreInput.setText(String.valueOf(member_activity.getIndivScore()));
        }
    }
    @FXML
    public void OnScoreSubmit(){
        String scoreT = scoreInput.getText().trim();
        try {
            double score = Double.parseDouble(scoreT);
            serviceMA.updateIndivScore(ma,score);
            ma.setIndivScore(score);
            submitBtn.setText("Modify");
            scoreInput.setText(String.valueOf(ma.getIndivScore()));
            if (onUpdateCallback != null) {
                onUpdateCallback.run();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

}
