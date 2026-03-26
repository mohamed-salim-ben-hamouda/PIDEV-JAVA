package com.pidev.Controllers.client.Challenge;
import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
public class ChallengeController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    @FXML private VBox challengeListContainer;
    @FXML private TextField TitleInput;
    @FXML private TextField TargetSkillInput;
    @FXML private ComboBox<String> DifficultyCombo;
    @FXML private TextField MinGroupNbrInput;
    @FXML private TextField MaxGroupNbrInput;
    @FXML private DatePicker DeadlineInput;
    @FXML private TextArea DescriptionInput;
    @FXML private Label fileInput;

    private File selectedPdf;
    private final ServiceChallenge service = new ServiceChallenge();

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

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList("Easy", "Medium", "Hard");
        DifficultyCombo.setItems(options);
        refreshChallenges();
    }

    private void refreshChallenges() {
        challengeListContainer.getChildren().clear();

        List<Challenge> challenges = service.display();
        if (challenges == null || challenges.isEmpty()) {
            challengeListContainer.getChildren().add(createEmptyState());
            return;
        }

        for (Challenge challenge : challenges) {
            challengeListContainer.getChildren().add(createChallengeCard(challenge));
        }
    }

    private VBox createEmptyState() {
        VBox emptyState = new VBox(10);
        emptyState.getStyleClass().addAll("challenge-card", "empty-state-card");
        emptyState.setPadding(new Insets(28, 24, 28, 24));

        Label title = new Label("No challenges yet");
        title.getStyleClass().add("challenge-title");

        Label subtitle = new Label("Create your first challenge from the form on the right.");
        subtitle.getStyleClass().add("empty-state-text");
        subtitle.setWrapText(true);

        emptyState.getChildren().addAll(title, subtitle);
        return emptyState;
    }

    private VBox createChallengeCard(Challenge challenge) {
        VBox card = new VBox(12);
        card.getStyleClass().add("challenge-card");
        card.setPadding(new Insets(15, 20, 15, 20));

        HBox header = new HBox();
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        Label title = new Label(valueOrFallback(challenge.getTitle(), "Untitled challenge"));
        title.getStyleClass().add("challenge-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox actions = new HBox(10);
        Button groupsButton = new Button("Groups");
        groupsButton.getStyleClass().add("action-btn-outline");
        Button deleteButton = new Button("Delete");
        deleteButton.getStyleClass().add("action-btn-delete");
        Button editButton = new Button("Edit");
        editButton.getStyleClass().add("action-btn-outline");
        actions.getChildren().addAll(groupsButton, deleteButton, editButton);

        header.getChildren().addAll(title, spacer, actions);

        Label description = new Label(valueOrFallback(challenge.getDescription(), "No description provided."));
        description.getStyleClass().add("challenge-description");
        description.setWrapText(true);

        Button fileButton = new Button("View File: " + extractFileName(challenge.getContent()));
        fileButton.getStyleClass().add("file-link-btn");
        fileButton.setDisable(challenge.getContent() == null || challenge.getContent().isBlank());

        Separator separator = new Separator();
        separator.setOpacity(0.5);
        VBox.setMargin(separator, new Insets(10, 0, 10, 0));

        HBox metaRow = new HBox(30);
        Label createdAt = new Label("Created: " + formatDate(challenge.getCreatedAt()));
        createdAt.getStyleClass().add("meta-text");
        Label targetSkill = new Label("Target Skill: " + valueOrFallback(challenge.getTargetSkill(), "-"));
        targetSkill.getStyleClass().add("meta-text");
        Label difficulty = new Label("Difficulty: " + valueOrFallback(challenge.getDifficulty(), "-"));
        difficulty.getStyleClass().add("meta-text");
        metaRow.getChildren().addAll(createdAt, targetSkill, difficulty);

        card.getChildren().addAll(header, description, fileButton, separator, metaRow);
        return card;
    }

    private String formatDate(LocalDateTime dateTime) {
        return dateTime == null ? "-" : DATE_FORMATTER.format(dateTime);
    }

    private String valueOrFallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String extractFileName(String contentPath) {
        if (contentPath == null || contentPath.isBlank()) {
            return "No file";
        }

        int separatorIndex = Math.max(contentPath.lastIndexOf('/'), contentPath.lastIndexOf('\\'));
        return separatorIndex >= 0 ? contentPath.substring(separatorIndex + 1) : contentPath;
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
    public void onCreateChallenge() {
        try {
            Challenge c = new Challenge();

            c.setTitle(TitleInput.getText());
            c.setTargetSkill(TargetSkillInput.getText());
            c.setDifficulty(DifficultyCombo.getValue());
            c.setMinGroupNbr(Integer.parseInt(MinGroupNbrInput.getText()));
            c.setMaxGroupNbr(Integer.parseInt(MaxGroupNbrInput.getText()));
            c.setDescription(DescriptionInput.getText());

            if (DeadlineInput.getValue() != null) {
                c.setDeadLine(DeadlineInput.getValue().atStartOfDay());
            }
            c.setCreatedAt(LocalDateTime.now());

            if (selectedPdf != null) {
                String folderPath = System.getProperty("user.dir") + "/src/main/resources/challenge_module/challenge_pdf";
                File destDir = new File(folderPath);
                File destFile = new File(destDir, selectedPdf.getName());
                java.nio.file.Files.copy(
                        selectedPdf.toPath(),
                        destFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                c.setContent("challenge_module/challenge_pdf/" + selectedPdf.getName());
            }

            service.add(c);
            refreshChallenges();
            showSuccessAlert();
            clearForm();
            System.out.println("Challenge created");
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
        }
    }
}