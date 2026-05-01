package com.pidev.Controllers.client;

import com.pidev.models.Offer;
import com.pidev.models.User;
import com.pidev.Services.OfferService;
import com.pidev.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

import javafx.scene.layout.VBox;

public class OfferListController implements Initializable {

    @FXML private VBox offersFlowPane;
    @FXML private Label resultCountLabel;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> domainFilter;
    @FXML private Button manageCvsBtn;
    @FXML private Button addOfferBtn;

    private final OfferService offerService = new OfferService();
    private List<Offer> allOffers = new ArrayList<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupFilters();
        loadOffers();
        setupRoleBasedUI();

        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterOffers());
        domainFilter.valueProperty().addListener((obs, oldVal, newVal) -> filterOffers());
    }

    private void setupRoleBasedUI() {
        boolean isLogged = SessionManager.getInstance().isLogged();
        User currentUser = SessionManager.getInstance().getUser();
        boolean isEnterprise = isLogged && currentUser != null && currentUser.getRole() == User.Role.ENTREPRISE;

        if (manageCvsBtn != null) {
            manageCvsBtn.setVisible(isLogged && !isEnterprise);
            manageCvsBtn.setManaged(isLogged && !isEnterprise);
        }

        if (addOfferBtn != null) {
            addOfferBtn.setVisible(isEnterprise);
            addOfferBtn.setManaged(isEnterprise);
        }
    }

    private void setupFilters() {
        domainFilter.getItems().addAll("Pertinence", "Plus récent", "Salaire");
        domainFilter.setValue("Pertinence");
    }

    private void loadOffers() {
        try {
            User currentUser = SessionManager.getInstance().getUser();
            if (currentUser != null && currentUser.getRole() == User.Role.ENTREPRISE) {
                allOffers = offerService.afficherParEntreprise(currentUser.getId());
            } else {
                allOffers = offerService.afficher();
            }
            displayOffers(allOffers);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les offres: " + e.getMessage());
        }
    }

    private void displayOffers(List<Offer> offers) {
        offersFlowPane.getChildren().clear();
        resultCountLabel.setText(String.valueOf(offers.size()));

        for (Offer offer : offers) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/OfferCard.fxml"));
                Node card = loader.load();
                OfferCardController controller = loader.getController();
                controller.setData(offer, this);
                offersFlowPane.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void filterOffers() {
        String query = searchField.getText().toLowerCase();
        String selectedSort = domainFilter.getValue();

        List<Offer> filtered = allOffers.stream()
                .filter(o -> (query.isEmpty() || o.getTitle().toLowerCase().contains(query) || o.getDescription().toLowerCase().contains(query)))
                .collect(Collectors.toList());
        displayOffers(filtered);
    }

    @FXML
    private void handleGoToMyCvs() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/MyCVView.fxml"));
            Parent root = loader.load();
            StackPane contentArea = (StackPane) offersFlowPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                Stage stage = (Stage) offersFlowPane.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void showOfferDetails(Offer offer) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/OfferDetails.fxml"));
            Parent root = loader.load();
            OfferDetailsController controller = loader.getController();

            // Set data and enterprise mode
            controller.setData(offer);
            User currentUser = SessionManager.getInstance().getUser();
            controller.setEnterpriseMode(currentUser != null && currentUser.getRole() == User.Role.ENTREPRISE);

            StackPane contentArea = (StackPane) offersFlowPane.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                Stage stage = (Stage) offersFlowPane.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleAddOffer() {
        // Logic to open add offer form
        System.out.println("Opening add offer form...");
        showAlert(Alert.AlertType.INFORMATION, "Information", "Le formulaire d'ajout d'offre sera bientôt disponible.");
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
