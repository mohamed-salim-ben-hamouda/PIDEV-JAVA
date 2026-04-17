package com.pidev.Controllers.client;

import com.pidev.Services.CourseService;
import com.pidev.models.Course;
import javafx.fxml.FXML;
import javafx.collections.FXCollections;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class CoursesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> sortFieldCombo;
    @FXML private ComboBox<String> directionCombo;
    @FXML private FlowPane courseFlow;
    @FXML private Label totalCoursesLabel;
    @FXML private Label activeCoursesLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label visibleCoursesLabel;
    @FXML private Label courseCountLabel;

    private final CourseService courseService = new CourseService();

    @FXML
    public void initialize() {
        sortFieldCombo.setItems(FXCollections.observableArrayList("title", "duration", "validationScore"));
        sortFieldCombo.setValue("title");
        directionCombo.setItems(FXCollections.observableArrayList("ASC", "DESC"));
        directionCombo.setValue("ASC");
        loadCourses();
    }

    @FXML
    private void onRefresh() {
        loadCourses();
    }

    @FXML
    private void onSearch() {
        loadCourses();
    }

    @FXML
    private void onSortChanged() {
        loadCourses();
    }

    private List<Node> buildCourseCards(List<Course> courses) {
        List<Node> cards = new ArrayList<>();
        if (courses == null || courses.isEmpty()) {
            cards.add(buildEmptyState("Aucun cours disponible."));
            return cards;
        }

        for (Course course : courses) {
            cards.add(buildCourseCard(course));
        }
        return cards;
    }

    private Node buildCourseCard(Course course) {
        VBox card = new VBox(14);
        card.getStyleClass().add("course-card");
        card.setPrefWidth(330);
        card.setMaxWidth(330);

        StackPane header = new StackPane();
        header.getStyleClass().add("course-card-header");
        header.setMinHeight(150);
        header.setPrefHeight(150);

        FontIcon icon = new FontIcon("fas-book");
        icon.getStyleClass().add("course-card-icon");
        icon.setIconSize(34);
        header.getChildren().add(icon);

        Label title = new Label(nullSafe(course.getTitle(), "Sans titre"));
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);

        Label description = new Label(truncate(course.getDescription(), 110));
        description.getStyleClass().add("course-card-description");
        description.setWrapText(true);

        HBox infoRow = new HBox(12);
        infoRow.setAlignment(Pos.CENTER_LEFT);
        infoRow.getChildren().addAll(
                pill(course.getDuration() + " min", "course-pill duration-pill"),
                pill(nullSafe(course.getDifficulty(), "N/A"), "course-pill difficulty-pill"),
                pill(course.isActive() ? "Actif" : "Inactif", course.isActive() ? "course-pill active-pill" : "course-pill inactive-pill")
        );

        VBox progressBox = new VBox(6);
        Label progressLabel = new Label("Score de validation");
        progressLabel.getStyleClass().add("course-progress-label");
        ProgressBar progressBar = new ProgressBar(Math.max(0, Math.min(1, course.getValidationScore() / 100f)));
        progressBar.getStyleClass().add("course-progress-bar");
        Label progressValue = new Label(Math.round(course.getValidationScore()) + "%");
        progressValue.getStyleClass().add("course-progress-value");
        HBox progressHeader = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        progressHeader.getChildren().addAll(progressLabel, spacer, progressValue);
        progressBox.getChildren().addAll(progressHeader, progressBar);

        HBox footer = new HBox(10);
        footer.setAlignment(Pos.CENTER_LEFT);
        Label durationIcon = new Label("⏱");
        durationIcon.getStyleClass().add("course-meta-icon");
        Label durationText = new Label(course.getDuration() + " minutes");
        durationText.getStyleClass().add("course-meta-text");
        Label sectionsIcon = new Label("▤");
        sectionsIcon.getStyleClass().add("course-meta-icon");
        Label sectionsText = new Label(course.getSectionsToReview() == null ? "0 sections" : course.getSectionsToReview().size() + " sections");
        sectionsText.getStyleClass().add("course-meta-text");
        footer.getChildren().addAll(durationIcon, durationText, sectionsIcon, sectionsText);

        Button consultButton = new Button("Consulter le cours");
        consultButton.getStyleClass().addAll("courses-action-btn", "primary");
        consultButton.setMaxWidth(Double.MAX_VALUE);
        consultButton.setOnAction(event -> openCourseDetail(course));

        VBox.setMargin(consultButton, new javafx.geometry.Insets(0, 18, 0, 18));
        card.getChildren().addAll(header, title, description, infoRow, progressBox, footer, consultButton);
        return card;
    }

    private void loadCourses() {
        try {
            List<Course> courses = courseService.findPage(
                searchField.getText(),
                sortFieldCombo.getValue(),
                directionCombo.getValue(),
                1,
                1000
            );
            courseFlow.getChildren().setAll(buildCourseCards(courses));

            int activeCourses = 0;
            float scoreSum = 0f;
            for (Course course : courses) {
                if (course.isActive()) {
                    activeCourses++;
                }
                scoreSum += course.getValidationScore();
            }

            float averageScore = courses.isEmpty() ? 0f : scoreSum / courses.size();

            totalCoursesLabel.setText(String.valueOf(courses.size()));
            activeCoursesLabel.setText(String.valueOf(activeCourses));
            averageScoreLabel.setText(Math.round(averageScore) + "%");
            visibleCoursesLabel.setText(String.valueOf(courses.size()));
            courseCountLabel.setText(String.valueOf(courses.size()));
        } catch (SQLException e) {
            courseFlow.getChildren().setAll(buildEmptyState("Impossible de charger les cours."));
            totalCoursesLabel.setText("0");
            activeCoursesLabel.setText("0");
            averageScoreLabel.setText("0%");
            visibleCoursesLabel.setText("0");
            courseCountLabel.setText("0");
            e.printStackTrace();
        }
    }

    private void openCourseDetail(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/CourseDetailView.fxml"));
            Parent root = loader.load();
            CourseDetailController controller = loader.getController();
            controller.setCourse(course);
            root.setUserData("course-detail-window");

            Stage detailStage = new Stage();
            Scene detailScene = new Scene(root, 1240, 860);
            detailStage.setTitle("Detail du cours - " + nullSafe(course.getTitle(), "Cours"));
            detailStage.setMinWidth(1080);
            detailStage.setMinHeight(760);
            detailStage.setScene(detailScene);
            detailStage.show();
        } catch (IOException e) {
            showError("Navigation", "Impossible d'ouvrir la page du cours.");
        }
    }

    private Node buildEmptyState(String message) {
        VBox empty = new VBox(10);
        empty.getStyleClass().add("course-empty-state");
        empty.setAlignment(Pos.CENTER);
        empty.setPrefWidth(1000);

        Label title = new Label(message);
        title.getStyleClass().add("course-empty-title");
        Label subtitle = new Label("Rechargez la page lorsque les données seront disponibles.");
        subtitle.getStyleClass().add("course-empty-subtitle");
        empty.getChildren().addAll(title, subtitle);
        return empty;
    }

    private Label pill(String text, String styleClass) {
        Label pill = new Label(text == null || text.isBlank() ? "N/A" : text);
        pill.getStyleClass().addAll(styleClass.split(" "));
        return pill;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            return "Aucune description disponible.";
        }
        String cleaned = value.trim();
        if (cleaned.length() <= maxLength) {
            return cleaned;
        }
        return cleaned.substring(0, Math.max(0, maxLength - 3)) + "...";
    }

    private String nullSafe(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}
