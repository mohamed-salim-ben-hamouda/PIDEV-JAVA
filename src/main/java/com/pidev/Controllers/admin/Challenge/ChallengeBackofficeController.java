package com.pidev.Controllers.admin.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
public class ChallengeBackofficeController implements Initializable {
    @FXML private VBox challengeCardsContainer;
    @FXML private TextField searchField;

    private ServiceChallenge serviceC = new ServiceChallenge();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        renderAllChallenges();
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal == null || newVal.trim().isEmpty()) {
                    renderAllChallenges();
                } else {
                    List<Challenge> filtered = serviceC.searchChallenge(newVal.trim());
                    populateContainer(filtered);
                }
            });
        }
    }

    private void renderAllChallenges() {
        populateContainer(serviceC.displayALL());
    }

    private void populateContainer(List<Challenge> challenges) {
        challengeCardsContainer.getChildren().clear();

        for (Challenge c : challenges) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/ChallengeBackCards.fxml"));
                HBox card = loader.load();
                ChallengeBackCardsController cardController = loader.getController();
                cardController.initData(c, v -> renderAllChallenges());
                challengeCardsContainer.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Error loading challenge card: " + e.getMessage());
            }
        }
    }
}
