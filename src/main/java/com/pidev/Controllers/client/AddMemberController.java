package com.pidev.Controllers.client;

import com.pidev.models.User;
import com.pidev.utils.GroupViewContext;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

public class AddMemberController {

    @FXML
    private TextField searchField;
    @FXML
    private VBox resultsContainer;
    @FXML
    private Label feedbackLabel;

    private final MembershipController membershipController = new MembershipController();

    @FXML
    private void handleSearch() {
        String term = searchField.getText() == null ? "" : searchField.getText().trim();
        if (term.isEmpty()) {
            feedbackLabel.setText("Type a name to search.");
            resultsContainer.getChildren().clear();
            return;
        }

        try {
            List<User> users = membershipController.searchUsersForGroupInvite(term);
            renderUsers(users);
            feedbackLabel.setText(users.isEmpty() ? "No users found." : "Found " + users.size() + " user(s).");
        } catch (SQLException e) {
            feedbackLabel.setText("Search failed: " + e.getMessage());
        }
    }

    @FXML
    private void handleBack() {
        openView("/Fxml/client/GroupShowView.fxml", null);
    }

    private void renderUsers(List<User> users) {
        resultsContainer.getChildren().clear();
        Integer groupId = GroupViewContext.getSelectedGroupId();
        if (groupId == null) {
            feedbackLabel.setText("No group selected.");
            return;
        }

        for (User user : users) {
            HBox row = new HBox(10);
            row.getStyleClass().add("search-user-row");

            String display = ((user.getPrenom() == null ? "" : user.getPrenom()) + " " + (user.getNom() == null ? "" : user.getNom())).trim();
            Label label = new Label(display.isEmpty() ? "User #" + user.getId() : display);

            Button addBtn = new Button("Add");
            addBtn.getStyleClass().add("primary-action");
            addBtn.setOnAction(e -> {
                try {
                    membershipController.addMemberConfirm(groupId, user.getId());
                    openView("/Fxml/client/GroupShowView.fxml", "User added to group.");
                } catch (SQLException ex) {
                    feedbackLabel.setText("Could not add member: " + ex.getMessage());
                }
            });

            row.getChildren().addAll(label, addBtn);
            resultsContainer.getChildren().add(row);
        }
    }

    private void openView(String fxmlPath, String message) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));
            javafx.scene.layout.StackPane contentArea = (javafx.scene.layout.StackPane) searchField.getScene().lookup("#contentArea");
            if (contentArea != null) {
                contentArea.getChildren().setAll(root);
            }
            if (message != null) {
                feedbackLabel.setText(message);
            }
        } catch (IOException ignored) {
        }
    }
}
