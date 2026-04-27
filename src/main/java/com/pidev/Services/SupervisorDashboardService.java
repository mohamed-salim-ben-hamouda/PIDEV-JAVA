package com.pidev.Services;

import com.pidev.models.StudentRiskInsight;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SupervisorDashboardService {

    private final Connection connection;
    private final LearningIntelligenceService learningIntelligenceService = new LearningIntelligenceService();

    public SupervisorDashboardService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public DashboardSnapshot loadSnapshot() throws SQLException {
        List<StudentRiskInsight> insights = learningIntelligenceService.findRiskInsights();
        LearningIntelligenceService.RiskDashboardMetrics riskMetrics = learningIntelligenceService.computeMetrics(insights);

        int activeLearners = fetchActiveLearners();
        int attemptsToday = fetchAttemptsToday();
        double averageScore = fetchAverageScore();
        int learnersToReview = riskMetrics.highRisk() + riskMetrics.mediumRisk();

        List<RecentStudentAction> recentActions = fetchRecentActions();
        List<WatchlistItem> watchlist = buildWatchlist(insights);
        List<SupervisorNote> notes = buildSupervisorNotes(insights);
        List<String> summaryLines = buildSummaryLines(insights, recentActions, activeLearners, attemptsToday, averageScore);

        return new DashboardSnapshot(
                activeLearners,
                attemptsToday,
                averageScore,
                learnersToReview,
                recentActions,
                watchlist,
                notes,
                summaryLines
        );
    }

    private int fetchActiveLearners() throws SQLException {
        String sql = "SELECT COUNT(DISTINCT student_id) AS total FROM quiz_attempts";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    private int fetchAttemptsToday() throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM quiz_attempts WHERE DATE(submitted_at) = CURDATE()";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? rs.getInt("total") : 0;
        }
    }

    private double fetchAverageScore() throws SQLException {
        String sql = "SELECT COALESCE(AVG(score), 0) AS average_score FROM quiz_attempts";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return rs.next() ? roundOne(rs.getDouble("average_score")) : 0.0;
        }
    }

    private List<RecentStudentAction> fetchRecentActions() throws SQLException {
        String sql = "SELECT qa.attempt_nbr, qa.score, qa.submitted_at, "
                + "u.prenom, u.nom, u.email, "
                + "q.title AS quiz_title, q.passing_score, "
                + "c.title AS course_title "
                + "FROM quiz_attempts qa "
                + "LEFT JOIN user u ON qa.student_id = u.id "
                + "LEFT JOIN quiz q ON qa.quiz_id = q.id "
                + "LEFT JOIN course c ON q.course_id = c.id "
                + "ORDER BY qa.submitted_at DESC "
                + "LIMIT 4";

        List<RecentStudentAction> result = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String studentName = resolveStudentName(
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        rs.getString("email")
                );
                String quizTitle = defaultText(rs.getString("quiz_title"), "Quiz");
                String courseTitle = defaultText(rs.getString("course_title"), "Cours");
                double score = roundOne(rs.getDouble("score"));
                double passingScore = rs.getDouble("passing_score");
                int attemptNumber = rs.getInt("attempt_nbr");
                LocalDateTime submittedAt = toLocalDateTime(rs.getTimestamp("submitted_at"));

                String title = studentName + " submitted " + quizTitle;
                String subtitle = "Course: " + courseTitle + " - " + formatRelativeTime(submittedAt);

                StatusAppearance statusAppearance;
                if (attemptNumber > 1) {
                    statusAppearance = new StatusAppearance("RETRY", "dashboard-status-warning", "dashboard-list-icon-warning", "fas-redo");
                } else if (score >= passingScore) {
                    statusAppearance = new StatusAppearance("PASSED", "dashboard-status-success", "dashboard-list-icon-success", "fas-check");
                } else {
                    statusAppearance = new StatusAppearance("ALERT", "dashboard-status-danger", "dashboard-list-icon-danger", "fas-exclamation");
                }

                result.add(new RecentStudentAction(
                        title,
                        subtitle + " - Score: " + formatPercent(score) + "%",
                        statusAppearance.statusText(),
                        statusAppearance.statusStyleClass(),
                        statusAppearance.iconWrapStyleClass(),
                        statusAppearance.iconLiteral()
                ));
            }
        }
        return result;
    }

    private List<WatchlistItem> buildWatchlist(List<StudentRiskInsight> insights) {
        if (insights == null || insights.isEmpty()) {
            return List.of(new WatchlistItem("No watchlist yet", "No student risk signals detected.", false));
        }

        List<WatchlistItem> items = new ArrayList<>();
        for (StudentRiskInsight insight : insights.stream().limit(3).toList()) {
            String studentName = insight.getStudent() == null
                    ? "Student"
                    : defaultText(insight.getStudent().getDisplayName(), "Student");
            String title = studentName + " - " + insight.getRiskLevel().name() + " risk";
            String text = "Average " + formatPercent(insight.getAverageScore()) + "%, pass rate "
                    + formatPercent(insight.getPassRate()) + "%. " + defaultText(insight.getReason(), "Supervisor follow-up recommended.");
            items.add(new WatchlistItem(title, text, insight.getRiskLevel() == StudentRiskInsight.RiskLevel.HIGH));
        }
        return items;
    }

    private List<SupervisorNote> buildSupervisorNotes(List<StudentRiskInsight> insights) throws SQLException {
        List<SupervisorNote> notes = new ArrayList<>();

        if (insights != null && !insights.isEmpty()) {
            StudentRiskInsight highestRisk = insights.get(0);
            String learnerName = highestRisk.getStudent() == null
                    ? "A learner"
                    : defaultText(highestRisk.getStudent().getDisplayName(), "A learner");
            notes.add(new SupervisorNote(
                    "Priority remediation",
                    "Generated from learner tracking",
                    learnerName + " has a " + highestRisk.getRiskLevel().name().toLowerCase(Locale.ROOT)
                            + " risk profile. Recommended action: "
                            + firstOrDefault(highestRisk.getRecommendedActions(), "plan a follow-up review session."),
                    true
            ));
        }

        TopQuizSignal topQuizSignal = fetchTopQuizSignal();
        if (topQuizSignal != null) {
            notes.add(new SupervisorNote(
                    topQuizSignal.quizTitle(),
                    "Generated from recent quiz traffic",
                    "This quiz has " + topQuizSignal.attemptCount() + " recent attempt(s) with an average score of "
                            + formatPercent(topQuizSignal.averageScore()) + "%. Use it to review recurring misunderstandings.",
                    false
            ));
        }

        if (notes.isEmpty()) {
            notes.add(new SupervisorNote(
                    "No supervisor notes yet",
                    "Generated from learner tracking",
                    "The dashboard will create supervision notes as soon as students begin generating activity.",
                    false
            ));
        }

        return notes;
    }

    private List<String> buildSummaryLines(List<StudentRiskInsight> insights,
                                           List<RecentStudentAction> recentActions,
                                           int activeLearners,
                                           int attemptsToday,
                                           double averageScore) throws SQLException {
        List<String> lines = new ArrayList<>();
        lines.add("Active learners tracked: " + activeLearners);
        lines.add("Quiz attempts recorded today: " + attemptsToday + " - average score " + formatPercent(averageScore) + "%");

        TopQuizSignal topQuizSignal = fetchTopQuizSignal();
        if (topQuizSignal != null) {
            lines.add("Most active quiz right now: " + topQuizSignal.quizTitle());
        } else if (insights != null && !insights.isEmpty()) {
            StudentRiskInsight topRisk = insights.get(0);
            String learnerName = topRisk.getStudent() == null
                    ? "A learner"
                    : defaultText(topRisk.getStudent().getDisplayName(), "A learner");
            lines.add("Priority follow-up: " + learnerName + " (" + topRisk.getRiskLevel().name().toLowerCase(Locale.ROOT) + " risk)");
        } else if (!recentActions.isEmpty()) {
            lines.add("Latest tracked action: " + recentActions.get(0).title());
        } else {
            lines.add("No recent learner activity available yet.");
        }

        return lines;
    }

    private TopQuizSignal fetchTopQuizSignal() throws SQLException {
        String sql = "SELECT q.title AS quiz_title, COUNT(*) AS attempts, COALESCE(AVG(qa.score), 0) AS average_score "
                + "FROM quiz_attempts qa "
                + "LEFT JOIN quiz q ON qa.quiz_id = q.id "
                + "GROUP BY q.id, q.title "
                + "ORDER BY attempts DESC, average_score ASC "
                + "LIMIT 1";

        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            if (!rs.next()) {
                return null;
            }
            return new TopQuizSignal(
                    defaultText(rs.getString("quiz_title"), "Quiz"),
                    rs.getInt("attempts"),
                    roundOne(rs.getDouble("average_score"))
            );
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String resolveStudentName(String prenom, String nom, String email) {
        String fullName = (defaultText(prenom, "") + " " + defaultText(nom, "")).trim();
        if (!fullName.isBlank()) {
            return fullName;
        }
        return defaultText(email, "Student");
    }

    private String formatRelativeTime(LocalDateTime timestamp) {
        if (timestamp == null) {
            return "time unavailable";
        }

        Duration duration = Duration.between(timestamp, LocalDateTime.now());
        long minutes = Math.max(0, duration.toMinutes());
        if (minutes < 1) {
            return "just now";
        }
        if (minutes < 60) {
            return minutes + (minutes == 1 ? " minute ago" : " minutes ago");
        }

        long hours = duration.toHours();
        if (hours < 24) {
            return hours + (hours == 1 ? " hour ago" : " hours ago");
        }

        long days = duration.toDays();
        if (days < 7) {
            return days + (days == 1 ? " day ago" : " days ago");
        }

        return timestamp.format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }

    private Connection requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available.");
        }
        return connection;
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    private String firstOrDefault(List<String> values, String fallback) {
        return values == null || values.isEmpty() ? fallback : defaultText(values.get(0), fallback);
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }

    private String formatPercent(double value) {
        double rounded = roundOne(value);
        if (Math.abs(rounded - Math.rint(rounded)) < 0.0001) {
            return String.valueOf((int) Math.round(rounded));
        }
        return String.format(Locale.US, "%.1f", rounded);
    }

    public record DashboardSnapshot(
            int activeLearners,
            int attemptsToday,
            double averageScore,
            int learnersToReview,
            List<RecentStudentAction> recentActions,
            List<WatchlistItem> watchlist,
            List<SupervisorNote> supervisorNotes,
            List<String> summaryLines
    ) {
    }

    public record RecentStudentAction(
            String title,
            String subtitle,
            String statusText,
            String statusStyleClass,
            String iconWrapStyleClass,
            String iconLiteral
    ) {
    }

    public record WatchlistItem(
            String title,
            String text,
            boolean accent
    ) {
    }

    public record SupervisorNote(
            String title,
            String meta,
            String text,
            boolean accent
    ) {
    }

    private record StatusAppearance(
            String statusText,
            String statusStyleClass,
            String iconWrapStyleClass,
            String iconLiteral
    ) {
    }

    private record TopQuizSignal(
            String quizTitle,
            int attemptCount,
            double averageScore
    ) {
    }
}
