package com.pidev.Controllers.client.Challenge.Evaluation;

import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

public class ProblemSolutionEvaluationCardsController {
    @FXML
    private Label problemIndex;
    @FXML
    private Label problemDescription;
    @FXML
    private Label groupSolution;
    @FXML
    private Label supervisorSolution;
    @FXML
    private TextField solutionInput;
    @FXML
    private Button AddSolutionBtn;
    @FXML
    private Label sup_solution;
    @FXML
    private Label edit_text;
    private ProblemSolution p;
    private ServiceProblemSolution serviceP = new ServiceProblemSolution();
    private Runnable onUpdateCallback;

    public void setDataProblem(int index, ProblemSolution ps,Runnable onUpdate){
        this.p=ps;
        this.onUpdateCallback = onUpdate;
        problemIndex.setText(String.valueOf(index));
        problemDescription.setText(ps.getProblemDescription());
        groupSolution.setText(ps.getGroupSolution() != null ? ps.getGroupSolution() : "No solution provided");
        if (ps.getSupervisorSolution() != null && !ps.getSupervisorSolution().isEmpty()) {
            supervisorSolution.setText(ps.getSupervisorSolution());
        }
        boolean condition=serviceP.isSupervisorSolution(ps);
        if (condition){
            AddSolutionBtn.setText("Modify");
            solutionInput.setText(ps.getSupervisorSolution());
        }

    }
    @FXML
    public void OnAddSolution(){
        String supervisorSol = solutionInput.getText().trim();
        if(!supervisorSol.isEmpty()){
            try {
                p.setSupervisorSolution(supervisorSol);
                serviceP.updateSupervisorSolution(p);

                supervisorSolution.setText(supervisorSol);
                AddSolutionBtn.setText("Modify");

                solutionInput.setText(supervisorSol);
                if (onUpdateCallback != null) {
                    onUpdateCallback.run();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }
    public void SetForStudent(){
        AddSolutionBtn.setVisible(false);
        AddSolutionBtn.setManaged(false);
        solutionInput.setVisible(false);
        solutionInput.setManaged(false);
        edit_text.setVisible(false);
        edit_text.setManaged(false);
        sup_solution.setText("Supervisor_solution");
    }


}
