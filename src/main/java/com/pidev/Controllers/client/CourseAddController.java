package com.pidev.Controllers.client;

import com.pidev.Services.AdminLookupService;
import com.pidev.Services.CourseService;
import com.pidev.models.Course;
import com.pidev.models.Quiz;
import com.pidev.models.User;
import com.pidev.utils.DurationUtils;
import com.pidev.utils.SessionManager;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.stage.FileChooser;

import java.io.File;
import java.sql.SQLException;
import java.util.List;

public class CourseAddController {

    @FXML private Label supervisorNameLabel;
    @FXML private Label supervisorValueLabel;
    @FXML private Label statusMessageLabel;
    @FXML private TextField titleField;
    @FXML private TextArea descriptionArea;
    @FXML private Spinner<Integer> daysSpinner;
    @FXML private Spinner<Integer> hoursSpinner;
    @FXML private Spinner<Integer> minutesSpinner;
    @FXML private ComboBox<String> difficultyCombo;
    @FXML private TextField scoreField;
    @FXML private TextField contentPdfField;
    @FXML private TextField materialField;
    @FXML private ComboBox<Quiz> prerequisiteQuizCombo;
    @FXML private Label titleErrorLabel;
    @FXML private Label descriptionErrorLabel;
    @FXML private Label durationErrorLabel;
    @FXML private Label difficultyErrorLabel;
    @FXML private Label scoreErrorLabel;
    @FXML private Label contentErrorLabel;
    @FXML private Label materialErrorLabel;

    private final CourseService courseService = new CourseService();
    private final AdminLookupService lookupService = new AdminLookupService();
    private User currentSupervisor;

    @FXML
    public void initialize() {
        currentSupervisor = SessionManager.getInstance().getUser();
        if (currentSupervisor == null || currentSupervisor.getRole() != User.Role.SUPERVISEUR) {
            showAlert(Alert.AlertType.WARNING, "Access denied", "Only supervisors can add courses from this page.");
            goBackToCourses();
            return;
        }

        configureSpinner(daysSpinner, 0, 365, 0);
        configureSpinner(hoursSpinner, 0, 23, 0);
        configureSpinner(minutesSpinner, 0, 59, 0);

        difficultyCombo.setItems(FXCollections.observableArrayList(
                Course.DIFFICULTY_BEGINNER,
                Course.DIFFICULTY_INTERMEDIATE,
                Course.DIFFICULTY_ADVANCED
        ));
        difficultyCombo.setValue(Course.DIFFICULTY_BEGINNER);

        prerequisiteQuizCombo.setPromptText("Optional prerequisite quiz");
        loadQuizzes();

        String displayName = currentSupervisor.getDisplayName();
        if (displayName == null || displayName.isBlank()) {
            displayName = currentSupervisor.getEmail() == null ? "Supervisor" : currentSupervisor.getEmail();
        }
        supervisorNameLabel.setText(displayName);
        supervisorValueLabel.setText(displayName);
        scoreField.setText("0");
    }

    @FXML
    private void onChoosePdf() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select course PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF files", "*.pdf"));

        String current = contentPdfField.getText() == null ? "" : contentPdfField.getText().trim();
        if (!current.isEmpty()) {
            File currentFile = new File(current);
            File baseDir = currentFile.isDirectory() ? currentFile : currentFile.getParentFile();
            if (baseDir != null && baseDir.exists()) {
                chooser.setInitialDirectory(baseDir);
            }
        }

        File selected = chooser.showOpenDialog(contentPdfField.getScene() == null ? null : contentPdfField.getScene().getWindow());
        if (selected != null) {
            contentPdfField.setText(selected.getAbsolutePath());
            statusMessageLabel.setText("PDF selected: " + selected.getName());
        }
    }

    @FXML
    private void onSubmit() {
        clearErrors();

        String title = safeText(titleField);
        String description = safeText(descriptionArea);
        String contentPdf = safeText(contentPdfField);
        String material = safeText(materialField);

        int days = readSpinnerValue(daysSpinner);
        int hours = readSpinnerValue(hoursSpinner);
        int minutes = readSpinnerValue(minutesSpinner);

        boolean valid = true;

        if (title.length() < 3 || title.length() > 30) {
            titleErrorLabel.setText("Title is required and must contain 3 to 30 characters.");
            valid = false;
        }
        if (description.isEmpty()) {
            descriptionErrorLabel.setText("Description is required.");
            valid = false;
        }
        if (days < 0 || hours < 0 || minutes < 0 || DurationUtils.toMinutes(days, hours, minutes) <= 0) {
            durationErrorLabel.setText("Duration must be greater than 0.");
            valid = false;
        }
        if (difficultyCombo.getValue() == null || difficultyCombo.getValue().isBlank()) {
            difficultyErrorLabel.setText("Difficulty is required.");
            valid = false;
        }

        float parsedScore = 0f;
        try {
            parsedScore = Float.parseFloat(safeText(scoreField));
            if (parsedScore < 0 || parsedScore > 100) {
                scoreErrorLabel.setText("Score must be between 0 and 100.");
                valid = false;
            }
        } catch (Exception e) {
            scoreErrorLabel.setText("Invalid score.");
            valid = false;
        }

        if (contentPdf.isEmpty()) {
            contentErrorLabel.setText("Course PDF is required.");
            valid = false;
        } else if (!isPdfReference(contentPdf)) {
            contentErrorLabel.setText("The selected file must be a .pdf.");
            valid = false;
        }

        if (!material.isEmpty() && material.length() > 255) {
            materialErrorLabel.setText("Material must not exceed 255 characters.");
            valid = false;
        }

        if (!valid) {
            statusMessageLabel.setText("Please fix the highlighted fields and try again.");
            return;
        }

        Course course = new Course();
        course.setTitle(title);
        course.setDescription(description);
        course.setDuration(DurationUtils.toMinutes(days, hours, minutes));
        course.setDifficulty(difficultyCombo.getValue());
        course.setValidationScore(parsedScore);
        course.setContent(contentPdf);
        course.setMaterial(material.isEmpty() ? null : material);
        course.setCreator(currentSupervisor);
        course.setPrerequisiteQuiz(prerequisiteQuizCombo.getValue());

        try {
            courseService.create(course);
            showAlert(Alert.AlertType.INFORMATION, "Course created", "The course has been added successfully.");
            goBackToCourses();
        } catch (SQLException e) {
            statusMessageLabel.setText("Course creation failed.");
            showAlert(Alert.AlertType.ERROR, "Database error", e.getMessage());
        }
    }

    @FXML
    private void onReset() {
        titleField.clear();
        descriptionArea.clear();
        daysSpinner.getValueFactory().setValue(0);
        hoursSpinner.getValueFactory().setValue(0);
        minutesSpinner.getValueFactory().setValue(0);
        difficultyCombo.setValue(Course.DIFFICULTY_BEGINNER);
        scoreField.setText("0");
        contentPdfField.clear();
        materialField.clear();
        prerequisiteQuizCombo.setValue(null);
        clearErrors();
        statusMessageLabel.setText("Form reset. You can enter a new course now.");
    }

    @FXML
    private void onBackToCourses() {
        goBackToCourses();
    }

    private void loadQuizzes() {
        try {
            List<Quiz> quizzes = lookupService.findAllQuizzes();
            prerequisiteQuizCombo.setItems(FXCollections.observableArrayList(quizzes));
        } catch (SQLException e) {
            statusMessageLabel.setText("Unable to load prerequisite quizzes.");
        }
    }

    private void configureSpinner(Spinner<Integer> spinner, int min, int max, int initialValue) {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(min, max, initialValue);
        spinner.setValueFactory(factory);
        spinner.setEditable(true);
        spinner.focusedProperty().addListener((obs, oldValue, newValue) -> {
            if (!newValue) {
                commitSpinnerText(spinner);
            }
        });
    }

    private void commitSpinnerText(Spinner<Integer> spinner) {
        SpinnerValueFactory<Integer> valueFactory = spinner.getValueFactory();
        if (valueFactory == null || valueFactory.getConverter() == null) {
            return;
        }
        try {
            Integer value = valueFactory.getConverter().fromString(spinner.getEditor().getText());
            valueFactory.setValue(value);
        } catch (Exception ignored) {
        }
    }

    private int readSpinnerValue(Spinner<Integer> spinner) {
        commitSpinnerText(spinner);
        Integer value = spinner.getValue();
        return value == null ? -1 : value;
    }

    private void clearErrors() {
        titleErrorLabel.setText("");
        descriptionErrorLabel.setText("");
        durationErrorLabel.setText("");
        difficultyErrorLabel.setText("");
        scoreErrorLabel.setText("");
        contentErrorLabel.setText("");
        materialErrorLabel.setText("");
    }

    private void goBackToCourses() {
        BaseController baseController = BaseController.getInstance();
        if (baseController != null) {
            if (SessionManager.getInstance().isLogged()) {
                baseController.loadCourses();
            } else {
                baseController.loadHome();
            }
        }
    }

    private String safeText(TextInputControl control) {
        return control.getText() == null ? "" : control.getText().trim();
    }

    private boolean isPdfReference(String value) {
        return value != null && value.toLowerCase().endsWith(".pdf");
    }

    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
