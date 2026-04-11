package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Activity;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.concurrent.Task;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;

import java.util.List;

public class SelectOldActiviesController {
    @FXML
    private FlowPane ActivityCards;
    ServiceActivity serviceAct = new ServiceActivity();

    public void initialize() {
        displayActivities();
    }

    public void displayActivities() {
        int user_id = 2;

        Task<List<Activity>> loadTask = new Task<List<Activity>>() {
            @Override
            protected List<Activity> call() {
                return serviceAct.getOldActivitiesForUser(user_id);
            }
        };

        loadTask.setOnSucceeded(event -> {
            List<Activity> list = loadTask.getValue();
            ActivityCards.getChildren().clear();

            for (Activity activity : list) {
                try {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/OldActivitiesCards.fxml"));
                    VBox card = loader.load();

                    OldActivitiesCardsController ctrl = loader.getController();
                    ctrl.setData(activity);
                    ActivityCards.getChildren().add(card);

                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
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
}
