package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
import com.pidev.Controllers.admin.AdminDialogStyler;
import com.pidev.Services.ChapterService;
import com.pidev.models.Chapter;
import com.pidev.models.Course;
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
import javafx.geometry.Insets;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.Priority;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public class ChapterManagementController {
    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldCombo;
    @FXML private ComboBox<String> directionCombo;
    @FXML private VBox chapterContainer;

    private final ChapterService chapterService = new ChapterService();
    private final AdminLookupService lookupService = new AdminLookupService();
    private Chapter selectedChapter;
    private HBox selectedCard;

    @FXML
    public void initialize() {
        sortFieldCombo.setItems(FXCollections.observableArrayList("id", "title", "chapterOrder", "status", "minScore"));
        sortFieldCombo.setValue("chapterOrder");
        directionCombo.setItems(FXCollections.observableArrayList("ASC", "DESC"));
        directionCombo.setValue("ASC");
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
        Optional<Chapter> chapter = showDialog(null);
        chapter.ifPresent(c -> {
            try {
                chapterService.create(c);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onEdit() {
        if (selectedChapter == null) {
            showWarning("Selection", "Select a chapter first.");
            return;
        }
        Optional<Chapter> chapter = showDialog(selectedChapter);
        chapter.ifPresent(c -> {
            try {
                chapterService.update(c);
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        });
    }

    @FXML
    private void onDelete() {
        if (selectedChapter == null || selectedChapter.getId() == null) {
            showWarning("Selection", "Select a chapter first.");
            return;
        }
        if (confirmDelete()) {
            try {
                chapterService.delete(selectedChapter.getId());
                refreshCards();
            } catch (SQLException e) {
                showError("DB Error", e.getMessage());
            }
        }
    }

    private void refreshCards() {
        try {
            List<Chapter> chapters = chapterService.findPage(searchField.getText(), null, sortFieldCombo.getValue(), directionCombo.getValue(), 1, 1000);
            selectedChapter = null;
            selectedCard = null;
            chapterContainer.getChildren().clear();
            for (Chapter chapter : chapters) {
                chapterContainer.getChildren().add(createChapterCard(chapter));
            }
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
        }
    }

    private HBox createChapterCard(Chapter chapter) {
        Label id = new Label(String.valueOf(chapter.getId() == null ? 0 : chapter.getId()));
        id.setStyle("-fx-min-width: 80;");
        id.getStyleClass().add("management-card-label");
        Label title = new Label(nullSafe(chapter.getTitle()));
        title.setStyle("-fx-min-width: 200;");
        title.getStyleClass().add("management-card-label");
        Label order = new Label(String.valueOf(chapter.getChapterOrder()));
        order.setStyle("-fx-min-width: 100;");
        order.getStyleClass().add("management-card-label");
        Label status = new Label(nullSafe(chapter.getStatus()));
        status.setStyle("-fx-min-width: 120;");
        status.getStyleClass().add("management-card-muted");
        Label minScore = new Label(String.valueOf(chapter.getMinScore()));
        minScore.setStyle("-fx-min-width: 100;");
        minScore.getStyleClass().add("management-card-label");
        String quizText = chapter.getQuiz() != null && chapter.getQuiz().getId() != null
            ? "Quiz #" + chapter.getQuiz().getId()
            : "Aucun";
        Label quiz = new Label(quizText);
        quiz.setStyle("-fx-min-width: 120;");
        quiz.getStyleClass().add("management-card-label");

        HBox card = new HBox(15, id, title, order, status, minScore, quiz);
        card.getStyleClass().add("management-card");
        card.setOnMouseClicked(event -> selectCard(chapter, card));
        return card;
    }

    private void selectCard(Chapter chapter, HBox card) {
        if (selectedCard != null) {
            selectedCard.getStyleClass().remove("management-card-selected");
        }
        selectedChapter = chapter;
        selectedCard = card;
        if (!selectedCard.getStyleClass().contains("management-card-selected")) {
            selectedCard.getStyleClass().add("management-card-selected");
        }
    }

    private Optional<Chapter> showDialog(Chapter existing) {
        boolean edit = existing != null;
        Dialog<Chapter> dialog = new Dialog<>();
        VBox dialogContent = AdminDialogStyler.apply(dialog,
            edit ? "Modifier chapitre" : "Ajouter chapitre",
            edit ? "Mettre a jour les informations du chapitre" : "Renseigner les informations du chapitre",
            760,
            560);
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Course> courseCombo = new ComboBox<>();
        AdminDialogStyler.styleComboBox(courseCombo);
        TextField titleField = new TextField(edit ? existing.getTitle() : "");
        AdminDialogStyler.styleField(titleField);
        TextField orderField = new TextField(edit ? String.valueOf(existing.getChapterOrder()) : "1");
        AdminDialogStyler.styleField(orderField);
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("draft", "published", "archived"));
        AdminDialogStyler.styleComboBox(statusCombo);
        statusCombo.setValue(edit ? existing.getStatus() : "draft");
        TextField minScoreField = new TextField(edit ? String.valueOf(existing.getMinScore()) : "0");
        AdminDialogStyler.styleField(minScoreField);
        TextArea contentArea = new TextArea(edit ? existing.getContent() : "");
        contentArea.setWrapText(true);
        contentArea.setPrefRowCount(3);
        AdminDialogStyler.styleTextArea(contentArea);

        try {
            List<Course> courses = lookupService.findAllCourses();
            courseCombo.setItems(FXCollections.observableArrayList(courses));
            if (edit && existing.getCourse() != null) {
                for (Course c : courses) {
                    if (c.getId().equals(existing.getCourse().getId())) {
                        courseCombo.setValue(c);
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
        Label titleError = new Label();
        Label courseError = new Label();
        Label orderError = new Label();
        Label minScoreError = new Label();
        Label statusError = new Label();
        Label contentError = new Label();

        titleError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        courseError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        orderError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        minScoreError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        statusError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        contentError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");

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
        Label orderLabel = new Label("Ordre");
        AdminDialogStyler.styleFormLabel(orderLabel);
        grid.add(orderLabel, 0, 4);
        grid.add(orderField, 1, 4);
        grid.add(orderError, 1, 5);
        Label statusLabel = new Label("Statut");
        AdminDialogStyler.styleFormLabel(statusLabel);
        grid.add(statusLabel, 0, 6);
        grid.add(statusCombo, 1, 6);
        grid.add(statusError, 1, 7);
        Label minScoreLabel = new Label("Score minimum");
        AdminDialogStyler.styleFormLabel(minScoreLabel);
        grid.add(minScoreLabel, 0, 8);
        grid.add(minScoreField, 1, 8);
        grid.add(minScoreError, 1, 9);
        Label contentLabel = new Label("Contenu");
        AdminDialogStyler.styleFormLabel(contentLabel);
        grid.add(contentLabel, 0, 10);
        grid.add(contentArea, 1, 10);
        grid.add(contentError, 1, 11);
        ColumnConstraints labelColumn = new ColumnConstraints();
        labelColumn.setMinWidth(150);
        labelColumn.setPrefWidth(150);
        labelColumn.setHalignment(javafx.geometry.HPos.LEFT);
        ColumnConstraints valueColumn = new ColumnConstraints();
        valueColumn.setHgrow(Priority.ALWAYS);
        valueColumn.setFillWidth(true);
        grid.getColumnConstraints().addAll(labelColumn, valueColumn);
        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Informations du chapitre"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(AdminDialogStyler.createSectionLabel("Informations du chapitre"));
        dialogContent.getChildren().add(AdminDialogStyler.createSectionSeparator());
        dialogContent.getChildren().add(grid);
        dialogContent.getChildren().add(AdminDialogStyler.createFooterHint("Le chapitre doit rester court, ordonne et rattache a un seul cours."));
        dialogContent.getChildren().add(AdminDialogStyler.createFooterHint("Le chapitre doit etre rattache a un cours et suivre un ordre logique."));

        final Chapter[] dialogResult = new Chapter[1];
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
            orderError.setText("");
            minScoreError.setText("");
            statusError.setText("");
            contentError.setText("");

            boolean valid = true;
            String title = titleField.getText() == null ? "" : titleField.getText().trim();
            if (title.length() < 3 || title.length() > 30) {
                titleError.setText("Titre obligatoire (3 a 30 caracteres).");
                valid = false;
            }
            if (courseCombo.getValue() == null || courseCombo.getValue().getId() == null) {
                courseError.setText("Cours obligatoire.");
                valid = false;
            }

            int parsedOrder = 0;
            try {
                parsedOrder = Integer.parseInt(orderField.getText().trim());
                if (parsedOrder <= 0) {
                    orderError.setText("Order doit etre > 0.");
                    valid = false;
                }
            } catch (Exception e) {
                orderError.setText("Order invalide.");
                valid = false;
            }

            String status = statusCombo.getValue() == null ? "" : statusCombo.getValue().trim();
            if (status.isEmpty() || status.length() > 30) {
                statusError.setText("Statut obligatoire (max 30 caracteres).");
                valid = false;
            }

            float parsedMinScore = 0f;
            try {
                parsedMinScore = Float.parseFloat(minScoreField.getText().trim());
                if (parsedMinScore < 0 || parsedMinScore > 100) {
                    minScoreError.setText("Min Score entre 0 et 100.");
                    valid = false;
                }
            } catch (Exception e) {
                minScoreError.setText("Min Score invalide.");
                valid = false;
            }

            String content = contentArea.getText() == null ? "" : contentArea.getText().trim();
            if (content.isEmpty() || content.length() > 255) {
                contentError.setText("Contenu obligatoire (max 255 caracteres).");
                valid = false;
            }

            if (!valid) {
                event.consume();
                return;
            }

            Chapter chapter = edit ? existing : new Chapter();
            chapter.setTitle(title);
            chapter.setCourse(courseCombo.getValue());
            chapter.setChapterOrder(parsedOrder);
            chapter.setStatus(status);
            chapter.setMinScore(parsedMinScore);
            chapter.setContent(content);
            dialogResult[0] = chapter;
        });

        dialog.setResultConverter(bt -> bt == save ? dialogResult[0] : null);

        return dialog.showAndWait();
    }

    private boolean confirmDelete() {
        return new Alert(Alert.AlertType.CONFIRMATION, "Delete selected item?").showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
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
