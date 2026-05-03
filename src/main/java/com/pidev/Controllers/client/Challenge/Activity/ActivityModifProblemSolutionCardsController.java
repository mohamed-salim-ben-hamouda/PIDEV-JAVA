package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;

public class ActivityModifProblemSolutionCardsController {
    @FXML
    private TextArea probModif;
    @FXML
    private TextArea solModif;
    @FXML
    private Button ModifProbSol;
    private ProblemSolution ps;
    private Runnable onUpdateCallback;
    private ServiceProblemSolution servicePS = new ServiceProblemSolution();
    public void initData(ProblemSolution problem_solution, Runnable onUpdate){
        this.ps=problem_solution;
        this.onUpdateCallback=onUpdate;
        probModif.setText(ps.getProblemDescription());
        solModif.setText(ps.getGroupSolution());
    }
    @FXML
    public void onUpdatePS(){
        String problem=probModif.getText();
        String solution=solModif.getText();
        ps.setProblemDescription(problem);
        ps.setGroupSolution(solution);
        try{
            servicePS.update(ps);
            if (onUpdateCallback != null) {
                onUpdateCallback.run();
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
