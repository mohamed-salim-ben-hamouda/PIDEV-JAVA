package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
import com.pidev.Controllers.admin.AdminDialogStyler;
import com.pidev.Services.CourseService;
import com.pidev.models.Course;
import com.pidev.models.Quiz;
import com.pidev.models.User;
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
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.HBox;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CourseManagementController {
    @FXML private VBox courseContainer;

    private final CourseService courseService = new CourseService();
    private final AdminLookupService lookupService = new AdminLookupService();
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
        Label score = new Label(String.format("%.0f%%", course.getValidationScore()));
        score.setStyle("-fx-min-width: 120;");
        score.getStyleClass().add("management-card-label");
        Label active = new Label(course.isIsActive() ? "Actif" : "Inactif");
        active.setStyle("-fx-min-width: 100;");
        active.getStyleClass().add("management-card-muted");
        String supervisorText = course.getCreator() != null
                ? course.getCreator().getDisplayName()
                : "N/A";
        Label supervisor = new Label(supervisorText);
        supervisor.setStyle("-fx-min-width: 140;");
        supervisor.getStyleClass().add("management-card-label");
        String prerequisite = course.getPrerequisiteQuiz() != null && course.getPrerequisiteQuiz().getId() != null
            ? "Quiz #" + course.getPrerequisiteQuiz().getId()
            : "Aucun";
        Label prerequisiteLabel = new Label(prerequisite);
        prerequisiteLabel.setStyle("-fx-min-width: 120;");
        prerequisiteLabel.getStyleClass().add("management-card-label");

        HBox card = new HBox(15, id, title, description, duration, difficulty, score, active, supervisor, prerequisiteLabel);
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
        VBox dialogContent = AdminDialogStyler.apply(dialog,
            editMode ? "Modifier le cours" : "Ajouter un cours",
            editMode ? "Mettre a jour les informations du cours" : "Renseigner les informations du cours",
            820,
            700);

        ButtonType saveButtonType = new ButtonType(editMode ? "Modifier" : "Ajouter", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, cancelButtonType);

        TextField titleField = new TextField(editMode ? existing.getTitle() : "");
        AdminDialogStyler.styleField(titleField);
        titleField.setPromptText("Titre du cours");
        TextArea descriptionArea = new TextArea(editMode ? existing.getDescription() : "");
        descriptionArea.setPrefRowCount(4);
        descriptionArea.setWrapText(true);
        AdminDialogStyler.styleTextArea(descriptionArea);
        descriptionArea.setPromptText("Description concise du cours");
        TextField durationField = new TextField(editMode ? String.valueOf(existing.getDuration()) : "0");
        AdminDialogStyler.styleField(durationField);
        durationField.setPromptText("Ex: 45");
        ComboBox<String> difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
                Course.DIFFICULTY_BEGINNER,
                Course.DIFFICULTY_INTERMEDIATE,
                Course.DIFFICULTY_ADVANCED
        ));
        AdminDialogStyler.styleComboBox(difficultyCombo);
        difficultyCombo.setValue(editMode ? existing.getDifficulty() : Course.DIFFICULTY_BEGINNER);

        CheckBox activeCheck = new CheckBox("Active");
        activeCheck.setSelected(!editMode || existing.isActive());

        TextField scoreField = new TextField(editMode ? String.valueOf(existing.getValidationScore()) : "0");
        AdminDialogStyler.styleField(scoreField);
        scoreField.setPromptText("0 - 100");
        TextArea contentArea = new TextArea(editMode ? existing.getContent() : "");
        contentArea.setPrefRowCount(5);
        contentArea.setWrapText(true);
        AdminDialogStyler.styleTextArea(contentArea);
        contentArea.setPromptText("Contenu détaillé du cours");
        TextField materialField = new TextField(editMode ? existing.getMaterial() : "");
        AdminDialogStyler.styleField(materialField);
        materialField.setPromptText("Optionnel");
        ComboBox<Quiz> prerequisiteQuizCombo = new ComboBox<>();
        AdminDialogStyler.styleComboBox(prerequisiteQuizCombo);
        ComboBox<User> supervisorCombo = new ComboBox<>();
        AdminDialogStyler.styleComboBox(supervisorCombo);
        TextArea sectionsToReviewArea = new TextArea(editMode && existing.getSectionsToReview() != null
                ? String.join(", ", existing.getSectionsToReview())
                : "");
        sectionsToReviewArea.setPrefRowCount(3);
        sectionsToReviewArea.setWrapText(true);
        AdminDialogStyler.styleTextArea(sectionsToReviewArea);
        sectionsToReviewArea.setPromptText("Section1, Section2, Section3");

        try {
            List<Quiz> quizzes = lookupService.findAllQuizzes();
            List<User> users = lookupService.findAllUsers();
            prerequisiteQuizCombo.setItems(FXCollections.observableArrayList(quizzes));
            supervisorCombo.setItems(FXCollections.observableArrayList(users));
            if (editMode && existing.getPrerequisiteQuiz() != null && existing.getPrerequisiteQuiz().getId() != null) {
                for (Quiz quiz : quizzes) {
                    if (quiz.getId() != null && quiz.getId().equals(existing.getPrerequisiteQuiz().getId())) {
                        prerequisiteQuizCombo.setValue(quiz);
                        break;
                    }
                }
            }
            if (editMode && existing.getCreator() != null && existing.getCreator().getId() != null) {
                for (User user : users) {
                    if (user.getId() != null && user.getId().equals(existing.getCreator().getId())) {
                        supervisorCombo.setValue(user);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            showError("Erreur base", e.getMessage());
        }

        GridPane generalGrid = new GridPane();
        generalGrid.setHgap(14);
        generalGrid.setVgap(12);
        generalGrid.setPrefWidth(760);

        GridPane relationGrid = new GridPane();
        relationGrid.setHgap(14);
        relationGrid.setVgap(12);
        relationGrid.setPrefWidth(760);

        GridPane reviewGrid = new GridPane();
        reviewGrid.setHgap(14);
        reviewGrid.setVgap(12);
        reviewGrid.setPrefWidth(760);

        Label titleError = new Label();
        Label descriptionError = new Label();
        Label durationError = new Label();
        Label difficultyError = new Label();
        Label scoreError = new Label();
        Label contentError = new Label();
        Label materialError = new Label();
        Label sectionsError = new Label();
        Label supervisorError = new Label();

        String errorStyle = "-fx-text-fill: #d32f2f; -fx-font-size: 11;";
        titleError.setStyle(errorStyle);
        descriptionError.setStyle(errorStyle);
        durationError.setStyle(errorStyle);
        difficultyError.setStyle(errorStyle);
        scoreError.setStyle(errorStyle);
        contentError.setStyle(errorStyle);
        materialError.setStyle(errorStyle);
        sectionsError.setStyle(errorStyle);
        supervisorError.setStyle(errorStyle);

        Label titleLabel = new Label("Titre");
        AdminDialogStyler.styleFormLabel(titleLabel);
        generalGrid.add(titleLabel, 0, 0);
        generalGrid.add(titleField, 1, 0);
        generalGrid.add(titleError, 1, 1);
        Label descriptionLabel = new Label("Description");
        AdminDialogStyler.styleFormLabel(descriptionLabel);
        generalGrid.add(descriptionLabel, 0, 2);
        generalGrid.add(descriptionArea, 1, 2);
        generalGrid.add(descriptionError, 1, 3);
        Label durationLabel = new Label("Duree (min)");
        AdminDialogStyler.styleFormLabel(durationLabel);
        generalGrid.add(durationLabel, 0, 4);
        generalGrid.add(durationField, 1, 4);
        generalGrid.add(durationError, 1, 5);
        Label difficultyLabel = new Label("Difficulte");
        AdminDialogStyler.styleFormLabel(difficultyLabel);
        generalGrid.add(difficultyLabel, 0, 6);
        generalGrid.add(difficultyCombo, 1, 6);
        generalGrid.add(difficultyError, 1, 7);
        Label scoreLabel = new Label("Score de validation");
        AdminDialogStyler.styleFormLabel(scoreLabel);
        generalGrid.add(scoreLabel, 0, 8);
        generalGrid.add(scoreField, 1, 8);
        generalGrid.add(scoreError, 1, 9);
        Label contentLabel = new Label("Contenu");
        AdminDialogStyler.styleFormLabel(contentLabel);
        generalGrid.add(contentLabel, 0, 10);
        generalGrid.add(contentArea, 1, 10);
        generalGrid.add(contentError, 1, 11);

        Label materialLabel = new Label("Support (optionnel)");
        AdminDialogStyler.styleFormLabel(materialLabel);
        relationGrid.add(materialLabel, 0, 0);
        relationGrid.add(materialField, 1, 0);
        relationGrid.add(materialError, 1, 1);
        Label supervisorLabel = new Label("Superviseur");
        AdminDialogStyler.styleFormLabel(supervisorLabel);
        relationGrid.add(supervisorLabel, 0, 2);
        relationGrid.add(supervisorCombo, 1, 2);
        relationGrid.add(supervisorError, 1, 3);
        Label prerequisiteLabel = new Label("Quiz prerequis");
        AdminDialogStyler.styleFormLabel(prerequisiteLabel);
        relationGrid.add(prerequisiteLabel, 0, 4);
        relationGrid.add(prerequisiteQuizCombo, 1, 4);

        Label sectionsLabel = new Label("Sections a revoir (csv)");
        AdminDialogStyler.styleFormLabel(sectionsLabel);
        reviewGrid.add(sectionsLabel, 0, 0);
        reviewGrid.add(sectionsToReviewArea, 1, 0);
        reviewGrid.add(sectionsError, 1, 1);

        HBox activeRow = new HBox(10, activeCheck);
        activeRow.setPadding(new javafx.geometry.Insets(4, 0, 0, 0));

        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(160);
        labelColumn.setPrefWidth(160);
        labelColumn.setHalignment(javafx.geometry.HPos.LEFT);

        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);

        generalGrid.getColumnConstraints().addAll(labelColumn, valueColumn);
        relationGrid.getColumnConstraints().addAll(labelColumn, valueColumn);
        reviewGrid.getColumnConstraints().addAll(labelColumn, valueColumn);

        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Informations generales"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(generalGrid);
        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Relations du cours"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(relationGrid);
        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Parametres complementaires"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(reviewGrid);
        dialogContent.getChildren().add(activeRow);
        dialogContent.getChildren().add(AdminDialogStyler.createFooterHint("Les champs supervises, contenu et duree structurent la fiche du cours."));

        final Course[] dialogResult = new Course[1];
        Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
        Node cancelButton = dialog.getDialogPane().lookupButton(cancelButtonType);
        if (saveButton instanceof javafx.scene.control.Button save) {
            AdminDialogStyler.styleButton(save, true);
            save.setDefaultButton(true);
        }
        if (cancelButton instanceof javafx.scene.control.Button cancel) {
            AdminDialogStyler.styleButton(cancel, false);
            cancel.setCancelButton(true);
        }
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            titleError.setText("");
            descriptionError.setText("");
            durationError.setText("");
            difficultyError.setText("");
            scoreError.setText("");
            contentError.setText("");
            materialError.setText("");
            sectionsError.setText("");
            supervisorError.setText("");

            boolean valid = true;
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.length() < 3 || title.length() > 30) {
                titleError.setText("Titre obligatoire (3 a 30 caracteres).");
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
            String material = materialField.getText() == null ? "" : materialField.getText().trim();
            if (!material.isEmpty() && material.length() > 255) {
                materialError.setText("Material ne doit pas depasser 255 caracteres.");
                valid = false;
            }

            String sectionsText = sectionsToReviewArea.getText() == null ? "" : sectionsToReviewArea.getText().trim();
            if (!sectionsText.isEmpty() && sectionsText.length() > 2000) {
                sectionsError.setText("Sections a revoir trop longues.");
                valid = false;
            }

            if (supervisorCombo.getValue() == null || supervisorCombo.getValue().getId() == null) {
                supervisorError.setText("Superviseur obligatoire.");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Course course = editMode ? existing : new Course();
            course.setTitle(title);
            course.setDescription(descriptionArea.getText().trim());
            course.setDuration(parsedDuration);
            course.setDifficulty(difficultyCombo.getValue());
            course.setValidationScore(parsedScore);
            course.setContent(contentArea.getText().trim());
            course.setMaterial(material.isEmpty() ? null : material);
            course.setCreator(supervisorCombo.getValue());
            course.setPrerequisiteQuiz(prerequisiteQuizCombo.getValue());

            List<String> sections = new ArrayList<>();
            if (!sectionsText.isEmpty()) {
                String[] raw = sectionsText.split(",");
                for (String token : raw) {
                    String value = token == null ? "" : token.trim();
                    if (!value.isEmpty()) {
                        sections.add(value);
                    }
                }
            }
            course.setSectionsToReview(sections);
            course.setIsActive(activeCheck.isSelected());
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
