package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.models.Challenge;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

public class EvaluationCardController {
    @FXML
    private Label titleLabel;
    @FXML
    private Button actionButton;
    private Challenge c;
    public void setData(Challenge c){
        this.c=c;
        titleLabel.setText(c.getTitle());
    }
    public Button getActionButton() {
        return actionButton;
    }

}
