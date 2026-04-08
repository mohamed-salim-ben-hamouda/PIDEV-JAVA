package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Controllers.client.Challenge.ChallengeCardController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.MemberActivity;
import com.pidev.models.ProblemSolution;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private ComboBox<ProblemSolution> problemCombo;
    @FXML
    private VBox solutionContainer;
    @FXML
    private TextArea solution_desc;
    @FXML
    private VBox submissionContainer;
    @FXML
    private Label fileNameLabel;
    @FXML
    private TableView<ProblemSolution> problemSolutionTable;
    @FXML
    private TableColumn<ProblemSolution,String> problemCol;
    @FXML
    private TableColumn<ProblemSolution,String> solutionCol;
    @FXML
    private TableColumn<ProblemSolution,Void> probActionCol;
    @FXML
    private VBox editOverlayHost;
    @FXML
    private TableView<MemberActivity> descTable;
    @FXML
    private TableColumn<MemberActivity,Integer> descIdCol;
    @FXML
    private TableColumn<MemberActivity,String> nameUser;
    @FXML
    private TableColumn<MemberActivity,String> descTextCol;
    @FXML
    private TableColumn<MemberActivity,Void> descActionCol;
    @FXML
    private VBox editMemberOverlayHost;
    @FXML
    private VBox DescriptionContainer;
    private ServiceActivity serviceActivity = new ServiceActivity();
    private ServiceMemberActivity serviceMember = new ServiceMemberActivity();
    private ServiceProblemSolution serviceProblem =new ServiceProblemSolution();
    private int grp_id;
    private int act_id;
    private File selectedPdf;
    private ProblemSolution editingProblemSolution;
    private MemberActivity editingMemberActivity;



    public void initData(Challenge c, int grpId,int actId) {
        this.c = c;
        this.grp_id = grpId;
        this.act_id=actId;
        solutionContainer.setVisible(false);
        solutionContainer.setManaged(false);
        String css = getClass().getResource("/styles/challenge.css").toExternalForm();
        initTable(this.act_id);
        initMemberActivityTable(this.act_id);
        UserPermissions();
        renderChallengeHeader();
        loadProblems();
        descriptionVisibility();

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
            refreshMemberTable(act_id);
            descriptionVisibility();
        } catch (Exception e) {
            System.err.println("Failed to save member activity: " + e.getMessage());
        }

    }

    public void initMemberActivityTable(int activityId) {
        if (descIdCol != null) {
            descIdCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        }
        if (descTextCol != null) {
            descTextCol.setCellValueFactory(new PropertyValueFactory<>("activityDescription"));
        }
        if (nameUser != null) {
            nameUser.setCellValueFactory(cellData ->
                    new SimpleStringProperty(cellData.getValue() != null && cellData.getValue().getUser() != null
                            ? cellData.getValue().getUser().getDisplayName()
                            : "-")
            );
        }

        if (descActionCol != null) {
            descActionCol.setCellFactory(param -> new ActionCell<>(
                    this::handleEditMember,
                    this::handleDeleteMember
            ));
        }

        refreshMemberTable(activityId);
    }

    private void refreshMemberTable(int activityId) {
        int user_id=2;
        List<MemberActivity> list = serviceMember.display(activityId,user_id);
        descTable.setItems(FXCollections.observableArrayList(list));
    }

    private void handleEditMember(MemberActivity selected) {
        if (selected == null) return;
        if (editMemberOverlayHost == null) return;

        if (editingMemberActivity != null
                && editingMemberActivity.getId() != null
                && editingMemberActivity.getId().equals(selected.getId())
                && editMemberOverlayHost.isVisible()) {
            hideEditMemberOverlay();
            return;
        }

        editingMemberActivity = selected;
        int index = (descTable == null || descTable.getItems() == null) ? -1 : descTable.getItems().indexOf(selected);
        if (index >= 0) {
            descTable.scrollTo(index);
            descTable.getSelectionModel().select(index);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/EditMemberActivity.fxml"));
            Parent content = loader.load();
            content.getStylesheets().add(getClass().getResource("/styles/challenge.css").toExternalForm());
            if (content instanceof Region region && descTable != null) {
                region.prefWidthProperty().bind(descTable.widthProperty().subtract(30));
            }

            EditMemberActivityController overlayController = loader.getController();
            overlayController.setData(selected);
            overlayController.setOnSave(updated -> {
                try {
                    serviceMember.update(updated);
                    refreshMemberTable(act_id);
                } catch (Exception e) {
                    System.err.println("Failed to update member activity: " + e.getMessage());
                } finally {
                    hideEditMemberOverlay();
                }
            });

            editMemberOverlayHost.getChildren().setAll(content);
            editMemberOverlayHost.setVisible(true);
            editMemberOverlayHost.setManaged(true);
        } catch (IOException e) {
            System.err.println("Failed to open member edit overlay: " + e.getMessage());
        }
    }

    private void hideEditMemberOverlay() {
        if (editMemberOverlayHost != null) {
            editMemberOverlayHost.getChildren().clear();
            editMemberOverlayHost.setVisible(false);
            editMemberOverlayHost.setManaged(false);
        }
        editingMemberActivity = null;
    }

    private void handleDeleteMember(MemberActivity selected) {
        if (selected == null) return;
        try {
            serviceMember.delete(selected.getId());
            if (editingMemberActivity != null && editingMemberActivity.getId().equals(selected.getId())) {
                hideEditMemberOverlay();
            }
            refreshMemberTable(act_id);
            descriptionVisibility();
        } catch (Exception e) {
            System.err.println("Failed to delete member activity: " + e.getMessage());
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
                if (problemCombo.getItems() == null) {
                    problemCombo.setItems(FXCollections.observableArrayList());
                }
                problemCombo.getItems().add(p);
                refreshTable(act_id);
            }
        } catch (Exception e) {
            System.err.println("Failed to save problem: " + e.getMessage());
        }
    }
    private void loadProblems() {
        ServiceProblemSolution service = new ServiceProblemSolution();
        List<ProblemSolution> problems = serviceProblem.displayProblems(act_id);
        ObservableList<ProblemSolution> observableList = FXCollections.observableArrayList(problems);
        problemCombo.setItems(observableList);
        problemCombo.setPromptText("Sélectionnez un problème");
        solutionContainer.visibleProperty().bind(Bindings.isNotEmpty(problemCombo.getItems()));
        solutionContainer.managedProperty().bind(solutionContainer.visibleProperty());
    }
    @FXML
    private void OnSolution(){
        ProblemSolution problem_selected=problemCombo.getSelectionModel().getSelectedItem();
        String sol=solution_desc.getText();
        try {
            problem_selected.setGroupSolution(sol);

            serviceProblem.addSolutionGrp(problem_selected);
            problemCombo.getItems().remove(problem_selected);
            solution_desc.clear();
            problemCombo.getSelectionModel().clearSelection();
            loadProblems();
            refreshTable(act_id);
        } catch (Exception e) {
            System.err.println("Failed to save solution: " + e.getMessage());
        }

    }
    public void UserPermissions(){
        int user_id = 2;
        boolean leader=serviceActivity.isUserLeader(this.grp_id,user_id);
        submissionContainer.setVisible(leader);
        submissionContainer.setManaged(leader);
    }
    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            fileNameLabel.setText(selectedPdf.getName());
        }
    }
    @FXML
    public void onSubmission(){
        if (selectedPdf == null) {
            System.err.println("Erreur: Aucun fichier sélectionné.");
            return;
        }
        try {
            Activity submission = new Activity();
            submission.setId(this.act_id);
            Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "activity_pdf");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(selectedPdf.getName());
            Files.copy(selectedPdf.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            submission.setSubmissionFile("challenge_module/activity_pdf/" + selectedPdf.getName());
            serviceActivity.submissionfile(submission);
            fileNameLabel.setText("Soumission terminée ✅");
            fileNameLabel.setStyle("-fx-text-fill: #2ecc71;");
            BaseController.getInstance().loadActivity();

        } catch (Exception e) {
            System.err.println("Échec de la soumission: " + e.getMessage());
        }

    }

    public void initTable(int activityId) {
        problemCol.setCellValueFactory(new PropertyValueFactory<>("problemDescription"));
        solutionCol.setCellValueFactory(cellData ->
                new SimpleStringProperty(dashIfBlank(cellData.getValue() == null ? null : cellData.getValue().getGroupSolution()))
        );

        probActionCol.setCellFactory(param -> new ActionCell<>(
                this::handleEditAction,
                this::handleDeleteAction
        ));

        refreshTable(activityId);
    }

    private static String dashIfBlank(String value) {
        return (value == null || value.isBlank()) ? "-" : value;
    }

    private void handleEditAction(ProblemSolution selected) {
        if (selected == null) return;
        if (editOverlayHost == null) return;

        if (problemSolutionTable != null) {
            problemSolutionTable.getSelectionModel().select(selected);
        }

        if (editingProblemSolution != null
                && editingProblemSolution.getId() == selected.getId()
                && editOverlayHost.isVisible()) {
            hideEditOverlay();
            return;
        }

        editingProblemSolution = selected;
        int index = (problemSolutionTable == null || problemSolutionTable.getItems() == null)
                ? -1
                : problemSolutionTable.getItems().indexOf(selected);
        if (index >= 0) {
            problemSolutionTable.scrollTo(index);
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/EditProblemActivity.fxml"));
            Parent content = loader.load();
            content.getStylesheets().add(getClass().getResource("/styles/challenge.css").toExternalForm());
            if (content instanceof Region region && problemSolutionTable != null) {
                region.prefWidthProperty().bind(problemSolutionTable.widthProperty().subtract(30));
            }

            EditProblemSolutionController overlayController = loader.getController();
            overlayController.setData(selected);
            overlayController.setOnSave(updated -> {
                try {
                    serviceProblem.update(updated);
                    refreshTable(act_id);
                } catch (Exception e) {
                    System.err.println("Failed to update problem/solution: " + e.getMessage());
                } finally {
                    hideEditOverlay();
                }
            });

            editOverlayHost.getChildren().setAll(content);
            editOverlayHost.setVisible(true);
            editOverlayHost.setManaged(true);
        } catch (IOException e) {
            System.err.println("Failed to open edit overlay: " + e.getMessage());
        }
    }

    private void hideEditOverlay() {
        if (editOverlayHost != null) {
            editOverlayHost.getChildren().clear();
            editOverlayHost.setVisible(false);
            editOverlayHost.setManaged(false);
        }
        editingProblemSolution = null;
    }

    private void handleDeleteAction(ProblemSolution selected) {
        if (selected == null) return;
        try {
            serviceProblem.delete(selected.getId());
            if (editingProblemSolution != null && editingProblemSolution.getId() == selected.getId()) {
                hideEditOverlay();
            }
            refreshTable(act_id);
            loadProblems();
        } catch (Exception e) {
            System.err.println("Failed to delete problem/solution: " + e.getMessage());
        }
    }
    public void refreshTable(int activityId) {
        List<ProblemSolution> list = serviceProblem.display(activityId);

        problemSolutionTable.setItems(FXCollections.observableArrayList(list));
    }
    public void descriptionVisibility(){
        int user_id=2;
        boolean rs =serviceMember.findDescription(act_id,user_id);
        DescriptionContainer.setVisible(!rs);
        DescriptionContainer.setManaged(!rs);

    }




}
