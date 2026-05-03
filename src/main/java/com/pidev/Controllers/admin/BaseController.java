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
        loadView("stats_dashboard");
    }

    @FXML
    public void loadHackathons() {
        loadView("hackathon_list");
    }

    @FXML
    public void loadSponsors() {
        loadView("sponsor_list");
    }

    @FXML
    public void loadSponsorHackathons() {
        loadView("sponsor_hackathon_list");
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
    public void loadOffers() {
        loadView("BackofficeOfferManagement");
    }

    @FXML
    public void loadCVs() {
        loadView("BackofficeCVManagement");
    }

    @FXML
    public void loadCourses() {
        loadView("course_management");
    }

    @FXML
    public void loadChapters() {
        loadView("chapter_management");
    }

    @FXML
    public void loadQuizzes() {
        loadView("quiz_management");
    }

    @FXML
    public void loadQuizResults() {
        loadView("quiz_results");
    }

    @FXML
    public void loadLearningIntelligence() {
        loadView("learning_intelligence");
    }

    @FXML
    public void loadQuestions() {
        loadView("question_management");
    }

    @FXML
    public void loadAnswers() {
        loadView("answer_management");
    }

    @FXML
    public void loadGenerateQuizAI() {
        loadView("generate_quiz_ai");
    }

    @FXML
    public void loadMultiplayerQuiz() {
        loadView("multiplayer_quiz");
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
            URL resource = resolveViewResource(viewName);
            if (resource == null) {
                throw new IOException("FXML introuvable pour la vue: " + viewName);
            }

            Parent view = FXMLLoader.load(resource);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Could not load admin view: " + viewName);
            e.printStackTrace();
        }
    }

    private URL resolveViewResource(String viewName) {
        String normalized = viewName.endsWith(".fxml") ? viewName : viewName + ".fxml";
        String path;

        if (normalized.startsWith("/")) {
            path = normalized;
        } else if (normalized.startsWith("client/") || normalized.startsWith("admin/")) {
            path = "/Fxml/" + normalized;
        } else {
            path = "/Fxml/admin/" + normalized;
        }

        return getClass().getResource(path);
    }

    @FXML
    public void loadBackChallenge(){
        loadView("Challenge/ChallengeBackoffice");
    }
    @FXML
    public void loadBackActivity(){
        loadView("Challenge/Activity/ActivityBackoffice");
    }
    @FXML
    public void loadBackEvaluation(){
        loadView("Challenge/Evaluation/EvaluationBackoffice");
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
