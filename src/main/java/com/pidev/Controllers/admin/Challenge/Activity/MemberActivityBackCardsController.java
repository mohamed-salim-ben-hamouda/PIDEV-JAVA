package com.pidev.Controllers.admin.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.models.MemberActivity;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.function.Consumer;

public class MemberActivityBackCardsController {

    @FXML
    private Label activityDescription;
    @FXML
    private Label indivScore;
    @FXML
    private Label idActivityRef;
    @FXML
    private Label idUserRef;

    private MemberActivity memberActivity;
    private Consumer<Void> onDeleteCallback;

    public void initData(MemberActivity memberActivity, Consumer<Void> onDeleteCallback) {
        this.memberActivity = memberActivity;
        this.onDeleteCallback = onDeleteCallback;
        String description = memberActivity != null ? memberActivity.getActivityDescription() : null;
        activityDescription.setText(description == null || description.isBlank() ? "No Data" : description);
        Double score = memberActivity != null ? memberActivity.getIndivScore() : null;
        indivScore.setText(score == null ? "No Data" : String.valueOf(score));
        Integer activityId = memberActivity != null && memberActivity.getActivity() != null ? memberActivity.getActivity().getId() : null;
        idActivityRef.setText(activityId == null ? "N/A" : "#" + activityId);
        Integer userId = memberActivity != null && memberActivity.getUser() != null ? memberActivity.getUser().getId() : null;
        idUserRef.setText(userId == null ? "N/A" : "#" + userId);
    }

    @FXML
    public void OnDelete() {
        if (memberActivity == null || memberActivity.getId() == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Member Activity");
        alert.setContentText("Are you sure you want to delete this Member Activity?");

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
        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            ServiceMemberActivity service = new ServiceMemberActivity();
            service.delete(memberActivity.getId());
            if (onDeleteCallback != null) {
                onDeleteCallback.accept(null);
            }
        }
    }
}

