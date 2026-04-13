package com.pidev.Services;

import com.pidev.models.Course;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class CourseService {
    private static final Set<String> ALLOWED_SORTS = Set.of("id", "title", "duration", "validationScore");
    private final Connection connection;

    public CourseService() {
        this.connection = DataSource.getInstance().getConnection();
        try {
            ensureCreatorIdNullable();
        } catch (SQLException e) {
            System.err.println("Unable to normalize course.creator_id nullability: " + e.getMessage());
        }
    }

    public List<Course> findAll() throws SQLException {
        String sql = "SELECT * FROM course ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            return mapCourses(rs);
        }
    }

    public List<Course> findPage(String search, String sort, String direction, int page, int limit) throws SQLException {
        String normalizedSort = normalizeSort(sort);
        String normalizedDirection = normalizeDirection(direction);
        int safePage = Math.max(page, 1);
        int safeLimit = Math.max(limit, 1);

        StringBuilder sql = new StringBuilder("SELECT * FROM course");
        if (search != null && !search.isBlank()) {
            sql.append(" WHERE title LIKE ? OR description LIKE ?");
        }
        sql.append(" ORDER BY ").append(normalizedSort).append(" ").append(normalizedDirection);
        sql.append(" LIMIT ? OFFSET ?");

        try (PreparedStatement statement = requireConnection().prepareStatement(sql.toString())) {
            int index = 1;
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim() + "%";
                statement.setString(index++, keyword);
                statement.setString(index++, keyword);
            }
            statement.setInt(index++, safeLimit);
            statement.setInt(index, (safePage - 1) * safeLimit);

            try (ResultSet rs = statement.executeQuery()) {
                return mapCourses(rs);
            }
        }
    }

    public int count(String search) throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM course");
        if (search != null && !search.isBlank()) {
            sql.append(" WHERE title LIKE ? OR description LIKE ?");
        }

        try (PreparedStatement statement = requireConnection().prepareStatement(sql.toString())) {
            if (search != null && !search.isBlank()) {
                String keyword = "%" + search.trim() + "%";
                statement.setString(1, keyword);
                statement.setString(2, keyword);
            }

            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return 0;
            }
        }
    }

    public void create(Course course) throws SQLException {
        String sql = "INSERT INTO course (title, description, duration, difficulty, is_active, validation_score, content, material, creator_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindCourse(statement, course, false);
            statement.executeUpdate();
        }
    }

    public void update(Course course) throws SQLException {
        String sql = "UPDATE course SET title=?, description=?, duration=?, difficulty=?, is_active=?, validation_score=?, content=?, material=?, creator_id=? WHERE id=?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindCourse(statement, course, true);
            statement.executeUpdate();
        }
    }

    public void delete(int id) throws SQLException {
        String sql = "DELETE FROM course WHERE id = ?";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        }
    }

    private Connection requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available. Check DataSource URL/user/password and database server.");
        }
        return connection;
    }

    private void bindCourse(PreparedStatement statement, Course course, boolean withId) throws SQLException {
        statement.setString(1, course.getTitle());
        statement.setString(2, course.getDescription());
        statement.setInt(3, course.getDuration());
        statement.setString(4, course.getDifficulty());
        statement.setBoolean(5, course.isActive());
        statement.setFloat(6, course.getValidationScore());
        statement.setString(7, course.getContent());
        statement.setString(8, course.getMaterial());

        if (course.getCreator() != null && course.getCreator().getId() != null) {
            statement.setInt(9, course.getCreator().getId());
        } else {
            statement.setNull(9, Types.INTEGER);
        }

        if (withId) {
            statement.setInt(10, course.getId());
        }
    }

    private void ensureCreatorIdNullable() throws SQLException {
        String metadataSql = "SELECT IS_NULLABLE FROM INFORMATION_SCHEMA.COLUMNS "
                + "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'course' AND COLUMN_NAME = 'creator_id'";

        try (PreparedStatement statement = requireConnection().prepareStatement(metadataSql);
             ResultSet rs = statement.executeQuery()) {
            if (rs.next() && "NO".equalsIgnoreCase(rs.getString("IS_NULLABLE"))) {
                try (PreparedStatement alterStatement = requireConnection().prepareStatement(
                        "ALTER TABLE course MODIFY creator_id INT NULL")) {
                    alterStatement.executeUpdate();
                }
            }
        }
    }

    private List<Course> mapCourses(ResultSet rs) throws SQLException {
        List<Course> courses = new ArrayList<>();
        ResultSetMetaData metaData = rs.getMetaData();
        Set<String> columns = new HashSet<>();
        for (int i = 1; i <= metaData.getColumnCount(); i++) {
            columns.add(metaData.getColumnLabel(i).toLowerCase(Locale.ROOT));
        }

        while (rs.next()) {
            Course course = new Course();
            if (columns.contains("id")) {
                course.setId(rs.getInt("id"));
            }
            if (columns.contains("title")) {
                course.setTitle(rs.getString("title"));
            }
            if (columns.contains("description")) {
                course.setDescription(rs.getString("description"));
            }
            if (columns.contains("duration")) {
                course.setDuration(rs.getInt("duration"));
            }
            if (columns.contains("difficulty")) {
                course.setDifficulty(rs.getString("difficulty"));
            }
            if (columns.contains("is_active")) {
                course.setActive(rs.getBoolean("is_active"));
            }
            if (columns.contains("validation_score")) {
                course.setValidationScore(rs.getFloat("validation_score"));
            }
            if (columns.contains("content")) {
                course.setContent(rs.getString("content"));
            }
            if (columns.contains("material")) {
                course.setMaterial(rs.getString("material"));
            }
            if (columns.contains("creator_id")) {
                int creatorId = rs.getInt("creator_id");
                if (!rs.wasNull()) {
                    course.setCreator(new User(creatorId));
                }
            }

            courses.add(course);
        }

        return courses;
    }

    private String normalizeSort(String requestedSort) {
        if (requestedSort == null || !ALLOWED_SORTS.contains(requestedSort)) {
            return "id";
        }

        return switch (requestedSort) {
            case "title" -> "title";
            case "duration" -> "duration";
            case "validationScore" -> "validation_score";
            default -> "id";
        };
    }

    private String normalizeDirection(String requestedDirection) {
        if (requestedDirection == null) {
            return "DESC";
        }
        return "ASC".equalsIgnoreCase(requestedDirection) ? "ASC" : "DESC";
    }
}
