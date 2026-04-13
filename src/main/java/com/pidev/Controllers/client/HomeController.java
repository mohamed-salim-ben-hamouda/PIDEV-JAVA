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

    @FXML
    private void goToCourses(MouseEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
            Parent root = loader.load();
            com.pidev.Controllers.client.BaseController controller = loader.getController();
            controller.loadCourses();

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            if (stage.getScene() == null) {
                stage.setScene(new Scene(root));
            } else {
                stage.getScene().setRoot(root);
            }
            stage.show();
        } catch (IOException e) {
            System.err.println("Could not open courses module from home view.");
            e.printStackTrace();
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
}