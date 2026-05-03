package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.models.MemberActivity;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;

import java.util.Objects;
import java.util.function.Consumer;

public class MemberActivityCardController {
    @FXML
    private Label descriptionLabel;

    @FXML
    private Button editBtn;

    @FXML
    private Button deleteBtn;

    public void setData(MemberActivity memberActivity, Consumer<MemberActivity> onEdit, Consumer<MemberActivity> onDelete) {
        Objects.requireNonNull(memberActivity, "memberActivity must not be null");
        Objects.requireNonNull(onEdit, "onEdit must not be null");
        Objects.requireNonNull(onDelete, "onDelete must not be null");

        descriptionLabel.setText(memberActivity.getActivityDescription() == null ? "" : memberActivity.getActivityDescription());
        editBtn.setOnAction(event -> onEdit.accept(memberActivity));
        deleteBtn.setOnAction(event -> onDelete.accept(memberActivity));
    }
}

