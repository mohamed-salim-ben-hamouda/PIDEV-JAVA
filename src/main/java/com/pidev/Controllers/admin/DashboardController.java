package com.pidev.Controllers.admin;

import com.pidev.Services.SupervisorDashboardService;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import org.kordamp.ikonli.javafx.FontIcon;

import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label activeLearnersLabel;
    @FXML private Label attemptsTodayLabel;
    @FXML private Label averageScoreLabel;
    @FXML private Label learnersToReviewLabel;
    @FXML private VBox recentActionsContainer;
    @FXML private VBox watchlistContainer;
    @FXML private VBox supervisorNotesContainer;
    @FXML private Label summaryLineOneLabel;
    @FXML private Label summaryLineTwoLabel;
    @FXML private Label summaryLineThreeLabel;

    private final SupervisorDashboardService dashboardService = new SupervisorDashboardService();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        loadDashboardData();
    }

    private void loadDashboardData() {
        try {
            SupervisorDashboardService.DashboardSnapshot snapshot = dashboardService.loadSnapshot();

            activeLearnersLabel.setText(String.valueOf(snapshot.activeLearners()));
            attemptsTodayLabel.setText(String.valueOf(snapshot.attemptsToday()));
            averageScoreLabel.setText(formatPercent(snapshot.averageScore()));
            learnersToReviewLabel.setText(String.valueOf(snapshot.learnersToReview()));

            populateRecentActions(snapshot.recentActions());
            populateWatchlist(snapshot.watchlist());
            populateSupervisorNotes(snapshot.supervisorNotes());
            populateSummaryLines(snapshot.summaryLines());
        } catch (Exception e) {
            activeLearnersLabel.setText("-");
            attemptsTodayLabel.setText("-");
            averageScoreLabel.setText("-");
            learnersToReviewLabel.setText("-");

            populateFallback(recentActionsContainer, "Unable to load recent student actions.");
            populateFallback(watchlistContainer, "Unable to load watchlist data.");
            populateFallback(supervisorNotesContainer, "Unable to load supervisor notes.");
            summaryLineOneLabel.setText("Dashboard data unavailable.");
            summaryLineTwoLabel.setText("Check database connection and learner activity tables.");
            summaryLineThreeLabel.setText("The workspace menu remains available.");

            System.err.println("Could not load supervisor dashboard data.");
            e.printStackTrace();
        }
    }

    private void populateRecentActions(List<SupervisorDashboardService.RecentStudentAction> actions) {
        recentActionsContainer.getChildren().clear();
        if (actions == null || actions.isEmpty()) {
            populateFallback(recentActionsContainer, "No recent tracked actions yet.");
            return;
        }

        for (SupervisorDashboardService.RecentStudentAction action : actions) {
            HBox row = new HBox(14);
            row.setAlignment(Pos.CENTER_LEFT);
            row.getStyleClass().add("dashboard-list-item");

            StackPane iconWrap = new StackPane();
            iconWrap.getStyleClass().addAll("dashboard-list-icon-wrap", action.iconWrapStyleClass());
            FontIcon icon = new FontIcon(action.iconLiteral());
            icon.getStyleClass().add("dashboard-list-icon");
            iconWrap.getChildren().add(icon);

            VBox textBox = new VBox(4);
            HBox.setHgrow(textBox, Priority.ALWAYS);
            Label title = new Label(action.title());
            title.getStyleClass().add("dashboard-item-title");
            title.setWrapText(true);
            Label subtitle = new Label(action.subtitle());
            subtitle.getStyleClass().add("dashboard-item-subtitle");
            subtitle.setWrapText(true);
            textBox.getChildren().addAll(title, subtitle);

            Label status = new Label(action.statusText());
            status.getStyleClass().addAll("dashboard-status-pill", action.statusStyleClass());

            row.getChildren().addAll(iconWrap, textBox, status);
            recentActionsContainer.getChildren().add(row);
        }
    }

    private void populateWatchlist(List<SupervisorDashboardService.WatchlistItem> items) {
        watchlistContainer.getChildren().clear();
        if (items == null || items.isEmpty()) {
            populateFallback(watchlistContainer, "No learners currently require extra review.");
            return;
        }

        for (SupervisorDashboardService.WatchlistItem item : items) {
            VBox card = new VBox(4);
            card.getStyleClass().add("dashboard-note-card");
            if (item.accent()) {
                card.getStyleClass().add("dashboard-note-card-accent");
            }

            Label title = new Label(item.title());
            title.getStyleClass().add("dashboard-note-title");
            title.setWrapText(true);
            Label text = new Label(item.text());
            text.getStyleClass().add("dashboard-note-text");
            text.setWrapText(true);

            card.getChildren().addAll(title, text);
            watchlistContainer.getChildren().add(card);
        }
    }

    private void populateSupervisorNotes(List<SupervisorDashboardService.SupervisorNote> notes) {
        supervisorNotesContainer.getChildren().clear();
        if (notes == null || notes.isEmpty()) {
            populateFallback(supervisorNotesContainer, "No supervision notes generated yet.");
            return;
        }

        for (SupervisorDashboardService.SupervisorNote note : notes) {
            VBox card = new VBox(6);
            card.getStyleClass().add("dashboard-note-card");
            if (note.accent()) {
                card.getStyleClass().add("dashboard-note-card-accent");
            }

            Label title = new Label(note.title());
            title.getStyleClass().add("dashboard-note-title");
            title.setWrapText(true);

            Label meta = new Label(note.meta());
            meta.getStyleClass().add("dashboard-note-meta");
            meta.setWrapText(true);

            Label text = new Label(note.text());
            text.getStyleClass().add("dashboard-note-text");
            text.setWrapText(true);

            card.getChildren().addAll(title, meta, text);
            supervisorNotesContainer.getChildren().add(card);
        }
    }

    private void populateSummaryLines(List<String> lines) {
        summaryLineOneLabel.setText(readLine(lines, 0, "No learner tracking summary available."));
        summaryLineTwoLabel.setText(readLine(lines, 1, ""));
        summaryLineThreeLabel.setText(readLine(lines, 2, ""));
    }

    private String readLine(List<String> lines, int index, String fallback) {
        return lines != null && index < lines.size() ? lines.get(index) : fallback;
    }

    private void populateFallback(VBox container, String message) {
        container.getChildren().clear();
        Label fallback = new Label(message);
        fallback.getStyleClass().add("dashboard-note-text");
        fallback.setWrapText(true);
        container.getChildren().add(fallback);
    }

    private String formatPercent(double value) {
        double rounded = Math.round(value * 10.0) / 10.0;
        if (Math.abs(rounded - Math.rint(rounded)) < 0.0001) {
            return ((int) Math.round(rounded)) + "%";
        }
        return rounded + "%";
    }
}
