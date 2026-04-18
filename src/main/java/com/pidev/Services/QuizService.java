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
        String sql = "SELECT q.id, q.course_id, c.title AS course_title, q.chapter_id, ch.title AS chapter_title, q.title, q.passing_score, q.max_attempts, q.questions_per_attempt, q.time_limit, "
            + "q.supervisor_id, u.nom AS supervisor_nom, u.prenom AS supervisor_prenom, u.email AS supervisor_email "
            + "FROM quiz q "
            + "LEFT JOIN course c ON q.course_id = c.id "
            + "LEFT JOIN chapter ch ON q.chapter_id = ch.id "
            + "LEFT JOIN user u ON q.supervisor_id = u.id ORDER BY q.id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return mapResult(rs);
        }
    }

    public List<Quiz> findPage(String search, Integer courseFilter, String sort, String direction, int page, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT q.id, q.course_id, c.title AS course_title, q.chapter_id, ch.title AS chapter_title, q.title, q.passing_score, q.max_attempts, q.questions_per_attempt, q.time_limit, "
            + "q.supervisor_id, u.nom AS supervisor_nom, u.prenom AS supervisor_prenom, u.email AS supervisor_email "
            + "FROM quiz q LEFT JOIN course c ON q.course_id = c.id LEFT JOIN chapter ch ON q.chapter_id = ch.id LEFT JOIN user u ON q.supervisor_id = u.id WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND q.title LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (courseFilter != null) {
            sql.append(" AND q.course_id = ?");
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
        if (quiz.getCourse() == null || quiz.getCourse().getId() == null) {
            throw new SQLException("Course is required for quiz.");
        }
        if (quiz.getSupervisor() == null || quiz.getSupervisor().getId() == null) {
            throw new SQLException("Supervisor is required for quiz.");
        }
        statement.setInt(1, quiz.getCourse().getId());

        if (quiz.getChapter() != null && quiz.getChapter().getId() != null) {
            statement.setInt(2, quiz.getChapter().getId());
        } else {
            statement.setNull(2, Types.INTEGER);
        }

        statement.setString(3, quiz.getTitle());
        statement.setFloat(4, quiz.getPassingScore());
        statement.setInt(5, quiz.getMaxAttempts() <= 0 ? 1 : quiz.getMaxAttempts());

        if (quiz.getQuestionsPerAttempt() != null) {
            statement.setInt(6, quiz.getQuestionsPerAttempt());
        } else {
            statement.setNull(6, Types.INTEGER);
        }

        statement.setInt(7, Math.max(quiz.getTimeLimit(), 0));
        statement.setInt(8, quiz.getSupervisor().getId());

        if (withId) {
            statement.setInt(9, quiz.getId());
        }
    }

    private List<Quiz> mapResult(ResultSet rs) throws SQLException {
        List<Quiz> result = new ArrayList<>();
        while (rs.next()) {
            Quiz quiz = new Quiz();
            quiz.setId(rs.getInt("id"));
            Course course = new Course(rs.getInt("course_id"));
            String courseTitle = rs.getString("course_title");
            if (courseTitle != null) {
                course.setTitle(courseTitle);
            }
            quiz.setCourse(course);

            int chapterId = rs.getInt("chapter_id");
            if (!rs.wasNull()) {
                Chapter chapter = new Chapter(chapterId);
                String chapterTitle = rs.getString("chapter_title");
                if (chapterTitle != null) {
                    chapter.setTitle(chapterTitle);
                }
                quiz.setChapter(chapter);
                chapter.setQuiz(quiz);
            }

            quiz.setTitle(rs.getString("title"));
            quiz.setPassingScore(rs.getFloat("passing_score"));
            int maxAttempts = rs.getInt("max_attempts");
            quiz.setMaxAttempts(maxAttempts <= 0 ? 1 : maxAttempts);

            int questionsPerAttempt = rs.getInt("questions_per_attempt");
            if (!rs.wasNull()) {
                quiz.setQuestionsPerAttempt(questionsPerAttempt);
            }

            quiz.setTimeLimit(Math.max(rs.getInt("time_limit"), 0));
            int supervisorId = rs.getInt("supervisor_id");
            if (!rs.wasNull()) {
                User supervisor = new User(supervisorId);
                String nom = rs.getString("supervisor_nom");
                String prenom = rs.getString("supervisor_prenom");
                String email = rs.getString("supervisor_email");
                if (nom != null) {
                    supervisor.setNom(nom);
                }
                if (prenom != null) {
                    supervisor.setPrenom(prenom);
                }
                if (email != null) {
                    supervisor.setEmail(email);
                }
                quiz.setSupervisor(supervisor);
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
            case "title" -> "q.title";
            case "passingScore" -> "q.passing_score";
            case "maxAttempts" -> "q.max_attempts";
            default -> "q.id";
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
