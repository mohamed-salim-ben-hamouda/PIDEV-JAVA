package com.pidev.Controllers.client.Challenge;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;

public class AdminGroupCardController {
    @FXML private Label groupNameLabel;
    @FXML private Button chooseBtn;

    private int groupId;
    private Challenge c;
    private ServiceActivity serviceAct = new ServiceActivity();
    public void setGroupData(Group g, Challenge c) {
        this.groupId = g.getId();
        this.c=c;
        this.groupNameLabel.setText("👥 " + g.getName());
    }
    @FXML
    private void OnChoose(ActionEvent event) {
        Activity a = new Activity();
        Group g = new Group();
        g.setId(groupId);
        serviceAct.StartActivity(a, c, g);
        Integer activityId = a.getId();
        if (activityId == null) {
            System.out.println("OnChoose: activityId is null after StartActivity; navigation canceled.");
            return;
        }
        BaseController.getInstance().loadActivityPage(c, g.getId(), activityId);
    }
}
