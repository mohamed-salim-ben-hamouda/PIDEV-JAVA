package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.*;
import com.pidev.utils.OpenPdfUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private VBox evaluationContent;
    @FXML
    private Button startEvaluationBtn;
    @FXML
    private VBox problemsContainer;
    @FXML
    private VBox membersContainer;
    @FXML
    private Button fileInp;
    @FXML
    private TextField groupScoreInput;
    @FXML
    private Button submit_feedback;

    private File selectedPdf;

    private Challenge c;
    private Group g;
    private Activity a;
    private ServiceEvaluation serviceEval = new ServiceEvaluation();
    private ServiceProblemSolution servicePS = new ServiceProblemSolution();
    private ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    private ServiceActivity serviceA = new ServiceActivity();
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
        boolean eva_status=serviceEval.isEvaluationfinished(a.getId());
        if(eva_status){
            submit_feedback.setText("Modify Final Evaluation");
        }else {
            submit_feedback.setText("Submit Final Evaluation");

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
    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            fileInp.setText(selectedPdf.getName());
        }
    }
    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Evaluation created");
        alert.setContentText("The activity has been successfully evaluated!!!");
        alert.setGraphic(null);

        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/challenge.css").toExternalForm());
        dialogPane.getStyleClass().add("my-custom-alert");

        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("alert-primary-btn");
        }

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);

        alert.showAndWait();
    }
    public void OnSubmit(){
        String grp_scoreT=groupScoreInput.getText();
        try {
            double grp_score = Double.parseDouble(grp_scoreT);
            Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "evaluation_pdf");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(selectedPdf.getName());
            Files.copy(selectedPdf.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            Evaluation e = serviceEval.findEvaluation(a.getId());

            if(e != null){
                e.setGroupScore(grp_score);
                e.setFeedback("challenge_module/evaluation_pdf/" + selectedPdf.getName());
                serviceEval.updateEvaluation(e);
                showSuccessAlert();
                serviceA.updateActivityStatus(a);
                BaseController.getInstance().loadEvaluation();

            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    @FXML
    public void openPdfSubmission(){
        if(a == null || a.getSubmissionFile() == null || a.getSubmissionFile().isBlank()){
            showError("No Submission file is available for this activity yet.");
            return;
        }
        try {
            OpenPdfUtil.openPdfInApp(a.getSubmissionFile(), "Submission PDF");
        } catch (IOException ex) {
            showError("Could not open submission PDF:\n" + ex.getMessage());
        }
    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Open PDF");
        alert.setHeaderText("Unable to open feedback PDF");
        alert.setContentText(message);
        alert.showAndWait();
    }


}


