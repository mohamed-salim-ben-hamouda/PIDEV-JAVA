package com.pidev.Controllers.admin;

import com.pidev.Services.UserService;
import com.pidev.utils.SessionManager;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BaseController implements Initializable {

    @FXML
    private StackPane contentArea;
    @FXML
    private Label adminNameLabel;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        if (!SessionManager.getInstance().isAdmin()) {
            System.out.println("Access Denied: Not an admin.");
            Platform.runLater(this::redirectToLogin);
        } else {
            if (adminNameLabel != null && SessionManager.getInstance().getUser() != null) {
                adminNameLabel.setText(SessionManager.getInstance().getUser().getNom());
            }
            loadDashboard();
        }
    }

    private void redirectToLogin() {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Fxml/client/User/login.fxml"));
            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void loadDashboard() {
        loadView("dashboard");
    }

    @FXML
    public void loadUserManagement() {
        loadView("user_management");
    }

    @FXML
    public void loadArchivedUsers() {
        loadView("archived_users");
    }

    @FXML
    public void loadProfile() {
        loadView("client/User/profile");
    }

    @FXML
    public void loadHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/base.fxml"));
            Parent root = loader.load();
            com.pidev.Controllers.client.BaseController controller = loader.getController();
            controller.loadHome();

            Scene scene = contentArea.getScene();
            if (scene != null) {
                scene.setRoot(root);
            }
        } catch (IOException e) {
            System.err.println("Could not load client home.");
            e.printStackTrace();
        }
    }

    private void loadView(String viewName) {
        try {
            String path = viewName.contains("/") ? "/Fxml/" + viewName + ".fxml" : "/Fxml/admin/" + viewName + ".fxml";
            Parent view = FXMLLoader.load(getClass().getResource(path));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Could not load view: " + viewName);
            e.printStackTrace();
        }
    }

    @FXML
    public void handleLogout() {
        if (SessionManager.getInstance().getUser() != null) {
            new UserService().setConnectedStatus(SessionManager.getInstance().getUser().getId(), false);
        }
        SessionManager.getInstance().logout();
        redirectToLogin();
    }
}
