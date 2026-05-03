package com.pidev.Controllers.client.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.paint.Color;


import javafx.fxml.FXML;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class ChallengeEditController implements Initializable {
    @FXML
    private TextField titleEdit;
    @FXML
    private TextField skillEdit;
    @FXML
    private ComboBox<String> difficultyEdit;
    @FXML
    private TextField minGroupEdit;
    @FXML
    private TextField maxGroupEdit;
    @FXML
    private DatePicker deadlineEdit;
    @FXML
    private TextArea descriptionEdit;
    @FXML
    private Label fileNameLabel;
    private File selectedPdf;
    private Challenge c;
    private Runnable onUpdated;
    @FXML
    private Label TitleError;
    @FXML
    private Label SkillError;
    @FXML
    private Label DifficultyError;
    @FXML
    private Label GroupError;
    @FXML
    private Label DeadlineError;
    @FXML
    private Label DescriptionError;
    @FXML
    private Button updateBtn;

    private boolean isSubmitted = false;

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList("Easy", "Medium", "Hard");
        difficultyEdit.setItems(options);

        titleEdit.textProperty().addListener((obs, old, val) -> {
            if (isSubmitted) toggleError(TitleError, val.isBlank());
        });
        skillEdit.textProperty().addListener((obs, old, val) -> {
            if (isSubmitted) toggleError(SkillError, val.isBlank());
        });
        descriptionEdit.textProperty().addListener((obs, old, val) -> {
            if (isSubmitted) toggleError(DescriptionError, val.isBlank());
        });
        difficultyEdit.valueProperty().addListener((obs, old, val) -> {
            if (isSubmitted) toggleError(DifficultyError, val == null);
        });
        deadlineEdit.valueProperty().addListener((obs, old, val) -> {
            if (isSubmitted) toggleError(DeadlineError, val == null);
        });
    }

    private void toggleError(Label label, boolean show) {
        if (label != null) {
            label.setVisible(show);
            label.setManaged(show);
            label.setTextFill(javafx.scene.paint.Color.RED);
        }
    }

    public void setDataEdit(Challenge c) {
        this.c = c;
        titleEdit.setText(c.getTitle());
        skillEdit.setText(c.getTargetSkill());
        difficultyEdit.setValue(c.getDifficulty());
        minGroupEdit.setText(String.valueOf(c.getMinGroupNbr()));
        maxGroupEdit.setText(String.valueOf(c.getMaxGroupNbr()));
        if (c.getDeadLine() != null) {
            deadlineEdit.setValue(c.getDeadLine().toLocalDate());
        } else {
            deadlineEdit.setValue(null);
        }
        descriptionEdit.setText(c.getDescription());
        if (c.getContent() == null || c.getContent().isBlank()) {
            fileNameLabel.setText("Aucun fichier choisi");
        } else {
            String fileName = Path.of(c.getContent().replace('\\', '/')).getFileName().toString();
            fileNameLabel.setText(fileName);
        }
    }

    public void setOnUpdated(Runnable onUpdated) {
        this.onUpdated = onUpdated;
    }

    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        Window owner = (fileNameLabel != null && fileNameLabel.getScene() != null) ? fileNameLabel.getScene().getWindow() : null;
        selectedPdf = fileChooser.showOpenDialog(owner);
        if (selectedPdf != null) {
            fileNameLabel.setText(selectedPdf.getName());
        }
    }

    @FXML
    public void OnEdit() {
        isSubmitted = true;
        boolean isValid = true;

        if (titleEdit.getText().isBlank()) {
            toggleError(TitleError, true);
            isValid = false;
        } else {
            toggleError(TitleError, false);
        }

        if (skillEdit.getText().isBlank()) {
            toggleError(SkillError, true);
            isValid = false;
        } else {
            toggleError(SkillError, false);
        }

        if (difficultyEdit.getValue() == null) {
            toggleError(DifficultyError, true);
            isValid = false;
        } else {
            toggleError(DifficultyError, false);
        }

        if (descriptionEdit.getText().isBlank()) {
            toggleError(DescriptionError, true);
            isValid = false;
        } else {
            toggleError(DescriptionError, false);
        }

        if (deadlineEdit.getValue() == null) {
            toggleError(DeadlineError, true);
            isValid = false;
        } else {
            toggleError(DeadlineError, false);
        }

        try {
            int min = Integer.parseInt(minGroupEdit.getText());
            int max = Integer.parseInt(maxGroupEdit.getText());
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

        if (!isValid) return;

        try {
            c.setTitle(titleEdit.getText().trim());
            c.setTargetSkill(skillEdit.getText().trim());
            c.setDifficulty(difficultyEdit.getValue());
            c.setMinGroupNbr(Integer.parseInt(minGroupEdit.getText()));
            c.setMaxGroupNbr(Integer.parseInt(maxGroupEdit.getText()));
            c.setDescription(descriptionEdit.getText());
            c.setDeadLine(deadlineEdit.getValue().atStartOfDay());

            if (selectedPdf != null) {
                Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "challenge_pdf");
                Files.createDirectories(destDir);
                Path destFile = destDir.resolve(selectedPdf.getName());
                Files.copy(selectedPdf.toPath(), destFile, StandardCopyOption.REPLACE_EXISTING);
                c.setContent("challenge_module/challenge_pdf/" + selectedPdf.getName());
            }

            ServiceChallenge service = new ServiceChallenge();
            service.update(c);

            if (onUpdated != null) {
                onUpdated.run();
            }
            if (updateBtn.getScene() != null) {
                Stage stage = (Stage) updateBtn.getScene().getWindow();
                stage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}



