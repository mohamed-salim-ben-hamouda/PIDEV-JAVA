package com.pidev.Controllers.client.User;

import com.pidev.models.User;
import com.pidev.Services.UserService;
import com.pidev.utils.SessionManager;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ResourceBundle;

public class ProfileController implements Initializable {

    @FXML private ImageView profileImage;
    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;

    private User currentUser;
    private UserService userService = new UserService();
    private String newPhotoPath = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        currentUser = SessionManager.getInstance().getUser();
        if (currentUser != null) {
            nomField.setText(currentUser.getNom());
            prenomField.setText(currentUser.getPrenom());
            emailField.setText(currentUser.getEmail());
            
            // Afficher l'image existante si disponible
            if (currentUser.getPhoto() != null && !currentUser.getPhoto().isEmpty()) {
                loadProfileImage(currentUser.getPhoto());
            }
        }
    }

    private void loadProfileImage(String photoPath) {
        try {
            File file = new File(photoPath);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                profileImage.setImage(image);
            } else {
                // If the path exists but file was not found, it might be relative in resources
                // Fallback attempt (advanced)
                try {
                    URL resource = getClass().getResource("/images/profiles/" + new File(photoPath).getName());
                    if (resource != null) {
                        profileImage.setImage(new Image(resource.toExternalForm()));
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            System.err.println("Impossible de charger la photo : " + e.getMessage());
        }
    }

    @FXML
    private void handleUploadPhoto(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images (PNG, JPG)", "*.png", "*.jpg", "*.jpeg")
        );
        
        File selectedFile = fileChooser.showOpenDialog(nomField.getScene().getWindow());
        if (selectedFile != null) {
            try {
                // Créer le dossier uploads dans le projet s'il n'existe pas
                String projectPath = System.getProperty("user.dir");
                File uploadDir = new File(projectPath + "/src/main/resources/images/profiles");
                if (!uploadDir.exists()) {
                    uploadDir.mkdirs();
                }

                String fileName = System.currentTimeMillis() + "_" + selectedFile.getName();
                Path targetPath = Paths.get(uploadDir.getAbsolutePath(), fileName);
                
                // Copier physiquement l'image dans le projet
                Files.copy(selectedFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                // Sauvegarder le chemin cible
                newPhotoPath = targetPath.toString();
                
                // Rafraîchir l'UI
                Image image = new Image(targetPath.toUri().toString());
                profileImage.setImage(image);

            } catch (IOException e) {
                showAlert(Alert.AlertType.ERROR, "Erreur d'upload", "Impossible de sauvegarder la photo.");
            }
        }
    }

    @FXML
    private void handleUpdateProfile(ActionEvent event) {
        if (currentUser == null) return;

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        if (nom.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le nom et l'email sont obligatoires.");
            return;
        }

        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        
        if (password != null && !password.isEmpty()) {
            currentUser.setPasswd(password);
        }

        if (newPhotoPath != null) {
            currentUser.setPhoto(newPhotoPath);
        }

        try {
            userService.update(currentUser);
            // Met à jour la session avec la nouvelle instance modifiée
            SessionManager.getInstance().setUser(currentUser);
            
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Mise à jour réussie ! Les modifications ont été enregistrées.");
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de la sauvegarde : " + e.getMessage());
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        if (currentUser != null) {
            userService.setConnectedStatus(currentUser.getId(), false);
        }
        SessionManager.getInstance().logout();
        
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Fxml/client/User/login.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            javafx.stage.Stage stage = (javafx.stage.Stage) nomField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de rediriger vers la page de connexion.");
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.show();
    }
}
