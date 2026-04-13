package com.pidev.Controllers.client;

import com.pidev.models.Cv;
import com.pidev.models.User;
import com.pidev.Services.CVService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class CVManagementController implements Initializable {

    @FXML private TextField nomCvField;
    @FXML private ComboBox<String> langueComboBox;
    @FXML private TextField linkedinUrlField;
    @FXML private TextArea summaryArea;
    @FXML private Button submitButton;

    @FXML private TableView<Cv> cvTable;
    @FXML private TableColumn<Cv, String> nomCol;
    @FXML private TableColumn<Cv, String> langueCol;
    @FXML private TableColumn<Cv, String> dateCol;
    @FXML private TableColumn<Cv, Integer> progressionCol;

    private final CVService cvService = new CVService();
    private ObservableList<Cv> cvList = FXCollections.observableArrayList();
    private Cv selectedCv;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        langueComboBox.getItems().addAll("Francais", "Anglais", "Arabe");

        setupTable();
        loadCvs();

        cvTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                selectedCv = newSelection;
                fillForm(newSelection);
                submitButton.setText("Modifier");
            }
        });
    }

    private void setupTable() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nomCv"));
        langueCol.setCellValueFactory(new PropertyValueFactory<>("langue"));
        dateCol.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreationDate();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });
        progressionCol.setCellValueFactory(new PropertyValueFactory<>("progression"));
    }

    private void loadCvs() {
        try {
            cvList.setAll(cvService.afficher());
            cvTable.setItems(cvList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les CVs: " + e.getMessage());
        }
    }

    @FXML
    private void handleSubmitCv() {
        if (nomCvField.getText().isEmpty() || langueComboBox.getValue() == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir les champs obligatoires (Nom et Langue)");
            return;
        }

        try {
            if (selectedCv == null) {
                Cv cv = new Cv();
                cv.setNomCv(nomCvField.getText());
                cv.setLangue(langueComboBox.getValue());
                cv.setLinkedinUrl(linkedinUrlField.getText());
                cv.setSummary(summaryArea.getText());
                cv.setCreationDate(LocalDateTime.now());
                cv.setUpdatedAt(LocalDateTime.now());

                // For now, use a dummy user since we don't have session management details
                cv.setUser(new User(1));

                cvService.ajouter(cv);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "CV ajouté avec succès !");
            } else {
                selectedCv.setNomCv(nomCvField.getText());
                selectedCv.setLangue(langueComboBox.getValue());
                selectedCv.setLinkedinUrl(linkedinUrlField.getText());
                selectedCv.setSummary(summaryArea.getText());
                selectedCv.setUpdatedAt(LocalDateTime.now());

                cvService.modifier(selectedCv);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "CV modifié avec succès !");
            }
            handleClearForm();
            loadCvs();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue: " + e.getMessage());
        }
    }

    @FXML
    private void handleDeleteCv() {
        Cv selected = cvTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un CV à supprimer");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce CV ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                cvService.supprimer(selected.getId());
                loadCvs();
                handleClearForm();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le CV: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleClearForm() {
        selectedCv = null;
        nomCvField.clear();
        langueComboBox.setValue(null);
        linkedinUrlField.clear();
        summaryArea.clear();
        submitButton.setText("Ajouter");
        cvTable.getSelectionModel().clearSelection();
    }

    private void fillForm(Cv cv) {
        nomCvField.setText(cv.getNomCv());
        langueComboBox.setValue(cv.getLangue());
        linkedinUrlField.setText(cv.getLinkedinUrl());
        summaryArea.setText(cv.getSummary());
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
