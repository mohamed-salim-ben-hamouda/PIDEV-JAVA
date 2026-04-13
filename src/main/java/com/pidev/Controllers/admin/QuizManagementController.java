package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
import com.pidev.Services.QuizService;
import com.pidev.models.Chapter;
import com.pidev.models.Course;
import com.pidev.models.Quiz;
import com.pidev.models.User;
import javafx.event.ActionEvent;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class QuizManagementController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldCombo;
    @FXML private ComboBox<String> directionCombo;
    @FXML private VBox quizContainer;

    private final QuizService quizService = new QuizService();
    private final AdminLookupService lookupService = new AdminLookupService();
    private Quiz selectedQuiz;
    private HBox selectedCard;

    @FXML
    public void initialize() {
        sortFieldCombo.setItems(FXCollections.observableArrayList("id", "title", "passingScore", "maxAttempts"));
        sortFieldCombo.setValue("id");
        directionCombo.setItems(FXCollections.observableArrayList("DESC", "ASC"));
        directionCombo.setValue("DESC");
        refreshCards();
    }

    @FXML
    private void onSearch() { refreshCards(); }

    @FXML
    private void onSortChanged() { refreshCards(); }

    @FXML
    private void onRefresh() { refreshCards(); }

    @FXML
    private void onAdd() {
        showDialog(null).ifPresent(q -> {
            try {
                quizService.create(q);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        if (selectedQuiz == null) {
            showWarning("Selection", "Select a quiz first.");
            return;
        }
        showDialog(selectedQuiz).ifPresent(q -> {
            try {
                quizService.update(q);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        if (selectedQuiz == null || selectedQuiz.getId() == null) {
            showWarning("Selection", "Select a quiz first.");
            return;
        }
        if (confirmDelete()) {
            try {
                quizService.delete(selectedQuiz.getId());
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        }
    }

    private void refreshCards() {
        try {
            List<Quiz> quizzes = quizService.findPage(searchField.getText(), null, sortFieldCombo.getValue(), directionCombo.getValue(), 1, 1000);
            selectedQuiz = null;
            selectedCard = null;
            quizContainer.getChildren().clear();
            for (Quiz quiz : quizzes) {
                quizContainer.getChildren().add(createQuizCard(quiz));
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    private HBox createQuizCard(Quiz quiz) {
        Label id = new Label(String.valueOf(quiz.getId() == null ? 0 : quiz.getId()));
        id.setStyle("-fx-min-width: 80;");
        id.getStyleClass().add("management-card-label");
        Label title = new Label(nullSafe(quiz.getTitle()));
        title.setStyle("-fx-min-width: 250;");
        title.getStyleClass().add("management-card-label");
        Label passingScore = new Label(String.valueOf(quiz.getPassingScore()));
        passingScore.setStyle("-fx-min-width: 120;");
        passingScore.getStyleClass().add("management-card-label");
        Label maxAttempts = new Label(String.valueOf(quiz.getMaxAttempts()));
        maxAttempts.setStyle("-fx-min-width: 120;");
        maxAttempts.getStyleClass().add("management-card-label");

        HBox card = new HBox(15, id, title, passingScore, maxAttempts);
        card.getStyleClass().add("management-card");
        card.setOnMouseClicked(event -> selectCard(quiz, card));
        return card;
    }

    private void selectCard(Quiz quiz, HBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("management-card-selected");
        }
        selectedQuiz = quiz;
        selectedCard = card;
        if (!selectedCard.getStyleClass().contains("management-card-selected")) {
            selectedCard.getStyleClass().add("management-card-selected");
        }
    }

    private Optional<Quiz> showDialog(Quiz existing) {
        boolean edit = existing != null;
        Dialog<Quiz> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Edit Quiz" : "Add Quiz");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Course> courseCombo = new ComboBox<>();
        ComboBox<Chapter> chapterCombo = new ComboBox<>();
        TextField titleField = new TextField(edit ? existing.getTitle() : "");
        TextField passingScoreField = new TextField(edit ? String.valueOf(existing.getPassingScore()) : "70");
        TextField maxAttemptsField = new TextField(edit ? String.valueOf(existing.getMaxAttempts()) : "3");
        TextField qpaField = new TextField(edit && existing.getQuestionsPerAttempt() != null ? String.valueOf(existing.getQuestionsPerAttempt()) : "");
        TextField timeLimitField = new TextField(edit ? String.valueOf(existing.getTimeLimit()) : "0");
        ComboBox<User> supervisorCombo = new ComboBox<>();

        try {
            List<Course> courses = lookupService.findAllCourses();
            List<Chapter> chapters = lookupService.findAllChapters();
            List<User> users = lookupService.findAllUsers();
            courseCombo.setItems(FXCollections.observableArrayList(courses));
            chapterCombo.setItems(FXCollections.observableArrayList(chapters));
            supervisorCombo.setItems(FXCollections.observableArrayList(users));
            if (edit) {
                pickById(courseCombo, existing.getCourse() != null ? existing.getCourse().getId() : null);
                pickById(chapterCombo, existing.getChapter() != null ? existing.getChapter().getId() : null);
                pickById(supervisorCombo, existing.getSupervisor() != null ? existing.getSupervisor().getId() : null);
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Label titleError = new Label();
        Label courseError = new Label();
        Label chapterError = new Label();
        Label passingScoreError = new Label();
        Label maxAttemptsError = new Label();
        Label qpaError = new Label();
        Label timeLimitError = new Label();
        String errorStyle = "-fx-text-fill: #d32f2f; -fx-font-size: 11;";
        titleError.setStyle(errorStyle);
        courseError.setStyle(errorStyle);
        chapterError.setStyle(errorStyle);
        passingScoreError.setStyle(errorStyle);
        maxAttemptsError.setStyle(errorStyle);
        qpaError.setStyle(errorStyle);
        timeLimitError.setStyle(errorStyle);

        grid.add(new Label("Title"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(titleError, 1, 1);
        grid.add(new Label("Course"), 0, 2);
        grid.add(courseCombo, 1, 2);
        grid.add(courseError, 1, 3);
        grid.add(new Label("Chapter"), 0, 4);
        grid.add(chapterCombo, 1, 4);
        grid.add(chapterError, 1, 5);
        grid.add(new Label("Passing Score"), 0, 6);
        grid.add(passingScoreField, 1, 6);
        grid.add(passingScoreError, 1, 7);
        grid.add(new Label("Max Attempts"), 0, 8);
        grid.add(maxAttemptsField, 1, 8);
        grid.add(maxAttemptsError, 1, 9);
        grid.add(new Label("Questions/Attempt"), 0, 10);
        grid.add(qpaField, 1, 10);
        grid.add(qpaError, 1, 11);
        grid.add(new Label("Time Limit (min)"), 0, 12);
        grid.add(timeLimitField, 1, 12);
        grid.add(timeLimitError, 1, 13);
        grid.add(new Label("Supervisor"), 0, 14);
        grid.add(supervisorCombo, 1, 14);
        dialog.getDialogPane().setContent(grid);

        final Quiz[] dialogResult = new Quiz[1];
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            titleError.setText("");
            courseError.setText("");
            chapterError.setText("");
            passingScoreError.setText("");
            maxAttemptsError.setText("");
            qpaError.setText("");
            timeLimitError.setText("");

            boolean valid = true;
            if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
                titleError.setText("Titre obligatoire (min 3 caracteres).");
                valid = false;
            }
            if (courseCombo.getValue() == null) {
                courseError.setText("Cours obligatoire.");
                valid = false;
            }
            if (chapterCombo.getValue() == null) {
                chapterError.setText("Chapitre obligatoire.");
                valid = false;
            }

            float passingScore = 0f;
            try {
                passingScore = Float.parseFloat(passingScoreField.getText().trim());
                if (passingScore < 0 || passingScore > 100) {
                    passingScoreError.setText("Score entre 0 et 100.");
                    valid = false;
                }
            } catch (Exception e) {
                passingScoreError.setText("Score invalide.");
                valid = false;
            }

            int maxAttempts = 0;
            try {
                maxAttempts = Integer.parseInt(maxAttemptsField.getText().trim());
                if (maxAttempts <= 0) {
                    maxAttemptsError.setText("Max Attempts doit etre > 0.");
                    valid = false;
                }
            } catch (Exception e) {
                maxAttemptsError.setText("Max Attempts invalide.");
                valid = false;
            }

            Integer qpa = null;
            String qpaText = qpaField.getText() == null ? "" : qpaField.getText().trim();
            if (!qpaText.isEmpty()) {
                try {
                    qpa = Integer.parseInt(qpaText);
                    if (qpa <= 0) {
                        qpaError.setText("Questions/Attempt doit etre > 0.");
                        valid = false;
                    }
                } catch (Exception e) {
                    qpaError.setText("Questions/Attempt invalide.");
                    valid = false;
                }
            }

            int timeLimit = 0;
            try {
                timeLimit = Integer.parseInt(timeLimitField.getText().trim());
                if (timeLimit < 0) {
                    timeLimitError.setText("Time Limit doit etre >= 0.");
                    valid = false;
                }
            } catch (Exception e) {
                timeLimitError.setText("Time Limit invalide.");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Quiz quiz = edit ? existing : new Quiz();
            quiz.setTitle(titleField.getText().trim());
            quiz.setCourse(courseCombo.getValue());
            quiz.setChapter(chapterCombo.getValue());
            quiz.setPassingScore(passingScore);
            quiz.setMaxAttempts(maxAttempts);
            quiz.setQuestionsPerAttempt(qpa);
            quiz.setTimeLimit(timeLimit);
            quiz.setSupervisor(supervisorCombo.getValue());
            dialogResult[0] = quiz;
        });

        dialog.setResultConverter(bt -> bt == save ? dialogResult[0] : null);

        return dialog.showAndWait();
    }

    private String validateQuizInput(String title, Course course, Chapter chapter, String passingScore, String maxAttempts, String questionsPerAttempt, String timeLimit) {
        if (title == null || title.trim().length() < 3) {
            return "Title is required (minimum 3 characters).";
        }
        if (course == null) {
            return "Course is required.";
        }
        if (chapter == null) {
            return "Chapter is required.";
        }

        try {
            float parsedPassingScore = Float.parseFloat(passingScore == null ? "" : passingScore.trim());
            if (parsedPassingScore < 0 || parsedPassingScore > 100) {
                return "Passing Score must be between 0 and 100.";
            }
        } catch (NumberFormatException e) {
            return "Passing Score must be a valid number.";
        }

        try {
            int parsedMaxAttempts = Integer.parseInt(maxAttempts == null ? "" : maxAttempts.trim());
            if (parsedMaxAttempts <= 0) {
                return "Max Attempts must be greater than 0.";
            }
        } catch (NumberFormatException e) {
            return "Max Attempts must be a valid integer.";
        }

        if (questionsPerAttempt != null && !questionsPerAttempt.trim().isEmpty()) {
            try {
                int parsedQuestionsPerAttempt = Integer.parseInt(questionsPerAttempt.trim());
                if (parsedQuestionsPerAttempt <= 0) {
                    return "Questions/Attempt must be greater than 0 when provided.";
                }
            } catch (NumberFormatException e) {
                return "Questions/Attempt must be a valid integer.";
            }
        }

        try {
            int parsedTimeLimit = Integer.parseInt(timeLimit == null ? "" : timeLimit.trim());
            if (parsedTimeLimit < 0) {
                return "Time Limit must be 0 or greater.";
            }
        } catch (NumberFormatException e) {
            return "Time Limit must be a valid integer.";
        }

        return null;
    }

    private <T> void pickById(ComboBox<T> comboBox, Integer id) {
        if (id == null) {
            return;
        }
        for (T item : comboBox.getItems()) {
            if (item instanceof Course c && c.getId().equals(id)) {
                comboBox.setValue(item);
                return;
            }
            if (item instanceof Chapter c && c.getId().equals(id)) {
                comboBox.setValue(item);
                return;
            }
            if (item instanceof User c && c.getId().equals(id)) {
                comboBox.setValue(item);
                return;
            }
        }
    }

    private boolean confirmDelete() {
        return new Alert(Alert.AlertType.CONFIRMATION, "Delete selected item?").showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
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
