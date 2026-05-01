package com.pidev.Controllers.client;

import com.pidev.Services.CvApplicationService;
import com.pidev.models.Cv;
import com.pidev.models.Offer;
import com.pidev.models.User;
import com.pidev.Services.CVService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

import com.pidev.models.CvApplication;

import com.pidev.Services.AIService;
import com.pidev.Services.MotivationLetterService;
import com.pidev.models.MotivationLetter;
import com.pidev.utils.SessionManager;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.StageStyle;

public class OfferDetailsController implements Initializable {

    @FXML private Label titleLabel;
    @FXML private Label companyLabel;
    @FXML private ImageView companyLogoImageView;
    @FXML private Label typeLabel;
    @FXML private Label fieldLabel;
    @FXML private Label levelLabel;
    @FXML private Label locationLabel;
    @FXML private Label salaryLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label skillsLabel;
    @FXML private HBox skillsContainer;
    @FXML private ComboBox<Cv> cvComboBox;
    @FXML private Button generateAIButton;
    @FXML private VBox applySection;
    @FXML private Button backButton;
    @FXML private VBox cvSelectionBox;
    @FXML private VBox applicationControls;
    @FXML private VBox guestMessage;

    private Offer offer;
    private final CVService cvService = new CVService();
    private final CvApplicationService cvApplicationService = new CvApplicationService();
    private final AIService aiService = new AIService();
    private final MotivationLetterService letterService = new MotivationLetterService();
    private boolean isEnterpriseMode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCvComboBox();
        loadUserCvs();
        updateUIForLoginStatus();
    }

    private void updateUIForLoginStatus() {
        boolean isLogged = SessionManager.getInstance().isLogged();

        if (cvSelectionBox != null) {
            cvSelectionBox.setVisible(isLogged);
            cvSelectionBox.setManaged(isLogged);
        }

        if (applicationControls != null) {
            applicationControls.setVisible(isLogged);
            applicationControls.setManaged(isLogged);
        }

        if (guestMessage != null) {
            guestMessage.setVisible(!isLogged);
            guestMessage.setManaged(!isLogged);
        }
    }

    @FXML
    private void handleGoToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/User/login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) guestMessage.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void setData(Offer offer) {
        this.offer = offer;
        titleLabel.setText(offer.getTitle());

        String companyName = (offer.getEntreprise() != null && offer.getEntreprise().getNom() != null)
                ? offer.getEntreprise().getNom()
                : "SKILLBRIDGE TECHNOLOGY";

        companyLabel.setText(companyName.toUpperCase());
        typeLabel.setText(offer.getOfferType() != null ? offer.getOfferType() : "Temps plein");
        fieldLabel.setText(offer.getField() != null ? offer.getField() : "Général");
        levelLabel.setText(offer.getRequiredLevel() != null ? offer.getRequiredLevel() : "Tous niveaux");
        locationLabel.setText(offer.getLocation() != null ? offer.getLocation() : "À distance");
        salaryLabel.setText(offer.getSalaryRange() != null ? offer.getSalaryRange() + " DT" : "Non spécifié");
        descriptionLabel.setText(offer.getDescription());

        // Charger le logo
        try {
            if (offer.getEntreprise() != null && offer.getEntreprise().getPhoto() != null && !offer.getEntreprise().getPhoto().isEmpty()) {
                java.io.File file = new java.io.File(offer.getEntreprise().getPhoto());
                if (file.exists()) {
                    companyLogoImageView.setImage(new Image(file.toURI().toString()));
                } else {
                    companyLogoImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
                }
            } else {
                companyLogoImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement logo: " + e.getMessage());
        }

        // Affichage des compétences sous forme de tags modernes
        skillsContainer.getChildren().clear();
        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            String[] skills = offer.getRequiredSkills().split(",");
            for (String skill : skills) {
                Label tag = new Label(skill.trim());
                tag.setStyle("-fx-background-color: #f1f5f9; " +
                        "-fx-text-fill: #475569; " +
                        "-fx-padding: 8 20; " +
                        "-fx-background-radius: 10; " +
                        "-fx-font-weight: bold; " +
                        "-fx-font-size: 14; " +
                        "-fx-border-color: #e2e8f0; " +
                        "-fx-border-radius: 10;");
                skillsContainer.getChildren().add(tag);
            }
        } else {
            Label noSkills = new Label("Aucune compétence spécifique requise");
            noSkills.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic;");
            skillsContainer.getChildren().add(noSkills);
        }
    }

    public void setEnterpriseMode(boolean isEnterprise) {
        this.isEnterpriseMode = isEnterprise;
        if (applySection != null) {
            applySection.setVisible(!isEnterprise);
            applySection.setManaged(!isEnterprise);
        }
    }

    public void hideBackButton() {
        if (backButton != null) {
            backButton.setVisible(false);
            backButton.setManaged(false);
        }
    }

    private void setupCvComboBox() {
        cvComboBox.setConverter(new StringConverter<Cv>() {
            @Override
            public String toString(Cv cv) {
                return cv == null ? null : cv.getNomCv() + " (" + cv.getLangue() + ")";
            }

            @Override
            public Cv fromString(String string) {
                return null;
            }
        });
    }

    private void loadUserCvs() {
        try {
            User currentUser = SessionManager.getInstance().getUser();
            if (currentUser != null) {
                cvComboBox.getItems().setAll(cvService.afficherParUtilisateur(currentUser.getId()));
            } else {
                cvComboBox.getItems().setAll(cvService.afficher());
            }
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger vos CVs: " + e.getMessage());
        }
    }

    @FXML
    private void handleApply() {
        if (!SessionManager.getInstance().isLogged()) {
            showAlert(Alert.AlertType.WARNING, "Connexion requise", "Vous devez être connecté pour postuler à une offre.");
            return;
        }

        Cv selectedCv = cvComboBox.getValue();
        if (selectedCv == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un CV pour postuler.");
            return;
        }

        // Check if a motivation letter exists for this CV and offer
        try {
            MotivationLetter letter = letterService.getByCvAndOffer(selectedCv.getId(), offer.getId());
            if (letter == null) {
                // Propose to generate one with IA
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Lettre de motivation manquante");
                alert.setHeaderText("Une lettre de motivation est obligatoire pour postuler.");
                alert.setContentText("Voulez-vous générer automatiquement une lettre personnalisée avec l'IA ?");

                Button btnGen = new Button("Générer avec IA");
                alert.getButtonTypes().setAll(javafx.scene.control.ButtonType.YES, javafx.scene.control.ButtonType.NO);

                if (alert.showAndWait().orElse(javafx.scene.control.ButtonType.NO) == javafx.scene.control.ButtonType.YES) {
                    handleGenerateLetter();
                    return; // The user will continue from the dialog
                } else {
                    return;
                }
            }

            submitApplication(selectedCv);
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la vérification de la lettre: " + e.getMessage());
        }
    }

    private void submitApplication(Cv cv) {
        try {
            CvApplication application = new CvApplication();
            application.setOffer(offer);
            application.setCv(cv);
            application.setStatus("PENDING");
            cvApplicationService.postuler(application);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Votre candidature avec le CV '" + cv.getNomCv() + "' a été envoyée avec succès pour l'offre '" + offer.getTitle() + "' !");
            handleBack();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de postuler: " + e.getMessage());
        }
    }

    @FXML
    private void handleGenerateLetter() {
        System.out.println("handleGenerateLetter cliqué !");
        Cv selectedCv = cvComboBox.getValue();
        if (selectedCv == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez d'abord sélectionner un CV.");
            return;
        }

        System.out.println("CV sélectionné: " + selectedCv.getNomCv());
        System.out.println("Offre: " + offer.getTitle());

        // Désactiver le bouton et montrer le chargement
        generateAIButton.setDisable(true);
        generateAIButton.setText("⏳ Génération en cours...");

        new Thread(() -> {
            try {
                System.out.println("Appel AIService.generateMotivationLetter...");
                String letterText = aiService.generateMotivationLetter(selectedCv, offer, selectedCv.getLangue());
                System.out.println("Lettre générée avec succès ! Longueur: " + (letterText != null ? letterText.length() : 0));

                Platform.runLater(() -> {
                    generateAIButton.setDisable(false);
                    generateAIButton.setText("✨ Générer une lettre avec IA");
                    if (letterText == null || letterText.trim().isEmpty()) {
                        showAlert(Alert.AlertType.ERROR, "Erreur IA", "L'IA a retourné une lettre vide. Veuillez réessayer.");
                    } else {
                        showMotivationLetterDialog(selectedCv, offer, letterText);
                    }
                });
            } catch (Exception e) {
                System.err.println("Erreur IA: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    generateAIButton.setDisable(false);
                    generateAIButton.setText("✨ Générer une lettre avec IA");
                    showAlert(Alert.AlertType.ERROR, "Erreur IA", "Impossible de générer la lettre: " + e.getMessage());
                });
            }
        }).start();
    }

    private void showMotivationLetterDialog(Cv cv, Offer offer, String letterText) {
        try {
            System.out.println("Ouverture du dialogue MotivationLetterDialog...");
            URL fxmlLocation = getClass().getResource("/Fxml/client/MotivationLetterDialog.fxml");
            if (fxmlLocation == null) {
                throw new IOException("Fichier FXML introuvable: /Fxml/client/MotivationLetterDialog.fxml");
            }

            FXMLLoader loader = new FXMLLoader(fxmlLocation);
            Parent root = loader.load();

            MotivationLetterDialogController controller = loader.getController();
            if (controller == null) {
                throw new IOException("Impossible de récupérer le contrôleur MotivationLetterDialogController.");
            }

            controller.setData(cv, offer, letterText);

            Stage stage = new Stage();
            stage.setTitle("Votre Lettre de Motivation Générée");
            stage.initModality(Modality.APPLICATION_MODAL);
            // Suppression de StageStyle.TRANSPARENT pour éviter les problèmes d'affichage

            Scene scene = new Scene(root);
            stage.setScene(scene);

            System.out.println("Affichage du stage...");
            stage.showAndWait();

            if (controller.isConfirmed()) {
                System.out.println("Lettre confirmée, envoi de la candidature...");
                submitApplication(cv);
            }
        } catch (IOException e) {
            System.err.println("Erreur d'ouverture du dialogue: " + e.getMessage());
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir le dialogue: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        try {
            String fxmlPath = isEnterpriseMode ? "/Fxml/client/MyOffers.fxml" : "/Fxml/client/OfferList.fxml";
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            StackPane contentArea = (StackPane) titleLabel.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            } else {
                Stage stage = (Stage) titleLabel.getScene().getWindow();
                stage.getScene().setRoot(root);
            }
        } catch (IOException e) {
            e.printStackTrace();
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
