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
import javafx.scene.layout.StackPane;
import javafx.util.StringConverter;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SponsorHackathonFormController implements Initializable {

    @FXML private ComboBox<Hackathon> hackathonCombo;
    @FXML private ComboBox<Sponsor> sponsorCombo;
    @FXML private Label hackathonError, sponsorError;

    private ServiceHackathon serviceH = new ServiceHackathon();
    private ServiceSponsor serviceS = new ServiceSponsor();
    private ServiceSponsorHackathon serviceSH = new ServiceSponsorHackathon();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupCombos();
        loadCombos();
    }

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

    private void loadCombos() {
        hackathonCombo.getItems().setAll(serviceH.getAll());
        sponsorCombo.getItems().setAll(serviceS.getAll());
    }

    @FXML
    private void save() {
        if (!validate()) return;

        SponsorHackathon sh = new SponsorHackathon();
        sh.setHackathon(hackathonCombo.getValue());
        sh.setSponsor(sponsorCombo.getValue());

        serviceSH.add(sh);
        goBack();
    }

    private boolean validate() {
        boolean valid = true;
        hideErrors();

        if (hackathonCombo.getValue() == null) {
            showError(hackathonCombo, hackathonError, "Please select a hackathon");
            valid = false;
        }

        if (sponsorCombo.getValue() == null) {
            showError(sponsorCombo, sponsorError, "Please select a sponsor");
            valid = false;
        } else if (hackathonCombo.getValue() != null) {
            // Optional: Check if already assigned
            boolean alreadyExists = serviceSH.getAll().stream().anyMatch(sh -> 
                sh.getHackathon().getId() == hackathonCombo.getValue().getId() &&
                sh.getSponsor().getId() == sponsorCombo.getValue().getId()
            );
            if (alreadyExists) {
                showError(sponsorCombo, sponsorError, "This sponsor is already assigned to this hackathon");
                valid = false;
            }
        }

        return valid;
    }

    private void showError(javafx.scene.control.Control control, Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        control.getStyleClass().add("form-control-error");
    }

    private void hideErrors() {
        hackathonError.setVisible(false);
        sponsorError.setVisible(false);
        hackathonCombo.getStyleClass().remove("form-control-error");
        sponsorCombo.getStyleClass().remove("form-control-error");
    }

    @FXML private void cancel() { goBack(); }

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
