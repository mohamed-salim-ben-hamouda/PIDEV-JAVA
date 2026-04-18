package com.pidev.Controllers.admin.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.time.format.DateTimeFormatter;
import java.util.function.Consumer;

public class ChallengeBackCardsController {

    @FXML
    private Label titleChallenge;
    @FXML
    private Label descChallenge;
    @FXML
    private Label skillChallenge;
    @FXML
    private Label levelChallenge;
    @FXML
    private Label minChallenge;
    @FXML
    private Label maxChallenge;
    @FXML
    private Label deadlineChallenge;
    private Challenge c;
    private Consumer<Void> onDeleteCallback;


    public void initData(Challenge c,Consumer<Void> onDeleteCallback){
        this.c = c;
        this.onDeleteCallback = onDeleteCallback;
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
        String formattedDate = c.getDeadLine().format(formatter);
        titleChallenge.setText(c.getTitle());
        descChallenge.setText(c.getDescription());
        skillChallenge.setText(c.getTargetSkill());
        levelChallenge.setText(c.getDifficulty());
        applyLevelStyle(c.getDifficulty().toUpperCase());
        minChallenge.setText(String.valueOf(c.getMinGroupNbr()));
        maxChallenge.setText(String.valueOf(c.getMaxGroupNbr()));
        deadlineChallenge.setText(formattedDate);
    }
    @FXML
    public void OnDelete(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Challenge");
        alert.setContentText("Are you sure you want to delete this challenge?");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/challenge.css").toExternalForm());
        dialogPane.getStyleClass().add("my-custom-alert");
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("alert-primary-btn");
        }

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        if (alert.showAndWait().get() == ButtonType.OK) {
            ServiceChallenge service = new ServiceChallenge();
            service.delete(c.getId());

            if (onDeleteCallback != null) {
                onDeleteCallback.accept(null);
            }
        }

    }
    private void applyLevelStyle(String level) {
        String bgColor;
        String textColor;

        switch (level) {
            case "EASY":
                bgColor = "#dcfce7"; // Soft Green
                textColor = "#166534"; // Dark Green
                break;
            case "MEDIUM":
                bgColor = "#fef3c7"; // Soft Yellow/Amber
                textColor = "#92400e"; // Dark Brown/Amber
                break;
            case "HARD":
                bgColor = "#fee2e2"; // Soft Red
                textColor = "#991b1b"; // Dark Red
                break;
            default:
                bgColor = "#f3f4f6"; // Gray
                textColor = "#374151";
                break;
        }

        levelChallenge.setStyle(
                "-fx-background-color: " + bgColor + ";" +
                        "-fx-text-fill: " + textColor + ";" +
                        "-fx-padding: 4 10; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 10;"
        );
    }

}
