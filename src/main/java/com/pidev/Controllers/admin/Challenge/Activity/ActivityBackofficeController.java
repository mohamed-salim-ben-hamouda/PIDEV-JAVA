package com.pidev.Controllers.admin.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Activity;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class ActivityBackofficeController implements Initializable {
    @FXML
    private VBox ActivityCardsContainer;
    private ServiceActivity serviceA = new ServiceActivity();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        displayActivities();
    }

    public void displayActivities(){
        ActivityCardsContainer.getChildren().clear();
        List<Activity> activities = serviceA.displayAll();
        for(Activity a : activities){
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Activity/ActivityBackCards.fxml"));
                HBox card = loader.load();
                ActivityBackCardsController cardController = loader.getController();
                cardController.initData(a,v -> displayActivities());
                ActivityCardsContainer.getChildren().add(card);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
}
