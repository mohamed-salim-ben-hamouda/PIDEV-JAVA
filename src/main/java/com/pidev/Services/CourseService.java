package com.pidev.Services;

import com.pidev.models.Course;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

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
        String sql = "SELECT c.*, u.nom AS creator_nom, u.prenom AS creator_prenom, u.email AS creator_email "
                + "FROM course c LEFT JOIN user u ON c.creator_id = u.id ORDER BY c.id DESC";
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

        StringBuilder sql = new StringBuilder("SELECT c.*, u.nom AS creator_nom, u.prenom AS creator_prenom, u.email AS creator_email "
                + "FROM course c LEFT JOIN user u ON c.creator_id = u.id");
        if (search != null && !search.isBlank()) {
            sql.append(" WHERE c.title LIKE ? OR c.description LIKE ?");
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
        StringBuilder sql = new StringBuilder("SELECT COUNT(*) FROM course c");
        if (search != null && !search.isBlank()) {
            sql.append(" WHERE c.title LIKE ? OR c.description LIKE ?");
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
        String sql = "INSERT INTO course (title, description, duration, difficulty, is_active, validation_score, content, material, creator_id, prerequisite_quiz_id, sections_to_review) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql)) {
            bindCourse(statement, course, false);
            statement.executeUpdate();
        }
    }

    public void update(Course course) throws SQLException {
        String sql = "UPDATE course SET title=?, description=?, duration=?, difficulty=?, is_active=?, validation_score=?, content=?, material=?, creator_id=?, prerequisite_quiz_id=?, sections_to_review=? WHERE id=?";
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
        if (course.getCreator() == null || course.getCreator().getId() == null) {
            throw new SQLException("Supervisor is required for course.");
        }

        statement.setString(1, course.getTitle());
        statement.setString(2, course.getDescription());
        statement.setInt(3, course.getDuration());
        statement.setString(4, course.getDifficulty() == null || course.getDifficulty().isBlank()
                ? Course.DIFFICULTY_BEGINNER
                : course.getDifficulty());
        statement.setBoolean(5, course.isIsActive());
        statement.setFloat(6, course.getValidationScore());
        statement.setString(7, course.getContent() == null ? "" : course.getContent());

        if (course.getMaterial() == null || course.getMaterial().isBlank()) {
            statement.setNull(8, Types.VARCHAR);
        } else {
            statement.setString(8, course.getMaterial());
        }

        statement.setInt(9, course.getCreator().getId());

        if (course.getPrerequisiteQuiz() != null && course.getPrerequisiteQuiz().getId() != null) {
            statement.setInt(10, course.getPrerequisiteQuiz().getId());
        } else {
            statement.setNull(10, Types.INTEGER);
        }

        String sectionsJson = serializeSectionsToReview(course.getSectionsToReview());
        if (sectionsJson == null) {
            statement.setNull(11, Types.VARCHAR);
        } else {
            statement.setString(11, sectionsJson);
        }

        if (withId) {
            statement.setInt(12, course.getId());
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
                course.setIsActive(rs.getBoolean("is_active"));
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
                    User creator = new User(creatorId);
                    if (columns.contains("creator_nom")) {
                        creator.setNom(rs.getString("creator_nom"));
                    }
                    if (columns.contains("creator_prenom")) {
                        creator.setPrenom(rs.getString("creator_prenom"));
                    }
                    if (columns.contains("creator_email")) {
                        creator.setEmail(rs.getString("creator_email"));
                    }
                    course.setCreator(creator);
                }
            }
            if (columns.contains("prerequisite_quiz_id")) {
                int prerequisiteQuizId = rs.getInt("prerequisite_quiz_id");
                if (!rs.wasNull()) {
                    course.setPrerequisiteQuiz(new com.pidev.models.Quiz(prerequisiteQuizId));
                }
            }
            if (columns.contains("sections_to_review")) {
                course.setSectionsToReview(parseSectionsToReview(rs.getString("sections_to_review")));
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
            case "title" -> "c.title";
            case "duration" -> "c.duration";
            case "validationScore" -> "c.validation_score";
            default -> "c.id";
        };
    }

    private String normalizeDirection(String requestedDirection) {
        if (requestedDirection == null) {
            return "DESC";
        }
        return "ASC".equalsIgnoreCase(requestedDirection) ? "ASC" : "DESC";
    }

    private String serializeSectionsToReview(List<String> sections) {
        if (sections == null || sections.isEmpty()) {
            return null;
        }
        return sections.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(value -> "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private List<String> parseSectionsToReview(String json) {
        if (json == null || json.isBlank()) {
            return new ArrayList<>();
        }
        String trimmed = json.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            return new ArrayList<>(Collections.singletonList(trimmed));
        }

        String content = trimmed.substring(1, trimmed.length() - 1).trim();
        if (content.isEmpty()) {
            return new ArrayList<>();
        }

        String[] chunks = content.split(",");
        List<String> result = new ArrayList<>();
        for (String chunk : chunks) {
            String token = chunk.trim();
            if (token.startsWith("\"") && token.endsWith("\"") && token.length() >= 2) {
                token = token.substring(1, token.length() - 1);
            }
            token = token.replace("\\\"", "\"").replace("\\\\", "\\");
            if (!token.isBlank()) {
                result.add(token);
            }
        }
        return result;
    }
}
