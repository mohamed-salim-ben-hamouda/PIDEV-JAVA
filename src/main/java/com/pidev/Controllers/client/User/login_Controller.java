package com.pidev.Controllers.client.User;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
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
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label emailErrorLabel;
    @FXML private Label passwordErrorLabel;
    @FXML private TextField lastNameField;
    @FXML private TextField registerEmailField;
    @FXML private PasswordField registerPasswordField;
    @FXML private Label accountTypeErrorLabel;
    @FXML private Label lastNameErrorLabel;
    @FXML private Label registerEmailErrorLabel;
    @FXML private Label registerPasswordErrorLabel;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> roles = FXCollections.observableArrayList(
                "Student",
                "Supervisor",
                "Entreprise"
        );

        accountType.setItems(roles);
        clearLoginErrors();
        clearRegisterErrors();
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
            clearRegisterErrors();
        } else {
            registerForm.setVisible(false);
            registerForm.setManaged(false);
            loginForm.setVisible(true);
            loginForm.setManaged(true);

            btnExisting.getStyleClass().setAll("toggle-btn", "active");
            btnNew.getStyleClass().setAll("toggle-btn", "inactive");
            clearLoginErrors();
        }
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        clearLoginErrors();

        boolean valid = true;
        if (!isValidEmail(emailField.getText())) {
            emailErrorLabel.setText("Email invalide.");
            valid = false;
        }
        if (passwordField.getText() == null || passwordField.getText().trim().length() < 6) {
            passwordErrorLabel.setText("Mot de passe min. 6 caracteres.");
            valid = false;
        }

        if (valid) {
            emailErrorLabel.setText("");
            passwordErrorLabel.setText("");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        clearRegisterErrors();

        boolean valid = true;
        if (accountType.getValue() == null || accountType.getValue().isBlank()) {
            accountTypeErrorLabel.setText("Type de compte obligatoire.");
            valid = false;
        }
        if (lastNameField.getText() == null || lastNameField.getText().trim().length() < 2) {
            lastNameErrorLabel.setText("Champ obligatoire (min. 2 caracteres).");
            valid = false;
        }
        if (!isValidEmail(registerEmailField.getText())) {
            registerEmailErrorLabel.setText("Email invalide.");
            valid = false;
        }
        if (registerPasswordField.getText() == null || registerPasswordField.getText().trim().length() < 6) {
            registerPasswordErrorLabel.setText("Mot de passe min. 6 caracteres.");
            valid = false;
        }

        if (valid) {
            clearRegisterErrors();
        }
    }

    private void clearLoginErrors() {
        emailErrorLabel.setText("");
        passwordErrorLabel.setText("");
    }

    private void clearRegisterErrors() {
        accountTypeErrorLabel.setText("");
        lastNameErrorLabel.setText("");
        registerEmailErrorLabel.setText("");
        registerPasswordErrorLabel.setText("");
    }

    private boolean isValidEmail(String email) {
        if (email == null) {
            return false;
        }
        String value = email.trim();
        return !value.isEmpty() && value.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");
    }
}
