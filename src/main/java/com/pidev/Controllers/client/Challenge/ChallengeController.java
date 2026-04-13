package com.pidev.Controllers.client.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class ChallengeController implements Initializable {
    @FXML
    private TextField TitleInput;
    @FXML
    private TextField TargetSkillInput;
    @FXML
    private ComboBox<String> DifficultyCombo;
    @FXML
    private TextField MinGroupNbrInput;
    @FXML
    private TextField MaxGroupNbrInput;
    @FXML
    private DatePicker DeadlineInput;
    @FXML
    private TextArea DescriptionInput;
    @FXML
    private Label fileInput;
    @FXML
    private VBox challengeListContainer;
    @FXML private Label TitleError;
    @FXML private Label TargetSkillError;
    @FXML private Label DifficultyError;
    @FXML private Label GroupError;
    @FXML private Label DeadlineError;
    @FXML private Label DescriptionError;
    @FXML private Label FileError;
    private File selectedPdf;
    private final ServiceChallenge service = new ServiceChallenge();

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList("Easy", "Medium", "Hard");
        DifficultyCombo.setItems(options);
        TitleInput.textProperty().addListener((obs, old, val) -> toggleError(TitleError, val.isBlank()));
        TargetSkillInput.textProperty().addListener((obs, old, val) -> toggleError(TargetSkillError, val.isBlank()));
        DescriptionInput.textProperty().addListener((obs, old, val) -> toggleError(DescriptionError, val.isBlank()));
        DifficultyCombo.valueProperty().addListener((obs, old, val) -> toggleError(DifficultyError, val == null));
        DeadlineInput.valueProperty().addListener((obs, old, val) -> toggleError(DeadlineError, val == null));
        refreshChallenges();
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Challenge created");
        alert.setContentText("Challenge created successfully!");
        alert.setGraphic(null);

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

        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Challenge not created");
        alert.setContentText(message);
        alert.setGraphic(null);

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

        alert.showAndWait();
    }

    private void clearForm() {
        TitleInput.clear();
        TargetSkillInput.clear();
        DescriptionInput.clear();
        MinGroupNbrInput.setText("0");
        MaxGroupNbrInput.setText("0");
        DeadlineInput.setValue(null);
        DifficultyCombo.getSelectionModel().clearSelection();
        fileInput.setText("Aucun fichier n'a ete selectionne");
        selectedPdf = null;
    }

    private int parseIntRequired(TextField input, String fieldName) {
        String raw = input.getText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    @FXML
    public void onChooseFile() {

        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            fileInput.setText(selectedPdf.getName());
        }
    }

    @FXML
    private void onStaticAction() {
    }

    @FXML
    public void onCreateChallenge() {
        boolean isValid = true;

        if (TitleInput.getText().isBlank()) { toggleError(TitleError, true); isValid = false; }
        if (TargetSkillInput.getText().isBlank()) { toggleError(TargetSkillError, true); isValid = false; }
        if (DescriptionInput.getText().isBlank()) { toggleError(DescriptionError, true); isValid = false; }

        if (DifficultyCombo.getValue() == null) { toggleError(DifficultyError, true); isValid = false; }
        if (DeadlineInput.getValue() == null) {
            DeadlineError.setText("Deadline is required");
            toggleError(DeadlineError, true);
            isValid = false;
        }

        try {
            int min = Integer.parseInt(MinGroupNbrInput.getText());
            int max = Integer.parseInt(MaxGroupNbrInput.getText());
            if (min < 0 || max < 0 || min > max) {
                GroupError.setText("Min must be positive and less than Max");
                toggleError(GroupError, true);
                isValid = false;
            } else {
                toggleError(GroupError, false);
            }
        } catch (NumberFormatException e) {
            GroupError.setText("Please enter valid numbers");
            toggleError(GroupError, true);
            isValid = false;
        }

        if (selectedPdf == null) {
            toggleError(FileError, true);
            isValid = false;
        } else {
            toggleError(FileError, false);
        }

        if (!isValid) return;

        try {
            Challenge c = new Challenge();
            c.setTitle(TitleInput.getText().trim());
            c.setTargetSkill(TargetSkillInput.getText().trim());
            c.setDifficulty(DifficultyCombo.getValue());
            c.setMinGroupNbr(Integer.parseInt(MinGroupNbrInput.getText()));
            c.setMaxGroupNbr(Integer.parseInt(MaxGroupNbrInput.getText()));
            c.setDescription(DescriptionInput.getText());
            c.setDeadLine(DeadlineInput.getValue().atStartOfDay());
            c.setCreatedAt(LocalDateTime.now());

            Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "challenge_pdf");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(selectedPdf.getName());
            Files.copy(selectedPdf.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            c.setContent("challenge_module/challenge_pdf/" + selectedPdf.getName());

            service.add(c);
            refreshChallenges();
            showSuccessAlert();
            clearForm();
            hideAllErrors();
        } catch (Exception e) {
            showErrorAlert("Could not save challenge: " + e.getMessage());
        }
    }
    private void hideAllErrors() {
        toggleError(TitleError, false);
        toggleError(TargetSkillError, false);
        toggleError(DifficultyError, false);
        toggleError(GroupError, false);
        toggleError(DeadlineError, false);
        toggleError(DescriptionError, false);
        toggleError(FileError, false);
    }
    private void refreshChallenges() {
        challengeListContainer.getChildren().clear();
        int user_id = 1;
        List<Challenge> challenges = service.displayForSupervisor(user_id);
        if (challenges == null || challenges.isEmpty()) {
            Label empty = new Label("No challenges yet");
            challengeListContainer.getChildren().add(empty);
            return;
        }

        for (Challenge c : challenges) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/ChallengeCard.fxml"));
                VBox card = loader.load();
                ChallengeCardController cardController = loader.getController();
                cardController.setData(c,v -> refreshChallenges());
                cardController.SupervisorCard(cardController.participationBtn);
                challengeListContainer.getChildren().add(card);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    private void toggleError(Label label, boolean show) {
        label.setVisible(show);
        label.setManaged(show);
    }
}
