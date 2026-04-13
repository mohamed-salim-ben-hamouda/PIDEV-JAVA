package com.pidev.Controllers.admin.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ChallengeBackofficeController implements Initializable {
    @FXML
    private VBox challengeCardsContainer;
    private ServiceChallenge serviceC = new ServiceChallenge();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        displayChallenges();
    }

    public void displayChallenges() {
        challengeCardsContainer.getChildren().clear();

        List<Challenge> challenges = serviceC.displayALL();

        for (Challenge c : challenges) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/ChallengeBackCards.fxml"));
                HBox card = loader.load();
                ChallengeBackCardsController cardController = loader.getController();
                cardController.initData(c,v -> displayChallenges());
                challengeCardsContainer.getChildren().add(card);

            } catch (IOException e) {
                System.err.println("Could not load Challenge card: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


}
