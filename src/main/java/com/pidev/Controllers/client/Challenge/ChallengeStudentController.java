package com.pidev.Controllers.client.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;

import javafx.scene.layout.VBox;


import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ResourceBundle;

public class ChallengeStudentController implements Initializable {
    @FXML
    private VBox challengeListContainer;

    private final ServiceChallenge service = new ServiceChallenge();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        refreshChallenges();
    }

    private void refreshChallenges() {
        challengeListContainer.getChildren().clear();

        List<Challenge> challenges = service.display();
        if (challenges == null || challenges.isEmpty()) {
            Label empty = new Label("No challenges yet");
            challengeListContainer.getChildren().add(empty);
            return;
        }

        for (Challenge c : challenges) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/ChallengeCard.fxml"));
                VBox card = loader.load();
                ChallengeCardController cardController = loader.getController();
                cardController.setData(c,v -> refreshChallenges());
                cardController.StudentCard(cardController.editBtn);
                cardController.StudentCard(cardController.deleteBtn);
                challengeListContainer.getChildren().add(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }


}
