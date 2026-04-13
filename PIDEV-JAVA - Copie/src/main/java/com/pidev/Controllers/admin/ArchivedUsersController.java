package com.pidev.Controllers.admin;

import com.pidev.Services.UserService;
import com.pidev.models.User;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ArchivedUsersController implements Initializable {

    @FXML private ListView<User> archivedUserListView;

    private UserService userService = new UserService();
    private ObservableList<User> archivedUsers = FXCollections.observableArrayList();

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        archivedUserListView.setCellFactory(param -> new ArchivedUserListCell());
        loadArchivedUsers();
    }

    private void loadArchivedUsers() {
        try {
            List<User> users = userService.getArchivedUsers();
            System.out.println("Debug: ArchivedUsersController loaded " + users.size() + " users.");
            archivedUsers.setAll(users);
            archivedUserListView.setItems(archivedUsers);
            archivedUserListView.refresh();
        } catch (Exception e) {
            System.err.println("Error loading archived users: " + e.getMessage());
        }
    }

    private class ArchivedUserListCell extends ListCell<User> {
        private final BorderPane container = new BorderPane();
        private final HBox leftBox = new HBox(15);
        private final ImageView avatar = new ImageView();
        private final VBox infoBox = new VBox(2);
        private final Label nameLabel = new Label();
        private final Label emailLabel = new Label();
        private final Label roleBadge = new Label();
        private final Button restoreBtn = new Button("Renvoyer");

        public ArchivedUserListCell() {
            container.setPadding(new Insets(12));
            container.getStyleClass().add("card-container");

            avatar.setFitWidth(50);
            avatar.setFitHeight(50);
            Circle clip = new Circle(25, 25, 25);
            avatar.setClip(clip);

            nameLabel.getStyleClass().add("card-name");
            emailLabel.getStyleClass().add("card-email");
            roleBadge.getStyleClass().add("card-role-badge");

            infoBox.getChildren().addAll(nameLabel, emailLabel, roleBadge, restoreBtn);
            infoBox.setAlignment(Pos.CENTER_LEFT);

            leftBox.setAlignment(Pos.CENTER_LEFT);
            leftBox.getChildren().addAll(avatar, infoBox);

            restoreBtn.getStyleClass().add("card-action-btn");
            // BRIGHT RED FOR DEBUG
            restoreBtn.setStyle("-fx-background-color: red; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 5 10;");
            restoreBtn.setMinWidth(110);
            restoreBtn.setCursor(javafx.scene.Cursor.HAND);

            container.setLeft(leftBox);
            // container.setRight(restoreBtn); // REMOVED FROM RIGHT
            BorderPane.setAlignment(leftBox, Pos.CENTER_LEFT);
            
            container.setMaxWidth(Double.MAX_VALUE);
        }

        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
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
                    setDefaultAvatar();
                }

                restoreBtn.setOnAction(e -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION, "Renvoyer cet utilisateur vers la liste active ?", ButtonType.YES, ButtonType.NO);
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.YES) {
                            user.setActive(true);
                            if (userService.update(user)) {
                                loadArchivedUsers();
                            } else {
                                Alert error = new Alert(Alert.AlertType.ERROR, "Erreur lors de la restauration.");
                                error.showAndWait();
                            }
                        }
                    });
                });

                setGraphic(container);
            }
        }

        private void setDefaultAvatar() {
            try {
                URL res = getClass().getResource("/images/default-user.png");
                if (res != null) {
                    avatar.setImage(new Image(res.toExternalForm()));
                } else {
                    avatar.setImage(null);
                }
            } catch (Exception e) {
                avatar.setImage(null);
            }
        }
    }
}
