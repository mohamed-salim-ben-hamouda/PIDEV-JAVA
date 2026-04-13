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

        previewExpBox.getChildren().clear();
        if (cv.getExperiences() != null) {
            for (Experience exp : cv.getExperiences()) {
                String dates = (exp.getStartDate() != null ? exp.getStartDate().format(DATE_FORMATTER) : "")
                        + " - " + (exp.getCurrentlyWorking() != null && exp.getCurrentlyWorking() ? "Aujourd'hui" : (exp.getEndDate() != null ? exp.getEndDate().format(DATE_FORMATTER) : ""));
                String subtitle = exp.getCompany() + (exp.getLocation() != null ? " | " + exp.getLocation() : "") + " (" + dates + ")";
                previewExpBox.getChildren().add(createPreviewItem(exp.getJobTitle(), subtitle, exp.getDescription()));
            }
        }

        previewEduBox.getChildren().clear();
        if (cv.getEducations() != null) {
            for (Education edu : cv.getEducations()) {
                String dates = (edu.getStartDate() != null ? edu.getStartDate().format(DATE_FORMATTER) : "")
                        + " - " + (edu.getEndDate() != null ? edu.getEndDate().format(DATE_FORMATTER) : "");
                String subtitle = edu.getSchool() + (edu.getCity() != null ? " | " + edu.getCity() : "") + " (" + dates + ")";
                previewEduBox.getChildren().add(createPreviewItem(edu.getDegree() + " en " + edu.getFieldOfStudy(), subtitle, edu.getDescription()));
            }
        }

        previewSkillBox.getChildren().clear();
        if (cv.getSkills() != null) {
            for (Skill skill : cv.getSkills()) {
                previewSkillBox.getChildren().add(createPreviewItem(skill.getNom(), skill.getType() + " | " + skill.getLevel(), null));
            }
        }

        previewCertBox.getChildren().clear();
        if (cv.getCertifs() != null) {
            for (Certif cert : cv.getCertifs()) {
                String dates = (cert.getIssueDate() != null ? cert.getIssueDate().format(DATE_FORMATTER) : "")
                        + (cert.getExpDate() != null ? " - Expire le " + cert.getExpDate().format(DATE_FORMATTER) : "");
                previewCertBox.getChildren().add(createPreviewItem(cert.getName(), cert.getIssuedBy() + " (" + dates + ")", null));
            }
        }

        previewLangBox.getChildren().clear();
        if (cv.getLanguages() != null) {
            for (Langue lang : cv.getLanguages()) {
                previewLangBox.getChildren().add(createPreviewItem(lang.getNom(), lang.getNiveau(), null));
            }
        }
    }

    private VBox createPreviewItem(String title, String subtitle, String description) {
        VBox item = new VBox(5);
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 16; -fx-text-fill: #1e293b;");
        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #6366f1; -fx-font-weight: bold;");
        item.getChildren().addAll(titleLbl, subtitleLbl);

        if (description != null && !description.isBlank()) {
            Label descLbl = new Label(description);
            descLbl.setWrapText(true);
            descLbl.setStyle("-fx-font-size: 14; -fx-text-fill: #64748b;");
            item.getChildren().add(descLbl);
        }
        return item;
    }
}
