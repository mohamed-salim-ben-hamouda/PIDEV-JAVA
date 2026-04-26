package com.pidev.Controllers.client.User;

import com.pidev.Services.UserService;
import com.pidev.utils.CurrentUserContext;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.ResourceBundle;

public class login_Controller implements Initializable {

    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML private Button btnExisting;
    @FXML private Button btnNew;
    @FXML private ComboBox<String> accountType;
    @FXML private TextField loginEmailField;
    @FXML private Label authFeedbackLabel;

    private final UserService userService = new UserService();

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> roles = FXCollections.observableArrayList(
                "Student",
                "Supervisor",
                "Entreprise"
        );

        accountType.setItems(roles);
    }

    @FXML
    private void handleToggle(ActionEvent event) {
        if (event.getSource() == btnNew) {
            loginForm.setVisible(false);
            loginForm.setManaged(false);
            registerForm.setVisible(true);
            registerForm.setManaged(true);

            btnNew.getStyleClass().setAll("toggle-btn", "active");
            btnExisting.getStyleClass().setAll("toggle-btn", "inactive");
        } else {
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            loginForm.setVisible(true);
            loginForm.setManaged(true);

            btnExisting.getStyleClass().setAll("toggle-btn", "active");
            btnNew.getStyleClass().setAll("toggle-btn", "inactive");
        }
    }

    @FXML
    private void handleLoginWithEmail() {
        String email = loginEmailField == null ? "" : loginEmailField.getText();
        if (email == null || email.isBlank()) {
            setFeedback("Please enter your email.", true);
            return;
        }

        try {
            var user = userService.findByEmail(email.trim());
            if (user == null || user.getId() == null) {
                setFeedback("No user found with this email.", true);
                return;
            }
            CurrentUserContext.loginAs(user.getId());
            setFeedback("Signed in as " + user.getDisplayName() + " (#" + user.getId() + ").", false);
        } catch (Exception e) {
            setFeedback("Login failed: " + e.getMessage(), true);
        }
    }

    private void setFeedback(String message, boolean error) {
        if (authFeedbackLabel == null) {
            return;
        }
        authFeedbackLabel.setText(message == null ? "" : message);
        authFeedbackLabel.setStyle(error ? "-fx-text-fill: #c62828;" : "-fx-text-fill: #2e7d32;");
    }
}
