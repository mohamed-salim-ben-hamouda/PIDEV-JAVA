package com.pidev.Controllers.client;

import com.pidev.Services.AIService;
import com.pidev.models.AtsAnalysis;
import com.pidev.models.Cv;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;

public class AtsAnalysisController {

    @FXML private VBox inputForm;
    @FXML private VBox loadingOverlay;
    @FXML private VBox resultsView;

    @FXML private TextField jobTitleField;
    @FXML private TextArea jobDescriptionArea;

    @FXML private Label resultJobTitleLabel;
    @FXML private Label scoreLabel;
    @FXML private ProgressBar scoreProgressBar;

    @FXML private VBox matchedSkillsBox;
    @FXML private VBox missingSkillsBox;
    @FXML private VBox strengthsBox;
    @FXML private VBox weaknessesBox;
    @FXML private VBox suggestionsBox;

    private Cv currentCv;
    private final AIService aiService = new AIService();

    public void setCv(Cv cv) {
        this.currentCv = cv;
    }

    @FXML
    public void handleAnalyze() {
        String title = jobTitleField.getText();
        String description = jobDescriptionArea.getText();

        if (title.isBlank() || description.isBlank()) {
            showError("Champs obligatoires", "Veuillez remplir le titre et la description du poste.");
            return;
        }

        inputForm.setDisable(true);
        loadingOverlay.setVisible(true);

        new Thread(() -> {
            try {
                AtsAnalysis analysis = aiService.analyzeAtsWithAI(currentCv, title, description);
                Platform.runLater(() -> displayResults(analysis, title));
            } catch (Exception e) {
                Platform.runLater(() -> {
                    loadingOverlay.setVisible(false);
                    inputForm.setDisable(false);
                    showError("Erreur d'analyse", "Impossible d'analyser le CV : " + e.getMessage());
                });
            }
        }).start();
    }

    private void displayResults(AtsAnalysis analysis, String title) {
        loadingOverlay.setVisible(false);
        inputForm.setVisible(false);
        resultsView.setVisible(true);

        resultJobTitleLabel.setText("Analyse pour : " + title);
        scoreLabel.setText(analysis.getScore() + "%");
        scoreProgressBar.setProgress(analysis.getScore() / 100.0);

        populateBox(matchedSkillsBox, analysis.getMatchedSkills(), "#166534");
        populateBox(missingSkillsBox, analysis.getMissingSkills(), "#991b1b");
        populateBox(strengthsBox, analysis.getStrengths(), "#1e293b");
        populateBox(weaknessesBox, analysis.getWeaknesses(), "#1e293b");
        populateBox(suggestionsBox, analysis.getSuggestions(), "#1e40af");
    }

    private void populateBox(VBox box, List<String> items, String color) {
        box.getChildren().clear();
        if (items == null || items.isEmpty()) {
            Label empty = new Label("Aucune donnée disponible");
            empty.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            box.getChildren().add(empty);
            return;
        }
        for (String item : items) {
            Label label = new Label("• " + item);
            label.setWrapText(true);
            label.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13;");
            box.getChildren().add(label);
        }
    }

    @FXML
    public void handleNewAnalysis() {
        resultsView.setVisible(false);
        inputForm.setVisible(true);
        inputForm.setDisable(false);
        jobTitleField.clear();
        jobDescriptionArea.clear();
    }

    @FXML
    public void handleClose() {
        ((Stage) inputForm.getScene().getWindow()).close();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
