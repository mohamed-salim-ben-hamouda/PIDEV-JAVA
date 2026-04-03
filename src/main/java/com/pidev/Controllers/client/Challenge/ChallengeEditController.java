package com.pidev.Controllers.client.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.scene.control.Label;
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
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList("Easy", "Medium", "Hard");
        difficultyEdit.setItems(options);
    }

    public void setDataEdit(Challenge c){
        this.c=c;
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
        Window owner = (fileNameLabel != null && fileNameLabel.getScene() != null) ? fileNameLabel.getScene().getWindow() : null;
        selectedPdf = fileChooser.showOpenDialog(owner);
        if (selectedPdf != null) {
            fileNameLabel.setText(selectedPdf.getName());
        }
    }


    @FXML
    public void OnEdit(){
        try{
            if (c == null || c.getId() == null) {
                throw new IllegalStateException("No challenge selected for editing.");
            }

            String title = titleEdit.getText();
            if (title == null || title.isBlank()) {
                throw new IllegalArgumentException("Title is required.");
            }
            String targetSkill = skillEdit.getText();
            if (targetSkill == null || targetSkill.isBlank()) {
                throw new IllegalArgumentException("Target skill is required.");
            }
            String difficulty = difficultyEdit.getValue();
            if (difficulty == null || difficulty.isBlank()) {
                throw new IllegalArgumentException("Difficulty is required.");
            }

            c.setTitle(title.trim());
            c.setTargetSkill(targetSkill.trim());
            c.setDifficulty(difficulty);

            int minGroupNbr = parseIntRequired(minGroupEdit, "Min group number");
            int maxGroupNbr = parseIntRequired(maxGroupEdit, "Max group number");
            if (minGroupNbr < 0 || maxGroupNbr < 0) {
                throw new IllegalArgumentException("Group numbers must be >= 0.");
            }
            if (minGroupNbr > maxGroupNbr) {
                throw new IllegalArgumentException("Min group number cannot be greater than max group number.");
            }

            c.setMinGroupNbr(minGroupNbr);
            c.setMaxGroupNbr(maxGroupNbr);
            c.setDescription(descriptionEdit.getText());

            if (deadlineEdit.getValue() != null) {
                c.setDeadLine(deadlineEdit.getValue().atStartOfDay());
            }

            if (selectedPdf != null) {
                Path destDir = Paths.get(System.getProperty("user.dir"),
                        "src", "main", "resources", "challenge_module", "challenge_pdf");

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
        }catch (Exception e) {
            e.printStackTrace();
        }

    }



}
