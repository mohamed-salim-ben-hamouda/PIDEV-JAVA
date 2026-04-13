package com.pidev.Services;

import com.pidev.models.Question;
import com.pidev.models.Quiz;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class QuestionService {
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "content", "type", "point");
    private final Connection connection;

    public QuestionService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<Question> findAll() throws SQLException {
        String sql = "SELECT id, quiz_id, content, type, point FROM question ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return mapResult(rs);
        }
    }

    public List<Question> findPage(String search, Integer quizFilter, String sort, String direction, int page, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id, quiz_id, content, type, point FROM question WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND content LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (quizFilter != null) {
            sql.append(" AND quiz_id = ?");
            params.add(quizFilter);
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

    public int count(String search, Integer quizFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM question WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND content LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (quizFilter != null) {
            sql.append(" AND quiz_id = ?");
            params.add(quizFilter);
        }

        try (PreparedStatement statement = requireConnection().prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void create(Question question) throws SQLException {
        String sql = "INSERT INTO question (quiz_id, content, type, point) VALUES (?, ?, ?, ?)";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindQuestion(statement, question, false);
            statement.executeUpdate();
        }
    }

    public void update(Question question) throws SQLException {
        String sql = "UPDATE question SET quiz_id=?, content=?, type=?, point=? WHERE id=?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindQuestion(statement, question, true);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM question WHERE id=?")) {
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

    private void bindQuestion(PreparedStatement statement, Question question, boolean withId) throws SQLException {
        statement.setInt(1, question.getQuiz().getId());
        statement.setString(2, question.getContent());
        statement.setString(3, question.getType());
        statement.setFloat(4, question.getPoint());
        if (withId) {
            statement.setInt(5, question.getId());
        }
    }

    private List<Question> mapResult(ResultSet rs) throws SQLException {
        List<Question> result = new ArrayList<>();
        while (rs.next()) {
            Question question = new Question();
            question.setId(rs.getInt("id"));
            question.setQuiz(new Quiz(rs.getInt("quiz_id")));
            question.setContent(rs.getString("content"));
            question.setType(rs.getString("type"));
            question.setPoint(rs.getFloat("point"));
            result.add(question);
        }
        return result;
    }

    private String normalizeSort(String sort) {
        if (sort == null || !ALLOWED_SORTS.contains(sort)) {
            return "id";
        }
        return sort;
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
