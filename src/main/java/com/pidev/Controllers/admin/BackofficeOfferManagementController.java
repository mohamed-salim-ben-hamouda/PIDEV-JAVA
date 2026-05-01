package com.pidev.Controllers.admin;

import com.pidev.models.Offer;
import com.pidev.models.User;
import com.pidev.Services.OfferService;
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
import java.util.ResourceBundle;

public class BackofficeOfferManagementController implements Initializable {

    @FXML private TableView<Offer> offerTable;
    @FXML private TableColumn<Offer, String> titleCol;
    @FXML private TableColumn<Offer, String> fieldCol;
    @FXML private TableColumn<Offer, String> typeCol;
    @FXML private TableColumn<Offer, String> locationCol;
    @FXML private TableColumn<Offer, String> statusCol;
    @FXML private TableColumn<Offer, Void> actionsCol;
    @FXML private TextField searchField;

    private final OfferService offerService = new OfferService();
    private ObservableList<Offer> offerList = FXCollections.observableArrayList();
    private ObservableList<Offer> filteredList = FXCollections.observableArrayList();
    private Offer selectedOffer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTable();
        loadOffers();
        setupSearch();
    }

    private void setupSearch() {
        if (searchField != null) {
            searchField.textProperty().addListener((observable, oldValue, newValue) -> {
                filterOffers(newValue);
            });
        }
    }

    private void filterOffers(String query) {
        if (query == null || query.isEmpty()) {
            offerTable.setItems(offerList);
            return;
        }

        String lowerCaseQuery = query.toLowerCase();
        filteredList.setAll(offerList.filtered(offer ->
                offer.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        offer.getField().toLowerCase().contains(lowerCaseQuery) ||
                        offer.getLocation().toLowerCase().contains(lowerCaseQuery) ||
                        offer.getStatus().toLowerCase().contains(lowerCaseQuery)
        ));
        offerTable.setItems(filteredList);
    }

    private void setupTable() {
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        fieldCol.setCellValueFactory(new PropertyValueFactory<>("field"));
        typeCol.setCellValueFactory(new PropertyValueFactory<>("offerType"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));

        actionsCol.setCellFactory(param -> new TableCell<Offer, Void>() {
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
                deleteBtn.setTooltip(new Tooltip("Supprimer l'offre"));
                deleteBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

                viewBtn.setOnAction(event -> {
                    Offer offer = getTableRow().getItem();
                    if (offer != null) {
                        selectedOffer = offer;
                        handleViewOfferDetails(offer);
                    }
                });

                deleteBtn.setOnAction(event -> {
                    Offer offer = getTableRow().getItem();
                    if (offer != null) {
                        handleDeleteOffer(offer);
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

    private void handleViewOfferDetails(Offer offer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/OfferDetails.fxml"));
            Parent root = loader.load();

            com.pidev.Controllers.client.OfferDetailsController controller = loader.getController();
            controller.setData(offer);
            controller.setEnterpriseMode(true); // Hide the "Apply" section for admin
            controller.hideBackButton(); // Hide the back button in backoffice popup

            Stage stage = new Stage();
            stage.setTitle("Détails de l'Offre - " + offer.getTitle());
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les détails de l'offre: " + e.getMessage());
        }
    }

    private void loadOffers() {
        try {
            offerList.setAll(offerService.afficher());
            offerTable.setItems(offerList);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les offres: " + e.getMessage());
        }
    }

    private void handleDeleteOffer(Offer selected) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText(null);
        alert.setContentText("Êtes-vous sûr de vouloir supprimer cette offre ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                offerService.supprimer(selected.getId());
                loadOffers();
            } catch (SQLException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de supprimer l'offre: " + e.getMessage());
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
