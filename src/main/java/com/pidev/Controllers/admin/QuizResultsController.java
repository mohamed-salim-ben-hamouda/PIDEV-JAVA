package com.pidev.Controllers.admin;

import com.pidev.Services.QuizStatisticsService;
import com.pidev.models.QuizAttemptDetail;
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
import javafx.scene.control.ComboBox;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class QuizResultsController {
    @FXML private TextField searchField;
    @FXML private TilePane quizCardsPane;
    @FXML private Label emptyStateLabel;
    @FXML private ScrollPane cardsScrollPane;

    private final QuizStatisticsService statisticsService = new QuizStatisticsService();
    private final ObservableList<QuizStatisticsSummary> allSummaries = FXCollections.observableArrayList();
    private final FilteredList<QuizStatisticsSummary> filteredSummaries = new FilteredList<>(allSummaries, item -> true);
    private static final DateTimeFormatter DETAIL_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.FRANCE);

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
        dialog.setTitle("Résultats détaillés");
        dialog.setHeaderText(null);
        dialog.getDialogPane().getStyleClass().add("quiz-detail-dialog-pane");
        dialog.getDialogPane().getStylesheets().add(getClass().getResource("/styles/backoffice.css").toExternalForm());
        dialog.getDialogPane().setPrefWidth(1260);
        dialog.getDialogPane().setPrefHeight(760);
        dialog.getDialogPane().getButtonTypes().add(new ButtonType("Fermer", ButtonBar.ButtonData.CANCEL_CLOSE));

        VBox shell = new VBox(14);
        shell.getStyleClass().add("quiz-detail-shell");
        shell.setPadding(new Insets(14, 14, 14, 14));

        Label breadcrumb = new Label("Résultats  >  " + safe(summary.getQuiz().getTitle()));
        breadcrumb.getStyleClass().add("quiz-detail-breadcrumb");

        Label title = new Label("Quiz " + safe(summary.getQuiz().getTitle()));
        title.getStyleClass().add("quiz-detail-title");

        String courseTitle = summary.getQuiz().getCourse() != null && summary.getQuiz().getCourse().getTitle() != null
                ? summary.getQuiz().getCourse().getTitle()
                : "Cours non renseigné";
        String chapterTitle = summary.getQuiz().getChapter() != null && summary.getQuiz().getChapter().getTitle() != null
                ? summary.getQuiz().getChapter().getTitle()
                : "Chapitre optionnel";
        Label subtitle = new Label("Cours: " + courseTitle + " • Chapitre: " + chapterTitle);
        subtitle.getStyleClass().add("quiz-detail-subtitle");

        HBox kpiRow = new HBox(12,
                createDetailKpiCard(String.valueOf(summary.getTotalAttempts()), "Tentatives totales", "violet"),
                createDetailKpiCard(String.valueOf(summary.getUniqueStudents()), "Étudiants uniques", "cyan"),
                createDetailKpiCard(String.valueOf(summary.getPassedCount()), "Réussites", "green"),
                createDetailKpiCard(String.valueOf(summary.getFailedCount()), "Échecs", "red"),
                createDetailKpiCard(String.format("%.1f%%", summary.getAverageScore()), "Score moyen", "orange"),
                createDetailKpiCard(String.format("%.1f%%", summary.getPassRate()), "Taux de réussite", "purple")
        );

        HBox filterRow = new HBox(10);
        filterRow.setAlignment(Pos.CENTER_LEFT);
        TextField detailSearchField = new TextField();
        detailSearchField.setPromptText("Rechercher un étudiant...");
        detailSearchField.getStyleClass().add("quiz-detail-search");
        HBox.setHgrow(detailSearchField, Priority.ALWAYS);

        ComboBox<String> statusFilterCombo = new ComboBox<>();
        statusFilterCombo.setItems(FXCollections.observableArrayList("Tous les statuts", "Réussi", "Échoué"));
        statusFilterCombo.setValue("Tous les statuts");
        statusFilterCombo.getStyleClass().add("quiz-detail-status-filter");

        Button applyFilterBtn = new Button("Filtrer");
        applyFilterBtn.getStyleClass().add("quiz-detail-filter-btn");
        filterRow.getChildren().addAll(detailSearchField, statusFilterCombo, applyFilterBtn);

        HBox tableHeader = new HBox(10,
                headerCell("Étudiant", 300),
                headerCell("Tentative", 90),
                headerCell("Score", 150),
                headerCell("Statut", 120),
                headerCell("Date", 180),
                headerCell("Actions", 100)
        );
        tableHeader.getStyleClass().add("quiz-detail-table-header");

        VBox rowsBox = new VBox(8);
        rowsBox.getStyleClass().add("quiz-detail-rows-box");

        Label emptyRowsLabel = new Label("Aucune tentative trouvée.");
        emptyRowsLabel.getStyleClass().add("quiz-detail-empty");
        emptyRowsLabel.setVisible(false);
        emptyRowsLabel.setManaged(false);

        List<QuizAttemptDetail> loadedAttempts;
        try {
            loadedAttempts = statisticsService.findAttemptsForQuiz(summary.getQuiz().getId(), summary.getQuiz().getPassingScore());
        } catch (SQLException e) {
            showError("DB Error", e.getMessage());
            loadedAttempts = List.of();
        }
        final List<QuizAttemptDetail> attempts = loadedAttempts;

        Runnable refreshRows = () -> renderAttemptRows(rowsBox, emptyRowsLabel, attempts, detailSearchField.getText(), statusFilterCombo.getValue());
        detailSearchField.textProperty().addListener((obs, oldVal, newVal) -> refreshRows.run());
        statusFilterCombo.valueProperty().addListener((obs, oldVal, newVal) -> refreshRows.run());
        applyFilterBtn.setOnAction(evt -> refreshRows.run());
        refreshRows.run();

        ScrollPane rowsScroll = new ScrollPane(rowsBox);
        rowsScroll.setFitToWidth(true);
        rowsScroll.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        rowsScroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        rowsScroll.getStyleClass().add("quiz-detail-table-scroll");
        rowsScroll.setPrefViewportHeight(360);

        shell.getChildren().addAll(
                breadcrumb,
                title,
                subtitle,
                kpiRow,
                filterRow,
                tableHeader,
                rowsScroll,
                emptyRowsLabel
        );

        dialog.getDialogPane().setContent(shell);

        dialog.showAndWait();
    }

    private VBox createDetailKpiCard(String value, String label, String variant) {
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().addAll("quiz-detail-kpi-value", "quiz-detail-kpi-value-" + variant);
        Label labelLabel = new Label(label);
        labelLabel.getStyleClass().add("quiz-detail-kpi-label");

        VBox box = new VBox(6, valueLabel, labelLabel);
        box.getStyleClass().addAll("quiz-detail-kpi-card", "quiz-detail-kpi-" + variant);
        box.setAlignment(Pos.CENTER);
        box.setPrefWidth(180);
        HBox.setHgrow(box, Priority.ALWAYS);
        return box;
    }

    private Label headerCell(String text, double minWidth) {
        Label label = new Label(text);
        label.getStyleClass().add("quiz-detail-header-cell");
        label.setMinWidth(minWidth);
        return label;
    }

    private void renderAttemptRows(VBox rowsBox, Label emptyRowsLabel, List<QuizAttemptDetail> allAttempts, String search, String status) {
        rowsBox.getChildren().clear();
        String normalized = search == null ? "" : search.trim().toLowerCase();
        boolean filterPassed = "Réussi".equals(status);
        boolean filterFailed = "Échoué".equals(status);

        for (QuizAttemptDetail attempt : allAttempts) {
            boolean matchesSearch = normalized.isEmpty()
                    || safe(attempt.getStudentName()).toLowerCase().contains(normalized)
                    || safe(attempt.getStudentEmail()).toLowerCase().contains(normalized);
            boolean matchesStatus = !filterPassed && !filterFailed
                    || (filterPassed && attempt.isPassed())
                    || (filterFailed && !attempt.isPassed());

            if (matchesSearch && matchesStatus) {
                rowsBox.getChildren().add(createAttemptRow(attempt));
            }
        }

        boolean empty = rowsBox.getChildren().isEmpty();
        emptyRowsLabel.setVisible(empty);
        emptyRowsLabel.setManaged(empty);
    }

    private HBox createAttemptRow(QuizAttemptDetail attempt) {
        VBox studentCol = new VBox(2);
        Label nameLabel = new Label(safe(attempt.getStudentName()));
        nameLabel.getStyleClass().add("quiz-detail-student-name");
        Label emailLabel = new Label(safe(attempt.getStudentEmail()));
        emailLabel.getStyleClass().add("quiz-detail-student-email");
        studentCol.getChildren().addAll(nameLabel, emailLabel);
        studentCol.setMinWidth(300);

        Label attemptLabel = new Label("#" + attempt.getAttemptNumber());
        attemptLabel.getStyleClass().add("quiz-detail-cell");
        attemptLabel.setMinWidth(90);

        ProgressBar scoreProgress = new ProgressBar(Math.max(0, Math.min(1, attempt.getScore() / 100.0)));
        scoreProgress.setPrefWidth(90);
        scoreProgress.getStyleClass().add("quiz-detail-score-progress");
        scoreProgress.setStyle(attempt.isPassed() ? "-fx-accent: #24c08f;" : "-fx-accent: #ef4444;");
        Label scoreLabel = new Label(String.format("%.1f%%", attempt.getScore()));
        scoreLabel.getStyleClass().addAll("quiz-detail-cell", attempt.isPassed() ? "quiz-detail-score-pass" : "quiz-detail-score-fail");
        HBox scoreCol = new HBox(8, scoreProgress, scoreLabel);
        scoreCol.setAlignment(Pos.CENTER_LEFT);
        scoreCol.setMinWidth(150);

        Label statusBadge = new Label(attempt.isPassed() ? "Réussi" : "Échoué");
        statusBadge.getStyleClass().addAll("quiz-detail-status-badge", attempt.isPassed() ? "quiz-detail-status-pass" : "quiz-detail-status-fail");
        statusBadge.setMinWidth(120);

        Label dateLabel = new Label(formatDate(attempt.getSubmittedAt()));
        dateLabel.getStyleClass().add("quiz-detail-cell");
        dateLabel.setMinWidth(180);

        Button actionBtn = new Button("Voir");
        actionBtn.getStyleClass().add("quiz-detail-row-action");
        actionBtn.setDisable(true);
        actionBtn.setMinWidth(88);

        HBox row = new HBox(10, studentCol, attemptLabel, scoreCol, statusBadge, dateLabel, actionBtn);
        row.getStyleClass().add("quiz-detail-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "-";
        }
        return DETAIL_DATE_FORMAT.format(dateTime);
    }

    private String safe(String value) {
        return value == null ? "-" : value;
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