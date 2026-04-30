package com.pidev.Controllers.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.scene.Node;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import com.pidev.utils.SessionManager;
import com.pidev.Services.UserService;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

public class HomeController {

    @FXML
    private MenuButton challengesMenu; // Linked to the button in image_a509af.png


    @FXML
    private void handleMenuExited(MouseEvent event) {
        if (!challengesMenu.isShowing()) {
            challengesMenu.hide();
        }
    }

    @FXML
    private void openSocialLink(MouseEvent event) {
        Node clickedNode = (Node) event.getSource();
        String url = (String) clickedNode.getUserData();

        if (url != null && !url.isEmpty()) {
            try {
                if (Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().browse(new URI(url));
                }
            } catch (Exception e) {
                System.err.println("Error opening social link: " + e.getMessage());
            }
        }
    }


    @FXML
    private void openProfile(MouseEvent event) {
        if (!SessionManager.getInstance().isLogged()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Accès refusé");
            alert.setHeaderText(null);
            alert.setContentText("Vous devez être connecté pour accéder à votre profil.");
            alert.show();
            return;
        }
        
        try {
            Parent view = FXMLLoader.load(getClass().getResource("/Fxml/client/User/profile.fxml"));
            Node source = (Node) event.getSource();
            Scene scene = source.getScene();
            StackPane contentArea = (StackPane) scene.lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(view);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void navigateToModule(MouseEvent event) {
        String fxmlFile = "";
        Node source = (Node) event.getSource();

        // Determine which button was clicked based on its ID or Text
        if (source.getId().contains("challenges")) {
            fxmlFile = "/com/skillbridge/views/challenges.fxml"; //
        } else if (source.getId().contains("courses")) {
            fxmlFile = "/com/skillbridge/views/courses.fxml";
        }

        if (!fxmlFile.isEmpty()) {
            switchScene(event, fxmlFile);
        }
    }

    private void switchScene(MouseEvent event, String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout(ActionEvent event) {
        var user = SessionManager.getInstance().getUser();
        if (user != null) {
            new UserService().setConnectedStatus(user.getId(), false);
        }
        SessionManager.getInstance().logout();
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Fxml/client/User/login.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}