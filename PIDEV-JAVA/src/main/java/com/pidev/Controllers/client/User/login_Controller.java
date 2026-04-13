package com.pidev.Controllers.client.User;

import com.pidev.Services.UserService;
import com.pidev.models.User;
import com.pidev.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.util.ResourceBundle;
import java.util.regex.Pattern;
import java.util.prefs.Preferences;

public class login_Controller implements Initializable {

    @FXML private VBox loginForm;
    @FXML private VBox registerForm;
    @FXML private Button btnExisting;
    @FXML private Button btnNew;
    @FXML private ComboBox<String> regAccountType;

    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private CheckBox keepSignedInCheckBox;

    @FXML private TextField regName;
    @FXML private TextField regEmail;
    @FXML private PasswordField regPassword;

    @FXML private Label loginEmailError;
    @FXML private Label loginPasswordError;
    @FXML private Label regAccountTypeError;
    @FXML private Label regNameError;
    @FXML private Label regEmailError;
    @FXML private Label regPasswordError;

    private UserService userService = new UserService();

    private void showError(Label label, String message) {
        if (label != null) {
            label.setText(message);
            label.setStyle("-fx-text-fill: #e53935; -fx-font-size: 11.5px; -fx-font-weight: bold;");
            label.setVisible(true);
            label.setManaged(true);
        }
    }

    private void hideError(Label label) {
        if (label != null) {
            label.setVisible(false);
            label.setManaged(false);
            label.setText("");
        }
    }

    private void hideAllErrors() {
        hideError(loginEmailError);
        hideError(loginPasswordError);
        hideError(regAccountTypeError);
        hideError(regNameError);
        hideError(regEmailError);
        hideError(regPasswordError);
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> roles = FXCollections.observableArrayList(
                "STUDENT",
                "SUPERVISEUR",
                "ENTREPRISE"
        );

        regAccountType.setItems(roles);

        // Load saved credentials if "Keep me signed in" was checked
        Preferences prefs = Preferences.userNodeForPackage(login_Controller.class);
        String savedEmail = prefs.get("savedEmail", null);
        String savedPassword = prefs.get("savedPassword", null);

        if (savedEmail != null && savedPassword != null) {
            loginEmail.setText(savedEmail);
            loginPassword.setText(savedPassword);
            keepSignedInCheckBox.setSelected(true);
        }
    }

    @FXML
    private void handleToggle(ActionEvent event) {
        hideAllErrors();
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
    private void handleLogin(ActionEvent event) {
        hideAllErrors();
        boolean isValid = true;
        
        String email = loginEmail.getText().trim();
        String password = loginPassword.getText();

        if (email.isEmpty()) {
            showError(loginEmailError, "L'email est requis.");
            isValid = false;
        } else if (!isValidEmail(email)) {
            showError(loginEmailError, "Format email invalide.");
            isValid = false;
        }

        if (password.isEmpty()) {
            showError(loginPasswordError, "Le mot de passe est requis.");
            isValid = false;
        }

        if (!isValid) return;

        User user = userService.login(email, password);
        if (user != null) {
            Preferences prefs = Preferences.userNodeForPackage(login_Controller.class);
            if (keepSignedInCheckBox.isSelected()) {
                prefs.put("savedEmail", email);
                prefs.put("savedPassword", password);
            } else {
                prefs.remove("savedEmail");
                prefs.remove("savedPassword");
            }
            
            SessionManager.getInstance().setUser(user);
            userService.setConnectedStatus(user.getId(), true); // Mark as online
            
            if (user.getRole() == User.Role.ADMIN) {
                switchScene("/Fxml/admin/base_back.fxml", "Admin Dashboard");
            } else {
                switchScene("/Fxml/client/base.fxml", "Skill Bridge");
            }
        } else {
            showError(loginPasswordError, "Email ou mot de passe incorrect.");
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        hideAllErrors();
        boolean isValid = true;
        
        String type = regAccountType.getValue();
        String name = regName.getText().trim();
        String email = regEmail.getText().trim();
        String password = regPassword.getText();

        if (type == null) {
            showError(regAccountTypeError, "Veuillez sélectionner un type de compte.");
            isValid = false;
        }
        
        if (name.isEmpty()) {
            showError(regNameError, "Le nom est requis.");
            isValid = false;
        } else if (!name.matches("^[a-zA-ZÀ-ÿ\\s]+$")) {
            showError(regNameError, "Le nom ne doit contenir que des lettres.");
            isValid = false;
        }
        
        if (email.isEmpty()) {
            showError(regEmailError, "L'email est requis.");
            isValid = false;
        } else if (!isValidEmail(email)) {
            showError(regEmailError, "Format email invalide.");
            isValid = false;
        }
        
        if (password.isEmpty()) {
            showError(regPasswordError, "Le mot de passe est requis.");
            isValid = false;
        } else if (password.length() < 6) {
            showError(regPasswordError, "Le mot de passe doit faire au moins 6 caractères.");
            isValid = false;
        }

        if (!isValid) return;

        // Check if email already exists
        if (userService.isEmailExists(email)) {
            showError(regEmailError, "Cet email est déjà utilisé.");
            return;
        }

        User user = new User();
        user.setNom(name);
        user.setPrenom(""); // Required in DB, even if empty
        user.setEmail(email);
        user.setPasswd(password);
        user.setRole(User.Role.valueOf(type));
        user.setDateInscrit(LocalDateTime.now());
        user.setActive(true);

        try {
            if (userService.add(user)) {
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
                handleToggle(new ActionEvent(btnExisting, null));
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur Serveur", "Impossible d'enregistrer l'utilisateur. Veuillez réessayer.");
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur Inconnue", "Une erreur critique s'est produite : " + e.getMessage());
        }
    }

    @FXML
    private void handleForgotPassword(javafx.scene.input.MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/User/forgot_password.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) loginForm.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Forgot Password");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la vue mot de passe oublié.");
        }
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        return Pattern.compile(emailRegex).matcher(email).matches();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void switchScene(String fxmlPath, String title) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent root = loader.load();
            
            // If it's the client base, we need to load home as default
            if (fxmlPath.equals("/Fxml/client/base.fxml")) {
                com.pidev.Controllers.client.BaseController controller = loader.getController();
                controller.loadHome();
            } else if (fxmlPath.equals("/Fxml/admin/base_back.fxml")) {
                com.pidev.Controllers.admin.BaseController controller = loader.getController();
                root.setUserData(controller);
            }

            Stage stage = (Stage) loginForm.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle(title);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger la vue : " + fxmlPath);
        }
    }
}
