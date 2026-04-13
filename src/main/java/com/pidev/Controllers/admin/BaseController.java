package com.pidev.Controllers.admin;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class BaseController implements Initializable {

    @FXML
    private StackPane contentArea;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDashboard();
    }

    @FXML
    public void loadDashboard() {
        loadView("dashboard");
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
    public void loadQuestions() {
        loadView("question_management");
    }

    @FXML
    public void loadAnswers() {
        loadView("answer_management");
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
            Parent view = FXMLLoader.load(getClass().getResource("/Fxml/admin/" + viewName + ".fxml"));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            System.err.println("Could not load admin view: " + viewName);
            e.printStackTrace();
        }
    }
}
