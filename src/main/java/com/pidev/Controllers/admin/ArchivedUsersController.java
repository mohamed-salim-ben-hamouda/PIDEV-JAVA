package com.pidev.Controllers.admin;

import com.pidev.Services.UserService;
import com.pidev.models.User;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ArchivedUsersController implements Initializable {

    @FXML private TableView<User> archivedUserTableView;
    @FXML private TextField searchField;

    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private FilteredList<User> filteredList;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setupTableColumns();
        setupSearch();
        loadUsers();
    }

    private void loadUsers() {
        try {
            List<User> users = userService.getArchivedUsers();
            userList.setAll(users);
        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Erreur", "Impossible de charger les utilisateurs archivés: " + e.getMessage());
        }
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

        // Email
        TableColumn<User, String> emailCol = new TableColumn<>("Email");
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailCol.setPrefWidth(220);

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

        // Restore button column
        TableColumn<User, Void> restoreCol = new TableColumn<>("Renvoyer");
        restoreCol.setPrefWidth(100);
        restoreCol.setCellFactory(col -> new TableCell<>() {
            private final Button btn = new Button("Renvoyer ");
            {
                FontIcon icon = new FontIcon("fas-redo");
                icon.setIconSize(12);
                icon.setIconColor(Color.WHITE);
                btn.setGraphic(icon);
                btn.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 11px;");
                btn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION, "Restaurer cet utilisateur vers la liste active ?", ButtonType.YES, ButtonType.NO);
                    confirm.showAndWait().ifPresent(r -> {
                        if (r == ButtonType.YES) {
                            user.setActive(true);
                            userService.update(user);
                            loadUsers(); // Refresh list after restoring
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

        archivedUserTableView.getColumns().addAll(photoCol, nomCol, prenomCol, emailCol, typeCol, inscritCol, restoreCol);
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
        archivedUserTableView.setItems(filteredList);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
