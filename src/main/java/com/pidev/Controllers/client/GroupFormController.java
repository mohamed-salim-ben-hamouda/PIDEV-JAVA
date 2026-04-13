package com.pidev.Controllers.client;

import com.pidev.utils.GroupViewContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class GroupFormController implements Initializable {

    @FXML
    private Label formTitleLabel;
    @FXML
    private TextField nameField;
    @FXML
    private TextArea descriptionField;
    @FXML
    private ComboBox<String> typeCombo;
    @FXML
    private ComboBox<String> levelCombo;
    @FXML
    private TextField maxMembersField;
    @FXML
    private TextField iconField;
    @FXML
    private Button saveButton;
    @FXML
    private Label feedbackLabel;

    private final GroupController groupController = new GroupController();
    private Integer editingGroupId;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        typeCombo.getItems().addAll("public", "private", "study", "gaming", "other");
        levelCombo.getItems().addAll("beginner", "intermediate", "advanced", "all");
        typeCombo.setValue("public");
        levelCombo.setValue("all");

        editingGroupId = GroupViewContext.getEditingGroupId();
        if (editingGroupId != null) {
            formTitleLabel.setText("Edit Group Settings");
            saveButton.setText("Save Changes");
            loadGroupForEdit(editingGroupId);
        } else {
            formTitleLabel.setText("Create a New Group");
            saveButton.setText("Create Group");
        }
    }

    @FXML
    private void handleSaveGroup() {
        String name = clean(nameField.getText());
        String description = clean(descriptionField.getText());
        String type = typeCombo.getValue();
        String level = levelCombo.getValue();
        Integer maxMembers = parseInt(maxMembersField.getText());
        String icon = clean(iconField.getText());

        if (name.isEmpty() || description.isEmpty() || type == null || level == null || maxMembers == null || maxMembers < 1) {
            setFeedback("Please fill all required fields. Max members must be a positive number.", true);
            return;
        }

        com.pidev.models.Group group = new com.pidev.models.Group();
        group.setName(name);
        group.setDescription(description);
        group.setType(type);
        group.setLevel(level);
        group.setMaxMembers(maxMembers);
        group.setIcon(icon.isEmpty() ? null : icon);

        try {
            if (editingGroupId == null) {
                com.pidev.models.Group created = groupController.add(group);
                GroupViewContext.setSelectedGroupId(created.getId());
                GroupViewContext.clearEditingGroupId();
                openView("/Fxml/client/GroupShowView.fxml");
            } else {
                group.setId(editingGroupId);
                boolean updated = groupController.edit(group);
                if (!updated) {
                    setFeedback("Group not found or not updated.", true);
                    return;
                }
                GroupViewContext.setSelectedGroupId(editingGroupId);
                GroupViewContext.clearEditingGroupId();
                openView("/Fxml/client/GroupShowView.fxml");
            }
        } catch (SQLException e) {
            setFeedback("Database error: " + e.getMessage(), true);
        }
    }

    @FXML
    private void handleCancel() {
        GroupViewContext.clearEditingGroupId();
        if (GroupViewContext.getSelectedGroupId() != null) {
            openView("/Fxml/client/GroupShowView.fxml");
        } else {
            openView("/Fxml/client/GroupsView.fxml");
        }
    }

    private void loadGroupForEdit(int groupId) {
        try {
            var details = groupController.show(groupId, null, null);
            if (details.isEmpty()) {
                setFeedback("Group not found.", true);
                return;
            }
            var group = details.get().getGroup();
            nameField.setText(group.getName());
            descriptionField.setText(group.getDescription());
            typeCombo.setValue(group.getType());
            levelCombo.setValue(group.getLevel());
            maxMembersField.setText(group.getMaxMembers() == null ? "" : String.valueOf(group.getMaxMembers()));
            iconField.setText(group.getIcon() == null ? "" : group.getIcon());
        } catch (SQLException e) {
            setFeedback("Could not load group: " + e.getMessage(), true);
        }
    }

    private Integer parseInt(String value) {
        try {
            return Integer.parseInt(clean(value));
        } catch (Exception e) {
            return null;
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private void setFeedback(String message, boolean error) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("error-text", "success-text");
        feedbackLabel.getStyleClass().add(error ? "error-text" : "success-text");
    }

    private void openView(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) saveButton.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
        } catch (IOException ignored) {
        }
    }
}
