package com.pidev.Controllers.admin;

import com.pidev.Services.QuizStatisticsService;
import com.pidev.models.QuestionStatistic;
import com.pidev.models.QuizStatisticsSummary;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

import java.sql.SQLException;
import java.util.List;

public class QuizResultsController {
    @FXML private TextField searchField;
    @FXML private TilePane quizCardsPane;
    @FXML private Label emptyStateLabel;
    @FXML private ScrollPane cardsScrollPane;

    private final QuizStatisticsService statisticsService = new QuizStatisticsService();
    private final ObservableList<QuizStatisticsSummary> allSummaries = FXCollections.observableArrayList();
    private final FilteredList<QuizStatisticsSummary> filteredSummaries = new FilteredList<>(allSummaries, item -> true);

    @FXML
    public void initialize() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilter(newValue));
        cardsScrollPane.viewportBoundsProperty().addListener((obs, oldBounds, newBounds) -> adaptTileColumns(newBounds.getWidth()));

        loadData();
    }

    @FXML
    private void onSearch() {
        applyFilter(searchField.getText());
    }

    @FXML
    private void onRefresh() {
        loadData();
    }

    private void loadData() {
        try {
            List<QuizStatisticsSummary> summaries = statisticsService.findQuizSummaries();
            allSummaries.setAll(summaries);
            applyFilter(searchField.getText());
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
            allSummaries.clear();
            filteredSummaries.setPredicate(item -> false);
            renderCards();
        }
    }

    private void applyFilter(String searchText) {
        String normalized = searchText == null ? "" : searchText.trim().toLowerCase();
        filteredSummaries.setPredicate(summary -> {
            if (normalized.isEmpty()) {
                return true;
            }

            String quizTitle = summary.getQuiz().getTitle() == null ? "" : summary.getQuiz().getTitle().toLowerCase();
            String courseTitle = summary.getQuiz().getCourse() != null && summary.getQuiz().getCourse().getTitle() != null
                    ? summary.getQuiz().getCourse().getTitle().toLowerCase()
                    : "";
            String chapterTitle = summary.getQuiz().getChapter() != null && summary.getQuiz().getChapter().getTitle() != null
                    ? summary.getQuiz().getChapter().getTitle().toLowerCase()
                    : "";
            return quizTitle.contains(normalized) || courseTitle.contains(normalized) || chapterTitle.contains(normalized);
        });
        renderCards();
    }

    private void renderCards() {
        quizCardsPane.getChildren().clear();
        emptyStateLabel.setVisible(filteredSummaries.isEmpty());
        emptyStateLabel.setManaged(filteredSummaries.isEmpty());

        if (filteredSummaries.isEmpty()) {
            return;
        }

        for (QuizStatisticsSummary summary : filteredSummaries) {
            quizCardsPane.getChildren().add(createQuizCard(summary));
        }
    }

    private Node createQuizCard(QuizStatisticsSummary summary) {
        VBox card = new VBox(14);
        card.getStyleClass().add("quiz-result-card");
        card.setPrefWidth(320);
        card.setMaxWidth(360);
        card.setPadding(new Insets(14));

        String quizTitle = summary.getQuiz().getTitle() == null ? "QUIZ" : summary.getQuiz().getTitle().toUpperCase();
        String category = summary.getQuiz().getCourse() != null && summary.getQuiz().getCourse().getTitle() != null
                ? summary.getQuiz().getCourse().getTitle()
                : "Sans catégorie";

        Label titleLabel = new Label(quizTitle);
        titleLabel.getStyleClass().add("quiz-result-title");
        Label categoryLabel = new Label(category);
        categoryLabel.getStyleClass().add("quiz-result-category");
        categoryLabel.setWrapText(true);
        categoryLabel.setMaxWidth(120);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        HBox headerRow = new HBox(10, titleLabel, spacer, categoryLabel);
        headerRow.setAlignment(Pos.TOP_LEFT);

        VBox attemptsBox = createMetricBox(String.valueOf(summary.getTotalAttempts()), "Tentatives", "attempts");
        VBox averageBox = createMetricBox(String.format("%.1f%%", summary.getAverageScore()), "Moyenne", "average");
        HBox metricsRow = new HBox(12, attemptsBox, averageBox);

        Label passRateLabel = new Label("Taux de réussite");
        passRateLabel.getStyleClass().add("quiz-result-meta");
        Label passRateValue = new Label(String.format("%.1f%%", summary.getPassRate()));
        passRateValue.getStyleClass().add("quiz-result-meta-value");
        passRateValue.getStyleClass().add(rateClass(summary.getPassRate()));
        Region passSpacer = new Region();
        HBox.setHgrow(passSpacer, Priority.ALWAYS);
        HBox passRateRow = new HBox(8, passRateLabel, passSpacer, passRateValue);
        passRateRow.setAlignment(Pos.CENTER_LEFT);

        ProgressBar passRateProgress = new ProgressBar(Math.max(0, Math.min(1, summary.getPassRate() / 100.0)));
        passRateProgress.getStyleClass().add("quiz-pass-progress");
        passRateProgress.setMaxWidth(Double.MAX_VALUE);
        passRateProgress.setStyle(progressAccent(summary.getPassRate()));

        Label successCountLabel = new Label(summary.getPassedCount() + " réussis");
        successCountLabel.getStyleClass().add("quiz-result-foot-success");
        Label failedCountLabel = new Label(summary.getFailedCount() + " échoués");
        failedCountLabel.getStyleClass().add("quiz-result-foot-fail");
        Region footSpacer = new Region();
        HBox.setHgrow(footSpacer, Priority.ALWAYS);
        HBox footerCounts = new HBox(8, successCountLabel, footSpacer, failedCountLabel);
        footerCounts.setAlignment(Pos.CENTER_LEFT);

        Button detailsButton = new Button("Voir les détails");
        detailsButton.getStyleClass().add("quiz-result-details-btn");
        detailsButton.setMaxWidth(Double.MAX_VALUE);
        detailsButton.setOnAction(event -> showDetailsDialog(summary));

        card.getChildren().addAll(headerRow, metricsRow, passRateRow, passRateProgress, footerCounts, detailsButton);
        return card;
    }

    private VBox createMetricBox(String value, String caption, String variant) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("quiz-metric-value");
        valueLabel.getStyleClass().add("quiz-metric-value-" + variant);
        Label captionLabel = new Label(caption);
        captionLabel.getStyleClass().add("quiz-metric-caption");
        VBox box = new VBox(6, valueLabel, captionLabel);
        box.getStyleClass().add("quiz-metric-box");
        box.setAlignment(Pos.CENTER);
        box.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private String progressAccent(double passRate) {
        if (passRate >= 70.0) {
            return "-fx-accent: #24c08f;";
        }
        if (passRate >= 40.0) {
            return "-fx-accent: #f0b429;";
        }
        return "-fx-accent: #e05b65;";
    }

    private String rateClass(double passRate) {
        if (passRate >= 70.0) {
            return "quiz-rate-good";
        }
        if (passRate >= 40.0) {
            return "quiz-rate-medium";
        }
        return "quiz-rate-low";
    }

    private void showDetailsDialog(QuizStatisticsSummary summary) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails du quiz");
        dialog.setHeaderText(summary.getQuiz().getTitle());
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox content = new VBox(10);
        content.setPadding(new Insets(8));

        Label summaryLabel = new Label(String.format(
                "Tentatives: %d | Étudiants: %d | Moyenne: %.1f%% | Réussite: %.1f%%",
                summary.getTotalAttempts(),
                summary.getUniqueStudents(),
                summary.getAverageScore(),
                summary.getPassRate()
        ));
        summaryLabel.getStyleClass().add("management-card-muted");
        content.getChildren().add(summaryLabel);

        try {
            List<QuestionStatistic> statistics = statisticsService.findQuestionStatistics(summary.getQuiz().getId());
            if (statistics.isEmpty()) {
                content.getChildren().add(new Label("Aucune statistique question disponible."));
            } else {
                for (QuestionStatistic statistic : statistics) {
                    content.getChildren().add(createQuestionRow(statistic));
                }
            }
        } catch (SQLException e) {
            content.getChildren().add(new Label("Erreur lors du chargement des détails."));
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(420);
        dialog.getDialogPane().setContent(scrollPane);

        dialog.showAndWait();
    }

    private HBox createQuestionRow(QuestionStatistic statistic) {
        Label questionLabel = new Label(statistic.getQuestion().toString());
        questionLabel.setWrapText(true);
        questionLabel.setMaxWidth(Double.MAX_VALUE);
        questionLabel.getStyleClass().add("management-card-label");

        Label attemptsLabel = new Label(String.valueOf(statistic.getTotalResponses()));
        attemptsLabel.getStyleClass().add("management-card-label");

        Label correctLabel = new Label(String.valueOf(statistic.getCorrectCount()));
        correctLabel.getStyleClass().add("management-card-label");

        Label rateLabel = new Label(String.format("%.1f%%", statistic.getSuccessRate()));
        rateLabel.getStyleClass().add("management-card-label");

        ProgressBar progressBar = new ProgressBar(statistic.getSuccessRate() / 100.0);
        progressBar.setPrefWidth(180.0);
        progressBar.setMaxWidth(Double.MAX_VALUE);

        VBox questionBox = new VBox(6, questionLabel, progressBar);
        questionBox.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(questionBox, Priority.ALWAYS);

        HBox row = new HBox(14, questionBox, attemptsLabel, correctLabel, rateLabel);
        row.getStyleClass().add("management-card");
        row.setStyle("-fx-alignment: center-left;");
        return row;
    }

    private void adaptTileColumns(double viewportWidth) {
        if (viewportWidth <= 360) {
            quizCardsPane.setPrefColumns(1);
        } else if (viewportWidth <= 760) {
            quizCardsPane.setPrefColumns(2);
        } else if (viewportWidth <= 1140) {
            quizCardsPane.setPrefColumns(3);
        } else {
            quizCardsPane.setPrefColumns(4);
        }
    }

    private void showError(String title, String message) {
        javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}