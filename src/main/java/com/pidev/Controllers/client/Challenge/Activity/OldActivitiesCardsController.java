package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Controllers.client.Challenge.Evaluation.StudentEvaluationController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.models.*;
import com.pidev.utils.OpenPdfUtil;
import com.pidev.utils.PreFeedbackPdfUtil;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;


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
    @FXML
    private Button ModifyBtn;
    @FXML
    private Button preFeedbackBtn;
    private ServiceEvaluation serviceEva = new ServiceEvaluation();
    private ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    private ServiceActivity serviceA = new ServiceActivity();

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
        int user_id = 2;
        boolean is_admin = serviceA.isUserLeader(a.getGroup().getId(),user_id);
        boolean is_evaluation = serviceEva.isEvaluation(a.getId());
        if(!is_admin || is_evaluation){
            ModifyBtn.setVisible(false);
            ModifyBtn.setManaged(false);
        }else {
            ModifyBtn.setVisible(true);
            ModifyBtn.setManaged(true);
        }
        if(status.equalsIgnoreCase("evaluated") || status.equalsIgnoreCase("submitted")){
            preFeedbackBtn.setVisible(true);
            preFeedbackBtn.setManaged(true);
        }else {
            preFeedbackBtn.setVisible(false);
            preFeedbackBtn.setManaged(false);
        }


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
    public void OnModif(){
        ModifyActivityController ModifActCntrl = BaseController.getInstance().loadModifyActivity();
        if(ModifActCntrl != null){
            ModifActCntrl.initData(a);
        }
    }
    public void OnPreFeedback() {
        if (a == null || a.getId() == null) {
            showError("No activity selected.");
            return;
        }

        Evaluation eval = serviceEva.findEvaluation(a.getId());
        if (eval == null || eval.getPreFeedback() == null || eval.getPreFeedback().isBlank()) {
            showError("No pre-feedback is available for this activity yet.");
            return;
        }

        try {
            String fileName = "prefeedback-activity-" + a.getId() + ".pdf";
            Path pdfPath = PreFeedbackPdfUtil.writePreFeedbackPdfToResources(eval.getPreFeedback(), fileName);
            OpenPdfUtil.openPdfInApp(pdfPath.toString(), "Pre-Feedback PDF");
        } catch (Exception ex) {
            showError("Could not open pre-feedback PDF:\n" + ex.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Pre-Feedback PDF");
        alert.setHeaderText("Unable to open pre-feedback PDF");
        alert.setContentText(message);
        alert.showAndWait();
    }


}
