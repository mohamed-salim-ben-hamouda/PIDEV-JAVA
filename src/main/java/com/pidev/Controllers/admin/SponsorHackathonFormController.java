package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceHackathon;
import com.pidev.Services.ServiceSponsor;
import com.pidev.Services.ServiceSponsorHackathon;
import com.pidev.models.Hackathon;
import com.pidev.models.Sponsor;
import com.pidev.models.SponsorHackathon;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SponsorHackathonFormController implements Initializable {

    @FXML private ComboBox<Hackathon> hackathonCombo;
    @FXML private ComboBox<Sponsor> sponsorCombo;
    @FXML private TextField typeField;
    @FXML private TextField valueField;
    @FXML private Label hackathonError, sponsorError, typeError, valueError, formTitle;

    private ServiceHackathon serviceH = new ServiceHackathon();
    private ServiceSponsor serviceS = new ServiceSponsor();
    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();
    
    private SponsorHackathon currentSH;

    /**
     * Initialise le formulaire en configurant les convertisseurs et en chargeant les données des combos.
     */
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCombos();
        loadCombos();
    }

    /**
     * Définit comment les objets Hackathon et Sponsor sont affichés dans les menus déroulants.
     */
    private void setupCombos() {
        hackathonCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Hackathon h) { return h == null ? "" : h.getTitle(); }
            @Override public Hackathon fromString(String string) { return null; }
        });
        sponsorCombo.setConverter(new StringConverter<>() {
            @Override public String toString(Sponsor s) { return s == null ? "" : s.getName(); }
            @Override public Sponsor fromString(String string) { return null; }
        });
    }

    /**
     * Remplit les menus déroulants avec les données de la base de données.
     */
    private void loadCombos() {
        hackathonCombo.getItems().setAll(serviceH.getAll());
        sponsorCombo.getItems().setAll(serviceS.getAll());
    }

    /**
     * Prépare le formulaire pour la modification d'une affectation existante.
     */
    public void setSponsorHackathon(SponsorHackathon sh) {
        this.currentSH = sh;
        if (sh != null) {
            formTitle.setText("Modifier l'affectation");
            
            // Sélectionner le hackathon correspondant par ID
            hackathonCombo.getItems().stream()
                .filter(h -> h.getId().equals(sh.getHackathon().getId()))
                .findFirst().ifPresent(h -> hackathonCombo.setValue(h));
                
            // Sélectionner le sponsor correspondant par ID
            sponsorCombo.getItems().stream()
                .filter(s -> s.getId().equals(sh.getSponsor().getId()))
                .findFirst().ifPresent(s -> sponsorCombo.setValue(s));
                
            typeField.setText(sh.getContributionType());
            valueField.setText(sh.getContributionValue() != null ? sh.getContributionValue().toString() : "0");
        } else {
            formTitle.setText("Affecter un sponsor à un hackathon");
        }
    }

    /**
     * Enregistre les données du formulaire (Ajout ou Mise à jour).
     */
    @FXML
    private void save() {
        if (!validate()) return;

        boolean isNew = (currentSH == null);
        if (isNew) currentSH = new SponsorHackathon();
        
        currentSH.setHackathon(hackathonCombo.getValue());
        currentSH.setSponsor(sponsorCombo.getValue());
        currentSH.setContributionType(typeField.getText());
        
        try {
            currentSH.setContributionValue(Double.parseDouble(valueField.getText().trim()));
        } catch (NumberFormatException e) {
            currentSH.setContributionValue(0.0);
        }

        if (isNew) {
            serviceSH.add(currentSH);
        } else {
            serviceSH.update(currentSH);
        }
        
        goBack();
    }

    /**
     * Valide les champs du formulaire et affiche des erreurs si nécessaire.
     */
    private boolean validate() {
        boolean valid = true;
        hideErrors();

        if (hackathonCombo.getValue() == null) {
            showError(hackathonCombo, hackathonError, "Veuillez sélectionner un hackathon");
            valid = false;
        }

        if (sponsorCombo.getValue() == null) {
            showError(sponsorCombo, sponsorError, "Veuillez sélectionner un sponsor");
            valid = false;
        } else if (hackathonCombo.getValue() != null && currentSH == null) {
            // Vérifier l'unicité uniquement pour les nouvelles affectations
            boolean alreadyExists = serviceSH.getAll().stream().anyMatch(sh -> 
                sh.getHackathon().getId().equals(hackathonCombo.getValue().getId()) &&
                sh.getSponsor().getId().equals(sponsorCombo.getValue().getId())
            );
            if (alreadyExists) {
                showError(sponsorCombo, sponsorError, "Ce sponsor est déjà affecté à ce hackathon");
                valid = false;
            }
        }

        if (typeField.getText().isEmpty()) {
            showError(typeField, typeError, "Veuillez spécifier le type de contribution");
            valid = false;
        }

        if (!valueField.getText().isEmpty()) {
            try {
                Double.parseDouble(valueField.getText().trim());
            } catch (NumberFormatException e) {
                showError(valueField, valueError, "Veuillez saisir une valeur numérique valide");
                valid = false;
            }
        } else {
            showError(valueField, valueError, "Veuillez spécifier la valeur de la contribution");
            valid = false;
        }

        return valid;
    }

    /**
     * Affiche un message d'erreur pour un champ spécifique.
     */
    private void showError(javafx.scene.control.Control control, Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        control.getStyleClass().add("form-control-error");
    }

    /**
     * Réinitialise l'affichage des erreurs.
     */
    private void hideErrors() {
        hackathonError.setVisible(false);
        sponsorError.setVisible(false);
        typeError.setVisible(false);
        valueError.setVisible(false);
        hackathonCombo.getStyleClass().remove("form-control-error");
        sponsorCombo.getStyleClass().remove("form-control-error");
        typeField.getStyleClass().remove("form-control-error");
        valueField.getStyleClass().remove("form-control-error");
    }

    /**
     * Annule l'opération en cours et retourne à la liste.
     */
    @FXML private void cancel() { goBack(); }

    /**
     * Retourne à l'affichage de la liste des affectations.
     */
    private void goBack() {
        try {
            Parent list = FXMLLoader.load(getClass().getResource("/Fxml/admin/sponsor_hackathon_list.fxml"));
            StackPane contentArea = (StackPane) hackathonCombo.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
