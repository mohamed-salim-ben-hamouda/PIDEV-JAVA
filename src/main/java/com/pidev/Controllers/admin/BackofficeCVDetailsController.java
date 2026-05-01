package com.pidev.Controllers.admin;

import com.pidev.models.*;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

public class BackofficeCVDetailsController implements Initializable {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    @FXML private Label previewNomLabel;
    @FXML private Label previewLangueLabel;
    @FXML private Label previewSummaryLabel;
    @FXML private Label previewLinkedinLabel;
    @FXML private VBox photoContainer;
    @FXML private Label photoPlaceholder;
    @FXML private VBox previewExpBox;
    @FXML private VBox previewEduBox;
    @FXML private VBox previewSkillBox;
    @FXML private VBox previewCertBox;
    @FXML private VBox previewLangBox;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setData(Cv cv) {
        previewNomLabel.setText(cv.getNomCv() != null ? cv.getNomCv().toUpperCase() : "SANS NOM");
        previewLangueLabel.setText(cv.getLangue() != null ? cv.getLangue() : "Langue non spécifiée");
        previewSummaryLabel.setText(cv.getSummary() != null && !cv.getSummary().isBlank() ? cv.getSummary() : "Aucun résumé fourni.");
        previewLinkedinLabel.setText(cv.getLinkedinUrl() != null && !cv.getLinkedinUrl().isBlank() ? cv.getLinkedinUrl() : "Non renseigné");

        // Handle Photo
        if (cv.getPhotoUrl() != null && !cv.getPhotoUrl().isBlank()) {
            try {
                javafx.scene.image.Image image = new javafx.scene.image.Image(cv.getPhotoUrl(), true);
                javafx.scene.image.ImageView imageView = new javafx.scene.image.ImageView(image);
                imageView.setFitHeight(100);
                imageView.setFitWidth(100);
                imageView.setPreserveRatio(true);

                // Rounded corners for the image
                javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(100, 100);
                clip.setArcWidth(30);
                clip.setArcHeight(30);
                imageView.setClip(clip);

                photoContainer.getChildren().clear();
                photoContainer.getChildren().add(imageView);
            } catch (Exception e) {
                System.err.println("Erreur chargement photo: " + e.getMessage());
            }
        }

        previewExpBox.getChildren().clear();
        if (cv.getExperiences() != null && !cv.getExperiences().isEmpty()) {
            for (Experience exp : cv.getExperiences()) {
                String dates = (exp.getStartDate() != null ? exp.getStartDate().format(DATE_FORMATTER) : "")
                        + " - " + (exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking() ? "Aujourd'hui" : (exp.getEndDate() != null ? exp.getEndDate().format(DATE_FORMATTER) : ""));
                String subtitle = exp.getCompany() + (exp.getLocation() != null ? " | " + exp.getLocation() : "") + " (" + dates + ")";
                previewExpBox.getChildren().add(createPreviewItem(exp.getJobTitle(), subtitle, exp.getDescription()));
            }
        } else {
            previewExpBox.getChildren().add(createEmptyLabel("Aucune expérience renseignée."));
        }

        previewEduBox.getChildren().clear();
        if (cv.getEducations() != null && !cv.getEducations().isEmpty()) {
            for (Education edu : cv.getEducations()) {
                String dates = (edu.getStartDate() != null ? edu.getStartDate().format(DATE_FORMATTER) : "")
                        + " - " + (edu.getEndDate() != null ? edu.getEndDate().format(DATE_FORMATTER) : "");
                String subtitle = edu.getSchool() + (edu.getCity() != null ? " | " + edu.getCity() : "") + " (" + dates + ")";
                previewEduBox.getChildren().add(createPreviewItem(edu.getDegree() + " en " + edu.getFieldOfStudy(), subtitle, edu.getDescription()));
            }
        } else {
            previewEduBox.getChildren().add(createEmptyLabel("Aucune formation renseignée."));
        }

        previewSkillBox.getChildren().clear();
        if (cv.getSkills() != null && !cv.getSkills().isEmpty()) {
            for (Skill skill : cv.getSkills()) {
                previewSkillBox.getChildren().add(createPreviewItem(skill.getNom(), skill.getType() + " | " + skill.getLevel(), null));
            }
        } else {
            previewSkillBox.getChildren().add(createEmptyLabel("Aucune compétence renseignée."));
        }

        previewCertBox.getChildren().clear();
        if (cv.getCertifs() != null && !cv.getCertifs().isEmpty()) {
            for (Certif cert : cv.getCertifs()) {
                String dates = (cert.getIssueDate() != null ? cert.getIssueDate().format(DATE_FORMATTER) : "")
                        + (cert.getExpDate() != null ? " - Expire le " + cert.getExpDate().format(DATE_FORMATTER) : "");
                previewCertBox.getChildren().add(createPreviewItem(cert.getName(), cert.getIssuedBy() + " (" + dates + ")", null));
            }
        } else {
            previewCertBox.getChildren().add(createEmptyLabel("Aucune certification renseignée."));
        }

        previewLangBox.getChildren().clear();
        if (cv.getLanguages() != null && !cv.getLanguages().isEmpty()) {
            for (Langue lang : cv.getLanguages()) {
                previewLangBox.getChildren().add(createPreviewItem(lang.getNom(), lang.getNiveau(), null));
            }
        } else {
            previewLangBox.getChildren().add(createEmptyLabel("Aucune langue renseignée."));
        }
    }

    private Label createEmptyLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #94a3b8; -fx-font-style: italic; -fx-font-size: 14;");
        return label;
    }

    private VBox createPreviewItem(String title, String subtitle, String description) {
        VBox item = new VBox(5);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1e293b;");
        titleLbl.setWrapText(true);

        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #6366f1; -fx-font-weight: bold;");
        subtitleLbl.setWrapText(true);

        item.getChildren().addAll(titleLbl, subtitleLbl);

        if (description != null && !description.isBlank()) {
            Label descLbl = new Label(description);
            descLbl.setWrapText(true);
            descLbl.setStyle("-fx-font-size: 14; -fx-text-fill: #475569; -fx-line-spacing: 3;");
            item.getChildren().add(descLbl);
        }
        return item;
    }
}
