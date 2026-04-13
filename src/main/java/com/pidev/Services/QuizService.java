package com.pidev.Services;

import com.pidev.models.Chapter;
import com.pidev.models.Course;
import com.pidev.models.Quiz;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QuizService {
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "title", "passingScore", "maxAttempts");
    private final Connection connection;

    public QuizService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<Quiz> findAll() throws SQLException {
        String sql = "SELECT id, course_id, chapter_id, title, passing_score, max_attempts, questions_per_attempt, time_limit, supervisor_id FROM quiz ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return mapResult(rs);
        }
    }

    public List<Quiz> findPage(String search, Integer courseFilter, String sort, String direction, int page, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id, course_id, chapter_id, title, passing_score, max_attempts, questions_per_attempt, time_limit, supervisor_id FROM quiz WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (courseFilter != null) {
            sql.append(" AND course_id = ?");
            params.add(courseFilter);
        }

        sql.append(" ORDER BY ").append(normalizeSort(sort)).append(" ").append(normalizeDirection(direction));
        sql.append(" LIMIT ? OFFSET ?");
        params.add(Math.max(limit, 1));
        params.add((Math.max(page, 1) - 1) * Math.max(limit, 1));

        try (PreparedStatement statement = requireConnection().prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return mapResult(rs);
            }
        }
    }

    public int count(String search, Integer courseFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM quiz WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (courseFilter != null) {
            sql.append(" AND course_id = ?");
            params.add(courseFilter);
        }

        try (PreparedStatement statement = requireConnection().prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void create(Quiz quiz) throws SQLException {
        String sql = "INSERT INTO quiz (course_id, chapter_id, title, passing_score, max_attempts, questions_per_attempt, time_limit, supervisor_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindQuiz(statement, quiz, false);
            statement.executeUpdate();
        }
    }

    public void update(Quiz quiz) throws SQLException {
        String sql = "UPDATE quiz SET course_id=?, chapter_id=?, title=?, passing_score=?, max_attempts=?, questions_per_attempt=?, time_limit=?, supervisor_id=? WHERE id=?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindQuiz(statement, quiz, true);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM quiz WHERE id=?")) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Connection requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available. Check DataSource URL/user/password and MySQL server.");
        }
        return connection;
    }

    private void bindQuiz(PreparedStatement statement, Quiz quiz, boolean withId) throws SQLException {
        statement.setInt(1, quiz.getCourse().getId());

        if (quiz.getChapter() != null && quiz.getChapter().getId() != null) {
            statement.setInt(2, quiz.getChapter().getId());
        } else {
            statement.setNull(2, Types.INTEGER);
        }

        statement.setString(3, quiz.getTitle());
        statement.setFloat(4, quiz.getPassingScore());
        statement.setInt(5, quiz.getMaxAttempts());

        if (quiz.getQuestionsPerAttempt() != null) {
            statement.setInt(6, quiz.getQuestionsPerAttempt());
        } else {
            statement.setNull(6, Types.INTEGER);
        }

        statement.setInt(7, quiz.getTimeLimit());
        if (quiz.getSupervisor() != null && quiz.getSupervisor().getId() != null) {
            statement.setInt(8, quiz.getSupervisor().getId());
        } else {
            statement.setNull(8, Types.INTEGER);
        }

        if (withId) {
            statement.setInt(9, quiz.getId());
        }
    }

    private List<Quiz> mapResult(ResultSet rs) throws SQLException {
        List<Quiz> result = new ArrayList<>();
        while (rs.next()) {
            Quiz quiz = new Quiz();
            quiz.setId(rs.getInt("id"));
            quiz.setCourse(new Course(rs.getInt("course_id")));

            int chapterId = rs.getInt("chapter_id");
            if (!rs.wasNull()) {
                quiz.setChapter(new Chapter(chapterId));
            }

            quiz.setTitle(rs.getString("title"));
            quiz.setPassingScore(rs.getFloat("passing_score"));
            quiz.setMaxAttempts(rs.getInt("max_attempts"));

            int questionsPerAttempt = rs.getInt("questions_per_attempt");
            if (!rs.wasNull()) {
                quiz.setQuestionsPerAttempt(questionsPerAttempt);
            }

            quiz.setTimeLimit(rs.getInt("time_limit"));
            int supervisorId = rs.getInt("supervisor_id");
            if (!rs.wasNull()) {
                quiz.setSupervisor(new User(supervisorId));
            }
            result.add(quiz);
        }
        return result;
    }

    private String normalizeSort(String sort) {
        if (sort == null || !ALLOWED_SORTS.contains(sort)) {
            return "id";
        }
        return switch (sort) {
            case "title" -> "title";
            case "passingScore" -> "passing_score";
            case "maxAttempts" -> "max_attempts";
            default -> "id";
        };
    }

    private String normalizeDirection(String direction) {
        return "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
