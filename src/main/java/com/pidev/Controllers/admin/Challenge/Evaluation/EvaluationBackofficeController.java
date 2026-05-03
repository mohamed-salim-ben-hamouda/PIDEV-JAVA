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
        sortComboBox.getItems().addAll("ID", "Group Score");

        sortComboBox.valueProperty().addListener((obs, oldVal, newVal) -> {
            refreshEvaluations();
        });

        refreshEvaluations();
    }

    public void displayEvaluation(List<Evaluation> evaluations) {
        EvaluationCardsContainer.getChildren().clear();

        for (Evaluation e : evaluations) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Evaluation/EvaluationBackCards.fxml"));
                HBox card = loader.load();
                EvaluationBackCardsController cardController = loader.getController();

                cardController.initData(e, v -> refreshEvaluations());

                EvaluationCardsContainer.getChildren().add(card);
            } catch (Exception ex) {
                System.err.println("Error loading evaluation card: " + ex.getMessage());
                ex.printStackTrace();
            }
        }
    }

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
