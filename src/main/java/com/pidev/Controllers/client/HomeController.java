package com.pidev.Controllers.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.MenuButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;

public class HomeController {

    @FXML
    private VBox homeRoot;

    @FXML
    private MenuButton challengesMenu;


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
    private void openGroupsFeed(MouseEvent event) {
        loadIntoBaseContent("/Fxml/client/GroupsView.fxml");
    }

    private void loadIntoBaseContent(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            StackPane contentArea = (StackPane) homeRoot.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setHeaderText("Unable to open feed");
            alert.setContentText("Could not load the groups feed page.");
            alert.showAndWait();
        }
    }
}
