package com.pidev.Controllers.client.Challenge;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Services.Membership.ServiceMembership;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import com.pidev.utils.OpenPdfUtil;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.control.*;
import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.io.IOException;
import java.util.List;
import java.util.function.Consumer;
import java.nio.file.Path;

public class ChallengeCardController {
    @FXML
    private Label titleCard;
    @FXML
    private Label descriptionCard;
    @FXML
    private Label createdAtCard;
    @FXML
    private Label targetSkillCard;
    @FXML
    private Label difficultyCard;
    @FXML
    public Button groupsBtn;
    @FXML
    public Button deleteBtn;
    @FXML
    public Button editBtn;
    @FXML
    public Button participationBtn;
    @FXML
    private VBox cardRoot;
    @FXML
    private VBox editForm;
    @FXML
    private Button fileBtn;
    private Challenge c;
    private ServiceMembership groupService;

    private Consumer<Void> onDeleteCallback;
    @FXML private VBox groupSection;
    @FXML private FlowPane groupsContainer;
    @FXML
    public void initialize() {
        groupService = new ServiceMembership();
        participationBtn.setOnAction(event -> toggleGroupSection());
    }
    private void toggleGroupSection() {
        boolean isVisible = groupSection.isVisible();
        groupSection.setVisible(!isVisible);
        groupSection.setManaged(!isVisible);

        if (!isVisible) {
            loadUserGroups();
        }
    }
    private void loadUserGroups() {
        groupsContainer.getChildren().clear();
        int user_id = 2;
        List<Group> userGroups = groupService.FindAdminGroups(user_id);
        groupsContainer.getChildren().clear();

        if (userGroups.isEmpty()) {

            Label emptyLabel = new Label("You are not a leader of any groups");
            emptyLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 14px;");

            groupsContainer.getChildren().add(emptyLabel);

        } else {
            try {
                for (Group group : userGroups) {
                    FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/AdminGroupCard.fxml"));
                    VBox card = loader.load();
                    AdminGroupCardController controller = loader.getController();
                    controller.setGroupData(group,this.c);
                    groupsContainer.getChildren().add(card);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    public void setData(Challenge c,Consumer<Void> onDeleteCallback) {
        this.c = c;
        this.onDeleteCallback = onDeleteCallback;
        titleCard.setText(c.getTitle());
        descriptionCard.setText(c.getDescription());

        createdAtCard.setText("Created At: " + (c.getCreatedAt() != null ? c.getCreatedAt().toLocalDate() : "-"));
        targetSkillCard.setText("Target Skill: " + (c.getTargetSkill() == null || c.getTargetSkill().isBlank() ? "-" : c.getTargetSkill()));
        difficultyCard.setText("Difficulty: " + (c.getDifficulty() == null || c.getDifficulty().isBlank() ? "-" : c.getDifficulty()));

        String content = c.getContent();
        if (content == null || content.isBlank()) {
            fileBtn.setManaged(false);
            fileBtn.setVisible(false);
            return;
        }
        fileBtn.setManaged(true);
        fileBtn.setVisible(true);
        String fileName = Path.of(content.replace('\\', '/')).getFileName().toString();
        fileBtn.setText("View File: " + fileName);
    }
    @FXML
    public void OnDelete(){
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Delete");
        alert.setHeaderText("Delete Challenge");
        alert.setContentText("Are you sure you want to delete this challenge?");
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.getStylesheets().add(getClass().getResource("/styles/challenge.css").toExternalForm());
        dialogPane.getStyleClass().add("my-custom-alert");
        Node okButton = dialogPane.lookupButton(ButtonType.OK);
        if (okButton != null) {
            okButton.getStyleClass().add("alert-primary-btn");
        }

        Stage stage = (Stage) dialogPane.getScene().getWindow();
        stage.initStyle(StageStyle.TRANSPARENT);
        dialogPane.getScene().setFill(Color.TRANSPARENT);
        if (alert.showAndWait().get() == ButtonType.OK) {
            ServiceChallenge service = new ServiceChallenge();
            service.delete(c.getId());

            if (onDeleteCallback != null) {
                onDeleteCallback.accept(null);
            }
        }

    }
    @FXML
    public void handleEdit() {
        try {
            if (editForm != null) {
                cardRoot.getChildren().remove(editForm);
                editForm = null;
                return;
            }
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/Fxml/client/Challenge/ChallengeEdit.fxml")
            );
            editForm = loader.load();
            ChallengeEditController controller = loader.getController();
            controller.setDataEdit(c);
            controller.setOnUpdated(() -> {
                if (editForm != null) {
                    cardRoot.getChildren().remove(editForm);
                    editForm = null;
                }
                if (onDeleteCallback != null) {
                    onDeleteCallback.accept(null);
                } else {
                    setData(this.c, null);
                }
            });
            cardRoot.getChildren().add(editForm);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public  void StudentCard(javafx.scene.control.Button b){
        b.setVisible(false);
        b.setManaged(false);
    }
    public  void SupervisorCard(javafx.scene.control.Button b){
        b.setVisible(false);
        b.setManaged(false);
    }
    @FXML
    public void openPdfChallenge(){
        if(c == null || c.getContent() == null || c.getContent().isBlank()){
            showError("No Submission file is available for this activity yet.");
            return;
        }
        try {
            OpenPdfUtil.openPdfInApp(c.getContent(), "Challenge PDF");
        } catch (IOException ex) {
            showError("Could not open challenge PDF:\n" + ex.getMessage());
        }

    }
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Open PDF");
        alert.setHeaderText("Unable to open PDF");
        alert.setContentText(message);
        alert.showAndWait();
    }
    @FXML
    private void onSuccessPrediction(){
        int user_id = 2;
        List<Group> userGroups = groupService.FindAdminGroups(user_id);

        BaseController.getInstance().loadPredictions(c,userGroups);
    }

}
