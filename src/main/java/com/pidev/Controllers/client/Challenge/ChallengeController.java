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

    private File selectedPdf;
    private final ServiceChallenge service = new ServiceChallenge();

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList("Easy", "Medium", "Hard");
        DifficultyCombo.setItems(options);
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
        try {
            String title = TitleInput.getText();
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title is required.");
            }
            String targetSkill = TargetSkillInput.getText();
            if (targetSkill == null || targetSkill.isBlank()) {
                throw new IllegalArgumentException("Target skill is required.");
            }
            String difficulty = DifficultyCombo.getValue();
            if (difficulty == null || difficulty.isBlank()) {
                throw new IllegalArgumentException("Difficulty is required.");
            }

            int minGroupNbr = parseIntRequired(MinGroupNbrInput, "Min group number");
            int maxGroupNbr = parseIntRequired(MaxGroupNbrInput, "Max group number");
            if (minGroupNbr < 0 || maxGroupNbr < 0) {
                throw new IllegalArgumentException("Group numbers must be >= 0.");
            }
            if (minGroupNbr > maxGroupNbr) {
                throw new IllegalArgumentException("Min group number cannot be greater than max group number.");
            }

            if (selectedPdf == null) {
                throw new IllegalArgumentException("PDF file is required.");
            }

            Challenge c = new Challenge();
            c.setTitle(title.trim());
            c.setTargetSkill(targetSkill.trim());
            c.setDifficulty(difficulty);
            c.setMinGroupNbr(minGroupNbr);
            c.setMaxGroupNbr(maxGroupNbr);
            c.setDescription(DescriptionInput.getText());

            if (DeadlineInput.getValue() != null) {
                c.setDeadLine(DeadlineInput.getValue().atStartOfDay());
            }
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
        } catch (Exception e) {
            String message = e.getMessage();
            if (message == null || message.isBlank()) {
                message = e.getClass().getSimpleName();
            }
            showErrorAlert(message);
            e.printStackTrace();
        }
    }
    private void refreshChallenges() {
        challengeListContainer.getChildren().clear();

        List<Challenge> challenges = service.display();
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
}
