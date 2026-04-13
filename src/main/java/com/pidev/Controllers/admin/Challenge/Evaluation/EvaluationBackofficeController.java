package com.pidev.Controllers.admin.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.models.Evaluation;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
public class EvaluationBackofficeController implements Initializable {
    @FXML
    private VBox EvaluationCardsContainer;

    @FXML
    private ComboBox<String> sortComboBox; // Added this to match your FXML

    private ServiceEvaluation serviceEva = new ServiceEvaluation();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // 1. Populate the ComboBox
        sortComboBox.getItems().addAll("ID", "Group Score");

        // 2. Listen for selection changes
        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            refreshEvaluations();
        });

        // 3. Initial display (keeps current behavior when no sort is selected)
        refreshEvaluations();
    }

    // Refactored to accept a list so it can handle both "all" and "sorted"
    public void displayEvaluation(List<Evaluation> evaluations) {
        EvaluationCardsContainer.getChildren().clear();

        for (Evaluation e : evaluations) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Evaluation/EvaluationBackCards.fxml"));
                HBox card = loader.load();
                EvaluationBackCardsController cardController = loader.getController();

                // Passing the refresh callback using a lambda
                cardController.initData(e, v -> refreshEvaluations());

                EvaluationCardsContainer.getChildren().add(card);
            } catch (Exception ex) {
                System.err.println("Error loading evaluation card: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

    // Overload for the callback refresh
    public void displayEvaluation() {
        refreshEvaluations();
    }

    private void refreshEvaluations() {
        String criteria = sortComboBox != null ? sortComboBox.getValue() : null;
        if (criteria == null || criteria.isBlank()) {
            displayEvaluation(serviceEva.displayAll());
            return;
        }
        List<Evaluation> sortedList = serviceEva.displaySorted(criteria);
        displayEvaluation(sortedList);
    }
}
