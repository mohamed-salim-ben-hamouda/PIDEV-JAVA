package com.pidev.Services;

import com.pidev.models.Answer;
import com.pidev.models.Question;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AnswerService {
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "content", "isCorrect");
    private final Connection connection;

    public AnswerService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<Answer> findAll() throws SQLException {
        String sql = "SELECT id, question_id, content, is_correct FROM answer ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return mapResult(rs);
        }
    }

    public List<Answer> findPage(String search, Integer questionFilter, String sort, String direction, int page, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT id, question_id, content, is_correct FROM answer WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND content LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (questionFilter != null) {
            sql.append(" AND question_id = ?");
            params.add(questionFilter);
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

    public int count(String search, Integer questionFilter) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM answer WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND content LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (questionFilter != null) {
            sql.append(" AND question_id = ?");
            params.add(questionFilter);
        }

        try (PreparedStatement statement = requireConnection().prepareStatement(sql.toString())) {
            bindParams(statement, params);
            try (ResultSet rs = statement.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        }
    }

    public void create(Answer answer) throws SQLException {
        String sql = "INSERT INTO answer (question_id, content, is_correct) VALUES (?, ?, ?)";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindAnswer(statement, answer, false);
            statement.executeUpdate();
        }
    }

    public void update(Answer answer) throws SQLException {
        String sql = "UPDATE answer SET question_id=?, content=?, is_correct=? WHERE id=?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindAnswer(statement, answer, true);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM answer WHERE id=?")) {
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

    private void bindAnswer(PreparedStatement statement, Answer answer, boolean withId) throws SQLException {
        statement.setInt(1, answer.getQuestion().getId());
        statement.setString(2, answer.getContent());
        statement.setBoolean(3, answer.isCorrect());
        if (withId) {
            statement.setInt(4, answer.getId());
        }
    }

    private List<Answer> mapResult(ResultSet rs) throws SQLException {
        List<Answer> result = new ArrayList<>();
        while (rs.next()) {
            Answer answer = new Answer();
            answer.setId(rs.getInt("id"));
            answer.setQuestion(new Question(rs.getInt("question_id")));
            answer.setContent(rs.getString("content"));
            answer.setCorrect(rs.getBoolean("is_correct"));
            result.add(answer);
        }
        return result;
    }

    private String normalizeSort(String sort) {
        if (sort == null || !ALLOWED_SORTS.contains(sort)) {
            return "id";
        }
        return "isCorrect".equals(sort) ? "is_correct" : sort;
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
