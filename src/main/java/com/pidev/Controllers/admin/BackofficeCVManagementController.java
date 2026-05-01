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
import org.kordamp.ikonli.javafx.FontIcon;

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
    @FXML private TextField searchField;

    private final CVService cvService = new CVService();
    private ObservableList<Cv> cvList = FXCollections.observableArrayList();
    private ObservableList<Cv> filteredList = FXCollections.observableArrayList();
    private Cv selectedCv;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadCvs();
        setupSearch();
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterCvs(newValue);
            });
        }
    }

    private void filterCvs(String query) {
        if (query == null || query.isEmpty()) {
            cvTable.setItems(cvList);
            return;
        }

        String lowerCaseQuery = query.toLowerCase();
        filteredList.setAll(cvList.filtered(cv ->
                cv.getNomCv().toLowerCase().contains(lowerCaseQuery) ||
                        (cv.getUser() != null && String.valueOf(cv.getUser().getId()).contains(lowerCaseQuery)) ||
                        cv.getLangue().toLowerCase().contains(lowerCaseQuery)
        ));
        cvTable.setItems(filteredList);
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
            private final Button viewBtn = new Button();
            private final Button deleteBtn = new Button();
            private final HBox container = new HBox(12, viewBtn, deleteBtn);

            {
                container.setAlignment(javafx.geometry.Pos.CENTER);

                FontIcon viewIcon = new FontIcon("fas-eye");
                viewIcon.setIconSize(16);
                viewIcon.setIconColor(javafx.scene.paint.Color.web("#3d68b2"));
                viewIcon.setMouseTransparent(true);
                viewBtn.setGraphic(viewIcon);
                viewBtn.setTooltip(new Tooltip("Voir les détails"));
                viewBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                FontIcon deleteIcon = new FontIcon("fas-trash-alt");
                deleteIcon.setIconSize(16);
                deleteIcon.setIconColor(javafx.scene.paint.Color.web("#e11d48"));
                deleteIcon.setMouseTransparent(true);
                deleteBtn.setGraphic(deleteIcon);
                deleteBtn.setTooltip(new Tooltip("Supprimer le CV"));
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                viewBtn.setOnAction(event -> {
                    Cv cv = getTableRow().getItem();
                    if (cv != null) {
                        selectedCv = cv;
                        handleViewCvDetails(cv);
                    }
                });

                deleteBtn.setOnAction(event -> {
                    Cv cv = getTableRow().getItem();
                    if (cv != null) {
                        handleDeleteCv(cv);
                    }
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
