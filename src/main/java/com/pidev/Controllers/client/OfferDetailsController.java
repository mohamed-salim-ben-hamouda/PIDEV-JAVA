package com.pidev.Controllers.client;

import com.pidev.models.Cv;
import com.pidev.models.Offer;
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
import com.pidev.Services.CvApplicationService;

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
    @FXML private VBox applySection;
    @FXML private Button backButton;

    private Offer offer;
    private final CVService cvService = new CVService();
    private final CvApplicationService cvApplicationService = new CvApplicationService();
    private boolean isEnterpriseMode;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCvComboBox();
        loadUserCvs();
    }

    public void setData(Offer offer) {
        this.offer = offer;
        titleLabel.setText(offer.getTitle());
        String companyName = offer.getEntreprise() != null ? "ENTREPRISE " + offer.getEntreprise().getId() : "SKILLBRIDGE";
        companyLabel.setText(companyName.toUpperCase());
        typeLabel.setText(offer.getOfferType());
        fieldLabel.setText(offer.getField());
        levelLabel.setText(offer.getRequiredLevel() != null ? offer.getRequiredLevel() : "Tous niveaux");
        locationLabel.setText(offer.getLocation() != null ? offer.getLocation() : "À distance");
        salaryLabel.setText(offer.getSalaryRange() != null ? offer.getSalaryRange() + " DT" : "Non spécifié");
        descriptionLabel.setText(offer.getDescription());

        // Charger le logo
        try {
            Image logo = new Image(getClass().getResourceAsStream("/images/logo.png"));
            companyLogoImageView.setImage(logo);
        } catch (Exception e) {
            System.err.println("Erreur chargement logo: " + e.getMessage());
        }

        // Affichage des compétences sous forme de tags
        skillsContainer.getChildren().clear();
        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            String[] skills = offer.getRequiredSkills().split(",");
            for (String skill : skills) {
                Label tag = new Label(skill.trim());
                tag.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #3b82f6; -fx-padding: 8 15; -fx-background-radius: 10; -fx-font-weight: bold; -fx-font-size: 13;");
                skillsContainer.getChildren().add(tag);
            }
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
            cvComboBox.getItems().setAll(cvService.afficher());
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger vos CVs: " + e.getMessage());
        }
    }

    @FXML
    private void handleApply() {
        Cv selectedCv = cvComboBox.getValue();
        if (selectedCv == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un CV pour postuler.");
            return;
        }

        try {
            CvApplication application = new CvApplication();
            application.setOffer(offer);
            application.setCv(selectedCv);
            cvApplicationService.postuler(application);

            showAlert(Alert.AlertType.INFORMATION, "Succès",
                    "Votre candidature avec le CV '" + selectedCv.getNomCv() + "' a été envoyée avec succès pour l'offre '" + offer.getTitle() + "' !");
            handleBack();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de postuler: " + e.getMessage());
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
