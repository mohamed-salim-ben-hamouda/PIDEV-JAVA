package com.pidev.Controllers.client;

import com.pidev.models.Offer;
import com.pidev.models.User;
import com.pidev.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import java.io.File;
import java.time.format.DateTimeFormatter;

public class OfferCardController {

    @FXML private Label titleLabel;
    @FXML private Label companyLabel;
    @FXML private ImageView companyLogoImageView;
    @FXML private Label typeLabel;
    @FXML private Label descriptionLabel;
    @FXML private Label locationLabel;
    @FXML private Label dateLabel;
    @FXML private Label offerTypeTag;
    @FXML private Label levelTag;
    @FXML private HBox skillsTagsContainer;
    @FXML private HBox enterpriseActions;
    @FXML private Button viewDetailsBtn;

    private Offer offer;
    private OfferListController parentController;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public void setData(Offer offer, OfferListController parent) {
        this.offer = offer;
        this.parentController = parent;

        String companyName = (offer.getEntreprise() != null && offer.getEntreprise().getNom() != null)
                ? offer.getEntreprise().getNom()
                : "SKILLBRIDGE";

        titleLabel.setText(offer.getTitle());
        companyLabel.setText(companyName.toUpperCase());

        // Charger l'image du logo
        try {
            if (offer.getEntreprise() != null && offer.getEntreprise().getPhoto() != null && !offer.getEntreprise().getPhoto().isEmpty()) {
                File file = new File(offer.getEntreprise().getPhoto());
                if (file.exists()) {
                    companyLogoImageView.setImage(new Image(file.toURI().toString()));
                } else {
                    companyLogoImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
                }
            } else {
                companyLogoImageView.setImage(new Image(getClass().getResourceAsStream("/images/logo.png")));
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'image logo : " + e.getMessage());
        }

        typeLabel.setText("Candidature rapide"); // Consistent with the screenshot style
        descriptionLabel.setText(offer.getDescription());
        locationLabel.setText(offer.getLocation() != null ? offer.getLocation() : "À distance");
        dateLabel.setText("Publié le " + (offer.getCreatedAt() != null ? offer.getCreatedAt().format(DATE_FORMATTER) : "---"));

        offerTypeTag.setText(offer.getOfferType());
        levelTag.setText(offer.getRequiredLevel() != null ? offer.getRequiredLevel() : "Tous niveaux");

        // Role-based UI for Enterprise
        User currentUser = SessionManager.getInstance().getUser();
        boolean isOwner = currentUser != null && offer.getEntreprise() != null && currentUser.getId().equals(offer.getEntreprise().getId());

        if (enterpriseActions != null) {
            enterpriseActions.setVisible(isOwner);
            enterpriseActions.setManaged(isOwner);
        }

        // Dynamic skills tags
        skillsTagsContainer.getChildren().clear();
        if (offer.getRequiredSkills() != null && !offer.getRequiredSkills().isEmpty()) {
            String[] skills = offer.getRequiredSkills().split(",");
            for (String skill : skills) {
                Label tag = new Label("#" + skill.trim());
                tag.setStyle("-fx-background-color: #eff6ff; -fx-text-fill: #3b82f6; -fx-padding: 4 12; -fx-background-radius: 20; -fx-border-color: #3b82f6; -fx-border-radius: 20; -fx-border-width: 0.5; -fx-font-size: 11;");
                skillsTagsContainer.getChildren().add(tag);
            }
        }
    }

    @FXML
    private void handleViewDetails() {
        parentController.showOfferDetails(offer);
    }

    @FXML
    private void handleEditOffer() {
        System.out.println("Editing offer: " + offer.getId());
        // Logic to open edit form
    }

    @FXML
    private void handleDeleteOffer() {
        System.out.println("Deleting offer: " + offer.getId());
        // Logic to delete offer via Service
    }
}
