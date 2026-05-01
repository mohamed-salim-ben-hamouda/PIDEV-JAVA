package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
import com.pidev.Controllers.admin.AdminDialogStyler;
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
import javafx.scene.control.Slider;
import javafx.scene.Node;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

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
        Label title = new Label(nullSafe(quiz.getTitle()));
        title.setStyle("-fx-min-width: 250;");
        title.getStyleClass().add("management-card-label");
        Label passingScore = new Label(String.valueOf(quiz.getPassingScore()));
        passingScore.setStyle("-fx-min-width: 120;");
        passingScore.getStyleClass().add("management-card-label");
        Label maxAttempts = new Label(String.valueOf(quiz.getMaxAttempts()));
        maxAttempts.setStyle("-fx-min-width: 120;");
        maxAttempts.getStyleClass().add("management-card-label");
        String chapterText = quiz.getChapter() != null && quiz.getChapter().getId() != null
            ? "Chap #" + quiz.getChapter().getId()
            : "Aucun";
        Label chapter = new Label(chapterText);
        chapter.setStyle("-fx-min-width: 120;");
        chapter.getStyleClass().add("management-card-muted");
        Label timeLimit = new Label(quiz.getTimeLimit() == 0 ? "Illimite" : quiz.getTimeLimit() + " min");
        timeLimit.setStyle("-fx-min-width: 100;");
        timeLimit.getStyleClass().add("management-card-label");
        String supervisorText = quiz.getSupervisor() != null
            ? quiz.getSupervisor().getDisplayName()
            : "N/A";
        Label supervisor = new Label(supervisorText);
        supervisor.setStyle("-fx-min-width: 120;");
        supervisor.getStyleClass().add("management-card-label");

        HBox card = new HBox(15, title, passingScore, maxAttempts, chapter, timeLimit, supervisor);
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
        VBox dialogContent = AdminDialogStyler.apply(dialog,
            edit ? "Modifier quiz" : "Ajouter quiz",
            edit ? "Mettre a jour les parametres du quiz" : "Renseigner les parametres du quiz",
            820,
            620);
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Course> courseCombo = new ComboBox<>();
        ComboBox<Chapter> chapterCombo = new ComboBox<>();
        AdminDialogStyler.styleComboBox(courseCombo);
        AdminDialogStyler.styleComboBox(chapterCombo);
        TextField titleField = new TextField(edit ? existing.getTitle() : "");
        AdminDialogStyler.styleField(titleField);
        TextField passingScoreField = new TextField(edit ? String.valueOf(existing.getPassingScore()) : "70");
        AdminDialogStyler.styleField(passingScoreField);
        TextField maxAttemptsField = new TextField(edit ? String.valueOf(existing.getMaxAttempts()) : "3");
        AdminDialogStyler.styleField(maxAttemptsField);
        TextField qpaField = new TextField(edit && existing.getQuestionsPerAttempt() != null ? String.valueOf(existing.getQuestionsPerAttempt()) : "");
        AdminDialogStyler.styleField(qpaField);
        TextField timeLimitField = new TextField(edit ? String.valueOf(existing.getTimeLimit()) : "0");
        AdminDialogStyler.styleField(timeLimitField);
        ComboBox<User> supervisorCombo = new ComboBox<>();
        AdminDialogStyler.styleComboBox(supervisorCombo);

        Slider logicSlider = new Slider(0, 100, 33);
        Slider syntaxSlider = new Slider(0, 100, 33);
        Slider theorySlider = new Slider(0, 100, 34);
        
        // Parse existing distribution
        if (edit && existing.getCategoryDistribution() != null) {
            String[] parts = existing.getCategoryDistribution().split("\\|");
            for (String p : parts) {
                String[] kv = p.split(":");
                if (kv.length == 2) {
                    try {
                        double val = Double.parseDouble(kv[1]);
                        if (kv[0].equals("Logique")) logicSlider.setValue(val);
                        if (kv[0].equals("Syntaxe")) syntaxSlider.setValue(val);
                        if (kv[0].equals("Theorie")) theorySlider.setValue(val);
                    } catch(Exception ignored) {}
                }
            }
        }

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
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(0, 4, 4, 4));
        grid.setPrefWidth(760);
        Label titleError = new Label();
        Label courseError = new Label();
        Label chapterError = new Label();
        Label passingScoreError = new Label();
        Label maxAttemptsError = new Label();
        Label qpaError = new Label();
        Label timeLimitError = new Label();
        Label supervisorError = new Label();
        Label slidersError = new Label();
        String errorStyle = "-fx-text-fill: #d32f2f; -fx-font-size: 11;";
        titleError.setStyle(errorStyle);
        courseError.setStyle(errorStyle);
        chapterError.setStyle(errorStyle);
        passingScoreError.setStyle(errorStyle);
        maxAttemptsError.setStyle(errorStyle);
        qpaError.setStyle(errorStyle);
        timeLimitError.setStyle(errorStyle);
        supervisorError.setStyle(errorStyle);
        slidersError.setStyle(errorStyle);

        Label titleLabel = new Label("Titre");
        AdminDialogStyler.styleFormLabel(titleLabel);
        grid.add(titleLabel, 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(titleError, 1, 1);
        Label courseLabel = new Label("Cours");
        AdminDialogStyler.styleFormLabel(courseLabel);
        grid.add(courseLabel, 0, 2);
        grid.add(courseCombo, 1, 2);
        grid.add(courseError, 1, 3);
        Label chapterLabel = new Label("Chapitre (optionnel)");
        AdminDialogStyler.styleFormLabel(chapterLabel);
        grid.add(chapterLabel, 0, 4);
        grid.add(chapterCombo, 1, 4);
        grid.add(chapterError, 1, 5);
        Label passingScoreLabel = new Label("Score requis");
        AdminDialogStyler.styleFormLabel(passingScoreLabel);
        grid.add(passingScoreLabel, 0, 6);
        grid.add(passingScoreField, 1, 6);
        grid.add(passingScoreError, 1, 7);
        Label maxAttemptsLabel = new Label("Tentatives max");
        AdminDialogStyler.styleFormLabel(maxAttemptsLabel);
        grid.add(maxAttemptsLabel, 0, 8);
        grid.add(maxAttemptsField, 1, 8);
        grid.add(maxAttemptsError, 1, 9);
        Label qpaLabel = new Label("Questions/Tentative");
        AdminDialogStyler.styleFormLabel(qpaLabel);
        grid.add(qpaLabel, 0, 10);
        grid.add(qpaField, 1, 10);
        grid.add(qpaError, 1, 11);
        Label timeLimitLabel = new Label("Temps limite (min)");
        AdminDialogStyler.styleFormLabel(timeLimitLabel);
        grid.add(timeLimitLabel, 0, 12);
        grid.add(timeLimitField, 1, 12);
        grid.add(timeLimitError, 1, 13);
        Label supervisorLabel = new Label("Superviseur");
        AdminDialogStyler.styleFormLabel(supervisorLabel);
        grid.add(supervisorLabel, 0, 14);
        grid.add(supervisorCombo, 1, 14);
        grid.add(supervisorError, 1, 15);
        
        Label logicLabel = new Label("Logique (%)");
        AdminDialogStyler.styleFormLabel(logicLabel);
        logicSlider.setShowTickLabels(true); logicSlider.setShowTickMarks(true); logicSlider.setMajorTickUnit(25);
        grid.add(logicLabel, 0, 16);
        grid.add(logicSlider, 1, 16);
        
        Label syntaxLabel = new Label("Syntaxe (%)");
        AdminDialogStyler.styleFormLabel(syntaxLabel);
        syntaxSlider.setShowTickLabels(true); syntaxSlider.setShowTickMarks(true); syntaxSlider.setMajorTickUnit(25);
        grid.add(syntaxLabel, 0, 17);
        grid.add(syntaxSlider, 1, 17);
        
        Label theoryLabel = new Label("Theorie (%)");
        AdminDialogStyler.styleFormLabel(theoryLabel);
        theorySlider.setShowTickLabels(true); theorySlider.setShowTickMarks(true); theorySlider.setMajorTickUnit(25);
        grid.add(theoryLabel, 0, 18);
        grid.add(theorySlider, 1, 18);
        grid.add(slidersError, 1, 19);
        
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(170);
        labelColumn.setPrefWidth(170);
        labelColumn.setHalignment(javafx.geometry.HPos.LEFT);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);
        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Parametres du quiz"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(grid);
        dialogContent.getChildren().add(AdminDialogStyler.createFooterHint("Le superviseur est obligatoire. Le chapitre reste optionnel selon votre configuration."));

        final Quiz[] dialogResult = new Quiz[1];
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        Node cancelButton = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        if (saveButton instanceof javafx.scene.control.Button saveBtn) {
            AdminDialogStyler.styleButton(saveBtn, true);
            saveBtn.setDefaultButton(true);
        }
        if (cancelButton instanceof javafx.scene.control.Button cancelBtn) {
            AdminDialogStyler.styleButton(cancelBtn, false);
            cancelBtn.setCancelButton(true);
        }
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            titleError.setText("");
            courseError.setText("");
            chapterError.setText("");
            passingScoreError.setText("");
            maxAttemptsError.setText("");
            qpaError.setText("");
            timeLimitError.setText("");
            supervisorError.setText("");
            slidersError.setText("");

            boolean valid = true;
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.length() < 3 || title.length() > 30) {
                titleError.setText("Titre obligatoire (3 a 30 caracteres).");
                valid = false;
            }
            if (courseCombo.getValue() == null) {
                courseError.setText("Cours obligatoire.");
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

            if (supervisorCombo.getValue() == null || supervisorCombo.getValue().getId() == null) {
                supervisorError.setText("Superviseur obligatoire.");
                valid = false;
            }
            
            double logic = Math.round(logicSlider.getValue());
            double syntax = Math.round(syntaxSlider.getValue());
            double theory = Math.round(theorySlider.getValue());
            double sum = logic + syntax + theory;
            if (Math.abs(sum - 100) > 2) {
                slidersError.setText("La somme doit etre de 100% (Actuel: " + sum + "%)");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Quiz quiz = edit ? existing : new Quiz();
            quiz.setTitle(title);
            quiz.setCourse(courseCombo.getValue());
            quiz.setChapter(chapterCombo.getValue());
            quiz.setPassingScore(passingScore);
            quiz.setMaxAttempts(maxAttempts);
            quiz.setQuestionsPerAttempt(qpa);
            quiz.setTimeLimit(timeLimit);
            quiz.setSupervisor(supervisorCombo.getValue());
            quiz.setCategoryDistribution("Logique:" + logic + "|Syntaxe:" + syntax + "|Theorie:" + theory);
            dialogResult[0] = quiz;
        });

        dialog.setResultConverter(bt -> bt == save ? dialogResult[0] : null);

        return dialog.showAndWait();
    }

    private String validateQuizInput(String title, Course course, User supervisor, String passingScore, String maxAttempts, String questionsPerAttempt, String timeLimit) {
        if (title == null || title.trim().length() < 3) {
            return "Title is required (minimum 3 characters).";
        }
        if (course == null) {
            return "Course is required.";
        }
        if (supervisor == null) {
            return "Supervisor is required.";
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
