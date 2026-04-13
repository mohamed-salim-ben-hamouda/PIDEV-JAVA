package com.pidev.Controllers.admin;

import com.pidev.Services.AdminLookupService;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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

        HBox card = new HBox(15, id, title, order, status, minScore);
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
        dialog.setTitle(edit ? "Edit Chapter" : "Add Chapter");
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(save, ButtonType.CANCEL);

        ComboBox<Course> courseCombo = new ComboBox<>();
        TextField titleField = new TextField(edit ? existing.getTitle() : "");
        TextField orderField = new TextField(edit ? String.valueOf(existing.getChapterOrder()) : "1");
        ComboBox<String> statusCombo = new ComboBox<>(FXCollections.observableArrayList("draft", "published", "archived"));
        statusCombo.setValue(edit ? existing.getStatus() : "draft");
        TextField minScoreField = new TextField(edit ? String.valueOf(existing.getMinScore()) : "0");
        TextField contentField = new TextField(edit ? existing.getContent() : "");

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
        grid.setHgap(10);
        grid.setVgap(10);
        Label titleError = new Label();
        Label courseError = new Label();
        Label orderError = new Label();
        Label minScoreError = new Label();

        titleError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        courseError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        orderError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");
        minScoreError.setStyle("-fx-text-fill: #d32f2f; -fx-font-size: 11;");

        grid.add(new Label("Title"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(titleError, 1, 1);
        grid.add(new Label("Course"), 0, 2);
        grid.add(courseCombo, 1, 2);
        grid.add(courseError, 1, 3);
        grid.add(new Label("Order"), 0, 4);
        grid.add(orderField, 1, 4);
        grid.add(orderError, 1, 5);
        grid.add(new Label("Status"), 0, 6);
        grid.add(statusCombo, 1, 6);
        grid.add(new Label("Min Score"), 0, 7);
        grid.add(minScoreField, 1, 7);
        grid.add(minScoreError, 1, 8);
        grid.add(new Label("Content URL"), 0, 9);
        grid.add(contentField, 1, 9);
        dialog.getDialogPane().setContent(grid);

        final Chapter[] dialogResult = new Chapter[1];
        Node saveButton = dialog.getDialogPane().lookupButton(save);
        saveButton.addEventFilter(ActionEvent.ACTION, event -> {
            titleError.setText("");
            courseError.setText("");
            orderError.setText("");
            minScoreError.setText("");

            boolean valid = true;
            if (titleField.getText() == null || titleField.getText().trim().length() < 3) {
                titleError.setText("Titre obligatoire (min 3 caracteres).");
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

            if (!valid) {
                event.consume();
                return;
            }

            Chapter chapter = edit ? existing : new Chapter();
            chapter.setTitle(titleField.getText().trim());
            chapter.setCourse(courseCombo.getValue());
            chapter.setChapterOrder(parsedOrder);
            chapter.setStatus(statusCombo.getValue());
            chapter.setMinScore(parsedMinScore);
            chapter.setContent(contentField.getText() == null ? "" : contentField.getText().trim());
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
