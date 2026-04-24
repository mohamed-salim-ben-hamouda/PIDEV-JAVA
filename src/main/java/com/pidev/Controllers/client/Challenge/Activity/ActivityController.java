package com.pidev.Controllers.client.Challenge.Activity;

import com.pidev.Controllers.client.BaseController;
import com.pidev.Controllers.client.Challenge.ChallengeCardController;
import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceEvaluation;
import com.pidev.Services.Challenge.Classes.ServiceMemberActivity;
import com.pidev.Services.Challenge.Classes.ServiceProblemSolution;
import com.pidev.Services.Membership.ServiceMembership;
import com.pidev.models.*;
import com.pidev.utils.FlowiseGraderUtil;
import com.pidev.utils.GithubUtil;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ActivityController {
    @FXML
    private VBox challengeListContainer;
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
    private VBox editOverlayHost;
    @FXML
    private VBox editMemberOverlayHost;
    @FXML
    private VBox memberActivityCards;
    @FXML
    private Label memberEmptyLabel;
    @FXML
    private VBox problemSolutionCards;
    @FXML
    private Label psEmptyLabel;
    @FXML
    private VBox DescriptionContainer;

    @FXML
    private Label activityDescError;
    @FXML
    private Label problemGrpError;
    @FXML
    private Label solutionComboError;
    @FXML
    private Label solutionDescError;
    @FXML
    private Label fileError;
    @FXML
    private VBox membersGitUserName;
    @FXML
    private VBox gitHub;
    private Challenge c;
    private Activity currentActivity;
    private ServiceActivity serviceActivity = new ServiceActivity();
    private ServiceMemberActivity serviceMember = new ServiceMemberActivity();
    private ServiceProblemSolution serviceProblem = new ServiceProblemSolution();
    private ServiceEvaluation serviceEva = new ServiceEvaluation();
    private ServiceMembership serviceMS = new ServiceMembership();
    private int grp_id;
    private int act_id;
    private File selectedPdf;
    private ProblemSolution editingProblemSolution;
    private MemberActivity editingMemberActivity;

    private boolean activitySubmitted = false;
    private boolean problemSubmitted = false;
    private boolean solutionSubmitted = false;
    private boolean finalSubmitted = false;

    @FXML
    public void initialize() {
        activity_desc.textProperty().addListener((obs, old, val) -> {
            if (activitySubmitted) toggleError(activityDescError, val.isBlank());
        });
        Problem_grp.textProperty().addListener((obs, old, val) -> {
            if (problemSubmitted) toggleError(problemGrpError, val.isBlank());
        });
        solution_desc.textProperty().addListener((obs, old, val) -> {
            if (solutionSubmitted) toggleError(solutionDescError, val.isBlank());
        });
        problemCombo.valueProperty().addListener((obs, old, val) -> {
            if (solutionSubmitted) toggleError(solutionComboError, val == null);
        });

    }

    private void toggleError(Label label, boolean show) {
        if (label != null) {
            label.setVisible(show);
            label.setManaged(show);
            label.setTextFill(Color.RED);
        }
    }

    public void initData(Challenge c, int grpId, int actId) {
        this.c = c;
        this.grp_id = grpId;
        this.act_id = actId;
        this.currentActivity = serviceActivity.findActivityByChallengeAndGroup(c.getId(), grpId);
        solutionContainer.setVisible(false);
        solutionContainer.setManaged(false);
        initProblemSolutionCards(this.act_id);
        initMemberActivityCards(this.act_id);
        boolean leader = UserPermissions();
        renderChallengeHeader();
        loadProblems();
        descriptionVisibility();
        if (c.getGithub() == 1 && leader && (currentActivity == null || currentActivity.getRepo_created() == 0)) {
            gitHub.setManaged(true);
            gitHub.setVisible(true);
            loadGitMembers();
        } else {
            gitHub.setManaged(false);
            gitHub.setVisible(false);
        }
    }

    private void renderChallengeHeader() {
        try {
            challengeListContainer.getChildren().clear();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/ChallengeCard.fxml"));
            VBox card = loader.load();
            ChallengeCardController cardController = loader.getController();
            cardController.setData(c, v -> renderChallengeHeader());
            cardController.StudentCard(cardController.editBtn);
            cardController.StudentCard(cardController.deleteBtn);
            cardController.StudentCard(cardController.groupsBtn);
            cardController.StudentCard(cardController.participationBtn);
            challengeListContainer.getChildren().add(card);
        } catch (IOException e) {
            System.err.println("Error loading challenge header: " + e.getMessage());
        }
    }

    @FXML
    public void OnMemberActivity() {
        activitySubmitted = true;
        String description = activity_desc.getText();
        if (description.isBlank()) {
            toggleError(activityDescError, true);
            return;
        }
        toggleError(activityDescError, false);
        try {
            MemberActivity ma = new MemberActivity();
            ma.setActivityDescription(description);
            int user_id = 2;
            serviceMember.addMemberActivity(ma, act_id, user_id);
            activity_desc.clear();
            activitySubmitted = false;
            refreshMemberCards(act_id);
            descriptionVisibility();
        } catch (Exception e) {
            System.err.println("Failed to save member activity: " + e.getMessage());
        }
    }

    @FXML
    public void OnProblem() {
        problemSubmitted = true;
        String problemDesc = Problem_grp.getText();
        if (problemDesc.isBlank()) {
            toggleError(problemGrpError, true);
            return;
        }
        toggleError(problemGrpError, false);
        try {
            ProblemSolution p = new ProblemSolution();
            p.setProblemDescription(problemDesc);
            int newId = serviceProblem.addProblem(p, act_id);
            if (newId != -1) {
                p.setId(newId);
                Problem_grp.clear();
                problemSubmitted = false;
                if (problemCombo.getItems() == null) {
                    problemCombo.setItems(FXCollections.observableArrayList());
                }
                problemCombo.getItems().add(p);
                refreshProblemSolutionCards(act_id);
            }
        } catch (Exception e) {
            System.err.println("Failed to save problem: " + e.getMessage());
        }
    }

    @FXML
    private void OnSolution() {
        solutionSubmitted = true;
        ProblemSolution problem_selected = problemCombo.getSelectionModel().getSelectedItem();
        String sol = solution_desc.getText();

        boolean isValid = true;
        if (problem_selected == null) {
            toggleError(solutionComboError, true);
            isValid = false;
        } else {
            toggleError(solutionComboError, false);
        }
        if (sol.isBlank()) {
            toggleError(solutionDescError, true);
            isValid = false;
        } else {
            toggleError(solutionDescError, false);
        }

        if (!isValid) return;

        try {
            problem_selected.setGroupSolution(sol);
            serviceProblem.addSolutionGrp(problem_selected);
            problemCombo.getItems().remove(problem_selected);
            solution_desc.clear();
            problemCombo.getSelectionModel().clearSelection();
            solutionSubmitted = false;
            loadProblems();
            refreshProblemSolutionCards(act_id);
        } catch (Exception e) {
            System.err.println("Failed to save solution: " + e.getMessage());
        }
    }

    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            fileNameLabel.setText(selectedPdf.getName());
            toggleError(fileError, false);
        }
    }

    @FXML
    public void onSubmission() {
        finalSubmitted = true;
        if (selectedPdf == null) {
            toggleError(fileError, true);
            return;
        }
        toggleError(fileError, false);
        try {
            Activity submission = new Activity();
            Evaluation e = new Evaluation();
            submission.setId(this.act_id);
            Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "activity_pdf");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(selectedPdf.getName());
            Files.copy(selectedPdf.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            submission.setSubmissionFile("challenge_module/activity_pdf/" + selectedPdf.getName());
            serviceActivity.submissionfile(submission);
            e.setPreFeedback(FlowiseGraderUtil.gradeFromPdfPaths(c.getContent(), "challenge_module/activity_pdf/" + selectedPdf.getName()).toString());
            serviceEva.CreatePreFeedback(e, act_id);

            BaseController.getInstance().loadActivity();
        } catch (Exception e) {
            System.err.println("Échec de la soumission: " + e.getMessage());
        }
    }

    public void initMemberActivityCards(int activityId) {
        refreshMemberCards(activityId);
    }

    private void refreshMemberCards(int activityId) {
        int user_id = 2;
        List<MemberActivity> list = serviceMember.display(activityId, user_id);
        if (memberEmptyLabel != null) {
            boolean empty = list == null || list.isEmpty();
            memberEmptyLabel.setVisible(empty);
            memberEmptyLabel.setManaged(empty);
        }
        if (memberActivityCards == null) return;
        memberActivityCards.getChildren().clear();
        if (list == null) return;
        for (MemberActivity memberActivity : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/MemberActivityCard.fxml"));
                Parent card = loader.load();
                MemberActivityCardController ctrl = loader.getController();
                ctrl.setData(memberActivity, this::handleEditMember, this::handleDeleteMember);
                memberActivityCards.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Failed to load member activity card: " + e.getMessage());
            }
        }
    }

    private void handleEditMember(MemberActivity selected) {
        if (selected == null || editMemberOverlayHost == null) return;
        if (editingMemberActivity != null && editingMemberActivity.getId().equals(selected.getId()) && editMemberOverlayHost.isVisible()) {
            hideEditMemberOverlay();
            return;
        }
        editingMemberActivity = selected;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/EditMemberActivity.fxml"));
            Parent content = loader.load();
            if (content instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
            EditMemberActivityController overlayController = loader.getController();
            overlayController.setData(selected);
            overlayController.setOnSave(updated -> {
                try {
                    serviceMember.update(updated);
                    refreshMemberCards(act_id);
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
            if (editingMemberActivity != null && editingMemberActivity.getId().equals(selected.getId()))
                hideEditMemberOverlay();
            refreshMemberCards(act_id);
            descriptionVisibility();
        } catch (Exception e) {
            System.err.println("Failed to delete member activity: " + e.getMessage());
        }
    }

    private void loadProblems() {
        List<ProblemSolution> problems = serviceProblem.displayProblems(act_id);
        ObservableList<ProblemSolution> observableList = FXCollections.observableArrayList(problems);
        problemCombo.setItems(observableList);
        problemCombo.setPromptText("Sélectionnez un problème");
        solutionContainer.visibleProperty().bind(Bindings.isNotEmpty(problemCombo.getItems()));
        solutionContainer.managedProperty().bind(solutionContainer.visibleProperty());
    }

    public boolean UserPermissions() {
        int user_id = 2;
        boolean leader = serviceActivity.isUserLeader(this.grp_id, user_id);
        submissionContainer.setVisible(leader);
        submissionContainer.setManaged(leader);
        gitHub.setVisible(leader);
        gitHub.setManaged(leader);
        return leader;
    }

    public void initProblemSolutionCards(int activityId) {
        refreshProblemSolutionCards(activityId);
    }

    private void refreshProblemSolutionCards(int activityId) {
        List<ProblemSolution> list = serviceProblem.display(activityId);
        if (psEmptyLabel != null) {
            boolean empty = list == null || list.isEmpty();
            psEmptyLabel.setVisible(empty);
            psEmptyLabel.setManaged(empty);
        }
        if (problemSolutionCards == null) return;
        problemSolutionCards.getChildren().clear();
        if (list == null) return;
        for (ProblemSolution ps : list) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/ProblemSolutionCard.fxml"));
                Parent card = loader.load();
                ProblemSolutionCardController ctrl = loader.getController();
                ctrl.setData(ps, this::handleEditAction, this::handleDeleteAction);
                problemSolutionCards.getChildren().add(card);
            } catch (IOException e) {
                System.err.println("Failed to load problem/solution card: " + e.getMessage());
            }
        }
    }

    private void handleEditAction(ProblemSolution selected) {
        if (selected == null || editOverlayHost == null) return;
        if (editingProblemSolution != null && editingProblemSolution.getId() == selected.getId() && editOverlayHost.isVisible()) {
            hideEditOverlay();
            return;
        }
        editingProblemSolution = selected;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/EditProblemActivity.fxml"));
            Parent content = loader.load();
            if (content instanceof Region region) region.setMaxWidth(Double.MAX_VALUE);
            EditProblemSolutionController overlayController = loader.getController();
            overlayController.setData(selected);
            overlayController.setOnSave(updated -> {
                try {
                    serviceProblem.update(updated);
                    refreshProblemSolutionCards(act_id);
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
            if (editingProblemSolution != null && editingProblemSolution.getId() == selected.getId()) hideEditOverlay();
            refreshProblemSolutionCards(act_id);
            loadProblems();
        } catch (Exception e) {
            System.err.println("Failed to delete problem/solution: " + e.getMessage());
        }
    }

    public void descriptionVisibility() {
        int user_id = 2;
        boolean rs = serviceMember.findDescription(act_id, user_id);
        DescriptionContainer.setVisible(!rs);
        DescriptionContainer.setManaged(!rs);
    }

    public void loadGitMembers() {
        membersGitUserName.getChildren().clear();
        List<User> users = serviceMS.getAllGroupMembersForGit(grp_id);
        for (User u : users) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/Activity/MemberGitCard.fxml"));
                VBox card = loader.load();
                MemberGitCardController controller = loader.getController();
                controller.initData(u, () -> {});
                membersGitUserName.getChildren().add(card);

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
    public void onCreateRepo(){
        String supervisor_git = serviceActivity.getSupervisorGitUsername(act_id);
        List<User> users = serviceMS.getAllGroupMembersForGit(grp_id);
        if (supervisor_git == null || supervisor_git.isBlank()) {
            showGithubAlert(Alert.AlertType.ERROR, "Missing supervisor GitHub",
                    "The supervisor does not have a GitHub username yet.");
            return;
        }

        List<String> git_usernames = new ArrayList<>();
        List<String> missingMembers = new ArrayList<>();
        for (User u : users) {
            String gitUsername = u.getGit_username();
            if (gitUsername == null || gitUsername.isBlank()) {
                missingMembers.add(u.getDisplayName());
                continue;
            }
            if (!gitUsername.equalsIgnoreCase(supervisor_git) && !git_usernames.contains(gitUsername)) {
                git_usernames.add(gitUsername);
            }
        }

        if (!missingMembers.isEmpty()) {
            showGithubAlert(Alert.AlertType.ERROR, "Missing member GitHub usernames",
                    "Please add GitHub usernames for: " + String.join(", ", missingMembers));
            return;
        }

        String repoName = buildRepositoryName();
        try {
            GithubUtil githubUtil = new GithubUtil();
            String repoUrl = githubUtil.setupRepository(repoName, supervisor_git, git_usernames);
            serviceActivity.markRepoCreated(act_id);
            if (currentActivity != null) {
                currentActivity.setRepo_created(1);
            }
            gitHub.setManaged(false);
            gitHub.setVisible(false);
            showGithubAlert(Alert.AlertType.INFORMATION, "Repository created",
                    "Repository created successfully:\n" + repoUrl);
        } catch (Exception e) {
            showGithubAlert(Alert.AlertType.ERROR, "GitHub creation failed",
                    e.getMessage() != null ? e.getMessage() : "Unable to create the repository.");
        }

    }

    private String buildRepositoryName() {
        String groupName = (currentActivity != null && currentActivity.getGroup() != null
                && currentActivity.getGroup().getName() != null && !currentActivity.getGroup().getName().isBlank())
                ? currentActivity.getGroup().getName()
                : "group-" + grp_id;
        String challengeTitle = (c != null && c.getTitle() != null && !c.getTitle().isBlank())
                ? c.getTitle()
                : "challenge";
        String base = groupName + "-" + challengeTitle;
        String sanitized = base.toLowerCase()
                .replaceAll("[^a-z0-9._-]+", "-")
                .replaceAll("^-+|-+$", "");
        if (sanitized.isBlank()) {
            sanitized = "challenge";
        }
        return sanitized;
    }

    private void showGithubAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
