package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.models.ProblemSolution;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Objects;
import java.util.function.Consumer;

public class ProblemSolutionCardController {
    @FXML
    private Label problemLabel;

    @FXML
    private Label solutionLabel;

    @FXML
    private Button editBtn;

    @FXML
    private Button deleteBtn;

    public void setData(ProblemSolution ps, Consumer<ProblemSolution> onEdit, Consumer<ProblemSolution> onDelete) {
        Objects.requireNonNull(ps, "ProblemSolution must not be null");
        Objects.requireNonNull(onEdit, "onEdit must not be null");
        Objects.requireNonNull(onDelete, "onDelete must not be null");

        problemLabel.setText(ps.getProblemDescription() == null ? "" : ps.getProblemDescription());
        solutionLabel.setText(dashIfBlank(ps.getGroupSolution()));

        editBtn.setOnAction(event -> onEdit.accept(ps));
        deleteBtn.setOnAction(event -> onDelete.accept(ps));
    }

    private static String dashIfBlank(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }
}

