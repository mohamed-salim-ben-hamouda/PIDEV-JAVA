package com.pidev.Controllers.admin.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.models.Evaluation;
import com.pidev.utils.OpenPdfUtil;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.function.Consumer;


public class EvaluationBackCardsController {
    @FXML
    private Label idEvaluation;
    @FXML
    private Label GroupScore;
    @FXML
    private Label statusEvaluation;
    @FXML
    private Label idActivityRef;
    private Evaluation e;
    private Consumer<Void> onDeleteCallback;
    public void initData(Evaluation e, Consumer<Void> onDeleteCallback){
        this.onDeleteCallback = onDeleteCallback;
        this.e=e;
        idEvaluation.setText("#"+e.getId());
        if(e.getGroupScore() != null){
            GroupScore.setText(e.getGroupScore().toString());
        }else {
            GroupScore.setText("No Data");
        }
        statusEvaluation.setText(e.getStatus());
        idActivityRef.setText("#" + e.getActivity().getId());
    }
    @FXML
    public void OnDelete(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Evaluation");
        alert.setContentText("Are you sure you want to delete this Evaluation?");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/challenge.css").toExternalForm());
        dialogPane.getStyleClass().add("my-custom-alert");
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("alert-primary-btn");
        }
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        if (alert.showAndWait().get() == ButtonType.OK) {
            ServiceEvaluation service = new ServiceEvaluation();
            service.delete(e.getId());
            if (onDeleteCallback != null) {
                onDeleteCallback.accept(null);
            }
        }
    }
    @FXML
    public void openPdfActivity(){
        if(e == null || e.getFeedback() == null || e.getFeedback().isBlank()){
            showError("No Submission file is available for this evaluation yet.");
            return;
        }
        try {
            OpenPdfUtil.openPdfInApp(e.getFeedback(), "Evaluation PDF");
        } catch (IOException ex) {
            showError("Could not open Activity PDF:\n" + ex.getMessage());
        }

    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Open PDF");
        alert.setHeaderText("Unable to open PDF");
        alert.setContentText(message);
        alert.showAndWait();
    }

}
