package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceSponsor;
import com.pidev.models.Sponsor;
import com.pidev.utils.SessionManager;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class SponsorFormController implements Initializable {

    @FXML private Label formTitle, nameError, descriptionError, logoUrlError, websiteUrlError;
    @FXML private TextField nameField, logoUrlField, websiteUrlField;
    @FXML private TextArea descriptionArea;

    @FXML private ImageView imagePreview;
    @FXML private Label fileNameLabel;

    private Sponsor sponsor;
    private ServiceSponsor service = new ServiceSponsor();
    private String selectedImagePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setSponsor(Sponsor s) {
        this.sponsor = s;
        if (s != null) {
            formTitle.setText("Edit Sponsor");
            nameField.setText(s.getName());
            descriptionArea.setText(s.getDescription());
            websiteUrlField.setText(s.getWebsiteUrl());
            
            this.selectedImagePath = s.getLogoUrl();
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                fileNameLabel.setText("Current Logo");
                imagePreview.setImage(new Image(selectedImagePath));
            }
        }
    }

    @FXML
    private void save() {
        if (!validate()) return;

        boolean isNew = (sponsor == null);
        if (isNew) sponsor = new Sponsor();

        sponsor.setName(nameField.getText());
        sponsor.setDescription(descriptionArea.getText());
        sponsor.setLogoUrl(selectedImagePath);
        sponsor.setWebsiteUrl(websiteUrlField.getText());
        sponsor.setCreator(SessionManager.getUser());

        if (isNew) {
            service.add(sponsor);
        } else {
            service.update(sponsor);
        }

        goBack();
    }

    private boolean validate() {
        boolean valid = true;
        hideErrors();

        if (nameField.getText().trim().isEmpty()) {
            showError(nameField, nameError, "Sponsor name is required");
            valid = false;
        } else if (nameField.getText().length() < 2) {
            showError(nameField, nameError, "Name must be at least 2 characters");
            valid = false;
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            showError(descriptionArea, descriptionError, "Description is required");
            valid = false;
        }

        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            showError(imagePreview, logoUrlError, "Sponsor logo is required");
            valid = false;
        }

        if (!websiteUrlField.getText().isEmpty() && !websiteUrlField.getText().startsWith("http")) {
            showError(websiteUrlField, websiteUrlError, "Must be a valid URL starting with http");
            valid = false;
        }

        return valid;
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Sponsor Logo");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(nameField.getScene().getWindow());

        if (selectedFile != null) {
            selectedImagePath = selectedFile.toURI().toString();
            fileNameLabel.setText(selectedFile.getName());
            imagePreview.setImage(new Image(selectedImagePath));
            logoUrlError.setVisible(false);
        }
    }

    private void showError(javafx.scene.Node node, Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        node.getStyleClass().add("form-control-error");
    }

    private void hideErrors() {
        Label[] labels = {nameError, descriptionError, logoUrlError, websiteUrlError};
        for (Label l : labels) l.setVisible(false);

        javafx.scene.Node[] nodes = {nameField, descriptionArea, imagePreview, websiteUrlField};
        for (javafx.scene.Node n : nodes) n.getStyleClass().remove("form-control-error");
    }

    @FXML
    private void cancel() {
        goBack();
    }

    private void goBack() {
        try {
            Parent list = FXMLLoader.load(getClass().getResource("/Fxml/admin/sponsor_list.fxml"));
            StackPane contentArea = (StackPane) nameField.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
