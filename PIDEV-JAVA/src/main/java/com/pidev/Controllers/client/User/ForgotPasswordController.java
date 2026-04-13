package com.pidev.Controllers.client.User;

import com.pidev.Services.UserService;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.io.IOException;

public class ForgotPasswordController {

    @FXML
    private TextField emailField;

    private UserService userService = new UserService();

    @FXML
    private void handleResetPassword(ActionEvent event) {
        String email = emailField.getText().trim();
        
        if (email.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez entrer votre adresse email.");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$")) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Format de l'email invalide.");
            return;
        }

        // Simulate sending a reset link (since there is no real email sending service yet)
        if (userService.isEmailExists(email)) {
            showAlert(Alert.AlertType.INFORMATION, "Vérifiez vos emails", 
                "Un lien de réinitialisation du mot de passe a été envoyé à : " + email);
            handleBackToLogin(event); // Redirect back to login after successful simulated request
        } else {
            // Un peu de sécurité: ne pas dire explicitement si l'email existe "Ce compte est introuvable"
            // showAlert(Alert.AlertType.ERROR, "Erreur", "Aucun compte trouvé avec cet email.");
            showAlert(Alert.AlertType.INFORMATION, "Vérifiez vos emails", 
                "Si cet email correspond à un compte, un lien de réinitialisation a été envoyé.");
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
