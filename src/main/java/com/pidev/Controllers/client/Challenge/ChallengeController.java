package com.pidev.Controllers.client.Challenge;

import com.pidev.Services.Challenge.Classes.ServiceActivity;
import com.pidev.Services.Challenge.Classes.ServiceChallenge;
import com.pidev.models.Challenge;
import com.pidev.utils.flowiseSuggestChallengeInputs;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.scene.control.OverrunStyle;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import org.json.JSONObject;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

public class ChallengeController implements Initializable {
    private static final int user_id = 1;
    private static final String DAY_CARD_STYLE = "-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-radius: 18; -fx-background-radius: 18; -fx-padding: 12;";
    private static final String DAY_NUMBER_STYLE = "-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #0f172a;";
    private static final String TODAY_BADGE_STYLE = "-fx-background-color: #3559e0; -fx-background-radius: 999; -fx-padding: 4 10 4 10; -fx-text-fill: white; -fx-font-size: 10; -fx-font-weight: bold;";
    private static final String DAY_ENTRY_STYLE = "-fx-background-radius: 10; -fx-padding: 6 8 6 8;";
    private static final DateTimeFormatter WEEK_RANGE_FORMATTER = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH);
    private static final DateTimeFormatter WEEK_RANGE_END_FORMATTER = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    @FXML
    private VBox myChallengesPane;
    @FXML
    private VBox createChallengePane;
    @FXML
    private VBox calendarPane;
    @FXML
    private Button myChallengesBtn;
    @FXML
    private Button createChallengeBtn;
    @FXML
    private Button calendarBtn;
    @FXML
    private GridPane calendarGrid;
    @FXML
    private Label monthLabel;
    @FXML
    private TextField TitleInput;
    @FXML
    private TextField TargetSkillInput;
    @FXML
    private ComboBox<String> DifficultyCombo;
    @FXML
    private TextField MinGroupNbrInput;
    @FXML
    private TextField MaxGroupNbrInput;
    @FXML
    private DatePicker DeadlineInput;
    @FXML
    private TextArea DescriptionInput;
    @FXML
    private Label fileInput;
    @FXML
    private VBox challengeListContainer;
    @FXML
    private Label TitleError;
    @FXML
    private Label TargetSkillError;
    @FXML
    private Label DifficultyError;
    @FXML
    private Label GroupError;
    @FXML
    private Label DeadlineError;
    @FXML
    private Label DescriptionError;
    @FXML
    private Label FileError;
    @FXML
    private Button GenerateBtn;
    @FXML ComboBox<String> GithubRequiredCombo;

    private File selectedPdf;
    private final ServiceChallenge service = new ServiceChallenge();
    private final ServiceActivity activityService = new ServiceActivity();
    private LocalDate currentWeekStart;
    private List<Challenge> calendarChallenges = new ArrayList<>();
    private List<ServiceActivity.CalendarActivityData> calendarActivities = new ArrayList<>();

    @FXML
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        ObservableList<String> options = FXCollections.observableArrayList("Easy", "Medium", "Hard");
        DifficultyCombo.setItems(options);
        ObservableList<String> options_git = FXCollections.observableArrayList("Yes","No");
        GithubRequiredCombo.setValue("No");
        GithubRequiredCombo.setItems(options_git);
        TitleInput.textProperty().addListener((obs, old, val) -> toggleError(TitleError, val.isBlank()));
        TargetSkillInput.textProperty().addListener((obs, old, val) -> toggleError(TargetSkillError, val.isBlank()));
        DescriptionInput.textProperty().addListener((obs, old, val) -> toggleError(DescriptionError, val.isBlank()));
        DifficultyCombo.valueProperty().addListener((obs, old, val) -> toggleError(DifficultyError, val == null));
        DeadlineInput.valueProperty().addListener((obs, old, val) -> toggleError(DeadlineError, val == null));
        refreshChallenges();
        showMyChallengesView();
        GenerateBtn.setVisible(false);
        GenerateBtn.setManaged(false);

        currentWeekStart = LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        refreshCalendarData();
        generateCalendar(currentWeekStart);
    }

    @FXML
    public void onShowMyChallenges() {
        refreshChallenges();
        showMyChallengesView();
    }

    @FXML
    public void onShowCreateChallenge() {
        hideAllErrors();
        showCreateChallengeView();
    }

    @FXML
    public void onShowCalendar() {
        showCalendarView();
    }

    private void showMyChallengesView() {
        setViewVisible(myChallengesPane, true);
        setViewVisible(createChallengePane, false);
        setViewVisible(calendarPane, false);
        setActiveSwitcher(myChallengesBtn, createChallengeBtn, calendarBtn);
    }

    private void showCreateChallengeView() {
        setViewVisible(myChallengesPane, false);
        setViewVisible(createChallengePane, true);
        setViewVisible(calendarPane, false);
        setActiveSwitcher(createChallengeBtn, myChallengesBtn, calendarBtn);
    }

    private void showCalendarView() {
        setViewVisible(myChallengesPane, false);
        setViewVisible(createChallengePane, false);
        setViewVisible(calendarPane, true);
        setActiveSwitcher(calendarBtn, myChallengesBtn, createChallengeBtn);
        refreshCalendarData();
        generateCalendar(currentWeekStart);
    }

    private void setViewVisible(VBox view, boolean visible) {
        if (view == null) {
            return;
        }
        view.setVisible(visible);
        view.setManaged(visible);
    }

    private void setActiveSwitcher(Button active, Button... inactiveButtons) {
        if (active != null && !active.getStyleClass().contains("switcher-btn-active")) {
            active.getStyleClass().add("switcher-btn-active");
        }
        if (inactiveButtons == null) {
            return;
        }
        for (Button inactive : inactiveButtons) {
            if (inactive != null) {
                inactive.getStyleClass().remove("switcher-btn-active");
            }
        }
    }

    private void showSuccessAlert() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText("Challenge created");
        alert.setContentText("Challenge created successfully!");
        alert.setGraphic(null);

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

        alert.showAndWait();
    }

    private void showErrorAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("Challenge not created");
        alert.setContentText(message);
        alert.setGraphic(null);

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

        alert.showAndWait();
    }

    private void clearForm() {
        TitleInput.clear();
        TargetSkillInput.clear();
        DescriptionInput.clear();
        MinGroupNbrInput.setText("0");
        MaxGroupNbrInput.setText("0");
        DeadlineInput.setValue(null);
        DifficultyCombo.getSelectionModel().clearSelection();
        fileInput.setText("Aucun fichier n'a ete selectionne");
        selectedPdf = null;
    }

    private int parseIntRequired(TextField input, String fieldName) {
        String raw = input.getText();
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " must be a valid integer.");
        }
    }

    @FXML
    public void onChooseFile() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        selectedPdf = fileChooser.showOpenDialog(null);
        if (selectedPdf != null) {
            fileInput.setText(selectedPdf.getName());
            GenerateBtn.setVisible(true);
            GenerateBtn.setManaged(true);
        }
    }

    @FXML
    private void onStaticAction() {
    }

    @FXML
    public void onCreateChallenge() {
        boolean isValid = true;

        if (TitleInput.getText().isBlank()) {
            toggleError(TitleError, true);
            isValid = false;
        }
        if (TargetSkillInput.getText().isBlank()) {
            toggleError(TargetSkillError, true);
            isValid = false;
        }
        if (DescriptionInput.getText().isBlank()) {
            toggleError(DescriptionError, true);
            isValid = false;
        }

        if (DifficultyCombo.getValue() == null) {
            toggleError(DifficultyError, true);
            isValid = false;
        }
        if (DeadlineInput.getValue() == null) {
            DeadlineError.setText("Deadline is required");
            toggleError(DeadlineError, true);
            isValid = false;
        }

        try {
            int min = Integer.parseInt(MinGroupNbrInput.getText());
            int max = Integer.parseInt(MaxGroupNbrInput.getText());
            if (min < 0 || max < 0 || min > max) {
                GroupError.setText("Min must be positive and less than Max");
                toggleError(GroupError, true);
                isValid = false;
            } else {
                toggleError(GroupError, false);
            }
        } catch (NumberFormatException e) {
            GroupError.setText("Please enter valid numbers");
            toggleError(GroupError, true);
            isValid = false;
        }

        if (selectedPdf == null) {
            toggleError(FileError, true);
            isValid = false;
        } else {
            toggleError(FileError, false);
        }

        if (!isValid) {
            return;
        }

        try {
            Challenge c = new Challenge();
            int isGithubRequired = GithubRequiredCombo.getValue().equals("Yes") ? 1 : 0;
            c.setGithub(isGithubRequired);
            c.setTitle(TitleInput.getText().trim());
            c.setTargetSkill(TargetSkillInput.getText().trim());
            c.setDifficulty(DifficultyCombo.getValue());
            c.setMinGroupNbr(Integer.parseInt(MinGroupNbrInput.getText()));
            c.setMaxGroupNbr(Integer.parseInt(MaxGroupNbrInput.getText()));
            c.setDescription(DescriptionInput.getText());
            c.setDeadLine(DeadlineInput.getValue().atStartOfDay());
            c.setCreatedAt(LocalDateTime.now());

            Path destDir = Paths.get(System.getProperty("user.dir"), "src", "main", "resources", "challenge_module", "challenge_pdf");
            Files.createDirectories(destDir);
            Path destFile = destDir.resolve(selectedPdf.getName());
            Files.copy(selectedPdf.toPath(), destFile, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            c.setContent("challenge_module/challenge_pdf/" + selectedPdf.getName());

            service.add(c);
            refreshChallenges();
            refreshCalendarData();
            generateCalendar(currentWeekStart);
            showSuccessAlert();
            clearForm();
            hideAllErrors();
            showMyChallengesView();
        } catch (Exception e) {
            showErrorAlert("Could not save challenge: " + e.getMessage());
        }
    }

    private void hideAllErrors() {
        toggleError(TitleError, false);
        toggleError(TargetSkillError, false);
        toggleError(DifficultyError, false);
        toggleError(GroupError, false);
        toggleError(DeadlineError, false);
        toggleError(DescriptionError, false);
        toggleError(FileError, false);
    }

    private void refreshChallenges() {
        challengeListContainer.getChildren().clear();
        List<Challenge> challenges = service.displayForSupervisor(user_id);
        if (challenges == null || challenges.isEmpty()) {
            Label empty = new Label("No challenges yet");
            challengeListContainer.getChildren().add(empty);
            return;
        }

        boolean loadError = false;
        for (Challenge c : challenges) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/Challenge/ChallengeCard.fxml"));
                VBox card = loader.load();
                ChallengeCardController cardController = loader.getController();
                cardController.setData(c, v -> refreshChallenges());
                cardController.SupervisorCard(cardController.participationBtn);
                cardController.SupervisorCard(cardController.groupsBtn);

                challengeListContainer.getChildren().add(card);
            } catch (Exception e) {
                loadError = true;
                e.printStackTrace();
            }
        }

        if (challengeListContainer.getChildren().isEmpty()) {
            Label fallback = new Label(loadError ? "Could not display challenges." : "No challenges yet");
            challengeListContainer.getChildren().add(fallback);
        }
    }

    private void refreshCalendarData() {
        try {
            calendarChallenges = service.displayForSupervisor(user_id);
            calendarActivities = activityService.getCalendarActivitiesForSupervisor(user_id);
        } catch (Exception e) {
            calendarChallenges = new ArrayList<>();
            calendarActivities = new ArrayList<>();
        }
    }

    @FXML
    public void previousWeek() {
        currentWeekStart = currentWeekStart.minusWeeks(1);
        generateCalendar(currentWeekStart);
    }

    @FXML
    public void nextWeek() {
        currentWeekStart = currentWeekStart.plusWeeks(1);
        generateCalendar(currentWeekStart);
    }

    public void generateCalendar(LocalDate weekStart) {
        if (calendarGrid == null || monthLabel == null) {
            return;
        }

        currentWeekStart = weekStart.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        monthLabel.setText(formatWeekLabel(currentWeekStart));
        calendarGrid.getChildren().removeIf(node -> {
            Integer rowIndex = GridPane.getRowIndex(node);
            return (rowIndex == null ? 0 : rowIndex) > 0;
        });

        for (int column = 0; column < 7; column++) {
            LocalDate date = currentWeekStart.plusDays(column);
            VBox dayCell = createDayCell(date);
            calendarGrid.add(dayCell, column, 1);
        }
    }

    private String formatWeekLabel(LocalDate weekStart) {
        LocalDate weekEnd = weekStart.plusDays(6);
        if (weekStart.getMonth() == weekEnd.getMonth() && weekStart.getYear() == weekEnd.getYear()) {
            return weekStart.format(WEEK_RANGE_FORMATTER) + " - " + weekEnd.format(DateTimeFormatter.ofPattern("d, yyyy", Locale.ENGLISH));
        }
        return weekStart.format(WEEK_RANGE_FORMATTER) + " - " + weekEnd.format(WEEK_RANGE_END_FORMATTER);
    }

    public List<CalendarStatus> getStatusesForDate(LocalDate date) {
        List<CalendarStatus> statuses = new ArrayList<>();
        for (DayCalendarEntry entry : getEntriesForDate(date)) {
            for (int i = 0; i < entry.count(); i++) {
                statuses.add(entry.status());
            }
        }
        return statuses;
    }

    private List<DayCalendarEntry> getEntriesForDate(LocalDate date) {
        Map<String, DayCalendarEntryAccumulator> entries = new LinkedHashMap<>();

        for (Challenge challenge : calendarChallenges) {
            if (challenge.getCreatedAt() != null && challenge.getCreatedAt().toLocalDate().equals(date)) {
                mergeEntry(entries, challenge.getTitle(), CalendarStatus.PUBLISHED);
            }
            if (challenge.getDeadLine() != null && challenge.getDeadLine().toLocalDate().equals(date)) {
                mergeEntry(entries, challenge.getTitle(), CalendarStatus.DEADLINE);
            }
        }

        for (ServiceActivity.CalendarActivityData activity : calendarActivities) {
            if (activity.getStartTime() != null
                    && activity.getStartTime().toLocalDate().equals(date)
                    && "in_progress".equalsIgnoreCase(activity.getStatus())) {
                mergeEntry(entries, activity.getChallengeTitle(), activity.getGroupName(), CalendarStatus.IN_PROGRESS);
            }

            LocalDate evaluationDisplayDate = resolveEvaluationDisplayDate(activity);
            if (evaluationDisplayDate != null && evaluationDisplayDate.equals(date) && isEvaluationFinished(activity)) {
                mergeEntry(entries, activity.getChallengeTitle(), activity.getGroupName(), CalendarStatus.EVALUATED);
                continue;
            }

            if (activity.getSubmissionDate() == null || !activity.getSubmissionDate().toLocalDate().equals(date)) {
                continue;
            }

            if (isNotEvaluatedAfterTwoDays(activity)) {
                mergeEntry(entries, activity.getChallengeTitle(), activity.getGroupName(), CalendarStatus.NOT_EVALUATED);
            } else if (!isEvaluationFinished(activity)) {
                mergeEntry(entries, activity.getChallengeTitle(), activity.getGroupName(), CalendarStatus.SUBMITTED);
            }
        }

        List<DayCalendarEntry> dayEntries = new ArrayList<>();
        for (DayCalendarEntryAccumulator value : entries.values()) {
            dayEntries.add(new DayCalendarEntry(value.title(), value.groupName(), value.status(), value.count()));
        }
        return dayEntries;
    }

    private boolean isNotEvaluatedAfterTwoDays(ServiceActivity.CalendarActivityData activity) {
        if (activity.getSubmissionDate() == null || isEvaluationFinished(activity)) {
            return false;
        }
        LocalDate overdueDate = activity.getSubmissionDate().toLocalDate().plusDays(2);
        return !LocalDate.now().isBefore(overdueDate);
    }

    private boolean isEvaluationFinished(ServiceActivity.CalendarActivityData activity) {
        return activity != null && activity.isEvaluationFinished();
    }

    private LocalDate resolveEvaluationDisplayDate(ServiceActivity.CalendarActivityData activity) {
        if (activity == null || activity.getSubmissionDate() == null) {
            return null;
        }

        return activity.getSubmissionDate().toLocalDate();
    }

    private VBox createDayCell(LocalDate date) {
        VBox dayBox = new VBox(8);
        dayBox.setPrefHeight(145);
        dayBox.setMaxWidth(Double.MAX_VALUE);
        dayBox.setStyle(DAY_CARD_STYLE);

        HBox header = new HBox(8);
        Label dayNumber = new Label(String.valueOf(date.getDayOfMonth()));
        dayNumber.setStyle(DAY_NUMBER_STYLE);
        header.getChildren().add(dayNumber);

        if (LocalDate.now().equals(date)) {
            Label todayLabel = new Label("Today");
            todayLabel.setStyle(TODAY_BADGE_STYLE);
            header.getChildren().add(todayLabel);
        }

        VBox entriesBox = new VBox(6);
        List<DayCalendarEntry> entries = getEntriesForDate(date);
        for (DayCalendarEntry entry : entries) {
            entriesBox.getChildren().add(createEntryLabel(entry));
        }

        dayBox.getChildren().addAll(header, entriesBox);
        GridPane.setHgrow(dayBox, Priority.ALWAYS);
        return dayBox;
    }

    private VBox createEntryLabel(DayCalendarEntry entry) {
        String suffix = entry.count() > 1 ? " (" + entry.count() + ")" : "";
        VBox entryBox = new VBox(2);
        entryBox.setMaxWidth(Double.MAX_VALUE);
        entryBox.setStyle(DAY_ENTRY_STYLE + entry.status().badgeStyle());

        Label titleLabel = new Label(buildEntryText(entry) + suffix);
        titleLabel.setMaxWidth(Double.MAX_VALUE);
        titleLabel.setWrapText(true);
        titleLabel.setTextOverrun(OverrunStyle.CLIP);
        titleLabel.setMinHeight(Region.USE_PREF_SIZE);
        titleLabel.setStyle("-fx-font-size: 11; " + entry.status().textStyle());
        entryBox.getChildren().add(titleLabel);

        if (shouldShowGroupName(entry)) {
            Label groupLabel = new Label("Group : " + entry.groupName());
            groupLabel.setMaxWidth(Double.MAX_VALUE);
            groupLabel.setWrapText(true);
            groupLabel.setTextOverrun(OverrunStyle.CLIP);
            groupLabel.setMinHeight(Region.USE_PREF_SIZE);
            groupLabel.setStyle("-fx-font-size: 10; " + entry.status().textStyle());
            entryBox.getChildren().add(groupLabel);
        }

        return entryBox;
    }

    private String buildEntryText(DayCalendarEntry entry) {
        if (entry.status() == CalendarStatus.DEADLINE) {
            return safeTitle(entry.title());
        }
        return safeTitle(entry.title());
    }

    private String safeTitle(String title) {
        if (title == null || title.isBlank()) {
            return "Challenge";
        }
        return title.trim();
    }

    private boolean shouldShowGroupName(DayCalendarEntry entry) {
        return (entry.status() == CalendarStatus.IN_PROGRESS
                || entry.status() == CalendarStatus.SUBMITTED
                || entry.status() == CalendarStatus.EVALUATED)
                && entry.groupName() != null
                && !entry.groupName().isBlank();
    }

    private void mergeEntry(Map<String, DayCalendarEntryAccumulator> entries, String title, CalendarStatus status) {
        mergeEntry(entries, title, null, status);
    }

    private void mergeEntry(Map<String, DayCalendarEntryAccumulator> entries, String title, String groupName, CalendarStatus status) {
        String safeTitle = (title == null || title.isBlank()) ? "Challenge" : title.trim();
        String safeGroupName = (groupName == null || groupName.isBlank()) ? null : groupName.trim();
        String key = status.name() + "::" + safeTitle + "::" + (safeGroupName == null ? "" : safeGroupName);
        DayCalendarEntryAccumulator existing = entries.get(key);
        if (existing == null) {
            entries.put(key, new DayCalendarEntryAccumulator(safeTitle, safeGroupName, status, 1));
        } else {
            existing.increment();
        }
    }

    private DayStatusCounts buildDayStatusCounts(LocalDate date) {
        List<CalendarStatus> statuses = getStatusesForDate(date);
        int published = 0;
        int inProgress = 0;
        int submitted = 0;
        int evaluated = 0;
        int notEvaluated = 0;

        for (CalendarStatus status : statuses) {
            switch (status) {
                case PUBLISHED -> published++;
                case IN_PROGRESS -> inProgress++;
                case SUBMITTED -> submitted++;
                case EVALUATED -> evaluated++;
                case NOT_EVALUATED -> notEvaluated++;
                case DEADLINE -> notEvaluated++;
            }
        }

        EnumSet<CalendarStatus> visibleStatuses = EnumSet.noneOf(CalendarStatus.class);
        if (published > 0) {
            visibleStatuses.add(CalendarStatus.PUBLISHED);
        }
        if (inProgress > 0) {
            visibleStatuses.add(CalendarStatus.IN_PROGRESS);
        }
        if (submitted > 0) {
            visibleStatuses.add(CalendarStatus.SUBMITTED);
        }
        if (evaluated > 0) {
            visibleStatuses.add(CalendarStatus.EVALUATED);
        }
        if (notEvaluated > 0) {
            visibleStatuses.add(CalendarStatus.NOT_EVALUATED);
        }

        return new DayStatusCounts(published, inProgress, submitted, evaluated, notEvaluated, visibleStatuses);
    }

    private void toggleError(Label label, boolean show) {
        label.setVisible(show);
        label.setManaged(show);
    }

    @FXML
    private void onGenerateInputs() {
        if (selectedPdf == null) {
            toggleError(FileError, true);
            return;
        }
        GenerateBtn.setDisable(true);
        String originalText = GenerateBtn.getText();
        GenerateBtn.setText("Generating...");
        hideAllErrors();

        Task<JSONObject> task = new Task<>() {
            @Override
            protected JSONObject call() throws Exception {
                return flowiseSuggestChallengeInputs.suggestChallenge(selectedPdf.getAbsolutePath());
            }
        };

        task.setOnSucceeded(e -> {
            JSONObject result = task.getValue();
            GenerateBtn.setDisable(false);
            GenerateBtn.setText(originalText);

            System.out.println("Flowise Raw Result: " + result.toString());

            if (result.has("error") && result.getBoolean("error")) {
                showErrorAlert("AI Generation failed: " + result.optString("message", "Unknown error"));
            } else {
                populateAIGeneratedFields(result);
            }
        });

        task.setOnFailed(e -> {
            GenerateBtn.setDisable(false);
            GenerateBtn.setText(originalText);
            showErrorAlert("Request failed. Please check your connection or Flowise server.");
        });

        new Thread(task).start();
    }

    private void populateAIGeneratedFields(JSONObject data) {
        Platform.runLater(() -> {
            TitleInput.setText(data.optString("title"));
            TargetSkillInput.setText(data.optString("target_skill"));
            DescriptionInput.setText(data.optString("description"));

            MinGroupNbrInput.setText(String.valueOf(data.optInt("min_group_nbr", 2)));
            MaxGroupNbrInput.setText(String.valueOf(data.optInt("max_group_nbr", 4)));

            String diff = data.optString("difficulty");
            if (diff != null && !diff.isEmpty()) {
                DifficultyCombo.setValue(diff);
            }

            if (data.has("dead_line")) {
                int days = data.getInt("dead_line");
                DeadlineInput.setValue(LocalDate.now().plusDays(days));
            }
        });
    }

    public enum CalendarStatus {
        PUBLISHED("#3559e0", "-fx-background-color: #dbeafe;", "-fx-text-fill: #1d4ed8;"),
        IN_PROGRESS("#f59e0b", "-fx-background-color: #fef3c7;", "-fx-text-fill: #b45309;"),
        SUBMITTED("#16a34a", "-fx-background-color: #dcfce7;", "-fx-text-fill: #15803d;"),
        EVALUATED("#06b6d4", "-fx-background-color: #cffafe;", "-fx-text-fill: #0e7490;"),
        NOT_EVALUATED("#ef4444", "-fx-background-color: #fee2e2;", "-fx-text-fill: #b91c1c;"),
        DEADLINE("#dc2626", "-fx-background-color: #fee2e2;", "-fx-text-fill: #b91c1c;");

        private final String color;
        private final String badgeStyle;
        private final String textStyle;

        CalendarStatus(String color, String badgeStyle, String textStyle) {
            this.color = color;
            this.badgeStyle = badgeStyle;
            this.textStyle = textStyle;
        }

        public String color() {
            return color;
        }

        public String badgeStyle() {
            return badgeStyle;
        }

        public String textStyle() {
            return textStyle;
        }
    }

    private record DayStatusCounts(
            int published,
            int inProgress,
            int submitted,
            int evaluated,
            int notEvaluated,
            EnumSet<CalendarStatus> visibleStatuses
    ) {
    }

    private record DayCalendarEntry(String title, String groupName, CalendarStatus status, int count) {
    }

    private static final class DayCalendarEntryAccumulator {
        private final String title;
        private final String groupName;
        private final CalendarStatus status;
        private int count;

        private DayCalendarEntryAccumulator(String title, String groupName, CalendarStatus status, int count) {
            this.title = title;
            this.groupName = groupName;
            this.status = status;
            this.count = count;
        }

        private String title() {
            return title;
        }

        private CalendarStatus status() {
            return status;
        }

        private String groupName() {
            return groupName;
        }

        private int count() {
            return count;
        }

        private void increment() {
            count++;
        }
    }
}
