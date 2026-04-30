package com.pidev.Controllers.client;

import com.pidev.models.*;
import com.pidev.Services.OfferService;
import com.pidev.Services.CvApplicationService;
import com.pidev.Services.CVService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import com.pidev.Services.MotivationLetterService;
import com.pidev.Services.PdfService;
import com.pidev.Controllers.admin.BackofficeCVDetailsController;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import java.io.File;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.geometry.Pos;

import java.io.IOException;
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
    private final MotivationLetterService letterService = new MotivationLetterService();
    private final PdfService pdfService = new PdfService();
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
        row.setAlignment(Pos.CENTER_LEFT);

        // Candidate Avatar Placeholder
        VBox avatar = new VBox();
        avatar.setAlignment(Pos.CENTER);
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

        // Action Buttons for viewing
        HBox viewButtons = new HBox(12);
        viewButtons.setAlignment(Pos.CENTER);

        Button viewCvBtn = new Button("📄 Voir CV");
        viewCvBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #3b82f6; -fx-border-color: #3b82f6; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 15; -fx-font-weight: bold;");
        viewCvBtn.setOnAction(e -> handleViewCv(app));

        Button viewLetterBtn = new Button("✉️ Voir Lettre");
        viewLetterBtn.setStyle("-fx-background-color: #f8fafc; -fx-text-fill: #6366f1; -fx-border-color: #6366f1; -fx-border-radius: 8; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 15; -fx-font-weight: bold;");
        viewLetterBtn.setOnAction(e -> handleViewLetter(app));

        viewButtons.getChildren().addAll(viewCvBtn, viewLetterBtn);

        // Status Badge
        Label statusLabel = new Label(app.getStatus().toUpperCase());
        String statusStyle = "-fx-padding: 8 15; -fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12; -fx-min-width: 100; -fx-alignment: center;";
        if (app.getStatus().equals("ACCEPTED")) {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #dcfce7; -fx-text-fill: #166534;");
        } else if (app.getStatus().equals("REJECTED")) {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #fee2e2; -fx-text-fill: #991b1b;");
        } else {
            statusLabel.setStyle(statusStyle + "-fx-background-color: #f1f5f9; -fx-text-fill: #475569;");
        }

        HBox actionButtons = new HBox(12);
        actionButtons.setAlignment(Pos.CENTER_RIGHT);
        actionButtons.setMinWidth(220); // Fixed width to prevent jumping

        if ("PENDING".equalsIgnoreCase(app.getStatus())) {
            Button acceptBtn = new Button("Accepter");
            acceptBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
            acceptBtn.setOnAction(e -> updateAppStatus(app, "ACCEPTED"));

            Button rejectBtn = new Button("Refuser");
            rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
            rejectBtn.setOnAction(e -> updateAppStatus(app, "REJECTED"));

            actionButtons.getChildren().addAll(acceptBtn, rejectBtn);
        } else {
            Label infoLabel = new Label("Décision prise");
            infoLabel.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            actionButtons.getChildren().add(infoLabel);
        }

        row.getChildren().addAll(avatar, info, spacer, viewButtons, statusLabel, actionButtons);
        return row;
    }

    private void handleViewCv(CvApplication app) {
        try {
            // Re-load the CV fully from DB to get experiences, skills, etc.
            Cv fullCv = cvService.getById(app.getCv().getId());
            if (fullCv == null) {
                showError("Erreur", "Impossible de charger les détails complets du CV.");
                return;
            }

            int templateId = (fullCv.getIdTemplate() != null) ? fullCv.getIdTemplate() : 1;
            String fxmlPath = "/Fxml/client/templates/Template" + templateId + ".fxml";

            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent templateRoot = loader.load();

            // Populate the template with data
            populateTemplateData(templateRoot, fullCv);

            Stage stage = new Stage();
            stage.setTitle("Aperçu du CV - " + fullCv.getNomCv());
            stage.initModality(Modality.APPLICATION_MODAL);

            VBox mainLayout = new VBox();
            mainLayout.setStyle("-fx-background-color: white;");

            ScrollPane scrollPane = new ScrollPane(templateRoot);
            scrollPane.setFitToWidth(true);
            scrollPane.setStyle("-fx-background-color: white; -fx-background: white; -fx-border-color: transparent;");
            VBox.setVgrow(scrollPane, Priority.ALWAYS);

            mainLayout.getChildren().add(scrollPane);

            // Footer with buttons (Always show PDF button, conditionally show Accept/Reject)
            HBox footer = new HBox(15);
            footer.setStyle("-fx-padding: 15; -fx-background-color: #f8fafc; -fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;");
            footer.setAlignment(Pos.CENTER_RIGHT);

            Button pdfBtn = new Button("Télécharger PDF");
            pdfBtn.setStyle("-fx-background-color: #3b82f6; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
            pdfBtn.setOnAction(e -> handleDownloadPdf(fullCv, stage));

            Button closeBtn = new Button("Fermer");
            closeBtn.setStyle("-fx-background-color: white; -fx-text-fill: #64748b; -fx-border-color: #e2e8f0; -fx-border-radius: 8; -fx-background-radius: 8; -fx-padding: 8 20; -fx-cursor: hand;");
            closeBtn.setOnAction(e -> stage.close());

            footer.getChildren().addAll(pdfBtn, closeBtn);

            if ("PENDING".equalsIgnoreCase(app.getStatus())) {
                Button acceptBtn = new Button("Accepter");
                acceptBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
                acceptBtn.setOnAction(e -> {
                    updateAppStatus(app, "ACCEPTED");
                    stage.close();
                });

                Button rejectBtn = new Button("Refuser");
                rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 8; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 20;");
                rejectBtn.setOnAction(e -> {
                    updateAppStatus(app, "REJECTED");
                    stage.close();
                });

                footer.getChildren().add(1, rejectBtn);
                footer.getChildren().add(2, acceptBtn);
            }

            mainLayout.getChildren().add(footer);

            // Window size: reduced for a better look
            Scene scene = new Scene(mainLayout, 800, 700);
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le CV: " + e.getMessage());
        }
    }

    // Helper methods for template population (copied from MyCVController for self-containment)
    private void populateTemplateData(Node root, Cv cv) {
        // Profile Image (Support both Circle and ImageView)
        String photoPath = cv.getPhotoUrl();
        // Fallback to user profile photo if CV photo is missing
        if ((photoPath == null || photoPath.isBlank()) && cv.getUser() != null) {
            photoPath = cv.getUser().getPhoto();
        }

        if (photoPath != null && !photoPath.isBlank()) {
            try {
                String finalPath;
                if (photoPath.startsWith("http") || photoPath.startsWith("file:")) {
                    finalPath = photoPath;
                } else {
                    java.io.File file = new java.io.File(photoPath);
                    if (!file.isAbsolute()) {
                        // Try relative to project root or user home if needed
                        file = new java.io.File(System.getProperty("user.dir"), photoPath);
                    }
                    finalPath = file.toURI().toString();
                }

                Image img = new Image(finalPath, false); // Load synchronously for immediate view

                // Try Circle placeholder
                Node circleNode = root.lookup("#profileCircle");
                if (circleNode instanceof javafx.scene.shape.Circle) {
                    ((javafx.scene.shape.Circle) circleNode).setFill(new javafx.scene.paint.ImagePattern(img, 0, 0, 1, 1, true));
                }

                // Try ImageView placeholder
                Node imageNode = root.lookup("#profileImage");
                if (imageNode instanceof javafx.scene.image.ImageView) {
                    ((javafx.scene.image.ImageView) imageNode).setImage(img);
                }
            } catch (Exception e) {
                System.err.println("Erreur chargement image: " + e.getMessage());
            }
        }

        setLabelText(root, "#nameLabel", (cv.getUser() != null ? cv.getUser().getDisplayName() : (cv.getNomCv() != null ? cv.getNomCv() : "")).toUpperCase());
        setLabelText(root, "#titleLabel", (cv.getLangue() != null ? cv.getLangue() : "").toUpperCase());
        setLabelText(root, "#emailLabel", cv.getUser() != null ? cv.getUser().getEmail() : "");
        setLabelText(root, "#linkedinLabel", cv.getLinkedinUrl());
        setLabelText(root, "#summaryLabel", cv.getSummary());

        // Additional contact info if labels exist in template
        setLabelText(root, "#phoneLabel", "Non spécifié");
        setLabelText(root, "#locationLabel", "Non spécifié");

        populateVBox(root, "#experienceBox", cv.getExperiences(), exp -> {
            VBox box = new VBox(5);
            Label title = new Label(exp.getJobTitle());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
            String dates = (exp.getStartDate() != null ? exp.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                    + " - " + (exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking() ? "Présent" : (exp.getEndDate() != null ? exp.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Présent"));
            Label subtitle = new Label(exp.getCompany() + " | " + exp.getLocation() + " (" + dates + ")");
            subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #6366f1;");
            Label desc = new Label(exp.getDescription());
            desc.setWrapText(true);
            desc.setStyle("-fx-font-size: 13; -fx-text-fill: #64748b;");
            box.getChildren().addAll(title, subtitle, desc);
            return box;
        });

        populateVBox(root, "#educationBox", cv.getEducations(), edu -> {
            VBox box = new VBox(5);
            Label title = new Label(edu.getDegree() + " en " + edu.getFieldOfStudy());
            title.setStyle("-fx-font-weight: bold; -fx-text-fill: #1e293b;");
            String dates = (edu.getStartDate() != null ? edu.getStartDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                    + " - " + (edu.getEndDate() != null ? edu.getEndDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "Présent");
            Label subtitle = new Label(edu.getSchool() + " | " + edu.getCity() + " (" + dates + ")");
            subtitle.setStyle("-fx-font-size: 12; -fx-text-fill: #6366f1;");
            box.getChildren().addAll(title, subtitle);
            return box;
        });

        populateVBox(root, "#skillsBox", cv.getSkills(), skill -> {
            Label label = new Label(skill.getNom());
            label.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #3b82f6; -fx-padding: 4 10; -fx-background-radius: 15; -fx-font-size: 12;");
            return label;
        });

        populateVBox(root, "#languagesBox", cv.getLanguages(), lang -> {
            Label label = new Label(lang.getNom() + " - " + lang.getNiveau());
            label.setStyle("-fx-font-size: 13; -fx-text-fill: #475569;");
            return label;
        });

        populateVBox(root, "#certificationsBox", cv.getCertifs(), cert -> {
            VBox box = new VBox(2);
            Label name = new Label(cert.getName());
            name.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
            String dates = (cert.getIssueDate() != null ? cert.getIssueDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "")
                    + (cert.getExpDate() != null ? " - Expire le: " + cert.getExpDate().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "");
            Label subtitle = new Label(cert.getIssuedBy() + " (" + dates + ")");
            subtitle.setStyle("-fx-font-size: 11; -fx-text-fill: #64748b;");
            box.getChildren().addAll(name, subtitle);
            return box;
        });
    }

    private void setLabelText(Node root, String id, String text) {
        Node node = root.lookup(id);
        if (node instanceof Label) {
            ((Label) node).setText(text != null && !text.isBlank() ? text : "...");
        }
    }

    private <T> void populateVBox(Node root, String id, List<T> items, java.util.function.Function<T, Node> mapper) {
        Node node = root.lookup(id);
        if (node instanceof VBox && items != null) {
            VBox box = (VBox) node;
            box.getChildren().clear();
            for (T item : items) {
                box.getChildren().add(mapper.apply(item));
            }
        } else if (node instanceof HBox && items != null) {
            HBox box = (HBox) node;
            box.getChildren().clear();
            for (T item : items) {
                box.getChildren().add(mapper.apply(item));
            }
        }
    }

    private void handleViewLetter(CvApplication app) {
        try {
            MotivationLetter letter = letterService.getByCvAndOffer(app.getCv().getId(), app.getOffer().getId());
            if (letter == null) {
                showInfo("Information", "Aucune lettre de motivation n'a été fournie pour cette candidature.");
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Lettre de Motivation - " + (app.getCv().getUser() != null ? app.getCv().getUser().getDisplayName() : app.getCv().getNomCv()));
            stage.initModality(Modality.APPLICATION_MODAL);

            VBox layout = new VBox(15);
            layout.setStyle("-fx-padding: 25; -fx-background-color: white;");

            Label title = new Label("Lettre de Motivation");
            title.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #1e293b;");

            // Simple text display as requested: "look like a simple text"
            TextArea contentArea = new TextArea(letter.getContent());
            contentArea.setEditable(false);
            contentArea.setWrapText(true);
            contentArea.setStyle("-fx-font-size: 14; -fx-text-fill: #334155; -fx-background-color: transparent; -fx-background-insets: 0; -fx-padding: 0; -fx-border-color: transparent;");
            contentArea.setPrefHeight(350);
            VBox.setVgrow(contentArea, Priority.ALWAYS);

            HBox footer = new HBox(12);
            footer.setAlignment(Pos.CENTER_RIGHT);
            footer.setStyle("-fx-padding: 10 0 0 0;");

            if ("PENDING".equalsIgnoreCase(app.getStatus())) {
                Button acceptBtn = new Button("Accepter");
                acceptBtn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 18;");
                acceptBtn.setOnAction(e -> {
                    updateAppStatus(app, "ACCEPTED");
                    stage.close();
                });

                Button rejectBtn = new Button("Refuser");
                rejectBtn.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; -fx-background-radius: 6; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 18;");
                rejectBtn.setOnAction(e -> {
                    updateAppStatus(app, "REJECTED");
                    stage.close();
                });

                footer.getChildren().addAll(rejectBtn, acceptBtn);
            }

            Button closeBtn = new Button("Fermer");
            closeBtn.setStyle("-fx-background-color: white; -fx-text-fill: #64748b; -fx-border-color: #e2e8f0; -fx-border-radius: 6; -fx-padding: 8 18; -fx-cursor: hand;");
            closeBtn.setOnAction(e -> stage.close());

            footer.getChildren().add(0, closeBtn);

            layout.getChildren().addAll(title, contentArea, footer);

            // Window size: reduced to 600x500 for a more "perfect" and compact look
            Scene scene = new Scene(layout, 600, 500);
            stage.setScene(scene);
            stage.show();
        } catch (SQLException e) {
            showError("Erreur", "Impossible de récupérer la lettre: " + e.getMessage());
        }
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

    private void handleDownloadPdf(Cv cv, Stage owner) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le CV en PDF");
        fileChooser.setInitialFileName("CV_" + (cv.getUser() != null ? cv.getUser().getNom() : cv.getNomCv()).replace(" ", "_") + ".pdf");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));

        File file = fileChooser.showSaveDialog(owner);
        if (file != null) {
            try {
                pdfService.generateCvPdf(cv, file.getAbsolutePath());
                showInfo("Succès", "Le PDF a été généré avec succès !");
            } catch (Exception e) {
                e.printStackTrace();
                showError("Erreur", "Impossible de générer le PDF : " + e.getMessage());
            }
        }
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private void showError(String title, String message) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setContentText(message);
        a.show();
    }
}

