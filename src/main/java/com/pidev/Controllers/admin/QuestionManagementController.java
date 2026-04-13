package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
import com.pidev.Services.QuestionService;
import com.pidev.models.Question;
import com.pidev.models.Quiz;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class QuestionManagementController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldCombo;
    @FXML private ComboBox<String> directionCombo;
    @FXML private VBox questionContainer;

    private final QuestionService questionService = new QuestionService();
    private final AdminLookupService lookupService = new AdminLookupService();
    private Question selectedQuestion;
    private HBox selectedCard;

    @FXML
    public void initialize() {
        sortFieldCombo.setItems(FXCollections.observableArrayList("id", "content", "type", "point"));
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
                questionService.create(q);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        if (selectedQuestion == null) {
            showWarning("Selection", "Select a question first.");
            return;
        }
        showDialog(selectedQuestion).ifPresent(q -> {
            try {
                questionService.update(q);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        if (selectedQuestion == null || selectedQuestion.getId() == null) {
            showWarning("Selection", "Select a question first.");
            return;
        }
        if (new Alert(Alert.AlertType.CONFIRMATION, "Delete selected item?").showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                questionService.delete(selectedQuestion.getId());
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        }
    }

    private void refreshCards() {
        try {
            List<Question> questions = questionService.findPage(searchField.getText(), null, sortFieldCombo.getValue(), directionCombo.getValue(), 1, 1000);
            selectedQuestion = null;
            selectedCard = null;
            questionContainer.getChildren().clear();
            for (Question question : questions) {
                questionContainer.getChildren().add(createQuestionCard(question));
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    private HBox createQuestionCard(Question question) {
        Label id = new Label(String.valueOf(question.getId() == null ? 0 : question.getId()));
        id.setStyle("-fx-min-width: 80;");
        id.getStyleClass().add("management-card-label");
        Label content = new Label(nullSafe(question.getContent()));
        content.setStyle("-fx-min-width: 300;");
        content.getStyleClass().add("management-card-label");
        Label type = new Label(nullSafe(question.getType()));
        type.setStyle("-fx-min-width: 120;");
        type.getStyleClass().add("management-card-muted");
        Label points = new Label(String.valueOf(question.getPoint()));
        points.setStyle("-fx-min-width: 100;");
        points.getStyleClass().add("management-card-label");

        HBox card = new HBox(15, id, content, type, points);
        card.getStyleClass().add("management-card");
        card.setOnMouseClicked(event -> selectCard(question, card));
        return card;
    }

    private void selectCard(Question question, HBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("management-card-selected");
        }
        selectedQuestion = question;
        selectedCard = card;
        if (!selectedCard.getStyleClass().contains("management-card-selected")) {
            selectedCard.getStyleClass().add("management-card-selected");
        }
    }

    private Optional<Question> showDialog(Question existing) {
        boolean edit = existing != null;
        Dialog<Question> dialog = new Dialog<>();
        dialog.setTitle(edit ? "Edit Question" : "Add Question");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Quiz> quizCombo = new ComboBox<>();
        TextArea contentArea = new TextArea(edit ? existing.getContent() : "");
        ComboBox<String> typeCombo = new ComboBox<>(FXCollections.observableArrayList("multiple_choice", "single_choice", "true_false"));
        typeCombo.setValue(edit ? existing.getType() : "multiple_choice");
        TextField pointField = new TextField(edit ? String.valueOf(existing.getPoint()) : "1");

        try {
            quizCombo.setItems(FXCollections.observableArrayList(lookupService.findAllQuizzes()));
            if (edit && existing.getQuiz() != null) {
                for (Quiz quiz : quizCombo.getItems()) {
                    if (quiz.getId().equals(existing.getQuiz().getId())) {
                        quizCombo.setValue(quiz);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        Label quizError = new Label();
        Label contentError = new Label();
        Label typeError = new Label();
        Label pointError = new Label();
        String errorStyle = "-fx-text-fill: #d32f2f; -fx-font-size: 11;";
        quizError.setStyle(errorStyle);
        contentError.setStyle(errorStyle);
        typeError.setStyle(errorStyle);
        pointError.setStyle(errorStyle);

        grid.add(new Label("Quiz"), 0, 0);
        grid.add(quizCombo, 1, 0);
        grid.add(quizError, 1, 1);
        grid.add(new Label("Content"), 0, 2);
        grid.add(contentArea, 1, 2);
        grid.add(contentError, 1, 3);
        grid.add(new Label("Type"), 0, 4);
        grid.add(typeCombo, 1, 4);
        grid.add(typeError, 1, 5);
        grid.add(new Label("Points"), 0, 6);
        grid.add(pointField, 1, 6);
        grid.add(pointError, 1, 7);
        dialog.getDialogPane().setContent(grid);

        final Question[] dialogResult = new Question[1];
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            quizError.setText("");
            contentError.setText("");
            typeError.setText("");
            pointError.setText("");

            boolean valid = true;
            if (quizCombo.getValue() == null) {
                quizError.setText("Quiz obligatoire.");
                valid = false;
            }
            if (contentArea.getText() == null || contentArea.getText().trim().length() < 3) {
                contentError.setText("Content obligatoire (min 3 caracteres).");
                valid = false;
            }
            if (typeCombo.getValue() == null || typeCombo.getValue().isBlank()) {
                typeError.setText("Type obligatoire.");
                valid = false;
            }

            float points = 0f;
            try {
                points = Float.parseFloat(pointField.getText().trim());
                if (points <= 0) {
                    pointError.setText("Points doit etre > 0.");
                    valid = false;
                }
            } catch (Exception e) {
                pointError.setText("Points invalide.");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Question question = edit ? existing : new Question();
            question.setQuiz(quizCombo.getValue());
            question.setContent(contentArea.getText().trim());
            question.setType(typeCombo.getValue());
            question.setPoint(points);
            dialogResult[0] = question;
        });

        dialog.setResultConverter(bt -> bt == save ? dialogResult[0] : null);

        return dialog.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String nullSafe(String value) {
        return value == null || value.isBlank() ? "N/A" : value;
    }

}
