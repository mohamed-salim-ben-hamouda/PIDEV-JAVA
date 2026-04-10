package com.pidev.Controllers.client;

import com.pidev.Controllers.client.Challenge.Activity.ActivityController;
import com.pidev.Controllers.client.Challenge.Evaluation.EvaluationMainController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
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
    @FXML
    private MenuButton ChallengesStudent;
    private static BaseController instance;

    public BaseController() {
        instance = this;
    }

    public static BaseController getInstance() {
        return instance;
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        configureHoverMenu(challengesMenu);
        configureHoverMenu(CvMenu);
        configureHoverMenu(ChallengesStudent);
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


    private Object loadViewFront(String fxmlName) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/" + fxmlName + ".fxml"));            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
            return loader.getController();
        } catch (IOException e) {
            System.err.println("Error: Could not load " + fxmlName + ". Check the path.");
            e.printStackTrace();
            return null;
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

    @FXML
    public void loadChallenge() {
        loadViewFront("client/Challenge/Challenge");
    }
    @FXML
    public void loadEvaluation(){loadViewFront("client/Challenge/Evaluation/SelectToEvaluate");}

    @FXML
    public void loadActivity() {
        loadViewFront("client/Challenge/challengeStudent");
    }

    @FXML
    public void loadActivityPage(Challenge challenge, int groupId,int activity_id) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/Activity.fxml"));
            Parent root = loader.load();
            ActivityController controller = loader.getController();

            controller.initData(challenge, groupId,activity_id);
            contentArea.getChildren().setAll(root);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleActivityPages() {
        ServiceActivity serviceActivity = new ServiceActivity();
        int user_id = 2;
        Activity a = serviceActivity.findActivityInprogress(user_id);
        if (a != null) {
            loadActivityPage(a.getChallenge(), a.getGroup().getId(),a.getId());
        } else {
            loadActivity();
        }

    }

    @FXML
    public void loadOldActivities() {
        loadViewFront("client/Challenge/OldActivities");
    }
    @FXML
    public EvaluationMainController loadEvaluationMainPage() {

        return (EvaluationMainController) loadViewFront("client/Challenge/Evaluation/Evaluation");    }
    @FXML
    public void loadGroups() {
        loadViewFront("client/GroupsView");
    }

    @FXML
    public void loadJobs() {
        loadViewFront("client/JobsView");
    }

    @FXML
    public void loadMyCV() {
        loadViewFront("client/MyCVView");
    }

    @FXML
    public void loadHackathon() {
        loadViewFront("client/HackathonView");
    }
}
