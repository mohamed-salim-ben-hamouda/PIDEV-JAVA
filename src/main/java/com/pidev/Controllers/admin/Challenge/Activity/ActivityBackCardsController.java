package com.pidev.Controllers.admin.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Activity;
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
import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ActivityBackCardsController {
    @FXML
    private Label idActivity;
    @FXML
    private Label submissionDate;
    @FXML
    private Label statusActivity;
    @FXML
    private Label idChallengeRef;
    @FXML
    private Label idGroupRef;
    private Activity a;
    private Consumer<Void> onDeleteCallback;

    public void initData(Activity a, Consumer<Void> onDeleteCallback){
        this.onDeleteCallback = onDeleteCallback;
        this.a=a;

        idActivity.setText("#" + a.getId());
        if (a.getSubmissionDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
            submissionDate.setText(a.getSubmissionDate().format(formatter));
        } else {
            submissionDate.setText("No Date");
        }
        statusActivity.setText(a.getStatus());
        applyStatusStyle(a.getStatus().toUpperCase());
        if (a.getChallenge() != null) {
            idChallengeRef.setText("#" + a.getChallenge().getId());
        } else {
            idChallengeRef.setText("N/A");
        }
        if (a.getGroup() != null) {
            idGroupRef.setText("#" + a.getGroup().getId());
        } else {
            idGroupRef.setText("N/A");
        }

    }
    @FXML
    public void OnDelete(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Activity");
        alert.setContentText("Are you sure you want to delete this Activity?");
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
            ServiceActivity service = new ServiceActivity();
            service.delete(a.getId());

            if (onDeleteCallback != null) {
                onDeleteCallback.accept(null);
            }
        }

    }
    @FXML
    public void openPdfActivity(){
        if(a == null || a.getSubmissionFile() == null || a.getSubmissionFile().isBlank()){
            showError("No Submission file is available for this activity yet.");
            return;
        }
        try {
            OpenPdfUtil.openPdfInApp(a.getSubmissionFile(), "Activity PDF");
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
    private void applyStatusStyle(String level) {
        String bgColor;
        String textColor;

        switch (level) {
            case "EVALUATED":
                bgColor = "#dcfce7";
                textColor = "#166534";
                break;
            case "SUBMITTED":
                bgColor = "#fef3c7";
                textColor = "#92400e";
                break;
            default:
                bgColor = "#f3f4f6"; // Gray
                textColor = "#374151";
                break;
        }

        statusActivity.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 10;"
        );
    }
}
