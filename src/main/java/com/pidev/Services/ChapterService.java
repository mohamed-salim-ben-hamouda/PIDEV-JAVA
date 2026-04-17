package com.pidev.Services;

import com.pidev.models.Chapter;
import com.pidev.models.Course;
import com.pidev.models.Quiz;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class ChapterService {
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "title", "chapterOrder", "status", "minScore");
    private final Connection connection;

    public ChapterService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<Chapter> findAll() throws SQLException {
        String sql = "SELECT ch.id, ch.course_id, ch.chapter_order, ch.status, ch.min_score, ch.content, ch.title, "
                + "(SELECT q.id FROM quiz q WHERE q.chapter_id = ch.id LIMIT 1) AS quiz_id "
                + "FROM chapter ch ORDER BY ch.id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return mapResult(rs);
        }
    }

    public List<Chapter> findPage(String search, Integer courseFilter, String sort, String direction, int page, int limit) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT ch.id, ch.course_id, ch.chapter_order, ch.status, ch.min_score, ch.content, ch.title, "
                + "(SELECT q.id FROM quiz q WHERE q.chapter_id = ch.id LIMIT 1) AS quiz_id "
                + "FROM chapter ch WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (search != null && !search.isBlank()) {
            sql.append(" AND ch.title LIKE ?");
            params.add("%" + search.trim() + "%");
        }
        if (courseFilter != null) {
            sql.append(" AND ch.course_id = ?");
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
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM chapter WHERE 1=1");
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

    public void create(Chapter chapter) throws SQLException {
        String sql = "INSERT INTO chapter (course_id, chapter_order, status, min_score, content, title) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindChapter(statement, chapter, false);
            statement.executeUpdate();
        }
    }

    public void update(Chapter chapter) throws SQLException {
        String sql = "UPDATE chapter SET course_id=?, chapter_order=?, status=?, min_score=?, content=?, title=? WHERE id=?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindChapter(statement, chapter, true);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        try (PreparedStatement statement = requireConnection().prepareStatement("DELETE FROM chapter WHERE id=?")) {
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

    private void bindChapter(PreparedStatement statement, Chapter chapter, boolean withId) throws SQLException {
        if (chapter == null) {
            throw new SQLException("Chapter payload is required.");
        }
        if (chapter.getCourse() == null || chapter.getCourse().getId() == null) {
            throw new SQLException("Course is required for chapter.");
        }
        statement.setInt(1, chapter.getCourse().getId());
        statement.setInt(2, chapter.getChapterOrder());
        statement.setString(3, chapter.getStatus());
        statement.setFloat(4, chapter.getMinScore());
        statement.setString(5, chapter.getContent());
        statement.setString(6, chapter.getTitle());
        if (withId) {
            statement.setInt(7, chapter.getId());
        }
    }

    private List<Chapter> mapResult(ResultSet rs) throws SQLException {
        List<Chapter> result = new ArrayList<>();
        while (rs.next()) {
            Chapter chapter = new Chapter();
            chapter.setId(rs.getInt("id"));
            chapter.setCourse(new Course(rs.getInt("course_id")));
            chapter.setChapterOrder(rs.getInt("chapter_order"));
            chapter.setStatus(rs.getString("status"));
            chapter.setMinScore(rs.getFloat("min_score"));
            chapter.setContent(rs.getString("content"));
            chapter.setTitle(rs.getString("title"));

            int quizId = rs.getInt("quiz_id");
            if (!rs.wasNull()) {
                chapter.setQuiz(new Quiz(quizId));
            }
            result.add(chapter);
        }
        return result;
    }

    private String normalizeSort(String sort) {
        if (sort == null || !ALLOWED_SORTS.contains(sort)) {
            return "chapter_order";
        }
        return switch (sort) {
            case "id" -> "id";
            case "title" -> "title";
            case "status" -> "status";
            case "minScore" -> "min_score";
            default -> "chapter_order";
        };
    }

    private String normalizeDirection(String direction) {
        return "DESC".equalsIgnoreCase(direction) ? "DESC" : "ASC";
    }

    private void bindParams(PreparedStatement statement, List<Object> params) throws SQLException {
        for (int i = 0; i < params.size(); i++) {
            statement.setObject(i + 1, params.get(i));
        }
    }
}
