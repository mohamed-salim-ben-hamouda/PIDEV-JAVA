package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.models.MemberActivity;
import javafx.fxml.FXML;
import javafx.scene.control.TextArea;

import java.util.Objects;
import java.util.function.Consumer;

public class EditMemberActivityController {
    @FXML
    private TextArea descriptionTextArea;

    private MemberActivity boundMemberActivity;
    private Consumer<MemberActivity> onSave;

    public void setData(MemberActivity memberActivity) {
        this.boundMemberActivity = Objects.requireNonNull(memberActivity, "memberActivity must not be null");
        descriptionTextArea.setText(memberActivity.getActivityDescription() == null ? "" : memberActivity.getActivityDescription());
    }

    public void setOnSave(Consumer<MemberActivity> onSave) {
        this.onSave = onSave;
    }

    @FXML
    private void onSave() {
        if (boundMemberActivity == null) return;
        boundMemberActivity.setActivityDescription(descriptionTextArea.getText());
        if (onSave != null) {
            onSave.accept(boundMemberActivity);
        }
    }
}

