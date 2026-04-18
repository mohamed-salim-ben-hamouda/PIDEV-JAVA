package com.pidev.Services;

import com.pidev.models.Course;
import com.pidev.models.StudentRiskInsight;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LearningIntelligenceService {
    public record RiskDashboardMetrics(int totalStudents, int highRisk, int mediumRisk, int lowRisk, double globalAverageScore) {}

    private final Connection connection;
    private final CourseService courseService = new CourseService();

    public LearningIntelligenceService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<StudentRiskInsight> findRiskInsights() throws SQLException {
        String sql = "SELECT u.id AS student_id, u.prenom, u.nom, u.email, "
                + "COUNT(qa.id) AS attempts, "
                + "COALESCE(AVG(qa.score), 0) AS average_score, "
                + "COALESCE(SUM(CASE WHEN qa.score >= q.passing_score THEN 1 ELSE 0 END), 0) AS passed_count "
                + "FROM user u "
                + "LEFT JOIN quiz_attempts qa ON qa.student_id = u.id "
                + "LEFT JOIN quiz q ON qa.quiz_id = q.id "
                + "GROUP BY u.id, u.prenom, u.nom, u.email "
                + "HAVING COUNT(qa.id) > 0 "
                + "ORDER BY average_score ASC, attempts DESC";

        Map<Integer, Course> coursesById = loadCoursesById();
        List<StudentRiskInsight> insights = new ArrayList<>();

        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                int studentId = rs.getInt("student_id");
                User student = new User(studentId);
                student.setPrenom(rs.getString("prenom"));
                student.setNom(rs.getString("nom"));
                student.setEmail(rs.getString("email"));

                int attempts = rs.getInt("attempts");
                double averageScore = roundOne(rs.getDouble("average_score"));
                int passedCount = rs.getInt("passed_count");
                double passRate = attempts > 0 ? roundOne((passedCount * 100.0) / attempts) : 0.0;

                int riskScore = computeRiskScore(attempts, averageScore, passRate);
                StudentRiskInsight.RiskLevel riskLevel = computeRiskLevel(riskScore);
                String reason = buildReason(attempts, averageScore, passRate, riskLevel);
                List<String> actions = buildActions(riskLevel);
                List<String> courses = buildCourseRecommendations(studentId, coursesById, riskLevel);

                insights.add(new StudentRiskInsight(
                        student,
                        attempts,
                        averageScore,
                        passRate,
                        riskScore,
                        riskLevel,
                        reason,
                        actions,
                        courses
                ));
            }
        }

        insights.sort(Comparator
                .comparingInt((StudentRiskInsight item) -> item.getRiskLevel().ordinal()).reversed()
                .thenComparingInt(StudentRiskInsight::getRiskScore).reversed()
                .thenComparing(item -> item.getStudent().getDisplayName(), String.CASE_INSENSITIVE_ORDER));
        return insights;
    }

    public RiskDashboardMetrics computeMetrics(List<StudentRiskInsight> insights) {
        if (insights == null || insights.isEmpty()) {
            return new RiskDashboardMetrics(0, 0, 0, 0, 0.0);
        }

        int high = 0;
        int medium = 0;
        int low = 0;
        double avgSum = 0.0;
        for (StudentRiskInsight insight : insights) {
            switch (insight.getRiskLevel()) {
                case HIGH -> high++;
                case MEDIUM -> medium++;
                default -> low++;
            }
            avgSum += insight.getAverageScore();
        }

        return new RiskDashboardMetrics(insights.size(), high, medium, low, roundOne(avgSum / insights.size()));
    }

    private Map<Integer, Course> loadCoursesById() throws SQLException {
        List<Course> courses = courseService.findAll();
        Map<Integer, Course> map = new HashMap<>();
        for (Course course : courses) {
            if (course.getId() != null) {
                map.put(course.getId(), course);
            }
        }
        return map;
    }

    private List<String> buildCourseRecommendations(int studentId, Map<Integer, Course> coursesById, StudentRiskInsight.RiskLevel riskLevel) {
        if (riskLevel == StudentRiskInsight.RiskLevel.LOW) {
            return List.of("Parcours actuel satisfaisant");
        }

        try {
            Integer weakestCourseId = findWeakestCourseId(studentId);
            Course reference = weakestCourseId == null ? null : coursesById.get(weakestCourseId);

            List<CourseAdvancedBusinessService.CourseSuggestion> suggestions = courseService.suggestNextCourses(reference, 3);
            if (suggestions.isEmpty()) {
                return List.of("Aucune recommandation disponible");
            }

            List<String> labels = new ArrayList<>();
            for (CourseAdvancedBusinessService.CourseSuggestion suggestion : suggestions) {
                if (suggestion.course() != null && suggestion.course().getTitle() != null && !suggestion.course().getTitle().isBlank()) {
                    labels.add(suggestion.course().getTitle() + " (" + suggestion.badge() + ")");
                }
            }
            return labels.isEmpty() ? List.of("Aucune recommandation disponible") : labels;
        } catch (SQLException e) {
            return List.of("Recommandation indisponible");
        }
    }

    private Integer findWeakestCourseId(int studentId) throws SQLException {
        String sql = "SELECT q.course_id, COALESCE(AVG(qa.score), 0) AS average_score "
                + "FROM quiz_attempts qa "
                + "INNER JOIN quiz q ON q.id = qa.quiz_id "
                + "WHERE qa.student_id = ? "
                + "GROUP BY q.course_id "
                + "ORDER BY average_score ASC "
                + "LIMIT 1";

        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("course_id");
                }
            }
        }
        return null;
    }

    private int computeRiskScore(int attempts, double averageScore, double passRate) {
        int score = 0;

        if (averageScore < 40) {
            score += 45;
        } else if (averageScore < 60) {
            score += 30;
        } else if (averageScore < 75) {
            score += 15;
        }

        if (passRate < 30) {
            score += 35;
        } else if (passRate < 50) {
            score += 25;
        } else if (passRate < 70) {
            score += 15;
        }

        if (attempts >= 5 && averageScore < 60) {
            score += 10;
        }
        if (attempts < 2) {
            score += 5;
        }

        return Math.max(0, Math.min(score, 100));
    }

    private StudentRiskInsight.RiskLevel computeRiskLevel(int riskScore) {
        if (riskScore >= 70) {
            return StudentRiskInsight.RiskLevel.HIGH;
        }
        if (riskScore >= 45) {
            return StudentRiskInsight.RiskLevel.MEDIUM;
        }
        return StudentRiskInsight.RiskLevel.LOW;
    }

    private String buildReason(int attempts, double averageScore, double passRate, StudentRiskInsight.RiskLevel riskLevel) {
        List<String> reasons = new ArrayList<>();
        if (averageScore < 60) {
            reasons.add("moyenne quiz faible");
        }
        if (passRate < 50) {
            reasons.add("taux de reussite limite");
        }
        if (attempts < 2) {
            reasons.add("historique insuffisant");
        } else if (attempts >= 6 && averageScore < 60) {
            reasons.add("difficulte persistante");
        }

        if (reasons.isEmpty()) {
            reasons.add(riskLevel == StudentRiskInsight.RiskLevel.LOW
                    ? "progression stable"
                    : "surveillance recommandee");
        }
        return String.join(" | ", reasons);
    }

    private List<String> buildActions(StudentRiskInsight.RiskLevel level) {
        return switch (level) {
            case HIGH -> List.of(
                    "Plan de remediation prioritaire (7 jours)",
                    "Notifier superviseur et programmer coaching",
                    "Attribuer modules de revision cibles"
            );
            case MEDIUM -> List.of(
                    "Suivi hebdomadaire des tentatives",
                    "Recommander 2 chapitres de renforcement",
                    "Relance automatisee en cas d'inactivite"
            );
            case LOW -> List.of(
                    "Maintenir rythme actuel",
                    "Proposer contenu avance optionnel"
            );
        };
    }

    private Connection requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available. Check DataSource URL/user/password and MySQL server.");
        }
        return connection;
    }

    private double roundOne(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}