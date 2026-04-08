package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import java.io.IOException;
import java.util.List;
import javafx.scene.layout.FlowPane;
import javafx.geometry.Pos;

public class SelectToEvaluateController {
    @FXML
    private FlowPane challengeCardsPane;

    private final ServiceChallenge serviceCh = new ServiceChallenge();
    private final ServiceActivity serviceAc = new ServiceActivity();
    private VBox openItemsContainer;
    private int openChallengeId = -1;

    @FXML
    public void initialize() {
        displayChallenges();
    }

    public void displayChallenges() {
        List<Challenge> list = serviceCh.findChallengeWithActivities();
        challengeCardsPane.getChildren().clear();
        openItemsContainer = null;
        openChallengeId = -1;

        for (Challenge challenge : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Evaluation/EvaluationCards.fxml"));
                VBox card = loader.load();

                EvaluationCardController ctrl = loader.getController();
                ctrl.setData(challenge);

                VBox wrapper = new VBox(12);
                wrapper.setAlignment(Pos.TOP_CENTER);
                wrapper.setFillWidth(true);
                wrapper.setPrefWidth(card.getPrefWidth());

                VBox itemsContainer = new VBox(12);
                itemsContainer.setAlignment(Pos.TOP_CENTER);
                itemsContainer.setVisible(false);
                itemsContainer.setManaged(false);

                ctrl.getActionButton().setOnAction(event -> toggleItemsUnderCard(challenge, itemsContainer));

                wrapper.getChildren().addAll(card, itemsContainer);

                challengeCardsPane.getChildren().add(wrapper);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void toggleItemsUnderCard(Challenge challenge, VBox itemsContainer) {
        if (itemsContainer.isVisible()) {
            hideItems(itemsContainer);
            openItemsContainer = null;
            openChallengeId = -1;
            return;
        }

        if (openItemsContainer != null && openItemsContainer != itemsContainer) {
            hideItems(openItemsContainer);
        }

        openItemsContainer = itemsContainer;
        openChallengeId = challenge.getId();
        showItemsForChallenge(challenge, itemsContainer);
    }

    private void showItemsForChallenge(Challenge challenge, VBox itemsContainer) {
        itemsContainer.getChildren().clear();
        List<Group> groups = serviceAc.findGroupsInActivity(challenge.getId());
        for (Group group : groups) {
            try {
                FXMLLoader itemLoader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Evaluation/EvaluationItem.fxml"));
                VBox itemRow = itemLoader.load();
                EvaluationItemController itemCtrl = itemLoader.getController();
                itemCtrl.setGroupData(group, challenge);
                itemsContainer.getChildren().add(itemRow);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        itemsContainer.setVisible(true);
        itemsContainer.setManaged(true);
    }

    private void hideItems(VBox itemsContainer) {
        itemsContainer.getChildren().clear();
        itemsContainer.setVisible(false);
        itemsContainer.setManaged(false);
    }
}
