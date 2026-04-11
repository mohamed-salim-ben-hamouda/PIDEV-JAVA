package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Controllers.client.Challenge.Evaluation.StudentEvaluationController;
import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.Evaluation;
import com.pidev.models.Group;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.time.format.DateTimeFormatter;


public class OldActivitiesCardsController {
    private Activity a;
    @FXML
    private Label titleChallenge;
    @FXML
    private Label metaLabel;
    @FXML
    private Label descriptionChallenge;
    @FXML
    private Label submissionActivity;
    @FXML
    private Label statusAct;
    @FXML
    private Button evaluationBtn;
    private ServiceEvaluation serviceEva = new ServiceEvaluation();

    public void setData(Activity a) {
        this.a = a;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        if (a.getChallenge() != null) {
            titleChallenge.setText(a.getChallenge().getTitle());
            descriptionChallenge.setText(a.getChallenge().getDescription());

            String deadlineStr = (a.getChallenge().getDeadLine() != null)
                    ? a.getChallenge().getDeadLine().format(formatter)
                    : "No deadline set";

            metaLabel.setText("Deadline: " + deadlineStr + " • Difficulty: " + a.getChallenge().getDifficulty());
        }

        if (a.getSubmissionDate() != null) {
            submissionActivity.setText("Submitted on: " + a.getSubmissionDate().format(formatter));
        } else {
            submissionActivity.setText("No submission date recorded");
        }

        String status = (a.getStatus() != null) ? a.getStatus() : "unknown";
        statusAct.setText("Status: " + status);

        if (status.equalsIgnoreCase("evaluated")) {
            evaluationBtn.setDisable(false);
            evaluationBtn.setText("See Evaluation");
        } else if (status.equalsIgnoreCase("in_progress")) {
            evaluationBtn.setDisable(true);
            evaluationBtn.setText("Submission pending");
        } else {
            evaluationBtn.setDisable(true);
            evaluationBtn.setText("Not evaluated yet");
        }
    }
    public void OnEvaluation(){
        Evaluation e = serviceEva.findEvaluation(a.getId());
        StudentEvaluationController StEvaCntrl = BaseController.getInstance().loadStudentEvaluation();
        if (StEvaCntrl != null){
            StEvaCntrl.setData(e,a);

        }
    }
}
