package com.pidev.Controllers.client;

import com.pidev.Services.ServiceHackathon;
import com.pidev.models.Hackathon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.FlowPane;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class HackathonClientController implements Initializable {

    @FXML private FlowPane hackathonsFlowPane;

    private ServiceHackathon serviceHackathon = new ServiceHackathon();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadHackathons();
    }

    private void loadHackathons() {
        List<Hackathon> list = serviceHackathon.getAll();
        hackathonsFlowPane.getChildren().clear();

        for (Hackathon h : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/hackathon_card.fxml"));
                Parent card = loader.load();
                HackathonCardController controller = loader.getController();
                controller.setHackathonData(h);
                hackathonsFlowPane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
