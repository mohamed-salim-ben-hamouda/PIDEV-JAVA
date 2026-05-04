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
    @FXML private TextField GitField;
    private User currentUser;
    private UserService userService = new UserService();
    private String newPhotoPath = null;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Simple avatar clip
        javafx.scene.shape.Circle clip = new javafx.scene.shape.Circle(50, 50, 50);
        profileImage.setClip(clip);

        currentUser = SessionManager.getInstance().getUser();
        if (currentUser != null) {
            nomField.setText(currentUser.getNom());
            prenomField.setText(currentUser.getPrenom());
            emailField.setText(currentUser.getEmail());
            GitField.setText(currentUser.getGit_username());
            
            // Load existing photo or default
            loadProfileImage(currentUser.getPhoto());
        }
    }

    private void loadProfileImage(String photoPath) {
        if (photoPath == null || photoPath.isEmpty()) {
            setDefaultAvatar();
            return;
        }
        try {
            if (photoPath.startsWith("http://") || photoPath.startsWith("https://")) {
                Image image = new Image(photoPath, true); // true = load in background
                profileImage.setImage(image);
            } else {
                File file = new File(photoPath);
                if (file.exists()) {
                    Image image = new Image(file.toURI().toString());
                    profileImage.setImage(image);
                } else {
                    setDefaultAvatar();
                }
            }
        } catch (Exception e) {
            System.err.println("Impossible de charger la photo : " + e.getMessage());
            setDefaultAvatar();
        }
    }

    private void setDefaultAvatar() {
        try {
            URL defaultUrl = getClass().getResource("/images/logo.png");
            if (defaultUrl != null) {
                profileImage.setImage(new Image(defaultUrl.toExternalForm()));
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleUploadImage(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de profil");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        File selectedFile = fileChooser.showOpenDialog(profileImage.getScene().getWindow());
        if (selectedFile != null) {
            try {
                File uploadDir = new File("uploads");
                if (!uploadDir.exists()) uploadDir.mkdirs();

                String fileName = "user_upload_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destPath = Paths.get("uploads", fileName);
                Files.copy(selectedFile.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);

                this.newPhotoPath = destPath.toString();
                profileImage.setImage(new Image(selectedFile.toURI().toString()));
            } catch (IOException e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger l'image : " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleGenerateAvatar(ActionEvent event) {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/Fxml/client/User/ai_avatar_capture.fxml"));
            javafx.scene.Parent root = loader.load();
            
            AIAvatarCaptureController captureController = loader.getController();
            captureController.setProfileController(this);
            
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Capture Avatar IA");
            stage.setScene(new javafx.scene.Scene(root));
            
            // Éviter que l'utilisateur clique derrière pendant la capture
            stage.initModality(javafx.stage.Modality.APPLICATION_MODAL);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir la fenêtre de capture IA : " + e.getMessage());
        }
    }

    public void setAvatarPath(String path) {
        this.newPhotoPath = path;
        try {
            File file = new File(path);
            if (file.exists()) {
                Image image = new Image(file.toURI().toString());
                profileImage.setImage(image);
            }
        } catch (Exception e) {
            System.err.println("Erreur chargement avatar généré: " + e.getMessage());
        }
    }



    @FXML
    private void handleUpdateProfile(ActionEvent event) {
        if (currentUser == null) return;

        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String git = GitField.getText().trim();
        if (nom.isEmpty() || email.isEmpty()) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Le nom et l'email sont obligatoires.");
            return;
        }

        currentUser.setNom(nom);
        currentUser.setPrenom(prenom);
        currentUser.setEmail(email);
        currentUser.setGit_username(git);
        if (password != null && !password.isEmpty()) {
            currentUser.setPasswd(password);
        }

        if (newPhotoPath != null) {
            currentUser.setPhoto(newPhotoPath);
        }

        try {
            userService.update(currentUser);
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

    @FXML
    private void handleSetupFaceId(ActionEvent event) {
        System.out.println("========= BOUTON FACE ID CLIQUÉ =========");
        try {
            if (currentUser != null) {
                FaceIdSetupController setupController = new FaceIdSetupController();
                setupController.startSetup(currentUser.getId());
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Utilisateur non connecté.");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Caméra", "Erreur lors du lancement de la caméra : " + e.getMessage());
        } catch (Error e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur Critique", "Erreur critique : " + e.getMessage());
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
