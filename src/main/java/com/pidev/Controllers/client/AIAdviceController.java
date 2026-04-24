package com.pidev.Controllers.client;

import com.pidev.models.Hackathon;
import com.pidev.utils.hackthon.GeminiService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class AIAdviceController {

    @FXML private Label hackathonNameLabel;
    @FXML private Label adviceLabel;
    @FXML private VBox loadingBox;
    @FXML private ScrollPane adviceScrollPane;

    private Hackathon currentHackathon;

    public void setHackathon(Hackathon h) {
        this.currentHackathon = h;
        hackathonNameLabel.setText("Hackathon : " + h.getTitle());
        loadAIAdvice();
    }

    private void loadAIAdvice() {
        loadingBox.setVisible(true);
        adviceScrollPane.setVisible(false);

        GeminiService.getAdvice(currentHackathon.getTitle(), currentHackathon.getTheme())
            .thenAccept(advice -> {
                Platform.runLater(() -> {
                    adviceLabel.setText(advice);
                    loadingBox.setVisible(false);
                    adviceScrollPane.setVisible(true);
                });
            });
    }

    @FXML
    private void goBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/HackathonView.fxml"));
            Parent view = loader.load();
            StackPane contentArea = (StackPane) hackathonNameLabel.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
