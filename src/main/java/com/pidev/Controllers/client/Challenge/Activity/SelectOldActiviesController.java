package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceActivity.OldActivityCardData;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.concurrent.Task;
import javafx.scene.CacheHint;
import javafx.scene.control.Label;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.util.List;

public class SelectOldActiviesController {
    private static final int USER_ID = 2;
    private static final double CARD_WIDTH = 350.0;
    private static final double CARD_HEIGHT = 720.0;
    private static final double H_GAP = 25.0;

    @FXML
    private TilePane ActivityCards;
    @FXML
    private VBox mainContentContainer;

    private final ServiceActivity serviceAct = new ServiceActivity();

    public void initialize() {
        ActivityCards.setPrefTileWidth(CARD_WIDTH);
        ActivityCards.setPrefTileHeight(CARD_HEIGHT);
        ActivityCards.setPrefColumns(1);

        mainContentContainer.widthProperty().addListener((obs, oldWidth, newWidth) -> {
            double availableWidth = Math.max(0, newWidth.doubleValue() - 40);
            int columns = Math.max(1, (int) Math.floor((availableWidth + H_GAP) / (CARD_WIDTH + H_GAP)));
            ActivityCards.setPrefColumns(columns);
        });
        displayActivities();
    }

    public void displayActivities() {
        Task<List<OldActivityCardData>> loadTask = new Task<List<OldActivityCardData>>() {
            @Override
            protected List<OldActivityCardData> call() {
                return serviceAct.getOldActivityCardDataForUser(USER_ID);
            }
        };

        loadTask.setOnSucceeded(event -> {
            renderActivities(loadTask.getValue());
        });

        loadTask.setOnFailed(event -> {
            if (loadTask.getException() != null) {
                loadTask.getException().printStackTrace();
            }
        });

        Thread worker = new Thread(loadTask, "old-activities-loader");
        worker.setDaemon(true);
        worker.start();
    }

    private void renderActivities(List<OldActivityCardData> activities) {
        ActivityCards.getChildren().clear();

        if (activities == null || activities.isEmpty()) {
            Label emptyLabel = new Label("No old activities found.");
            emptyLabel.getStyleClass().add("meta-text");
            ActivityCards.getChildren().add(emptyLabel);
            return;
        }

        for (OldActivityCardData activityData : activities) {
            ActivityCards.getChildren().add(createCard(activityData));
        }
    }

    private VBox createCard(OldActivityCardData activityData) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/OldActivitiesCards.fxml"));
            VBox card = loader.load();

            OldActivitiesCardsController ctrl = loader.getController();
            ctrl.setData(activityData.getActivity(), activityData.isLeader(), activityData.hasEvaluation());
            card.setCache(true);
            card.setCacheHint(CacheHint.SPEED);
            return card;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
