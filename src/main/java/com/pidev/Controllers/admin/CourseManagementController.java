package com.pidev.Controllers.admin;

import com.pidev.Services.CourseService;
import com.pidev.models.Course;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class CourseManagementController {
    @FXML private VBox courseContainer;

    private final CourseService courseService = new CourseService();
    private Course selectedCourse;
    private HBox selectedCard;

    @FXML
    public void initialize() {
        refreshCards();
    }

    @FXML
    private void onAddCourse() {
        Optional<Course> result = showCourseDialog(null);
        result.ifPresent(course -> {
            try {
                courseService.create(course);
                showInfo("Success", "Course created successfully.");
                refreshCards();
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEditCourse() {
        if (selectedCourse == null) {
            showWarning("Selection required", "Please select a course to edit.");
            return;
        }

        Optional<Course> result = showCourseDialog(selectedCourse);
        result.ifPresent(course -> {
            try {
                courseService.update(course);
                showInfo("Success", "Course updated successfully.");
                refreshCards();
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDeleteCourse() {
        if (selectedCourse == null || selectedCourse.getId() == null) {
            showWarning("Selection required", "Please select a course to delete.");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete course");
        confirmation.setHeaderText("Delete selected course?");
        confirmation.setContentText("This action cannot be undone.");

        if (confirmation.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                courseService.delete(selectedCourse.getId());
                showInfo("Success", "Course deleted successfully.");
                refreshCards();
            } catch (SQLException e) {
                showError("Database Error", e.getMessage());
            }
        }
    }

    @FXML
    private void onRefresh() {
        refreshCards();
    }

    private void refreshCards() {
        try {
            List<Course> courses = courseService.findAll();
            selectedCourse = null;
            selectedCard = null;
            courseContainer.getChildren().clear();
            for (Course course : courses) {
                courseContainer.getChildren().add(createCourseCard(course));
            }
        } catch (SQLException e) {
            showError("Database Error", e.getMessage());
        }
    }

    private HBox createCourseCard(Course course) {
        Label id = new Label(String.valueOf(course.getId() == null ? 0 : course.getId()));
        id.setStyle("-fx-min-width: 80;");
        id.getStyleClass().add("management-card-label");
        Label title = new Label(nullSafe(course.getTitle()));
        title.setStyle("-fx-min-width: 200;");
        title.getStyleClass().add("management-card-label");
        Label description = new Label(nullSafe(course.getDescription()));
        description.setStyle("-fx-min-width: 150;");
        description.getStyleClass().add("management-card-muted");
        Label duration = new Label(String.valueOf(course.getDuration()));
        duration.setStyle("-fx-min-width: 100;");
        duration.getStyleClass().add("management-card-label");
        Label difficulty = new Label(nullSafe(course.getDifficulty()));
        difficulty.setStyle("-fx-min-width: 120;");
        difficulty.getStyleClass().add("management-card-label");

        HBox card = new HBox(15, id, title, description, duration, difficulty);
        card.getStyleClass().add("management-card");
        card.setOnMouseClicked(event -> selectCard(course, card));
        return card;
    }

    private void selectCard(Course course, HBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("management-card-selected");
        }
        selectedCourse = course;
        selectedCard = card;
        if (!selectedCard.getStyleClass().contains("management-card-selected")) {
            selectedCard.getStyleClass().add("management-card-selected");
        }
    }

    private Optional<Course> showCourseDialog(Course existing) {
        boolean editMode = existing != null;

        Dialog<Course> dialog = new Dialog<>();
        dialog.setTitle(editMode ? "Edit Course" : "New Course");
        dialog.setHeaderText(editMode ? "Update course details" : "Fill in course details");

        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        TextField titleField = new TextField(editMode ? existing.getTitle() : "");
        TextArea descriptionArea = new TextArea(editMode ? existing.getDescription() : "");
        TextField durationField = new TextField(editMode ? String.valueOf(existing.getDuration()) : "0");
        ComboBox<String> difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
                Course.DIFFICULTY_BEGINNER,
                Course.DIFFICULTY_INTERMEDIATE,
                Course.DIFFICULTY_ADVANCED
        ));
        difficultyCombo.setValue(editMode ? existing.getDifficulty() : Course.DIFFICULTY_BEGINNER);

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(!editMode || existing.isActive());

        TextField scoreField = new TextField(editMode ? String.valueOf(existing.getValidationScore()) : "0");
        TextArea contentArea = new TextArea(editMode ? existing.getContent() : "");
        TextField materialField = new TextField(editMode ? existing.getMaterial() : "");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        Label titleError = new Label();
        Label descriptionError = new Label();
        Label durationError = new Label();
        Label difficultyError = new Label();
        Label scoreError = new Label();
        Label contentError = new Label();
        Label materialError = new Label();

        String errorStyle = "-fx-text-fill: #d32f2f; -fx-font-size: 11;";
        titleError.setStyle(errorStyle);
        descriptionError.setStyle(errorStyle);
        durationError.setStyle(errorStyle);
        difficultyError.setStyle(errorStyle);
        scoreError.setStyle(errorStyle);
        contentError.setStyle(errorStyle);
        materialError.setStyle(errorStyle);

        grid.add(new Label("Title"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(titleError, 1, 1);
        grid.add(new Label("Description"), 0, 2);
        grid.add(descriptionArea, 1, 2);
        grid.add(descriptionError, 1, 3);
        grid.add(new Label("Duration"), 0, 4);
        grid.add(durationField, 1, 4);
        grid.add(durationError, 1, 5);
        grid.add(new Label("Difficulty"), 0, 6);
        grid.add(difficultyCombo, 1, 6);
        grid.add(difficultyError, 1, 7);
        grid.add(new Label("Validation Score"), 0, 8);
        grid.add(scoreField, 1, 8);
        grid.add(scoreError, 1, 9);
        grid.add(new Label("Content"), 0, 10);
        grid.add(contentArea, 1, 10);
        grid.add(contentError, 1, 11);
        grid.add(new Label("Material"), 0, 12);
        grid.add(materialField, 1, 12);
        grid.add(materialError, 1, 13);
        grid.add(activeCheck, 1, 14);

        dialog.getDialogPane().setContent(grid);

        final Course[] dialogResult = new Course[1];
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            titleError.setText("");
            descriptionError.setText("");
            durationError.setText("");
            difficultyError.setText("");
            scoreError.setText("");
            contentError.setText("");
            materialError.setText("");

            boolean valid = true;
            if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
                titleError.setText("Titre obligatoire (min 3 caracteres).");
                valid = false;
            }
            if (descriptionArea.getText() == null || descriptionArea.getText().trim().isEmpty()) {
                descriptionError.setText("Description obligatoire.");
                valid = false;
            }

            int parsedDuration = 0;
            try {
                parsedDuration = Integer.parseInt(durationField.getText().trim());
                if (parsedDuration <= 0) {
                    durationError.setText("Duration doit etre > 0.");
                    valid = false;
                }
            } catch (Exception e) {
                durationError.setText("Duration invalide.");
                valid = false;
            }

            if (difficultyCombo.getValue() == null || difficultyCombo.getValue().isBlank()) {
                difficultyError.setText("Difficulty obligatoire.");
                valid = false;
            }

            float parsedScore = 0f;
            try {
                parsedScore = Float.parseFloat(scoreField.getText().trim());
                if (parsedScore < 0 || parsedScore > 100) {
                    scoreError.setText("Score entre 0 et 100.");
                    valid = false;
                }
            } catch (Exception e) {
                scoreError.setText("Score invalide.");
                valid = false;
            }

            if (contentArea.getText() == null || contentArea.getText().trim().isEmpty()) {
                contentError.setText("Content obligatoire.");
                valid = false;
            }
            if (materialField.getText() == null || materialField.getText().trim().isEmpty()) {
                materialError.setText("Material obligatoire.");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Course course = editMode ? existing : new Course();
            course.setTitle(titleField.getText().trim());
            course.setDescription(descriptionArea.getText().trim());
            course.setDuration(parsedDuration);
            course.setDifficulty(difficultyCombo.getValue());
            course.setValidationScore(parsedScore);
            course.setContent(contentArea.getText().trim());
            course.setMaterial(materialField.getText().trim());
            course.setActive(activeCheck.isSelected());
            dialogResult[0] = course;
        });

        dialog.setResultConverter(buttonType -> buttonType == saveButtonType ? dialogResult[0] : null);

        return dialog.showAndWait();
    }

    private String validateCourseInput(String title, String description, String duration, String score, String content, String material) {
        if (title == null || title.trim().length() < 3) {
            return "Title is required (minimum 3 characters).";
        }
        if (description == null || description.trim().isEmpty()) {
            return "Description is required.";
        }
        if (content == null || content.trim().isEmpty()) {
            return "Content is required.";
        }
        if (material == null || material.trim().isEmpty()) {
            return "Material is required.";
        }

        try {
            int parsedDuration = Integer.parseInt(duration == null ? "" : duration.trim());
            if (parsedDuration <= 0) {
                return "Duration must be greater than 0.";
            }
        } catch (NumberFormatException e) {
            return "Duration must be a valid integer.";
        }

        try {
            float parsedScore = Float.parseFloat(score == null ? "" : score.trim());
            if (parsedScore < 0 || parsedScore > 100) {
                return "Validation Score must be between 0 and 100.";
            }
        } catch (NumberFormatException e) {
            return "Validation Score must be a valid number.";
        }

        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(title);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

}
