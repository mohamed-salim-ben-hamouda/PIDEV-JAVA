package com.pidev.Controllers.admin;

import com.pidev.models.Cv;
import com.pidev.models.User;
import com.pidev.Services.CVService;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BackofficeCVManagementController implements Initializable {

    @FXML private TableView<Cv> cvTable;
    @FXML private TableColumn<Cv, String> nomCol;
    @FXML private TableColumn<Cv, String> langueCol;
    @FXML private TableColumn<Cv, String> userCol;
    @FXML private TableColumn<Cv, String> dateCol;
    @FXML private TableColumn<Cv, Void> actionsCol;

    private final CVService cvService = new CVService();
    private ObservableList<Cv> cvList = FXCollections.observableArrayList();
    private Cv selectedCv;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadCvs();
    }

    private void setupTable() {
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nomCv"));
        langueCol.setCellValueFactory(new PropertyValueFactory<>("langue"));
        userCol.setCellValueFactory(cellData -> {
            User user = cellData.getValue().getUser();
            return new SimpleStringProperty(user != null ? String.valueOf(user.getId()) : "N/A");
        });
        dateCol.setCellValueFactory(cellData -> {
            LocalDateTime date = cellData.getValue().getCreationDate();
            return new SimpleStringProperty(date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "");
        });

        actionsCol.setCellFactory(param -> new TableCell<Cv, Void>() {
            private final Button viewBtn = new Button("Voir");
            private final Button deleteBtn = new Button("Supprimer");
            private final HBox container = new HBox(10, viewBtn, deleteBtn);

            {
                viewBtn.getStyleClass().add("action-btn-view");
                deleteBtn.getStyleClass().add("action-btn-delete");

                viewBtn.setOnAction(event -> {
                    Cv cv = getTableView().getItems().get(getIndex());
                    selectedCv = cv;
                    handleViewCvDetails(cv);
                });

                deleteBtn.setOnAction(event -> {
                    Cv cv = getTableView().getItems().get(getIndex());
                    handleDeleteCv(cv);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(container);
                }
            }
        });
    }

    private void handleViewCvDetails(Cv cv) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/BackofficeCVDetails.fxml"));
            Parent root = loader.load();

            BackofficeCVDetailsController controller = loader.getController();
            controller.setData(cv);

            Stage stage = new Stage();
            stage.setTitle("Aperçu du CV - " + cv.getNomCv());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir l'aperçu du CV: " + e.getMessage());
        }
    }

    private void loadCvs() {
        try {
            cvList.setAll(cvService.afficher());
            cvTable.setItems(cvList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les CVs: " + e.getMessage());
        }
    }

    private void handleDeleteCv(Cv selected) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer ce CV ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                cvService.supprimer(selected.getId());
                loadCvs();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer le CV: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
