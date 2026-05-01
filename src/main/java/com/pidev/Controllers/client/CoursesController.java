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
        VBox card = new VBox();
        card.getStyleClass().add("course-card");
        if ("LOCKED".equals(status)) card.getStyleClass().add("locked");
        card.setPrefWidth(340);
        card.setMaxWidth(340);

        // Header with Icon
        StackPane header = new StackPane();
        header.getStyleClass().add("course-card-header");
        header.setMinHeight(140);
        header.setPrefHeight(140);

        FontIcon icon = new FontIcon("fas-book-open");
        icon.getStyleClass().add("course-card-icon");
        icon.setIconSize(40);
        header.getChildren().add(icon);

        // Content Wrapper
        VBox content = new VBox(12);
        content.setPadding(new Insets(18, 20, 18, 20));
        content.getStyleClass().add("course-card-content");

        // Badges Row
        HBox badgesRow = new HBox(8);
        badgesRow.setAlignment(Pos.CENTER_LEFT);
        badgesRow.getChildren().addAll(
                pill(nullSafe(course.getDifficulty(), "N/A"), "course-pill difficulty-pill"),
                pill(statusLabel(status), statusPillClass(status))
        );
        if (recommended) {
            badgesRow.getChildren().add(pill("⭐ Recommandé", "course-pill recommended-pill"));
        }

        // Title
        Label title = new Label(nullSafe(course.getTitle(), "Sans titre"));
        title.getStyleClass().add("course-card-title");
        title.setWrapText(true);
        title.setMinHeight(50); // Ensure consistency

        // Description
        Label description = new Label(truncate(course.getDescription(), 95));
        description.getStyleClass().add("course-card-description");
        description.setWrapText(true);

        // Progress Section (only if relevant)
        VBox progressSection = new VBox(8);
        if (!"LOCKED".equals(status)) {
            HBox progressHeader = new HBox();
            Label progressLabel = new Label("Progression");
            progressLabel.getStyleClass().add("course-progress-label");
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            Label progressValue = new Label(Math.round(course.getValidationScore()) + "%");
            progressValue.getStyleClass().add("course-progress-value");
            progressHeader.getChildren().addAll(progressLabel, spacer, progressValue);

            ProgressBar progressBar = new ProgressBar(Math.max(0, Math.min(1, course.getValidationScore() / 100f)));
            progressBar.getStyleClass().add("course-progress-bar");
            progressBar.setMaxWidth(Double.MAX_VALUE);
            
            progressSection.getChildren().addAll(progressHeader, progressBar);
        }

        // Footer Meta Info
        HBox footer = new HBox(15);
        footer.getStyleClass().add("course-card-footer");
        footer.setAlignment(Pos.CENTER_LEFT);
        
        HBox durBox = new HBox(5, new FontIcon("fas-clock"), new Label(course.getDuration() + "m"));
        durBox.getStyleClass().add("meta-info-item");
        
        HBox secBox = new HBox(5, new FontIcon("fas-layer-group"), new Label(course.getSectionsToReview() == null ? "0" : course.getSectionsToReview().size() + ""));
        secBox.getStyleClass().add("meta-info-item");

        footer.getChildren().addAll(durBox, secBox);
        
        if (completeness != null) {
            HBox qualBox = new HBox(5, new FontIcon("fas-check-circle"), new Label(completeness.completenessScore() + "%"));
            qualBox.getStyleClass().add("meta-info-item");
            footer.getChildren().add(qualBox);
        }

        // Action Button
        Button actionButton = new Button(actionLabel(status));
        actionButton.getStyleClass().addAll("courses-action-btn", "primary");
        actionButton.setMaxWidth(Double.MAX_VALUE);
        actionButton.setOnAction(event -> openCourseDetail(course));

        content.getChildren().addAll(badgesRow, title, description, progressSection, footer, actionButton);
        card.getChildren().addAll(header, content);
        
        return card;
    }

    private void openCourseDetail(Course course) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Fxml/client/CourseDetailView.fxml"));
            Parent root = loader.load();
            CourseDetailController controller = loader.getController();
            controller.setCourse(course);
            // On passe l'utilisateur connecté (Option A : via UserSession singleton)
            controller.setCurrentUser(com.pidev.utils.UserSession.getCurrentUser());
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
