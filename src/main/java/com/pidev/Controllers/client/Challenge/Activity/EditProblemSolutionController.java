package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.util.Objects;
import java.util.function.Consumer;

public class EditProblemSolutionController {
    @FXML
    private TextArea problemTextArea;

    @FXML
    private TextArea solutionTextArea;

    private ProblemSolution boundProblemSolution;
    private Consumer<ProblemSolution> onSave;

    public void setData(ProblemSolution ps) {
        this.boundProblemSolution = Objects.requireNonNull(ps, "ProblemSolution must not be null");
        problemTextArea.setText(ps.getProblemDescription() == null ? "" : ps.getProblemDescription());
        solutionTextArea.setText(ps.getGroupSolution() == null ? "" : ps.getGroupSolution());
    }

    public void setOnSave(Consumer<ProblemSolution> onSave) {
        this.onSave = onSave;
    }

    @FXML
    private void onSave() {
        if (boundProblemSolution == null) return;
        boundProblemSolution.setProblemDescription(problemTextArea.getText());
        boundProblemSolution.setGroupSolution(solutionTextArea.getText());
        if (onSave != null) {
            onSave.accept(boundProblemSolution);
        }
    }
}

