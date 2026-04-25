package com.pidev.Controllers.admin;

import com.pidev.Services.UserService;
import com.pidev.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;
import java.io.File;

import java.net.URL;
import java.time.LocalDate;
import java.util.List;
import java.util.ResourceBundle;
import java.util.regex.Pattern;

public class UserManagementController implements Initializable {

    @FXML private TextField nomField;
    @FXML private TextField prenomField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwdField;
    @FXML private ComboBox<User.Role> roleComboBox;
    @FXML private DatePicker dateNaissancePicker;

    @FXML private ListView<User> userListView;

    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Load roles
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));

        // Setup ListView CellFactory
        userListView.setCellFactory(param -> new UserListCell());

        // Load users
        loadUsers();

        // Add selection listener
        userListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateFields(newSelection);
            }
        });
    }

    private void loadUsers() {
        try {
            List<User> activeUsers = userService.getActiveUsers();
            userList.setAll(activeUsers);
            userListView.setItems(userList);
            userListView.refresh();
        } catch (Exception e) {
            System.err.println("Error in loadUsers(): " + e.getMessage());
            showAlert(Alert.AlertType.ERROR, "Erreur de Chargement", "Erreur : " + e.getMessage());
        }
    }

    private void populateFields(User user) {
        nomField.setText(user.getNom());
        prenomField.setText(user.getPrenom());
        emailField.setText(user.getEmail());
        passwdField.setText(user.getPasswd());
        roleComboBox.setValue(user.getRole());
        dateNaissancePicker.setValue(user.getDateNaissance());
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        if (validateInput()) {
            User user = new User();
            user.setNom(nomField.getText());
            user.setPrenom(prenomField.getText());
            user.setEmail(emailField.getText());
            user.setPasswd(passwdField.getText());
            user.setRole(roleComboBox.getValue());
            user.setDateNaissance(dateNaissancePicker.getValue());
            
            if (userService.add(user)) {
                loadUsers();
                handleClearFields(null);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur ajouté avec succès !");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de l'ajout.");
            }
        }
    }

    @FXML
    private void handleUpdateUser(ActionEvent event) {
        User selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un utilisateur à modifier.");
            return;
        }

        if (validateInput()) {
            selectedUser.setNom(nomField.getText());
            selectedUser.setPrenom(prenomField.getText());
            selectedUser.setEmail(emailField.getText());
            selectedUser.setPasswd(passwdField.getText());
            selectedUser.setRole(roleComboBox.getValue());
            selectedUser.setDateNaissance(dateNaissancePicker.getValue());

            if (userService.update(selectedUser)) {
                loadUsers();
                handleClearFields(null);
                showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur modifié avec succès !");
            } else {
                showAlert(Alert.AlertType.ERROR, "Erreur", "Une erreur est survenue lors de la modification.");
            }
        }
    }

    @FXML
    private void handleDeleteUser(ActionEvent event) {
        User selectedUser = userListView.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert(Alert.AlertType.WARNING, "Attention", "Veuillez sélectionner un utilisateur à archiver.");
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous archiver cet utilisateur ? Il n'aura plus d'accès.", ButtonType.YES, ButtonType.NO);
        alert.showAndWait();

        if (alert.getResult() == ButtonType.YES) {
            selectedUser.setActive(false); // Soft Delete
            userService.update(selectedUser);
            loadUsers();
            handleClearFields(null);
            showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur archivé avec succès !");
        }
    }

    @FXML
    private void handleClearFields(ActionEvent event) {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwdField.clear();
        dateNaissancePicker.setValue(null);
        userListView.getSelectionModel().clearSelection();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    // --- Custom ListCell for User Cards ---
    private class UserListCell extends ListCell<User> {
        private final HBox container = new HBox(15);
        private final StackPane avatarStack = new StackPane();
        private final ImageView avatar = new ImageView();
        private final Circle statusDot = new Circle(6);
        private final VBox infoBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label emailLabel = new Label();
        private final Label roleBadge = new Label();
        private final Region spacer = new Region();
        private final HBox actions = new HBox(10);
        
        private final Button editBtn = new Button("Modifier");
        private final Button archiveBtn = new Button("Archiver");
        private final Button detailsBtn = new Button("Détails");

        public UserListCell() {
            container.setAlignment(Pos.CENTER_LEFT);
            container.setPadding(new Insets(10));
            container.getStyleClass().add("card-container");

            // Avatar setup
            avatar.setFitWidth(50);
            avatar.setFitHeight(50);
            Circle clip = new Circle(25, 25, 25);
            avatar.setClip(clip);

            // Status dot setup
            statusDot.setStroke(javafx.scene.paint.Color.WHITE);
            statusDot.setStrokeWidth(2);
            StackPane.setAlignment(statusDot, Pos.BOTTOM_RIGHT);
            avatarStack.getChildren().addAll(avatar, statusDot);

            nameLabel.getStyleClass().add("card-name");
            emailLabel.getStyleClass().add("card-email");
            roleBadge.getStyleClass().add("card-role-badge");

            infoBox.getChildren().addAll(nameLabel, emailLabel, roleBadge);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            HBox.setHgrow(spacer, Priority.ALWAYS);

            editBtn.getStyleClass().add("card-action-btn");
            archiveBtn.getStyleClass().addAll("card-action-btn", "card-btn-archive");
            detailsBtn.getStyleClass().add("card-action-btn");

            actions.getChildren().addAll(editBtn, archiveBtn, detailsBtn);
            actions.setAlignment(Pos.CENTER_RIGHT);

            container.getChildren().addAll(avatarStack, infoBox, spacer, actions);
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            System.out.println("Debug: updateItem called. Empty=" + empty + ", User=" + (user != null ? user.getEmail() : "null"));
            if (empty || user == null) {
                setGraphic(null);
            } else {
                String fullName = (user.getNom() != null ? user.getNom() : "") + " " + (user.getPrenom() != null ? user.getPrenom() : "");
                nameLabel.setText(fullName.trim().isEmpty() ? "Sans nom" : fullName.trim());
                emailLabel.setText(user.getEmail() != null ? user.getEmail() : "Pas d'email");
                roleBadge.setText(user.getRole() != null ? user.getRole().name() : "Sans rôle");

                // Safe Image Loading
                try {
                    if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                        File file = new File(user.getPhoto());
                        if (file.exists()) {
                            avatar.setImage(new Image(file.toURI().toString()));
                        } else {
                            setDefaultAvatar();
                        }
                    } else {
                        setDefaultAvatar();
                    }
                } catch (Exception e) {
                    System.err.println("Error loading avatar for " + user.getEmail() + ": " + e.getMessage());
                    setDefaultAvatar();
                }

                // Update status dot color
                if (user.isConnected()) {
                    statusDot.setFill(javafx.scene.paint.Color.web("#38a169")); // Green
                } else {
                    statusDot.setFill(javafx.scene.paint.Color.web("#a0aec0")); // Grey
                }

                // Action handlers
                editBtn.setOnAction(e -> {
                    userListView.getSelectionModel().select(user);
                    populateFields(user);
                });

                archiveBtn.setOnAction(e -> {
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Voulez-vous archiver cet utilisateur ?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            user.setActive(false);
                            userService.update(user);
                            loadUsers();
                        }
                    });
                });

                detailsBtn.setOnAction(e -> {
                    showAlert(Alert.AlertType.INFORMATION, "Détails Utilisateur", 
                        "Utilisateur: " + user.getNom() + " " + user.getPrenom() +
                        "\nEmail: " + user.getEmail() +
                        "\nRole: " + user.getRole() +
                        "\nDate d'inscription: " + user.getDateInscrit());
                });

                setGraphic(container);
            }
        }

        private void setDefaultAvatar() {
            try {
                // Primary path
                URL res = getClass().getResource("/images/default-user.png");
                if (res != null) {
                    avatar.setImage(new Image(res.toExternalForm()));
                } else {
                    // Fail gracefully - no crash
                    avatar.setImage(null);
                }
            } catch (Exception e) {
                avatar.setImage(null);
            }
        }
    }

    private boolean validateInput() {
        String nom = nomField.getText().trim();
        String prenom = prenomField.getText().trim();
        String email = emailField.getText().trim();
        String passwd = passwdField.getText();
        User.Role role = roleComboBox.getValue();
        LocalDate dob = dateNaissancePicker.getValue();

        StringBuilder errors = new StringBuilder();

        if (nom.isEmpty()) errors.append("- Le nom est obligatoire.\n");
        else if (!nom.matches("^[a-zA-Z\\s]+$")) errors.append("- Le nom ne doit contenir que des lettres.\n");

        if (prenom.isEmpty()) errors.append("- Le prénom est obligatoire.\n");
        else if (!prenom.matches("^[a-zA-Z\\s]+$")) errors.append("- Le prénom ne doit contenir que des lettres.\n");

        if (email.isEmpty()) errors.append("- L'email est obligatoire.\n");
        else if (!isValidEmail(email)) errors.append("- L'adresse email n'est pas valide.\n");
        else {
            User selectedUser = userListView.getSelectionModel().getSelectedItem();
            // If adding new user (no selection) or email changed for existing user
            if (selectedUser == null || !selectedUser.getEmail().equals(email)) {
                if (userService.isEmailExists(email)) {
                    errors.append("- Cet email est déjà utilisé par un autre compte.\n");
                }
            }
        }

        if (passwd.isEmpty()) errors.append("- Le mot de passe est obligatoire.\n");
        else if (passwd.length() < 6) errors.append("- Le mot de passe doit contenir au moins 6 caractères.\n");

        if (role == null) errors.append("- Le rôle est obligatoire.\n");

        if (dob == null) errors.append("- La date de naissance est obligatoire.\n");
        else if (dob.isAfter(LocalDate.now().minusYears(13))) errors.append("- L'utilisateur doit avoir au moins 13 ans.\n");

        if (errors.length() > 0) {
            showAlert(Alert.AlertType.ERROR, "Erreur de validation", errors.toString());
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }
}
