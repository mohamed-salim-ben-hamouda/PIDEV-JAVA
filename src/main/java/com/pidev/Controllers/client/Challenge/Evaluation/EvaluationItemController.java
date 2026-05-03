package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.models.Challenge;
import com.pidev.models.Evaluation;
import com.pidev.models.Activity;
import com.pidev.models.Group;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import javafx.event.ActionEvent;

public class EvaluationItemController {
    @FXML
    private Label groupName;
    @FXML
    private Button seeActivityBtn;

    private Group group;
    private Challenge challenge;
    private ServiceActivity serviceAct=new ServiceActivity();

    public void setGroupData(Group g, Challenge c) {
        this.group = g;
        this.challenge = c;
        groupName.setText(g.getName());
        String status= serviceAct.ActivityStatus(c.getId(),g.getId());
        if("in_progress".equalsIgnoreCase(status)){
            seeActivityBtn.setDisable(true);
            seeActivityBtn.setText("Still In progress you have to wait ");
        } else if ("submitted".equalsIgnoreCase(status)) {
            seeActivityBtn.setDisable(false);
            seeActivityBtn.setText("Evaluate");
            seeActivityBtn.setOnAction(this::OnEvaluate);
        }else {
            seeActivityBtn.setDisable(false);
            seeActivityBtn.setText("Modify");
            seeActivityBtn.setOnAction(this::OnModif);
        }


    }
    public void OnEvaluate(ActionEvent event){
        Activity a = serviceAct.findActivityByChallengeAndGrp(challenge, group);
        EvaluationMainController mainCtrl = BaseController.getInstance().loadEvaluationMainPage();
        if (mainCtrl != null) {
            mainCtrl.setData(challenge, group, a);
        }

    }
    public void OnModif(ActionEvent event){
        Activity a = serviceAct.findActivityByChallengeAndGrp(challenge, group);
        EvaluationMainController mainCtrl = BaseController.getInstance().loadEvaluationMainPage();
        if (mainCtrl != null) {
            mainCtrl.setData(challenge, group, a);
        }

    }

}
