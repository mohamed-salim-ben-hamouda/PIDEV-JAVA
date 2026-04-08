package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.models.Challenge;
import com.pidev.models.Group;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class EvaluationItemController {
    @FXML
    private Label groupName;
    @FXML
    private Button seeActivityBtn;

    private Group group;
    private Challenge challenge;

    public void setGroupData(Group g, Challenge c) {
        this.group = g;
        this.challenge = c;
        groupName.setText(g.getName());
    }

    @FXML
    public void initialize() {
        seeActivityBtn.setOnAction(event -> {
            System.out.println("Opening activity for Group: " + group.getName() +
                    " in Challenge: " + challenge.getTitle());
        });
    }
}