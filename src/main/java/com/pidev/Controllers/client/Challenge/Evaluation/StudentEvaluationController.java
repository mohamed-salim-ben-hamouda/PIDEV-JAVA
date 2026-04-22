package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.utils.ImageAssets;
import com.pidev.utils.OpenPdfUtil;
import com.pidev.models.Activity;
import com.pidev.models.Evaluation;
import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;


public class StudentEvaluationController {
    @FXML
    private ImageView trophyImage;
    @FXML
    private Label challengeTitle;
    @FXML
    private Label challengeDescription;

    @FXML
    private Label submittedDate;
    @FXML
    private VBox problemsContainer;
    @FXML
    private Label individualScore;
    @FXML
    private Label groupScore;
    @FXML
    private Label finalScore;
    @FXML
    private Label result;


    private Evaluation e;
    private Activity a;
    private ServiceProblemSolution servicePS = new ServiceProblemSolution();
    private ServiceMemberActivity serviceMA = new ServiceMemberActivity();
    private ServiceEvaluation serviceEval = new ServiceEvaluation();

    @FXML
    public void initialize() {
        trophyImage.setImage(ImageAssets.TROPHY_ICON_80);
    }

    public void setData(Evaluation e, Activity a){
        this.a=a;
        this.e=e;
        challengeTitle.setText(this.a.getChallenge().getTitle());
        challengeDescription.setText(this.a.getChallenge().getDescription());
        if (a.getSubmissionDate() != null) {
            submittedDate.setText("Submitted on: " + a.getSubmissionDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        } else {
            submittedDate.setText("Submission date: Not available");
        }
        loadProblemSolutions(a.getId());
        int user_id=2;
        double indiv_score = serviceMA.SelectIndivScore(user_id,this.a.getId());
        String indiv_scoreStr = String.format("%.2f", indiv_score);
        individualScore.setText(indiv_scoreStr);
        double grp_score=serviceEval.SelectGrpScore(e.getId().intValue());
        String grp_scoreStr = String.format("%.2f", grp_score);
        groupScore.setText(grp_scoreStr);
        double final_score=CalculateFinalScore(indiv_score,grp_score);
        String final_scoreStr =String.format("%.2f",final_score);
        finalScore.setText(final_scoreStr);
        ChallengeResult(final_score);


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
                controller.SetForStudent();
                problemsContainer.getChildren().add(card);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public double CalculateFinalScore(double indiv_score,double grp_score){
        double final_score;
        if(indiv_score != 0){
            final_score = indiv_score*0.7 + grp_score*0.3;
            return final_score;
        } else{
            return 0;
        }
    }
    public void ChallengeResult(double final_score){
        if(final_score >= 10){
            result.setText("Passed");
            result.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #166534; -fx-padding: 10 30; -fx-background-radius: 30; -fx-font-weight: 800; -fx-font-size: 14;");
        }else {
            result.setText("Failed");
            result.setStyle("-fx-background-color: #fee2e2; -fx-text-fill: #991b1b; -fx-padding: 10 30; -fx-background-radius: 30; -fx-font-weight: 800; -fx-font-size: 14;");
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
    @FXML
    public void openPdfFeedback() {
        if (e == null || e.getFeedback() == null || e.getFeedback().isBlank()) {
            showError("No feedback file is available for this evaluation yet.");
            return;
        }

        try {
            OpenPdfUtil.openPdfInApp(e.getFeedback(), "Feedback PDF");
        } catch (Exception ex) {
            showError("Could not open feedback PDF:\n" + ex.getMessage());
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
