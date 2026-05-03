package com.pidev.Controllers.client.Challenge;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import com.pidev.models.GroupPredictionCardData;
import com.pidev.models.PredictionInput;
import com.pidev.utils.ML_python;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class GrpsPredictionController {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ServiceActivity serviceActivity = new ServiceActivity();

    private List<Group> groups = List.of();
    private Challenge challenge;

    @FXML
    private Label ChallengeName;
    @FXML
    private FlowPane groupCards;
    @FXML
    private Button predictAllBtn;
    @FXML
    private Label bestCandidateName;
    @FXML
    private Label bestCandidateScore;

    @FXML
    private Label averageConfidenceValue;
    @FXML
    private Label averageConfidenceCaption;
    @FXML
    private Label pageStatusLabel;

    public void initData(List<Group> groups, Challenge challenge) {
        this.groups = groups != null ? groups : List.of();
        this.challenge = challenge;
        ChallengeName.setText("Challenge - " + challenge.getTitle());
        loadPlaceholderCards();
        resetSummary();
        Platform.runLater(this::startPredictionTask);
    }

    @FXML
    private void onPredictAll() {
        startPredictionTask();
    }

    private void startPredictionTask() {
        if (predictAllBtn.isDisabled()) {
            return;
        }
        predictAllBtn.setDisable(true);
        pageStatusLabel.setText("Loading predictions...");

        Task<List<GroupPredictionCardData>> predictionTask = new Task<>() {
            @Override
            protected List<GroupPredictionCardData> call() throws Exception {
                return buildPredictionCards();
            }
        };

        predictionTask.setOnSucceeded(event -> {
            List<GroupPredictionCardData> predictionCards = predictionTask.getValue();
            renderCards(predictionCards);
            updateSummary(predictionCards);
            long availableCount = predictionCards.stream().filter(GroupPredictionCardData::isAvailable).count();
            pageStatusLabel.setText("Loaded predictions for " + availableCount + " group(s).");
            predictAllBtn.setDisable(false);
        });

        predictionTask.setOnFailed(event -> {
            Throwable error = predictionTask.getException();
            pageStatusLabel.setText("Prediction failed. " + (error != null ? error.getMessage() : ""));
            predictAllBtn.setDisable(false);
            if (error != null) {
                error.printStackTrace();
            }
        });

        Thread predictionThread = new Thread(predictionTask, "challenge-group-predictions");
        predictionThread.setDaemon(true);
        predictionThread.start();
    }

    private List<GroupPredictionCardData> buildPredictionCards() throws Exception {
        List<GroupPredictionCardData> cardData = new ArrayList<>();
        List<Group> predictableGroups = new ArrayList<>();
        List<Map<String, Object>> batchPayload = new ArrayList<>();

        for (Group group : groups) {
            try {
                PredictionInput input = serviceActivity.getPredictionInput(group.getId(), challenge.getId());
                Map<String, Object> payloadRow = new LinkedHashMap<>();
                payloadRow.put("avg_group_score", input.getAvgGroupScore());
                payloadRow.put("avg_completion_time", input.getAvgCompletionTime());
                payloadRow.put("difficulty", input.getDifficulty());
                payloadRow.put("deadline_days", input.getDeadlineDays());
                payloadRow.put("group_skill_variance", input.getGroupSkillVariance());
                payloadRow.put("group_size", input.getGroupSize());
                batchPayload.add(payloadRow);
                predictableGroups.add(group);
            } catch (IllegalStateException noHistoryError) {
                cardData.add(
                        new GroupPredictionCardData(
                                group,
                                "NO DATA",
                                0,
                                0,
                                false,
                                "No previous challenge history",
                                "This group cannot be scored yet because it has no historical submitted challenges."
                        )
                );
            }
        }

        if (!batchPayload.isEmpty()) {
            String payloadJson = OBJECT_MAPPER.writeValueAsString(batchPayload);
            List<ML_python.PredictionResult> predictionResults = ML_python.predictAll(payloadJson);

            for (int index = 0; index < predictableGroups.size(); index++) {
                Group group = predictableGroups.get(index);
                ML_python.PredictionResult result = predictionResults.get(index);
                cardData.add(
                        new GroupPredictionCardData(
                                group,
                                result.getLabel(),
                                result.getSuccess_percentage(),
                                result.getFail_percentage(),
                                true,
                                buildSubtitle(result),
                                buildRecommendation(result)
                        )
                );
            }
        }

        cardData.sort(
                Comparator.comparing(GroupPredictionCardData::isAvailable).reversed()
                        .thenComparing(GroupPredictionCardData::getSuccessPercentage, Comparator.reverseOrder())
        );

        return cardData;
    }

    private String buildSubtitle(ML_python.PredictionResult result) {
        if ("success".equalsIgnoreCase(result.getLabel()) && result.getSuccess_percentage() >= 75) {
            return "Strong candidate for this challenge";
        }
        if ("success".equalsIgnoreCase(result.getLabel())) {
            return "Moderate confidence result";
        }
        if (result.getFail_percentage() >= 70) {
            return "High risk profile";
        }
        return "Needs closer review";
    }

    private String buildRecommendation(ML_python.PredictionResult result) {
        if ("success".equalsIgnoreCase(result.getLabel()) && result.getSuccess_percentage() >= 75) {
            return "This group looks like a strong fit. Prioritize it for the challenge if other constraints also align.";
        }
        if ("success".equalsIgnoreCase(result.getLabel())) {
            return "This group is viable, but compare it with stronger candidates before making the final choice.";
        }
        if (result.getFail_percentage() >= 70) {
            return "This group appears risky. Consider another group or review capability gaps before assigning the challenge.";
        }
        return "Prediction is weak. Review the group manually before making a decision.";
    }

    private void loadPlaceholderCards() {
        List<GroupPredictionCardData> placeholders = new ArrayList<>();
        for (Group group : groups) {
            placeholders.add(
                    new GroupPredictionCardData(
                            group,
                            "PENDING",
                            0,
                            0,
                            false,
                            "Prediction not generated yet",
                            "Use Predict All Groups to generate model output for this challenge."
                    )
            );
        }
        renderCards(placeholders);
    }

    private void renderCards(List<GroupPredictionCardData> cards) {
        groupCards.getChildren().clear();
        for (GroupPredictionCardData cardData : cards) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/GrpsPredictionCards.fxml"));
                VBox card = loader.load();
                GrpsPredictionCardsController controller = loader.getController();
                controller.setCardData(cardData);
                groupCards.getChildren().add(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void resetSummary() {
        bestCandidateName.setText("Not computed yet");
        bestCandidateScore.setText("Run Predict All Groups");

        averageConfidenceValue.setText("--");
        averageConfidenceCaption.setText("Waiting for prediction results");
        pageStatusLabel.setText("Click Predict All Groups to load scores.");
    }

    private void updateSummary(List<GroupPredictionCardData> cards) {
        List<GroupPredictionCardData> availableCards = cards.stream()
                .filter(GroupPredictionCardData::isAvailable)
                .toList();

        if (availableCards.isEmpty()) {
            bestCandidateName.setText("No available data");
            bestCandidateScore.setText("No group had enough history");

            averageConfidenceValue.setText("--");
            averageConfidenceCaption.setText("No historical challenge data was available");
            return;
        }

        GroupPredictionCardData bestCandidate = availableCards.stream()
                .max(Comparator.comparing(GroupPredictionCardData::getSuccessPercentage))
                .orElseThrow();
        GroupPredictionCardData riskyGroup = availableCards.stream()
                .max(Comparator.comparing(GroupPredictionCardData::getFailPercentage))
                .orElseThrow();
        double averageConfidence = availableCards.stream()
                .mapToDouble(GroupPredictionCardData::getSuccessPercentage)
                .average()
                .orElse(0);

        bestCandidateName.setText(bestCandidate.getGroup().getName());
        bestCandidateScore.setText(String.format("%.2f%% success probability", bestCandidate.getSuccessPercentage()));

        averageConfidenceValue.setText(String.format("%.2f%%", averageConfidence));
        averageConfidenceCaption.setText("Average success confidence across predicted groups");
    }
}
