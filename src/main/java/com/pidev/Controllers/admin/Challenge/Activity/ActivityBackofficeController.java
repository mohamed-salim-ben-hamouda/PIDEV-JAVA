package com.pidev.Controllers.admin.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.Activity;
import com.pidev.models.MemberActivity;
import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class ActivityBackofficeController implements Initializable {
    @FXML
    private VBox ActivityCardsContainer;

    @FXML
    private VBox MemberActivityCardsContainer;

    @FXML
    private VBox ProblemSolutionCardsContainer;

    @FXML
    private VBox activityTable;

    @FXML
    private VBox memberActivityTable;

    @FXML
    private VBox problemSolutionTable;

    @FXML
    private Button activityBtn;

    @FXML
    private Button memberActivityBtn;

    @FXML
    private Button problemSolutionBtn;

    private final ServiceActivity serviceA = new ServiceActivity();
    private final ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    private final ServiceProblemSolution servicePS = new ServiceProblemSolution();

    private enum ViewMode {
        ACTIVITY,
        MEMBER_ACTIVITY,
        PROBLEM_SOLUTION
    }

    private ViewMode currentView = ViewMode.ACTIVITY;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        setView(ViewMode.ACTIVITY);
    }

    @FXML
    private void onShowActivities() {
        setView(ViewMode.ACTIVITY);
    }

    @FXML
    private void onShowMemberActivities() {
        setView(ViewMode.MEMBER_ACTIVITY);
    }

    @FXML
    private void onShowProblemSolutions() {
        setView(ViewMode.PROBLEM_SOLUTION);
    }

    private void setView(ViewMode mode) {
        this.currentView = mode;
        setSectionVisible(activityTable, mode == ViewMode.ACTIVITY);
        setSectionVisible(memberActivityTable, mode == ViewMode.MEMBER_ACTIVITY);
        setSectionVisible(problemSolutionTable, mode == ViewMode.PROBLEM_SOLUTION);

        setButtonPrimary(activityBtn, mode == ViewMode.ACTIVITY);
        setButtonPrimary(memberActivityBtn, mode == ViewMode.MEMBER_ACTIVITY);
        setButtonPrimary(problemSolutionBtn, mode == ViewMode.PROBLEM_SOLUTION);

        refreshCurrentView();
    }

    private void refreshCurrentView() {
        switch (currentView) {
            case MEMBER_ACTIVITY:
                displayMemberActivities();
                break;
            case PROBLEM_SOLUTION:
                displayProblemSolutions();
                break;
            case ACTIVITY:
            default:
                displayActivities();
                break;
        }
    }

    private void setSectionVisible(VBox section, boolean visible) {
        if (section == null) {
            return;
        }
        section.setVisible(visible);
        section.setManaged(visible);
    }

    private void setButtonPrimary(Button button, boolean primary) {
        if (button == null) {
            return;
        }
        button.getStyleClass().remove("action-btn-primary");
        if (primary) {
            button.getStyleClass().add("action-btn-primary");
        }
    }

    private void displayActivities() {
        ActivityCardsContainer.getChildren().clear();
        List<Activity> activities = serviceA.displayAll();
        for (Activity a : activities) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Activity/ActivityBackCards.fxml"));
                HBox card = loader.load();
                ActivityBackCardsController cardController = loader.getController();
                cardController.initData(a, v -> refreshCurrentView());
                ActivityCardsContainer.getChildren().add(card);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void displayMemberActivities() {
        MemberActivityCardsContainer.getChildren().clear();
        List<MemberActivity> list = serviceMA.displayAll();
        for (MemberActivity ma : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Activity/MemberActivityBackCards.fxml"));
                HBox card = loader.load();
                MemberActivityBackCardsController cardController = loader.getController();
                cardController.initData(ma, v -> refreshCurrentView());
                MemberActivityCardsContainer.getChildren().add(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void displayProblemSolutions() {
        ProblemSolutionCardsContainer.getChildren().clear();
        List<ProblemSolution> list = servicePS.displayAll();
        for (ProblemSolution ps : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/admin/Challenge/Activity/ProblemSolutionBackCards.fxml"));
                HBox card = loader.load();
                ProblemSolutionBackCardsController cardController = loader.getController();
                cardController.initData(ps, v -> refreshCurrentView());
                ProblemSolutionCardsContainer.getChildren().add(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
