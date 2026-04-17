package com.pidev.Controllers.client;

import com.pidev.Services.CourseAdvancedBusinessService;
import com.pidev.Services.CourseService;
import com.pidev.models.Course;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.kordamp.ikonli.javafx.FontIcon;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class CoursesController {

    @FXML private TextField searchField;
    @FXML private ComboBox<String> difficultyFilterCombo;
    @FXML private FlowPane inProgressFlow;
    @FXML private FlowPane completedFlow;
    @FXML private FlowPane lockedFlow;
    @FXML private FlowPane recommendedFlow;
    @FXML private Label inProgressLabel;
    @FXML private Label completedLabel;
    @FXML private Label lockedLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label inProgressCountLabel;
    @FXML private Label completedCountLabel;
    @FXML private Label lockedCountLabel;
    @FXML private Label recommendedCountLabel;
    @FXML private Label recommendedTopLabel;
    @FXML private Label recommendationMessageLabel;

    private final CourseService courseService = new CourseService();
    private List<Course> cachedCourses = new ArrayList<>();
    private List<CourseAdvancedBusinessService.CourseSuggestion> cachedSuggestions = new ArrayList<>();

    @FXML
    public void initialize() {
        difficultyFilterCombo.setItems(FXCollections.observableArrayList("ALL", "BEGINNER", "INTERMEDIATE", "ADVANCED"));
        difficultyFilterCombo.setValue("ALL");
        loadCourses();
    }

    @FXML
    private void onRefresh() {
        searchField.clear();
        difficultyFilterCombo.setValue("ALL");
        loadCourses();
    }

    @FXML
    private void onSearch() {
        applyFiltersAndRender();
    }

    @FXML
    private void onSortChanged() {
        applyFiltersAndRender();
    }

    private void loadCourses() {
        try {
            cachedCourses = courseService.findAll();
            cachedSuggestions = courseService.suggestNextCourses(null, 6);
            applyFiltersAndRender();
        } catch (SQLException e) {
            inProgressFlow.getChildren().setAll(buildEmptyState("Impossible de charger les cours."));
            completedFlow.getChildren().setAll(buildEmptyState("Impossible de charger les cours."));
            lockedFlow.getChildren().setAll(buildEmptyState("Impossible de charger les cours."));
            recommendedFlow.getChildren().setAll(buildEmptyState("Impossible de charger les recommandations."));
            inProgressLabel.setText("0");
            completedLabel.setText("0");
            lockedLabel.setText("0");
            averageScoreLabel.setText("0%");
            inProgressCountLabel.setText("0");
            completedCountLabel.setText("0");
            lockedCountLabel.setText("0");
            recommendedCountLabel.setText("0");
            recommendedTopLabel.setText("Top: -");
            recommendationMessageLabel.setText("Erreur de chargement");
            e.printStackTrace();
        }
    }

    private void applyFiltersAndRender() {
        List<Course> courses = filterCourses(cachedCourses);

        List<Course> inProgressCourses = new ArrayList<>();
        List<Course> completedCourses = new ArrayList<>();
        List<Course> lockedCourses = new ArrayList<>();

        Map<Integer, CourseAdvancedBusinessService.CourseCompleteness> completenessById = new HashMap<>();
        for (Course course : courses) {
            CourseAdvancedBusinessService.CourseCompleteness completeness = course.getId() == null
                    ? null
                    : courseService.evaluateCompleteness(course);

            if (course.getId() != null && completeness != null) {
                completenessById.put(course.getId(), completeness);
            }

            if (!course.isActive()) {
                lockedCourses.add(course);
                continue;
            }

            boolean completeByScore = course.getValidationScore() >= 80f;
            boolean completeByCompleteness = completeness != null && completeness.completenessScore() >= 75;
            if (completeByScore && completeByCompleteness) {
                completedCourses.add(course);
            } else {
                inProgressCourses.add(course);
            }
        }

        Set<Integer> recommendedIds = cachedSuggestions.stream()
                .map(suggestion -> suggestion.course().getId())
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Integer, CourseAdvancedBusinessService.CourseSuggestion> suggestionById = cachedSuggestions.stream()
                .filter(suggestion -> suggestion.course().getId() != null)
                .collect(Collectors.toMap(suggestion -> suggestion.course().getId(), suggestion -> suggestion, (left, right) -> left));

        List<Course> recommendedCourses = courses.stream()
                .filter(course -> course.getId() != null && recommendedIds.contains(course.getId()))
                .filter(course -> !completedCourses.contains(course))
                .toList();

        inProgressFlow.getChildren().setAll(buildCourseCards(inProgressCourses, "IN_PROGRESS", recommendedIds, completenessById, suggestionById));
        completedFlow.getChildren().setAll(buildCourseCards(completedCourses, "COMPLETED", recommendedIds, completenessById, suggestionById));
        lockedFlow.getChildren().setAll(buildCourseCards(lockedCourses, "LOCKED", recommendedIds, completenessById, suggestionById));
        recommendedFlow.getChildren().setAll(buildCourseCards(recommendedCourses, "RECOMMENDED", recommendedIds, completenessById, suggestionById));

        float scoreSum = 0f;
        int scoreCount = 0;
        for (Course course : completedCourses) {
            scoreSum += course.getValidationScore();
            scoreCount++;
        }
        float averageScore = scoreCount == 0 ? 0f : scoreSum / scoreCount;

        inProgressLabel.setText(String.valueOf(inProgressCourses.size()));
        completedLabel.setText(String.valueOf(completedCourses.size()));
        lockedLabel.setText(String.valueOf(lockedCourses.size()));
        averageScoreLabel.setText(Math.round(averageScore) + "%");
        inProgressCountLabel.setText(String.valueOf(inProgressCourses.size()));
        completedCountLabel.setText(String.valueOf(completedCourses.size()));
        lockedCountLabel.setText(String.valueOf(lockedCourses.size()));
        recommendedCountLabel.setText(String.valueOf(recommendedCourses.size()));

        CourseAdvancedBusinessService.CourseSuggestion topSuggestion = cachedSuggestions.isEmpty() ? null : cachedSuggestions.get(0);
        recommendedTopLabel.setText(topSuggestion == null
                ? "Top: -"
                : "Top: " + truncate(topSuggestion.course().getTitle(), 24));
        recommendationMessageLabel.setText(recommendedCourses.isEmpty()
                ? "Aucune recommandation pour le moment"
                : "Suggestions personnalisees selon votre progression");
    }

    private List<Course> filterCourses(List<Course> source) {
        if (source == null || source.isEmpty()) {
            return List.of();
        }

        String query = searchField.getText() == null
                ? ""
                : searchField.getText().trim().toLowerCase(Locale.ROOT);
        String difficulty = difficultyFilterCombo.getValue() == null
                ? "ALL"
                : difficultyFilterCombo.getValue();

        return source.stream()
                .filter(course -> "ALL".equals(difficulty) || difficulty.equalsIgnoreCase(nullSafe(course.getDifficulty(), "")))
                .filter(course -> query.isEmpty() || matchesQuery(course, query))
                .toList();
    }

    private boolean matchesQuery(Course course, String query) {
        return contains(course.getTitle(), query)
                || contains(course.getDescription(), query)
                || contains(course.getDifficulty(), query)
                || contains(course.getMaterial(), query);
    }

    private boolean contains(String value, String query) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(query);
    }

    private List<Node> buildCourseCards(
            List<Course> courses,
            String status,
            Set<Integer> recommendedCourseIds,
            Map<Integer, CourseAdvancedBusinessService.CourseCompleteness> completenessById,
            Map<Integer, CourseAdvancedBusinessService.CourseSuggestion> suggestionById
    ) {
        List<Node> cards = new ArrayList<>();
        if (courses == null || courses.isEmpty()) {
            cards.add(buildEmptyState(emptyStateMessage(status)));
            return cards;
        }

        for (Course course : courses) {
            boolean recommended = course.getId() != null && recommendedCourseIds.contains(course.getId());
            CourseAdvancedBusinessService.CourseCompleteness completeness = course.getId() != null
                    ? completenessById.get(course.getId())
                    : null;
            CourseAdvancedBusinessService.CourseSuggestion suggestion = course.getId() != null
                    ? suggestionById.get(course.getId())
                    : null;
            cards.add(buildCourseCard(course, status, recommended, completeness, suggestion));
        }
        return cards;
    }

    private Node buildCourseCard(
            Course course,
            String status,
            boolean recommended,
            CourseAdvancedBusinessService.CourseCompleteness completeness,
            CourseAdvancedBusinessService.CourseSuggestion suggestion
    ) {
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
                pill(statusLabel(status), statusPillClass(status))
        );
        if (recommended) {
            infoRow.getChildren().add(pill("Recommande", "course-pill active-pill"));
        }
        if (suggestion != null) {
            infoRow.getChildren().add(pill(suggestion.badge(), "course-pill duration-pill"));
        }

        VBox progressBox = new VBox(6);
        Label progressLabel = new Label("Score de validation");
        progressLabel.getStyleClass().add("course-progress-label");
        ProgressBar progressBar = new ProgressBar(Math.max(0, Math.min(1, course.getValidationScore() / 100f)));
        progressBar.getStyleClass().add("course-progress-bar");
        Label progressValue = new Label(Math.round(course.getValidationScore()) + "%");
        progressValue.getStyleClass().add("course-progress-value");
        HBox progressHeader = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
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

        if (completeness != null) {
            Label qualityText = new Label("Qualite: " + completeness.completenessScore() + "%");
            qualityText.getStyleClass().add("course-meta-text");
            footer.getChildren().add(qualityText);
        }
        if (suggestion != null && suggestion.reason() != null && !suggestion.reason().isBlank()) {
            Label reasonText = new Label("Conseil: " + truncate(suggestion.reason(), 60));
            reasonText.getStyleClass().add("course-meta-text");
            footer.getChildren().add(reasonText);
        }

        Button consultButton = new Button(actionLabel(status));
        consultButton.getStyleClass().addAll("courses-action-btn", "primary");
        consultButton.setMaxWidth(Double.MAX_VALUE);
        consultButton.setOnAction(event -> openCourseDetail(course));

        VBox.setMargin(consultButton, new Insets(0, 18, 0, 18));
        card.getChildren().addAll(header, title, description, infoRow, progressBox, footer, consultButton);
        return card;
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
        Label subtitle = new Label("Rechargez la page lorsque les donnees seront disponibles.");
        subtitle.getStyleClass().add("course-empty-subtitle");
        empty.getChildren().addAll(title, subtitle);
        return empty;
    }

    private String emptyStateMessage(String status) {
        return switch (status) {
            case "IN_PROGRESS" -> "Aucun cours en cours.";
            case "COMPLETED" -> "Aucun cours termine.";
            case "LOCKED" -> "Vous etes inscrit a tous les cours.";
            case "RECOMMENDED" -> "Aucune recommandation disponible.";
            default -> "Aucun cours disponible.";
        };
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "IN_PROGRESS" -> "En cours";
            case "COMPLETED" -> "Termine";
            case "LOCKED" -> "Non inscrit";
            case "RECOMMENDED" -> "Recommande";
            default -> "N/A";
        };
    }

    private String actionLabel(String status) {
        return switch (status) {
            case "IN_PROGRESS" -> "Continuer";
            case "COMPLETED" -> "Revoir";
            case "LOCKED" -> "S'inscrire";
            case "RECOMMENDED" -> "Voir le cours";
            default -> "Consulter le cours";
        };
    }

    private String statusPillClass(String status) {
        return switch (status) {
            case "IN_PROGRESS", "RECOMMENDED" -> "course-pill active-pill";
            case "COMPLETED" -> "course-pill duration-pill";
            case "LOCKED" -> "course-pill inactive-pill";
            default -> "course-pill inactive-pill";
        };
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
