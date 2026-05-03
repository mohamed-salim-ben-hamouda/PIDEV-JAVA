package com.pidev.Controllers.client.Challenge;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import com.pidev.models.PredictionInput;
import com.pidev.utils.ML_python;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import org.json.JSONObject;

import java.time.LocalDateTime;

public class AdminGroupCardController {
    @FXML
    private Label groupNameLabel;
    @FXML
    private Button chooseBtn;
    @FXML
    private Button predictBtn;
    @FXML
    private Label predictionStatusLabel;
    @FXML
    private Label predictionPercentagesLabel;

    private int groupId;
    private Challenge c;
    private final ServiceActivity serviceAct = new ServiceActivity();

    public void setGroupData(Group g, Challenge c) {
        this.groupId = g.getId();
        this.c = c;
        groupNameLabel.setText("Group: " + g.getName());
        predictionStatusLabel.setText("Prediction not loaded yet.");
        predictionPercentagesLabel.setText("");
        predictionStatusLabel.setStyle("-fx-text-fill: #475467; -fx-font-size: 12;");

        boolean isLocked = serviceAct.isActivityPassedByGrp(this.groupId, this.c.getId());
        if (isLocked) {
            chooseBtn.setDisable(false);
        } else {
            chooseBtn.setDisable(true);
            chooseBtn.setText("Already Passed");
            chooseBtn.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white;");
        }
    }

    @FXML
    private void OnChoose(ActionEvent event) {
        Activity a = new Activity();
        Group g = new Group();
        g.setId(groupId);
        a.setActivity_start_time(LocalDateTime.now());
        serviceAct.StartActivity(a, c, g);
        Integer activityId = a.getId();
        if (activityId == null) {
            System.out.println("OnChoose: activityId is null after StartActivity; navigation canceled.");
            return;
        }
        BaseController.getInstance().loadActivityPage(c, g.getId(), activityId);
    }


    private String getPredictionInputJson(int groupId, int challengeId) {
        PredictionInput input = serviceAct.getPredictionInput(groupId, challengeId);
        JSONObject json = new JSONObject();
        json.put("avg_group_score", input.getAvgGroupScore());
        json.put("avg_completion_time", input.getAvgCompletionTime());
        json.put("difficulty", input.getDifficulty());
        json.put("deadline_days", input.getDeadlineDays());
        json.put("group_skill_variance", input.getGroupSkillVariance());
        json.put("group_size", input.getGroupSize());
        return json.toString();
    }

    @FXML
    public void OnPredict() {
        predictionStatusLabel.setStyle("-fx-text-fill: #475467; -fx-font-size: 12;");
        predictionStatusLabel.setText("Loading prediction...");
        predictionPercentagesLabel.setText("");
        predictBtn.setDisable(true);

        Task<ML_python.PredictionResult> predictionTask = new Task<>() {
            @Override
            protected ML_python.PredictionResult call() throws Exception {
                String jsonInput = getPredictionInputJson(groupId, c.getId());
                return ML_python.predict(jsonInput);
            }
        };

        predictionTask.setOnSucceeded(event -> {
            ML_python.PredictionResult result = predictionTask.getValue();
            predictionStatusLabel.setText("Prediction: " + capitalizeLabel(result.getLabel()));
            predictionStatusLabel.setStyle(
                    "success".equalsIgnoreCase(result.getLabel())
                            ? "-fx-text-fill: #1f7a1f; -fx-font-size: 12; -fx-font-weight: bold;"
                            : "-fx-text-fill: #b42318; -fx-font-size: 12; -fx-font-weight: bold;"
            );
            predictionPercentagesLabel.setText(
                    String.format(
                            "Success: %.2f%% | Fail: %.2f%%",
                            result.getSuccess_percentage(),
                            result.getFail_percentage()
                    )
            );
            predictBtn.setDisable(false);
        });

        predictionTask.setOnFailed(event -> {
            Throwable error = unwrapCause(predictionTask.getException());
            predictionStatusLabel.setStyle("-fx-text-fill: #b42318; -fx-font-size: 12; -fx-font-weight: bold;");
            predictionStatusLabel.setText(buildPredictionErrorMessage(error));
            predictionPercentagesLabel.setText("");
            predictBtn.setDisable(false);
            error.printStackTrace();
        });

        Thread predictionThread = new Thread(predictionTask, "group-prediction-" + groupId);
        predictionThread.setDaemon(true);
        predictionThread.start();
    }

    private String buildPredictionErrorMessage(Throwable error) {
        if (error instanceof IllegalStateException && error.getMessage() != null) {
            if (error.getMessage().contains("No historical")) {
                return "No historical challenge data for this group yet.";
            }
            return error.getMessage();
        }
        return "Prediction failed. Check Python and artifacts setup.";
    }

    private Throwable unwrapCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null && current.getCause() != null) {
            current = current.getCause();
        }
        return current != null ? current : throwable;
    }

    private String capitalizeLabel(String label) {
        if (label == null || label.isBlank()) {
            return "Unknown";
        }
        return label.substring(0, 1).toUpperCase() + label.substring(1).toLowerCase();
    }
}
