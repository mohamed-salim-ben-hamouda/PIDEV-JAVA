package com.pidev.Controllers.client;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.layout.StackPane;
import java.io.IOException;
import javafx.scene.control.MenuButton;
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


    private void loadViewFront(String fxmlName) {
        try {
            java.net.URL resource = getClass().getResource("/Fxml/" + fxmlName + ".fxml");
            if (resource == null) {
                System.err.println("Missing FXML view: /Fxml/" + fxmlName + ".fxml");
                return;
            }

            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("Error: Could not load " + fxmlName + ". Check the path.");
            e.printStackTrace();
        }
    }

    private void switchRoot(String fxmlName) {
        try {
            java.net.URL resource = getClass().getResource("/Fxml/" + fxmlName + ".fxml");
            if (resource == null) {
                System.err.println("Missing FXML view: /Fxml/" + fxmlName + ".fxml");
                return;
            }

            Parent root = FXMLLoader.load(resource);
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error: Could not switch to " + fxmlName + ". Check the path.");
            e.printStackTrace();
        }
    }

    @FXML public void loadDashboard() { switchRoot("admin/base_back"); }

    @FXML public void loadHome() { loadViewFront("client/home"); }
    @FXML public void loadLogin() { loadViewFront("client/User/login"); }

    @FXML public void loadCourses() { loadViewFront("client/CoursesView"); }
    @FXML public void loadChallenge() { loadViewFront("client/Challenge"); }
    @FXML public void loadGroups() { loadViewFront("client/GroupsView"); }
    @FXML public void loadJobs() { loadViewFront("client/JobsView"); }
    @FXML public void loadMyCV() { loadViewFront("client/MyCVView"); }
    @FXML public void loadHackathon() { loadViewFront("client/HackathonView"); }
}
