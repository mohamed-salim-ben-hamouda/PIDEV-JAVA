package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.Challenge.ChallengeCardController;
import com.pidev.models.Challenge;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class ActivityController {
    @FXML
    private VBox challengeListContainer;
    private Challenge c;
    private int grpId;


    public void initData(Challenge c, int grpId) {
        this.c = c;
        this.grpId = grpId;
        renderChallengeHeader();

    }

    private void renderChallengeHeader() {
        try {
            challengeListContainer.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/ChallengeCard.fxml"));
            VBox card = loader.load();
            ChallengeCardController cardController = loader.getController();
            cardController.setData(c,v->renderChallengeHeader());
            cardController.StudentCard(cardController.editBtn);
            cardController.StudentCard(cardController.deleteBtn);
            cardController.StudentCard(cardController.groupsBtn);
            cardController.StudentCard(cardController.participationBtn);
            challengeListContainer.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("Error loading challenge header: " + e.getMessage());
        }
    }


}
