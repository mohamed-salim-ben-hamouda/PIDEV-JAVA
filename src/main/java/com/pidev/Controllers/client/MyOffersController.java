package com.pidev.Controllers.client;

import com.pidev.models.CvApplication;
import com.pidev.models.Offer;
import com.pidev.models.User;
import com.pidev.Services.OfferService;
import com.pidev.Services.CvApplicationService;
import com.pidev.Services.CVService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.net.URL;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;

public class MyOffersController implements Initializable {

    @FXML private VBox listPage;
    @FXML private VBox formPage;
    @FXML private VBox applicationsPage;
    @FXML private FlowPane offersContainer;
    @FXML private VBox applicationsContainer;
    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;
    @FXML private Button saveOfferBtn;

    // Form fields
    @FXML private TextField titleField;
    @FXML private ComboBox<String> typeComboBox;
    @FXML private TextField fieldField;
    @FXML private TextArea descriptionArea;
    @FXML private ComboBox<String> levelComboBox;
    @FXML private TextField locationField;
    @FXML private ComboBox<String> contractComboBox;
    @FXML private TextField salaryField;

    private final OfferService offerService = new OfferService();
    private final CvApplicationService cvApplicationService = new CvApplicationService();
    private final CVService cvService = new CVService();
    private Offer currentOffer;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeComboBox.getItems().setAll("Stage", "Emploi", "Alternance");
        levelComboBox.getItems().setAll("Bac", "Bac+2", "Bac+3", "Bac+5", "Doctorat");
        contractComboBox.getItems().setAll("CDI", "CDD", "SIVP", "Freelance");

        showListPage();
        loadMyOffers();
    }

    private void loadMyOffers() {
        offersContainer.getChildren().clear();
        try {
            // In a real app, filter by enterprise_id. For now, show all.
            List<Offer> allOffers = offerService.afficher();
            for (Offer offer : allOffers) {
                offersContainer.getChildren().add(createOfferCard(offer));
            }
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les offres: " + e.getMessage());
        }
    }

    private VBox createOfferCard(Offer offer) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-padding: 25; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.04), 15, 0, 0, 5); -fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-border-radius: 20;");
        card.setPrefWidth(380);

        // Top Row: Title & Status
        HBox topRow = new HBox(10);
        topRow.setAlignment(javafx.geometry.Pos.TOP_LEFT);

        Label title = new Label(offer.getTitle());
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #0f172a;");
        title.setWrapText(true);
        HBox.setHgrow(title, Priority.ALWAYS);

        // Status Badge (Custom look)
        Label statusBadge = new Label(offer.getStatus().toUpperCase());
        statusBadge.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #64748b; -fx-padding: 4 10; -fx-background-radius: 8; -fx-font-size: 10; -fx-font-weight: bold; -fx-border-color: #e2e8f0; -fx-border-radius: 8;");

        // Info Grid-like layout
        VBox infoBox = new VBox(10);

        HBox typeAndLocation = new HBox(15);
        Label type = new Label("💼 " + offer.getOfferType());
        type.setStyle("-fx-text-fill: #475569; -fx-font-weight: bold; -fx-font-size: 14;");
        Label location = new Label("📍 " + offer.getLocation());
        location.setStyle("-fx-text-fill: #64748b; -fx-font-size: 14;");
        typeAndLocation.getChildren().addAll(type, location);

        Label salary = new Label("💰 " + (offer.getSalaryRange() != null ? offer.getSalaryRange() + " DT" : "Non spécifié"));
        salary.setStyle("-fx-text-fill: #0f172a; -fx-font-weight: bold; -fx-font-size: 15;");

        infoBox.getChildren().addAll(typeAndLocation, salary);

        // Main Action: View Applications
        Button viewAppsBtn = new Button("Voir candidatures reçues");
        viewAppsBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 12; -fx-font-weight: bold; -fx-padding: 12; -fx-cursor: hand; -fx-font-size: 14;");
        viewAppsBtn.setMaxWidth(Double.MAX_VALUE);
        viewAppsBtn.setOnAction(e -> showApplications(offer));

        // Bottom Actions: Icons for View, Edit, Delete
        HBox actions = new HBox(12);
        actions.setAlignment(javafx.geometry.Pos.CENTER);

        Button viewBtn = createIconButton("👁", "#f1f5f9", "#475569", "Voir");
        viewBtn.setOnAction(e -> handleViewOfferDetails(offer));

        Button editBtn = createIconButton("📝", "#fef3c7", "#92400e", "Modifier");
        editBtn.setOnAction(e -> handleEditOffer(offer));

        Button deleteBtn = createIconButton("🗑", "#fee2e2", "#991b1b", "Supprimer");
        deleteBtn.setOnAction(e -> handleDeleteOffer(offer));

        actions.getChildren().addAll(viewBtn, editBtn, deleteBtn);

        card.getChildren().addAll(new HBox(10, title, statusBadge), infoBox, new Separator(), viewAppsBtn, actions);

        return card;
    }

    private Button createIconButton(String icon, String bgColor, String textColor, String tooltip) {
        Button btn = new Button(icon + " " + tooltip);
        btn.setStyle("-fx-background-color: " + bgColor + "; -fx-text-fill: " + textColor + "; -fx-background-radius: 10; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        btn.setPrefWidth(110);
        return btn;
    }

    private void handleViewOfferDetails(Offer offer) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Fxml/client/OfferDetails.fxml"));
            javafx.scene.Parent root = loader.load();
            OfferDetailsController controller = loader.getController();
            controller.setData(offer);
            controller.setEnterpriseMode(true);

            StackPane contentArea = (StackPane) listPage.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (java.io.IOException e) {
            showError("Erreur", "Impossible d'afficher les détails: " + e.getMessage());
        }
    }

    private void handleEditOffer(Offer offer) {
        currentOffer = offer;
        formTitleLabel.setText("Modifier l'offre");
        formSubtitleLabel.setText("Mettez à jour les détails de votre offre d'emploi.");
        saveOfferBtn.setText("Enregistrer les modifications");

        titleField.setText(offer.getTitle());
        typeComboBox.setValue(offer.getOfferType());
        fieldField.setText(offer.getField());
        descriptionArea.setText(offer.getDescription());
        levelComboBox.setValue(offer.getRequiredLevel());
        locationField.setText(offer.getLocation());
        contractComboBox.setValue(offer.getContractType());
        salaryField.setText(String.valueOf(offer.getSalaryRange()));

        listPage.setVisible(false);
        listPage.setManaged(false);
        formPage.setVisible(true);
        formPage.setManaged(true);
        applicationsPage.setVisible(false);
        applicationsPage.setManaged(false);
    }

    private void handleDeleteOffer(Offer offer) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer l'offre");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer l'offre \"" + offer.getTitle() + "\" ?");

        if (alert.showAndWait().get() == ButtonType.OK) {
            try {
                offerService.supprimer(offer.getId());
                loadMyOffers();
                showInfo("Succès", "L'offre a été supprimée.");
            } catch (SQLException e) {
                showError("Erreur", "Impossible de supprimer l'offre: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleSaveOffer() {
        try {
            Offer offer = (currentOffer != null) ? currentOffer : new Offer();
            offer.setTitle(titleField.getText());
            offer.setOfferType(typeComboBox.getValue());
            offer.setField(fieldField.getText());
            offer.setDescription(descriptionArea.getText());
            offer.setRequiredLevel(levelComboBox.getValue());
            offer.setLocation(locationField.getText());
            offer.setContractType(contractComboBox.getValue());
            offer.setSalaryRange(Double.parseDouble(salaryField.getText()));
            offer.setStatus("OPEN");

            if (currentOffer == null) {
                offer.setCreatedAt(LocalDateTime.now());
                offer.setRequiredSkills("N/A"); // Default for now
                offer.setEntreprise(new User(1)); // Dummy enterprise user
                offerService.ajouter(offer);
                showInfo("Succès", "L'offre a été publiée avec succès.");
            } else {
                offerService.modifier(offer);
                showInfo("Succès", "L'offre a été mise à jour avec succès.");
            }

            currentOffer = null;
            showListPage();
            loadMyOffers();
        } catch (Exception e) {
            showError("Erreur", "Veuillez vérifier les champs: " + e.getMessage());
        }
    }

    private void showApplications(Offer offer) {
        currentOffer = offer;
        applicationsContainer.getChildren().clear();
        try {
            List<CvApplication> apps = cvApplicationService.getApplicationsByOffer(offer.getId());
            for (CvApplication app : apps) {
                applicationsContainer.getChildren().add(createApplicationRow(app));
            }

            listPage.setVisible(false);
            listPage.setManaged(false);
            formPage.setVisible(false);
            formPage.setManaged(false);
            applicationsPage.setVisible(true);
            applicationsPage.setManaged(true);
        } catch (SQLException e) {
            showError("Erreur", "Impossible de charger les candidatures: " + e.getMessage());
        }
    }

    private HBox createApplicationRow(CvApplication app) {
        HBox row = new HBox(25);
        row.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.05), 10, 0, 0, 5); -fx-margin: 0 0 10 0; -fx-border-color: #f1f5f9; -fx-border-width: 1; -fx-border-radius: 15;");
        row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);

        // Candidate Avatar Placeholder
        VBox avatar = new VBox();
        avatar.setAlignment(javafx.geometry.Pos.CENTER);
        avatar.setPrefSize(50, 50);
        avatar.setStyle("-fx-background-color: #eff6ff; -fx-background-radius: 25;");
        Label initial = new Label(app.getCv().getNomCv().substring(0, 1).toUpperCase());
        initial.setStyle("-fx-text-fill: #3b82f6; -fx-font-weight: bold; -fx-font-size: 18;");
        avatar.getChildren().add(initial);

        VBox info = new VBox(5);
        Label name = new Label(app.getCv().getNomCv());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #0f172a;");

        HBox metaBox = new HBox(15);
        Label lang = new Label("🌐 " + app.getCv().getLangue());
        lang.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13;");
        Label date = new Label("📅 " + app.getAppliedAt().toLocalDate());
        date.setStyle("-fx-text-fill: #64748b; -fx-font-size: 13;");
        metaBox.getChildren().addAll(lang, date);

        info.getChildren().addAll(name, metaBox);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Status Badge
        Label statusLabel = new Label(app.getStatus().toUpperCase());
        String statusStyle = "-fx-padding: 6 12; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 11;";
        if (app.getStatus().equals("ACCEPTED")) {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
        } else if (app.getStatus().equals("REJECTED")) {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        } else {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
        }

        HBox actionButtons = new HBox(10);
        actionButtons.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);

        Button acceptBtn = new Button("Accepter");
        acceptBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        acceptBtn.setOnAction(e -> updateAppStatus(app, "ACCEPTED"));

        Button rejectBtn = new Button("Refuser");
        rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        rejectBtn.setOnAction(e -> updateAppStatus(app, "REJECTED"));

        actionButtons.getChildren().addAll(acceptBtn, rejectBtn);

        row.getChildren().addAll(avatar, info, spacer, statusLabel, actionButtons);
        return row;
    }

    private void updateAppStatus(CvApplication app, String status) {
        try {
            cvApplicationService.updateStatus(app.getId(), status);
            showApplications(currentOffer);
        } catch (SQLException e) {
            showError("Erreur", e.getMessage());
        }
    }

    @FXML
    private void handleShowAddForm() {
        currentOffer = null;
        formTitleLabel.setText("Publier une nouvelle offre");
        formSubtitleLabel.setText("Attirez les meilleurs talents en décrivant votre besoin.");
        saveOfferBtn.setText("Publier l'offre");
        clearForm();
        listPage.setVisible(false);
        listPage.setManaged(false);
        formPage.setVisible(true);
        formPage.setManaged(true);
        applicationsPage.setVisible(false);
        applicationsPage.setManaged(false);
    }

    @FXML
    private void showListPage() {
        listPage.setVisible(true);
        listPage.setManaged(true);
        formPage.setVisible(false);
        formPage.setManaged(false);
        applicationsPage.setVisible(false);
        applicationsPage.setManaged(false);
    }

    private void clearForm() {
        titleField.clear();
        typeComboBox.setValue(null);
        fieldField.clear();
        descriptionArea.clear();
        levelComboBox.setValue(null);
        locationField.clear();
        contractComboBox.setValue(null);
        salaryField.clear();
    }

    private void showError(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(msg);
        a.show();
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setContentText(msg);
        a.show();
    }
}

