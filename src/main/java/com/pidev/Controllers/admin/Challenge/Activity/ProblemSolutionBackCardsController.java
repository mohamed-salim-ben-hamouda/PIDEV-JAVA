package com.pidev.Controllers.admin.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.ProblemSolution;
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

public class ProblemSolutionBackCardsController {

    @FXML
    private Label problemDescription;
    @FXML
    private Label groupSolution;
    @FXML
    private Label supervisorSolution;
    @FXML
    private Label idActivityRef;

    private ProblemSolution problemSolutionModel;
    private Consumer<Void> onDeleteCallback;

    public void initData(ProblemSolution problemSolutionModel, Consumer<Void> onDeleteCallback) {
        this.problemSolutionModel = problemSolutionModel;
        this.onDeleteCallback = onDeleteCallback;
        String desc = problemSolutionModel != null ? problemSolutionModel.getProblemDescription() : null;
        problemDescription.setText(desc == null || desc.isBlank() ? "No Data" : desc);
        String grp = problemSolutionModel != null ? problemSolutionModel.getGroupSolution() : null;
        groupSolution.setText(grp == null || grp.isBlank() ? "No Data" : grp);
        String sup = problemSolutionModel != null ? problemSolutionModel.getSupervisorSolution() : null;
        supervisorSolution.setText(sup == null || sup.isBlank() ? "No Data" : sup);
        Integer activityId = problemSolutionModel != null && problemSolutionModel.getActivity() != null ? problemSolutionModel.getActivity().getId() : null;
        idActivityRef.setText(activityId == null ? "N/A" : "#" + activityId);
    }

    @FXML
    public void OnDelete() {
        if (problemSolutionModel == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Problem Solution");
        alert.setContentText("Are you sure you want to delete this Problem Solution?");

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
            ServiceProblemSolution service = new ServiceProblemSolution();
            service.delete(problemSolutionModel.getId());
            if (onDeleteCallback != null) {
                onDeleteCallback.accept(null);
            }
        }
    }
}

