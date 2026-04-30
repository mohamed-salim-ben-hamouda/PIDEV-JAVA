package com.pidev.Controllers.admin;

import com.pidev.Services.UserService;
import com.pidev.models.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfWriter;
import javafx.stage.FileChooser;
import java.io.FileOutputStream;

import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
    @FXML private TextField searchField;
    @FXML private VBox formPanel;

    @FXML private TableView<User> userTableView;

    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;
    private User editingUser = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        roleComboBox.setItems(FXCollections.observableArrayList(User.Role.values()));
        setupTableColumns();
        loadUsers();
        setupSearch();

        // Selection listener to populate form
        userTableView.getSelectionModel().selectedItemProperty().addListener((obs, oldSel, newSel) -> {
            if (newSel != null) {
                populateFields(newSel);
            }
        });
    }

    @SuppressWarnings("unchecked")
    private void setupTableColumns() {


        // Nom
        TableColumn<User, String> nomCol = new TableColumn<>("Nom");
        nomCol.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nomCol.setPrefWidth(100);

        // Prenom
        TableColumn<User, String> prenomCol = new TableColumn<>("Prénom");
        prenomCol.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        prenomCol.setPrefWidth(100);

        // Date Naissance
        TableColumn<User, String> dobCol = new TableColumn<>("Date Naissance");
        dobCol.setCellValueFactory(data -> {
            LocalDate d = data.getValue().getDateNaissance();
            return new SimpleStringProperty(d != null ? d.toString() : "—");
        });
        dobCol.setPrefWidth(125);

        // Email
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(180);

        // Ban status (isActive & ban_until)
        TableColumn<User, String> statutCol = new TableColumn<>("Statut");
        statutCol.setCellValueFactory(data -> {
            User u = data.getValue();
            if (!u.isActive()) return new SimpleStringProperty("Archivé");
            if (u.isBanned()) return new SimpleStringProperty("Banni");
            return new SimpleStringProperty("Actif");
        });
        statutCol.setPrefWidth(70);
        statutCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }
                Label badge = new Label(item);
                String style = switch(item) {
                    case "Actif" -> "-fx-background-color: #22c55e;";
                    case "Banni" -> "-fx-background-color: #f59e0b;";
                    case "Archivé" -> "-fx-background-color: #ef4444;";
                    default -> "-fx-background-color: #64748b;";
                };
                badge.setStyle(style + " -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Photo
        TableColumn<User, String> photoCol = new TableColumn<>("Photo");
        photoCol.setCellValueFactory(new PropertyValueFactory<>("photo"));
        photoCol.setPrefWidth(50);
        photoCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) { setGraphic(null); return; }
                Circle circle = new Circle(14);
                circle.setFill(Color.web("#e2e8f0"));
                if (item != null && !item.isEmpty()) {
                    try {
                        File f = new File(item);
                        if (f.exists()) {
                            ImageView iv = new ImageView(new Image(f.toURI().toString(), 28, 28, true, true));
                            iv.setClip(new Circle(14, 14, 14));
                            setGraphic(iv);
                            return;
                        }
                    } catch (Exception ignored) {}
                }
                setGraphic(circle);
                setAlignment(Pos.CENTER);
            }
        });

        // Password (masked)
        TableColumn<User, String> passCol = new TableColumn<>("Mot de passe");
        passCol.setCellValueFactory(data -> new SimpleStringProperty("••••••••"));
        passCol.setPrefWidth(110);

        // Date Inscrit
        TableColumn<User, String> inscritCol = new TableColumn<>("Date Inscrit");
        inscritCol.setCellValueFactory(data -> {
            var dt = data.getValue().getDateInscrit();
            return new SimpleStringProperty(dt != null ? dt.toLocalDate().toString() : "—");
        });
        inscritCol.setPrefWidth(110);

        // Type/Role
        TableColumn<User, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getRole() != null ? data.getValue().getRole().name() : "—"));
        typeCol.setPrefWidth(90);
        typeCol.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null || item.equals("—")) { setGraphic(null); setText(item); return; }
                Label badge = new Label(item);
                String color = switch(item) {
                    case "Admin" -> "#f59e0b";
                    case "Etudiant" -> "#3b82f6";
                    case "Encadrant" -> "#8b5cf6";
                    case "Entreprise" -> "#10b981";
                    default -> "#64748b";
                };
                badge.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; -fx-padding: 2 8; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                setGraphic(badge);
                setText(null);
                setAlignment(Pos.CENTER);
            }
        });

        // Edit button column
        TableColumn<User, Void> editCol = new TableColumn<>("Edit");
        editCol.setPrefWidth(50);
        editCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            { 
                FontIcon icon = new FontIcon("fas-pen");
                icon.setIconSize(12);
                icon.setIconColor(Color.web("#3b82f6"));
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    editingUser = user;
                    populateFields(user);
                    formPanel.setVisible(true);
                    formPanel.setManaged(true);
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });

        // Archive button column
        TableColumn<User, Void> archiveCol = new TableColumn<>("Archiver");
        archiveCol.setPrefWidth(80);
        archiveCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                FontIcon icon = new FontIcon("fas-archive");
                icon.setIconSize(12);
                icon.setIconColor(Color.web("#ef4444"));
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Archiver cet utilisateur ?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.YES) {
                            user.setActive(false);
                            userService.update(user);
                            loadUsers();
                        }
                    });
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : btn);
                setAlignment(Pos.CENTER);
            }
        });

        // Ban button column
        TableColumn<User, Void> banActionCol = new TableColumn<>("Bannir");
        banActionCol.setPrefWidth(60);
        banActionCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button();
            {
                FontIcon icon = new FontIcon("fas-gavel");
                icon.setIconSize(12);
                icon.setIconColor(Color.web("#f59e0b"));
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                btn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    if (user.isBanned()) {
                        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "L'utilisateur " + user.getDisplayName() + " est banni. Voulez-vous le débannir ?", ButtonType.YES, ButtonType.NO);
                        confirm.showAndWait().ifPresent(r -> {
                            if (r == ButtonType.YES) {
                                user.setBanUntil(null);
                                userService.update(user);
                                loadUsers();
                            }
                        });
                    } else {
                        TextInputDialog dialog = new TextInputDialog("1");
                        dialog.setTitle("Bannir l'utilisateur");
                        dialog.setHeaderText("Bannir " + user.getDisplayName());
                        dialog.setContentText("Durée du bannissement (en heures) :");
                        dialog.showAndWait().ifPresent(hoursStr -> {
                            try {
                                int hours = Integer.parseInt(hoursStr);
                                user.setBanUntil(java.time.LocalDateTime.now().plusHours(hours));
                                userService.update(user);
                                loadUsers();
                            } catch (NumberFormatException ex) {
                                showAlert(Alert.AlertType.ERROR, "Erreur", "Veuillez entrer un nombre valide d'heures.");
                            }
                        });
                    }
                });
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    FontIcon icon = (FontIcon) btn.getGraphic();
                    if (user != null && user.isBanned()) {
                        icon.setIconLiteral("fas-unlock");
                        icon.setIconColor(Color.web("#22c55e"));
                    } else {
                        icon.setIconLiteral("fas-gavel");
                        icon.setIconColor(Color.web("#f59e0b"));
                    }
                    setGraphic(btn);
                }
                setAlignment(Pos.CENTER);
            }
        });

        userTableView.getColumns().addAll(nomCol, prenomCol, dobCol, emailCol, statutCol, photoCol, passCol, inscritCol, typeCol, editCol, banActionCol, archiveCol);
    }

    private void setupSearch() {
        filteredList = new FilteredList<>(userList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredList.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lower = newVal.toLowerCase();
                return (user.getNom() != null && user.getNom().toLowerCase().contains(lower))
                    || (user.getPrenom() != null && user.getPrenom().toLowerCase().contains(lower))
                    || (user.getEmail() != null && user.getEmail().toLowerCase().contains(lower));
            });
        });
        userTableView.setItems(filteredList);
    }

    private void loadUsers() {
        try {
            List<User> activeUsers = userService.getActiveUsers();
            userList.setAll(activeUsers);
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
    private void handleShowAddForm(ActionEvent event) {
        editingUser = null;
        handleClearFields(null);
        formPanel.setVisible(true);
        formPanel.setManaged(true);
    }

    @FXML
    private void handleAddUser(ActionEvent event) {
        if (validateInput()) {
            if (editingUser != null) {
                // Update mode
                editingUser.setNom(nomField.getText());
                editingUser.setPrenom(prenomField.getText());
                editingUser.setEmail(emailField.getText());
                editingUser.setPasswd(passwdField.getText());
                editingUser.setRole(roleComboBox.getValue());
                editingUser.setDateNaissance(dateNaissancePicker.getValue());
                if (userService.update(editingUser)) {
                    loadUsers();
                    handleClearFields(null);
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur modifié !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de la modification.");
                }
            } else {
                // Add mode
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
                    showAlert(Alert.AlertType.INFORMATION, "Succès", "Utilisateur ajouté !");
                } else {
                    showAlert(Alert.AlertType.ERROR, "Erreur", "Erreur lors de l'ajout.");
                }
            }
        }
    }

    @FXML
    private void handleClearFields(ActionEvent event) {
        nomField.clear();
        prenomField.clear();
        emailField.clear();
        passwdField.clear();
        dateNaissancePicker.setValue(null);
        userTableView.getSelectionModel().clearSelection();
        editingUser = null;
        formPanel.setVisible(false);
        formPanel.setManaged(false);
    }

    @FXML
    private void handleExportPDF(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le fichier PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
        File file = fileChooser.showSaveDialog(userTableView.getScene().getWindow());

        if (file != null) {
            try {
                Document document = new Document();
                PdfWriter.getInstance(document, new FileOutputStream(file));
                document.open();

                // Define colors
                BaseColor primaryColor = new BaseColor(79, 128, 216); // Nice blue matching UI
                BaseColor darkColor = new BaseColor(30, 41, 59); // Slate 800
                BaseColor lightGray = new BaseColor(248, 250, 252); // Slate 50
                BaseColor borderColor = new BaseColor(226, 232, 240); // Slate 200

                // Title
                Font titleFont = new Font(Font.FontFamily.HELVETICA, 22, Font.BOLD, darkColor);
                Paragraph title = new Paragraph("Liste des Utilisateurs du Système", titleFont);
                title.setAlignment(Element.ALIGN_CENTER);
                title.setSpacingAfter(30);
                document.add(title);

                // Table Definition
                PdfPTable pdfTable = new PdfPTable(6); 
                pdfTable.setWidthPercentage(100);
                // Adjust column proportions: Email wider, Role/Status smaller
                pdfTable.setWidths(new float[]{1.5f, 1.5f, 1.5f, 3.2f, 1.2f, 1.2f});
                
                // Table Headers
                Font headerFont = new Font(Font.FontFamily.HELVETICA, 11, Font.BOLD, BaseColor.WHITE);
                String[] headers = {"Date Inscrit", "Nom", "Prénom", "Email", "Rôle", "Statut"};
                for (String header : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(header, headerFont));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setBackgroundColor(primaryColor);
                    cell.setPadding(10);
                    cell.setBorderColor(primaryColor);
                    pdfTable.addCell(cell);
                }

                // Table Data Rows
                Font dataFont = new Font(Font.FontFamily.HELVETICA, 10, Font.NORMAL, darkColor);
                boolean alternate = false;
                
                for (User user : filteredList) {
                    BaseColor rowColor = alternate ? lightGray : BaseColor.WHITE;
                    
                    String[] rowData = {
                        user.getDateInscrit() != null ? user.getDateInscrit().toLocalDate().toString() : "--",
                        user.getNom() != null ? user.getNom() : "",
                        user.getPrenom() != null ? user.getPrenom() : "",
                        user.getEmail() != null ? user.getEmail() : "",
                        user.getRole() != null ? user.getRole().name() : "",
                        user.isActive() ? "Actif" : "Banni"
                    };

                    for (String data : rowData) {
                        PdfPCell cell = new PdfPCell(new Phrase(data, dataFont));
                        cell.setPadding(8);
                        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                        cell.setBorderColor(borderColor);
                        cell.setBackgroundColor(rowColor);
                        pdfTable.addCell(cell);
                    }
                    alternate = !alternate;
                }

                document.add(pdfTable);
                document.close();

                showAlert(Alert.AlertType.INFORMATION, "Succès", "L'exportation PDF a été réalisée avec succès !");

            } catch (Exception e) {
                e.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Erreur", "La création du fichier PDF a échoué: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleOpenArchive(ActionEvent event) {
        try {
            Stage stage = (Stage) userTableView.getScene().getWindow();
            var baseController = (BaseController) stage.getScene().getRoot().getUserData();
            if (baseController != null) {
                baseController.loadArchivedUsers();
            } else {
                // Flashback directly if not embedded
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/archived_users.fxml"));
                Parent root = loader.load();
                stage.setScene(new Scene(root));
            }
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible d'ouvrir les archives: " + e.getMessage());
        }
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setContentText(content);
        alert.showAndWait();
    }

    //controle de saisir
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
            User selectedUser = editingUser;
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
    //fin validation
    private boolean isValidEmail(String email) {
        String emailRegex = "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$";
        Pattern pat = Pattern.compile(emailRegex);
        return pat.matcher(email).matches();
    }
}
