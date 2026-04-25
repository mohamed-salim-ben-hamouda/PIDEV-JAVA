package com.pidev.Controllers.client;

import com.pidev.utils.SessionManager;
import com.pidev.Services.UserService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ButtonBar;
import java.util.Optional;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import com.pidev.models.User;
import java.io.File;

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
    @FXML
    private Label dashboardLink;
    @FXML
    private Label profileLink;
    @FXML
    private Button loginBtn;
    
    private UserService userService = new UserService();
    
    @FXML private HBox userInfoNav;
    @FXML private ImageView navProfileImg;
    @FXML private Label navUserName;
    @FXML private Label navUserRole;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureHoverMenu(challengesMenu);
        configureHoverMenu(CvMenu);
        updateNavbar();
    }

    private void updateNavbar() {
        boolean isAdmin = SessionManager.getInstance().isAdmin();
        boolean isLogged = SessionManager.getInstance().isLogged();
        User user = SessionManager.getInstance().getUser();

        dashboardLink.setVisible(isAdmin);
        dashboardLink.setManaged(isAdmin);

        profileLink.setVisible(isLogged);
        profileLink.setManaged(isLogged);
        
        if (userInfoNav != null) {
            userInfoNav.setVisible(isLogged);
            userInfoNav.setManaged(isLogged);
        }

        if (isLogged) {
            loginBtn.setText("Logout");
            
            if (navUserName != null && user != null) {
                navUserName.setText(user.getNom() + " " + user.getPrenom());
                navUserRole.setText(user.getRole() != null ? user.getRole().name() : "");
                
                if (user.getPhoto() != null && !user.getPhoto().isEmpty()) {
                    try {
                        File file = new File(user.getPhoto());
                        if (file.exists()) {
                            navProfileImg.setImage(new Image(file.toURI().toString()));
                        }
                    } catch (Exception ignored) {}
                }
            }
        } else {
            loginBtn.setText("Sign in");
        }
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
            Parent view = FXMLLoader.load(getClass().getResource("/Fxml/" + fxmlName + ".fxml"));
            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            System.err.println("Error: Could not load " + fxmlName + ". Check the path.");
            e.printStackTrace();
        }
    }

    private void switchRoot(String fxmlName) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Fxml/" + fxmlName + ".fxml"));
            contentArea.getScene().setRoot(root);
        } catch (IOException e) {
            System.err.println("Error: Could not switch to " + fxmlName + ". Check the path.");
            e.printStackTrace();
        }
    }

    @FXML public void loadDashboard() { switchRoot("admin/base_back"); }

    @FXML public void loadHome() { loadViewFront("client/home"); }
    @FXML public void loadProfile() { loadViewFront("client/User/profile"); }
    
    @FXML 
    public void loadLogin() { 
        if (SessionManager.getInstance().isLogged()) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Confirmation de déconnexion");
            alert.setHeaderText("Êtes-vous sûr de vouloir vous déconnecter ?");
            alert.setContentText("Votre session en cours sera fermée.");

            ButtonType buttonYes = new ButtonType("Yes", ButtonBar.ButtonData.OK_DONE);
            ButtonType buttonNo = new ButtonType("No", ButtonBar.ButtonData.CANCEL_CLOSE);
            alert.getButtonTypes().setAll(buttonYes, buttonNo);

            Optional<ButtonType> result = alert.showAndWait();
            if (result.isPresent() && result.get() == buttonYes) {
                // Remove Preferences
                try {
                    java.util.prefs.Preferences prefs = java.util.prefs.Preferences.userNodeForPackage(com.pidev.Controllers.client.User.login_Controller.class);
                    prefs.remove("savedEmail");
                    prefs.remove("savedPassword");
                } catch (Exception ignored) {}

                User user = SessionManager.getInstance().getUser();
                if (user != null) {
                    userService.setConnectedStatus(user.getId(), false);
                }

                SessionManager.getInstance().cleanUserSession();
                updateNavbar();
                loadViewFront("client/User/login"); 
            }
        } else {
            loadViewFront("client/User/login"); 
        }
    }

    @FXML public void loadCourses() { loadViewFront("client/CoursesView"); }
    @FXML public void loadChallenge() { loadViewFront("client/Challenge"); }
    @FXML public void loadGroups() { loadViewFront("client/GroupsView"); }
    @FXML public void loadJobs() { loadViewFront("client/JobsView"); }
    @FXML public void loadMyCV() { loadViewFront("client/MyCVView"); }
    @FXML public void loadHackathon() { loadViewFront("client/HackathonView"); }
}
