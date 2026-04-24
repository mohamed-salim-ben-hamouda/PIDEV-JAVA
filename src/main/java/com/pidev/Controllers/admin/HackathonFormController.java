package com.pidev.Controllers.admin;

import com.pidev.Services.ServiceHackathon;
import com.pidev.models.Hackathon;
import com.pidev.utils.SessionManager;
import com.pidev.utils.hackthon.CloudinaryUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class HackathonFormController implements Initializable {

    @FXML private Label formTitle, titleError, themeError, descriptionError, rulesError, startDateError, endDateError, regOpenError, regCloseError, feeError, maxTeamsError, locationError, coverUrlError;
    @FXML private TextField titleField, themeField, feeField, maxTeamsField, locationField, coverUrlField;
    @FXML private TextArea descriptionArea, rulesArea;
    @FXML private DatePicker startDatePicker, endDatePicker, regOpenPicker, regClosePicker;

    @FXML private ImageView imagePreview;
    @FXML private Label fileNameLabel;

    private Hackathon hackathon;
    private ServiceHackathon service = new ServiceHackathon();
    private String selectedImagePath;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    public void setHackathon(Hackathon h) {
        this.hackathon = h;
        if (h != null) {
            formTitle.setText("Edit Hackathon");
            titleField.setText(h.getTitle());
            themeField.setText(h.getTheme());
            descriptionArea.setText(h.getDescription());
            rulesArea.setText(h.getRules());
            startDatePicker.setValue(h.getStartAt().toLocalDate());
            endDatePicker.setValue(h.getEndAt().toLocalDate());
            regOpenPicker.setValue(h.getRegistrationOpenAt().toLocalDate());
            regClosePicker.setValue(h.getRegistrationCloseAt().toLocalDate());
            feeField.setText(String.valueOf(h.getFee()));
            maxTeamsField.setText(String.valueOf(h.getMaxTeams()));
            locationField.setText(h.getLocation());
            
            this.selectedImagePath = h.getCoverUrl();
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                fileNameLabel.setText("Current Image");
                imagePreview.setImage(new Image(selectedImagePath));
            }
        }
    }


    @FXML
    private void save() {
        if (!validate()) return;

        boolean isNew = (hackathon == null);
        if (isNew) hackathon = new Hackathon();

        hackathon.setTitle(titleField.getText());
        hackathon.setTheme(themeField.getText());
        hackathon.setDescription(descriptionArea.getText());
        hackathon.setRules(rulesArea.getText());
        hackathon.setStartAt(startDatePicker.getValue().atStartOfDay());
        hackathon.setEndAt(endDatePicker.getValue().atTime(23, 59));
        hackathon.setRegistrationOpenAt(regOpenPicker.getValue().atStartOfDay());
        hackathon.setRegistrationCloseAt(regClosePicker.getValue().atTime(23, 59));
        hackathon.setFee(Double.parseDouble(feeField.getText()));
        hackathon.setMaxTeams(Integer.parseInt(maxTeamsField.getText()));
        hackathon.setTeamSizeMax(5); // Default for now
        hackathon.setLocation(locationField.getText());
        hackathon.setCoverUrl(selectedImagePath);
        hackathon.setStatus("upcoming");
        hackathon.setCreator(SessionManager.getUser());

        if (isNew) {
            service.add(hackathon);
        } else {
            service.update(hackathon);
        }

        goBack();
    }

    private boolean validate() {
        boolean valid = true;
        hideErrors();

        if (titleField.getText().trim().isEmpty()) {
            showError(titleField, titleError, "Title is required");
            valid = false;
        } else if (titleField.getText().length() < 3 || titleField.getText().length() > 30) {
            showError(titleField, titleError, "Title must be between 3 and 30 characters");
            valid = false;
        }

        if (themeField.getText().trim().isEmpty()) {
            showError(themeField, themeError, "Theme is required");
            valid = false;
        }

        if (descriptionArea.getText().trim().isEmpty()) {
            showError(descriptionArea, descriptionError, "Description is required");
            valid = false;
        }

        if (rulesArea.getText().trim().isEmpty()) {
            showError(rulesArea, rulesError, "Rules are required");
            valid = false;
        }

        if (startDatePicker.getValue() == null) {
            showError(startDatePicker, startDateError, "Start date is required");
            valid = false;
        } else if (startDatePicker.getValue().isBefore(java.time.LocalDate.now())) {
            showError(startDatePicker, startDateError, "Start date cannot be in the past");
            valid = false;
        }

        if (endDatePicker.getValue() == null) {
            showError(endDatePicker, endDateError, "End date is required");
            valid = false;
        } else if (startDatePicker.getValue() != null && !endDatePicker.getValue().isAfter(startDatePicker.getValue())) {
            showError(endDatePicker, endDateError, "End date must be after start date");
            valid = false;
        }

        if (regClosePicker.getValue() != null && startDatePicker.getValue() != null && !regClosePicker.getValue().isBefore(startDatePicker.getValue())) {
            showError(regClosePicker, regCloseError, "Registration must close before the event starts");
            valid = false;
        }

        try {
            double fee = Double.parseDouble(feeField.getText());
            if (fee < 0) {
                showError(feeField, feeError, "Fee cannot be negative");
                valid = false;
            }
        } catch (NumberFormatException e) {
            showError(feeField, feeError, "Please enter a valid number for fee");
            valid = false;
        }

        try {
            int maxTeams = Integer.parseInt(maxTeamsField.getText());
            if (maxTeams <= 0) {
                showError(maxTeamsField, maxTeamsError, "Max teams must be at least 1");
                valid = false;
            }
        } catch (NumberFormatException e) {
            showError(maxTeamsField, maxTeamsError, "Please enter a valid integer");
            valid = false;
        }

        if (locationField.getText().trim().isEmpty()) {
            showError(locationField, locationError, "Location is required");
            valid = false;
        }

        if (selectedImagePath == null || selectedImagePath.isEmpty()) {
            showError(imagePreview, coverUrlError, "Please select a cover image");
            valid = false;
        }

        return valid;
    }

    @FXML
    private void handleUploadImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Cover Image");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.gif"));
        File selectedFile = fileChooser.showOpenDialog(titleField.getScene().getWindow());

        if (selectedFile != null) {
            try {
                // Show loading or something? For now simple upload
                String url = CloudinaryUtil.upload(selectedFile);
                selectedImagePath = url;
                fileNameLabel.setText(selectedFile.getName());
                imagePreview.setImage(new Image(selectedImagePath));
                coverUrlError.setVisible(false);
            } catch (IOException e) {
                e.printStackTrace();
                showError(imagePreview, coverUrlError, "Failed to upload image to Cloudinary");
            }
        }
    }

    private void showError(javafx.scene.Node node, Label label, String msg) {
        label.setText(msg);
        label.setVisible(true);
        node.getStyleClass().add("form-control-error");
    }

    private void hideErrors() {
        Label[] labels = {titleError, themeError, descriptionError, rulesError, startDateError, endDateError, regOpenError, regCloseError, feeError, maxTeamsError, locationError, coverUrlError};
        for (Label l : labels) l.setVisible(false);

        javafx.scene.Node[] nodes = {titleField, themeField, descriptionArea, rulesArea, startDatePicker, endDatePicker, regOpenPicker, regClosePicker, feeField, maxTeamsField, locationField, imagePreview};
        for (javafx.scene.Node n : nodes) n.getStyleClass().remove("form-control-error");
    }

    @FXML
    private void cancel() {
        goBack();
    }

    private void goBack() {
        try {
            Parent list = FXMLLoader.load(getClass().getResource("/Fxml/admin/hackathon_list.fxml"));
            StackPane contentArea = (StackPane) titleField.getScene().lookup("#contentArea");
            contentArea.getChildren().setAll(list);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
