package com.pidev.Controllers.client;

import com.pidev.Services.MotivationLetterService;
import com.pidev.models.Cv;
import com.pidev.models.MotivationLetter;
import com.pidev.models.Offer;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.time.LocalDateTime;

public class MotivationLetterDialogController {

    @FXML private TextArea letterTextArea;
    @FXML private Button cancelButton;
    @FXML private Button saveButton;
    @FXML private Button useToApplyButton;

    private Cv cv;
    private Offer offer;
    private String generatedLetter;
    private final MotivationLetterService letterService = new MotivationLetterService();
    private boolean isConfirmed = false;
    private MotivationLetter savedLetter;

    public void setData(Cv cv, Offer offer, String letterText) {
        this.cv = cv;
        this.offer = offer;
        this.generatedLetter = letterText;
        this.letterTextArea.setText(letterText);
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }

    public MotivationLetter getSavedLetter() {
        return savedLetter;
    }

    @FXML
    private void handleSave() {
        if (saveLetter()) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Lettre de motivation sauvegardée avec succès.");
            closeStage();
        }
    }

    @FXML
    private void handleUseToApply() {
        if (saveLetter()) {
            isConfirmed = true;
            closeStage();
        }
    }

    @FXML
    private void handleCancel() {
        closeStage();
    }

    private boolean saveLetter() {
        String content = letterTextArea.getText();
        if (content == null || content.trim().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "La lettre ne peut pas être vide.");
            return false;
        }

        try {
            savedLetter = new MotivationLetter();
            savedLetter.setContent(content);
            savedLetter.setCvId(cv.getId());
            savedLetter.setOfferId(offer.getId());
            savedLetter.setCreatedAt(LocalDateTime.now());

            letterService.save(savedLetter);
            return true;
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de sauvegarder la lettre: " + e.getMessage());
            return false;
        }
    }

    private void closeStage() {
        Stage stage = (Stage) cancelButton.getScene().getWindow();
        stage.close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
