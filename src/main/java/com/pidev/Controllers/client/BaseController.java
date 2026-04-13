package com.pidev.Controllers.client;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import javafx.scene.control.MenuButton;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BaseController implements Initializable {

    @FXML
    private StackPane contentArea;

    @FXML
    private MenuButton challengesMenu;

    @FXML
    private MenuButton CvMenu;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureHoverMenu(challengesMenu);
        configureHoverMenu(CvMenu);
    }

    private void configureHoverMenu(MenuButton menuButton) {
        if (menuButton == null) {
            return;
        }

        menuButton.setOnMouseEntered(event -> menuButton.show());
        menuButton.setOnMouseExited(event -> {
            if (!menuButton.isShowing()) {
                menuButton.hide();
            }
        });
    }

    // 🔹 Charger une vue dans le contentArea
    private void loadViewFront(String fxmlName) {
        Platform.runLater(() -> {
            try {
                System.out.println("Attempting to load view: " + fxmlName);

                URL url = getClass().getResource("/Fxml/" + fxmlName + ".fxml");

                if (url == null) {
                    System.err.println("❌ FXML not found: /Fxml/" + fxmlName + ".fxml");
                    return;
                }

                FXMLLoader loader = new FXMLLoader(url);
                Parent view = loader.load();

                if (contentArea != null) {
                    contentArea.getChildren().setAll(view);
                    System.out.println("✅ View loaded: " + fxmlName);
                } else {
                    System.err.println("❌ contentArea is null!");
                }

            } catch (Exception e) {
                System.err.println("❌ Error loading " + fxmlName);
                e.printStackTrace();
                Platform.runLater(() -> {
                    javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR);
                    alert.setTitle("Erreur de chargement");
                    alert.setHeaderText("Impossible de charger la vue: " + fxmlName);
                    alert.setContentText(e.toString());
                    alert.showAndWait();
                });
            }
        });
    }

    // 🔹 Changer toute la scène (root)
    private void switchRoot(String fxmlName) {
        try {
            Parent root = FXMLLoader.load(
                    getClass().getResource("/Fxml/" + fxmlName + ".fxml")
            );
            contentArea.getScene().setRoot(root);

        } catch (IOException e) {
            System.err.println("❌ Could not switch to " + fxmlName);
            e.printStackTrace();
        }
    }

    // 🔥 EVENTS (IMPORTANT)

    @FXML
    public void loadDashboard() {
        switchRoot("admin/base_back");
    }

    @FXML
    public void loadHome() {
        loadViewFront("client/home");
    }

    @FXML
    public void loadLogin() {
        loadViewFront("client/User/login");
    }

    @FXML
    public void loadCourses() {
        loadViewFront("client/CoursesView");
    }

    // 🔥 CORRECTION ICI (manquait)
    @FXML
    public void loadGroups() {
        loadViewFront("client/GroupsView"); // assure-toi que le fichier existe
    }

    @FXML
    public void loadMyCV() {
        loadViewFront("client/MyCVView");
    }

    @FXML
    public void loadJobs() {
        loadViewFront("client/OfferList");
    }

    @FXML
    public void loadMyOffers() {
        loadViewFront("client/MyOffers");
    }

    @FXML
    public void loadChallenge() {
        loadViewFront("client/Challenge");
    }

    @FXML
    public void loadHackathon() {
        loadViewFront("client/HackathonView");
    }
}