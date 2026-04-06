package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.Challenge.ChallengeCardController;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.Challenge;
import com.pidev.models.MemberActivity;
import com.pidev.models.ProblemSolution;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.VBox;
import javafx.scene.control.TextArea;
import java.io.IOException;
import java.util.List;

public class ActivityController {
    @FXML
    private VBox challengeListContainer;
    private Challenge c;
    @FXML
    private TextArea activity_desc;
    @FXML
    private TextArea Problem_grp;
    @FXML
    private ComboBox problemCombo;
    private ServiceMemberActivity serviceMember = new ServiceMemberActivity();
    private ServiceProblemSolution serviceProblem =new ServiceProblemSolution();
    private int grp_id;
    private int act_id;


    public void initData(Challenge c, int grpId,int actId) {
        this.c = c;
        this.grp_id = grpId;
        this.act_id=actId;
        renderChallengeHeader();
        loadProblems();

    }

    private void renderChallengeHeader() {
        try {
            challengeListContainer.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/ChallengeCard.fxml"));
            VBox card = loader.load();
            ChallengeCardController cardController = loader.getController();
            cardController.setData(c,v->renderChallengeHeader());
            cardController.StudentCard(cardController.editBtn);
            cardController.StudentCard(cardController.deleteBtn);
            cardController.StudentCard(cardController.groupsBtn);
            cardController.StudentCard(cardController.participationBtn);
            challengeListContainer.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("Error loading challenge header: " + e.getMessage());
        }
    }

    public void OnMemberActivity(){
        String description=activity_desc.getText();
        try {
            MemberActivity ma = new MemberActivity();
            ma.setActivityDescription(description);
            int user_id = 2;
            serviceMember.addMemberActivity(ma, act_id, user_id);
            activity_desc.clear();
        } catch (Exception e) {
            System.err.println("Failed to save member activity: " + e.getMessage());
        }

    }
    @FXML
    public void OnProblem() {
        String problemDesc = Problem_grp.getText();
        if (problemDesc.isEmpty()) return;
        try {
            ProblemSolution p = new ProblemSolution();
            p.setProblemDescription(problemDesc);
            int newId = serviceProblem.addProblem(p, act_id);
            if (newId != -1) {
                p.setId(newId);
                Problem_grp.clear();
                problemCombo.getItems().add(p);
            }
        } catch (Exception e) {
            System.err.println("Failed to save problem: " + e.getMessage());
        }
    }
    private void loadProblems() {
        ServiceProblemSolution service = new ServiceProblemSolution();
        List<ProblemSolution> problems = serviceProblem.displayProblems(act_id);

        // Convert the list to an ObservableList for JavaFX
        ObservableList<ProblemSolution> observableList = FXCollections.observableArrayList(problems);

        problemCombo.setItems(observableList);

        // Set a prompt text if it's empty
        problemCombo.setPromptText("Sélectionnez un problème");
    }


}
