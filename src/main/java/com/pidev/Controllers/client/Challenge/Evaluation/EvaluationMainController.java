package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.util.List;
import java.util.Optional;

public class EvaluationMainController {
    private static Challenge navChallenge;
    private static Group navGroup;
    private static Activity navActivity;

    @FXML
    private Label challengeMetaLabel;
    @FXML
    private Label groupMetaLabel;
    @FXML
    private Label challengeTitleLabel;
    @FXML
    private Label challengeDescriptionLabel;
    @FXML
    private Label activityIdLabel;
    @FXML
    private Label submissionFileLabel;
    @FXML
    private VBox evaluationContent;
    @FXML
    private Button startEvaluationBtn;
    @FXML
    private VBox problemsContainer;
    @FXML
    private VBox membersContainer;
    private Challenge c;
    private Group g;
    private Activity a;
    private ServiceEvaluation serviceEval = new ServiceEvaluation();
    private ServiceProblemSolution servicePS = new ServiceProblemSolution();
    private ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    public void setData(Challenge challenge, Group grp, Activity activity) {
        this.c = challenge;
        this.g = grp;
        this.a = activity;

        if (challenge != null) {
            challengeTitleLabel.setText(challenge.getTitle());
            challengeMetaLabel.setText(challenge.getTitle());
            challengeDescriptionLabel.setText(challenge.getDescription());
        }

        if (grp != null) {
            groupMetaLabel.setText(grp.getName());
        }

        if (activity != null) {
            activityIdLabel.setText("Activity #" + activity.getId());
            submissionFileLabel.setText(activity.getSubmissionFile());
            boolean isEvaluation = serviceEval.isEvaluation(activity.getId());
            if (!isEvaluation) {
                evaluationContent.setVisible(false);
                evaluationContent.setManaged(false);
                startEvaluationBtn.setVisible(true);
                startEvaluationBtn.setManaged(true);
            } else {
                evaluationContent.setVisible(true);
                evaluationContent.setManaged(true);
                startEvaluationBtn.setVisible(false);
                startEvaluationBtn.setManaged(false);
            }
            loadProblemSolutions(a.getId());
            loadMemberActivity();

        }
    }

    @FXML
    public void OnStart() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Evaluation");
        alert.setHeaderText("Lock Group Activity");
        alert.setContentText("Once started, the group cannot modify their work. Proceed?");
        DialogPane dialogPane = alert.getDialogPane();
        String cssPath = "/styles/challenge.css";
        if (getClass().getResource(cssPath) != null) {
            dialogPane.getStylesheets().add(getClass().getResource(cssPath).toExternalForm());
            dialogPane.getStyleClass().add("my-custom-alert");
        }
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("alert-primary-btn");
        }
        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                Evaluation eval = new Evaluation();
                serviceEval.StartEvaluation(eval, a);
                evaluationContent.setVisible(true);
                evaluationContent.setManaged(true);
                startEvaluationBtn.setVisible(false);
                startEvaluationBtn.setManaged(false);

            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
    private void loadProblemSolutions(int activityId) {
        problemsContainer.getChildren().clear();
        List<ProblemSolution> problems = servicePS.display(activityId);
        int index = 1;
        for (ProblemSolution ps : problems) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Evaluation/ProblemSolutionEvaluationCards.fxml"));
                VBox card = loader.load();
                ProblemSolutionEvaluationCardsController controller = loader.getController();
                controller.setDataProblem(index++, ps, () -> {
                });
                problemsContainer.getChildren().add(card);


            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    private void loadMemberActivity(){
        membersContainer.getChildren().clear();
        List<MemberActivity> statusList = serviceMA.getAllGroupMembersForActivity(this.g, this.a);
        for (MemberActivity ma : statusList) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Evaluation/MemberEvaluationCards.fxml"));
                HBox card = loader.load();

                MemberEvaluationCardsController controller = loader.getController();
                controller.setMemberActivityData(ma,() -> {
                });

                membersContainer.getChildren().add(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }


    }


}


