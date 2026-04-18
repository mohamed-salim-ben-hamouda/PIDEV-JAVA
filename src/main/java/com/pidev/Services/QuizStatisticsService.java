package com.pidev.Services;

import com.pidev.models.Course;
import com.pidev.models.Question;
import com.pidev.models.QuestionStatistic;
import com.pidev.models.Quiz;
import com.pidev.models.QuizStatisticsSummary;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class QuizStatisticsService {
    private final Connection connection;
    private final QuizService quizService = new QuizService();

    public QuizStatisticsService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<QuizStatisticsSummary> findQuizSummaries() throws SQLException {
        List<Quiz> quizzes = quizService.findAll();
        List<QuizStatisticsSummary> summaries = new ArrayList<>();
        for (Quiz quiz : quizzes) {
            summaries.add(findSummary(quiz));
        }
        return summaries;
    }

    public QuizStatisticsSummary findSummary(Quiz quiz) throws SQLException {
        if (quiz == null || quiz.getId() == null) {
            throw new SQLException("Quiz is required for statistics.");
        }

        String sql = "SELECT COUNT(*) AS total_attempts, COUNT(DISTINCT student_id) AS unique_students, "
                + "COALESCE(AVG(score), 0) AS average_score, "
                + "COALESCE(SUM(CASE WHEN score >= ? THEN 1 ELSE 0 END), 0) AS passed_count "
                + "FROM quiz_attempts WHERE quiz_id = ?";

        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setFloat(1, quiz.getPassingScore());
            statement.setInt(2, quiz.getId());

            try (ResultSet rs = statement.executeQuery()) {
                if (!rs.next()) {
                    return new QuizStatisticsSummary(quiz, 0, 0, 0, 0, 0.0, 0.0);
                }

                int totalAttempts = rs.getInt("total_attempts");
                int uniqueStudents = rs.getInt("unique_students");
                int passedCount = rs.getInt("passed_count");
                int failedCount = Math.max(0, totalAttempts - passedCount);
                double averageScore = roundOneDecimal(rs.getDouble("average_score"));
                double passRate = totalAttempts > 0 ? roundOneDecimal((passedCount * 100.0) / totalAttempts) : 0.0;

                return new QuizStatisticsSummary(quiz, totalAttempts, uniqueStudents, passedCount, failedCount, averageScore, passRate);
            }
        }
    }

    public List<QuestionStatistic> findQuestionStatistics(int quizId) throws SQLException {
        String sql = "SELECT q.id AS question_id, q.content AS question_content, q.point AS question_point, "
                + "COALESCE(COUNT(sr.id), 0) AS total_responses, "
                + "COALESCE(SUM(CASE WHEN sr.is_correct = 1 THEN 1 ELSE 0 END), 0) AS correct_count "
                + "FROM question q "
                + "LEFT JOIN student_response sr ON sr.question_id = q.id "
                + "WHERE q.quiz_id = ? "
                + "GROUP BY q.id, q.content, q.point "
                + "ORDER BY q.id ASC";

        List<QuestionStatistic> statistics = new ArrayList<>();
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, quizId);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Question question = new Question();
                    question.setId(rs.getInt("question_id"));
                    question.setContent(rs.getString("question_content"));
                    question.setPoint(rs.getFloat("question_point"));

                    int totalResponses = rs.getInt("total_responses");
                    int correctCount = rs.getInt("correct_count");
                    double successRate = totalResponses > 0 ? roundOneDecimal((correctCount * 100.0) / totalResponses) : 0.0;

                    statistics.add(new QuestionStatistic(question, totalResponses, correctCount, successRate));
                }
            }
        }
        return statistics;
    }

    private Connection requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available. Check DataSource URL/user/password and MySQL server.");
        }
        return connection;
    }

    private double roundOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}