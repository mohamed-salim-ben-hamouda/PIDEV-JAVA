package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
import com.pidev.Controllers.admin.AdminDialogStyler;
import com.pidev.Services.AnswerService;
import com.pidev.models.Answer;
import com.pidev.models.Question;
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
import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class AnswerManagementController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldCombo;
    @FXML private ComboBox<String> directionCombo;
    @FXML private VBox answerContainer;

    private final AnswerService answerService = new AnswerService();
    private final AdminLookupService lookupService = new AdminLookupService();
    private Answer selectedAnswer;
    private HBox selectedCard;

    @FXML
    public void initialize() {
        sortFieldCombo.setItems(FXCollections.observableArrayList("id", "content", "isCorrect"));
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
        showDialog(null).ifPresent(a -> {
            try {
                answerService.create(a);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        if (selectedAnswer == null) {
            showWarning("Selection", "Select an answer first.");
            return;
        }
        showDialog(selectedAnswer).ifPresent(a -> {
            try {
                answerService.update(a);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        if (selectedAnswer == null || selectedAnswer.getId() == null) {
            showWarning("Selection", "Select an answer first.");
            return;
        }
        if (new Alert(Alert.AlertType.CONFIRMATION, "Delete selected item?").showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                answerService.delete(selectedAnswer.getId());
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        }
    }

    private void refreshCards() {
        try {
            List<Answer> answers = answerService.findPage(searchField.getText(), null, sortFieldCombo.getValue(), directionCombo.getValue(), 1, 1000);
            selectedAnswer = null;
            selectedCard = null;
            answerContainer.getChildren().clear();
            for (Answer answer : answers) {
                answerContainer.getChildren().add(createAnswerCard(answer));
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    private HBox createAnswerCard(Answer answer) {
        Label id = new Label(String.valueOf(answer.getId() == null ? 0 : answer.getId()));
        id.setStyle("-fx-min-width: 80;");
        id.getStyleClass().add("management-card-label");
        Label content = new Label(nullSafe(answer.getContent()));
        content.setStyle("-fx-min-width: 350;");
        content.getStyleClass().add("management-card-label");
        String questionText = answer.getQuestion() != null && answer.getQuestion().getId() != null
            ? "Question #" + answer.getQuestion().getId()
            : "N/A";
        Label question = new Label(questionText);
        question.setStyle("-fx-min-width: 140;");
        question.getStyleClass().add("management-card-label");
        Label correct = new Label(answer.isCorrect() ? "Oui" : "Non");
        correct.setStyle("-fx-min-width: 120;");
        correct.getStyleClass().add("management-card-muted");

        HBox card = new HBox(15, id, content, question, correct);
        card.getStyleClass().add("management-card");
        card.setOnMouseClicked(event -> selectCard(answer, card));
        return card;
    }

    private void selectCard(Answer answer, HBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("management-card-selected");
        }
        selectedAnswer = answer;
        selectedCard = card;
        if (!selectedCard.getStyleClass().contains("management-card-selected")) {
            selectedCard.getStyleClass().add("management-card-selected");
        }
    }

    private Optional<Answer> showDialog(Answer existing) {
        boolean edit = existing != null;
        Dialog<Answer> dialog = new Dialog<>();
        VBox dialogContent = AdminDialogStyler.apply(dialog,
            edit ? "Modifier reponse" : "Ajouter reponse",
            edit ? "Mettre a jour la reponse associee" : "Renseigner la reponse et indiquer si elle est correcte",
            760,
            520);
        ButtonType save = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Question> questionCombo = new ComboBox<>();
        AdminDialogStyler.styleComboBox(questionCombo);
        TextArea contentArea = new TextArea(edit ? existing.getContent() : "");
        AdminDialogStyler.styleTextArea(contentArea);
        CheckBox correctCheck = new CheckBox("Correct answer");
        correctCheck.setSelected(edit && existing.isCorrect());

        try {
            questionCombo.setItems(FXCollections.observableArrayList(lookupService.findAllQuestions()));
            if (edit && existing.getQuestion() != null) {
                for (Question question : questionCombo.getItems()) {
                    if (question.getId().equals(existing.getQuestion().getId())) {
                        questionCombo.setValue(question);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }

        GridPane grid = new GridPane();
        grid.setHgap(14);
        grid.setVgap(12);
        grid.setPadding(new Insets(0, 4, 4, 4));
        grid.setPrefWidth(700);
        Label questionError = new Label();
        Label contentError = new Label();
        String errorStyle = "-fx-text-fill: #d32f2f; -fx-font-size: 11;";
        questionError.setStyle(errorStyle);
        contentError.setStyle(errorStyle);

        Label questionLabel = new Label("Question");
        AdminDialogStyler.styleFormLabel(questionLabel);
        grid.add(questionLabel, 0, 0);
        grid.add(questionCombo, 1, 0);
        grid.add(questionError, 1, 1);
        Label contentLabel = new Label("Contenu");
        AdminDialogStyler.styleFormLabel(contentLabel);
        grid.add(contentLabel, 0, 2);
        grid.add(contentArea, 1, 2);
        grid.add(contentError, 1, 3);
        grid.add(correctCheck, 1, 4);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(150);
        labelColumn.setPrefWidth(150);
        labelColumn.setHalignment(javafx.geometry.HPos.LEFT);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);
        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Definition de la reponse"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(grid);
        dialogContent.getChildren().add(AdminDialogStyler.createFooterHint("Associez la reponse a la bonne question et marquez-la si elle est correcte."));

        final Answer[] dialogResult = new Answer[1];
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
            questionError.setText("");
            contentError.setText("");

            boolean valid = true;
            if (questionCombo.getValue() == null) {
                questionError.setText("Question obligatoire.");
                valid = false;
            }
            if (contentArea.getText() == null || contentArea.getText().trim().isEmpty()) {
                contentError.setText("Contenu obligatoire.");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Answer answer = edit ? existing : new Answer();
            answer.setQuestion(questionCombo.getValue());
            answer.setContent(contentArea.getText().trim());
            answer.setCorrect(correctCheck.isSelected());
            dialogResult[0] = answer;
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
