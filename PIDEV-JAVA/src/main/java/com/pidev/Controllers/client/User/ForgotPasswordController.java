package com.pidev.Controllers.client.User;

import com.pidev.Services.EmailService;
import com.pidev.Services.UserService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Random;

public class ForgotPasswordController {

    @FXML private VBox step1Box;
    @FXML private VBox step2Box;
    @FXML private VBox step3Box;

    @FXML private TextField emailField;
    @FXML private TextField codeField;
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;

    private UserService userService = new UserService();
    private EmailService emailService = new EmailService();

    private String targetEmail;
    private String generatedCode;

    @FXML
    private void handleSendCode(ActionEvent event) {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer votre adresse email.");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format de l'email invalide.");
            return;
        }

        if (!userService.isEmailExists(email)) {
            // Affichage clair pour le debug (à remettre en Information en production)
            showAlert(Alert.AlertType.ERROR, "Debug: Email introuvable", 
                "Cet email (" + email + ") n'existe pas dans la base de données ! L'envoi est annulé.");
            return;
        }

        targetEmail = email;
        // Generate a 6-digit code
        generatedCode = String.format("%06d", new Random().nextInt(999999));

        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Envoi en cours");
        loadingAlert.setHeaderText("Veuillez patienter...");
        loadingAlert.setContentText("Envoi du code de vérification par email.");
        loadingAlert.show();

        Thread emailThread = new Thread(() -> {
            boolean success = emailService.sendResetCode(targetEmail, generatedCode);
            
            Platform.runLater(() -> {
                loadingAlert.close();
                if (success) {
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Le code a été envoyé à votre adresse email !");
                    // Switch to Step 2
                    step1Box.setVisible(false);
                    step1Box.setManaged(false);
                    step2Box.setVisible(true);
                    step2Box.setManaged(true);
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Échec de l'envoi de l'email. Vérifiez votre configuration réseau ou les identifiants SMTP.");
                }
            });
        });
        emailThread.setDaemon(true);
        emailThread.start();
    }

    @FXML
    private void handleVerifyCode(ActionEvent event) {
        String inputCode = codeField.getText().trim();
        
        if (inputCode.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer le code.");
            return;
        }

        if (inputCode.equals(generatedCode)) {
            // Code correct, switch to Step 3
            step2Box.setVisible(false);
            step2Box.setManaged(false);
            step3Box.setVisible(true);
            step3Box.setManaged(true);
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le code est incorrect.");
        }
    }

    @FXML
    private void handleUpdatePassword(ActionEvent event) {
        String newPass = newPasswordField.getText();
        String confirmPass = confirmPasswordField.getText();

        if (newPass.isEmpty() || confirmPass.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez remplir tous les champs.");
            return;
        }

        if (newPass.length() < 6) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le mot de passe doit faire au moins 6 caractères.");
            return;
        }

        if (!newPass.equals(confirmPass)) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Les mots de passe ne correspondent pas.");
            return;
        }

        boolean success = userService.updatePassword(targetEmail, newPass);
        
        if (success) {
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Votre mot de passe a été mis à jour avec succès !");
            handleBackToLogin(event);
        } else {
            showAlert(Alert.AlertType.ERROR, "Erreur Serveur", "Impossible de mettre à jour le mot de passe.");
        }
    }

    @FXML
    private void handleBackToLogin(ActionEvent event) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Fxml/client/User/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de retourner à la page de connexion.");
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
