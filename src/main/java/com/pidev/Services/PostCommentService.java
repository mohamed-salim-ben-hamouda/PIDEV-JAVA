package com.pidev.Services;

import com.pidev.models.PostComment;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class PostCommentService {
    private static final String COMMENTS_TABLE = "post_comments";
    private final Connection connection;

    public PostCommentService() {
        this.connection = DataSource.getInstance().getConnection();
        ensureSchema();
    }

    public void addComment(int postId, int userId, String content) throws SQLException {
        String normalized = normalizeContent(content);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Comment cannot be empty.");
        }

        String sql = "INSERT INTO `" + COMMENTS_TABLE + "` (post_id, user_id, content, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.setString(3, normalized);
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
    }

    public List<PostComment> findRecentByPost(int postId, int limit) throws SQLException {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        String sql = "SELECT id, post_id, user_id, content, created_at FROM `" + COMMENTS_TABLE + "` " +
                "WHERE post_id = ? ORDER BY created_at DESC LIMIT " + safeLimit;

        List<PostComment> comments = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    comments.add(mapRow(rs));
                }
            }
        }

        comments.sort((a, b) -> {
            LocalDateTime da = a.getCreatedAt();
            LocalDateTime db = b.getCreatedAt();
            if (da == null && db == null) {
                return 0;
            }
            if (da == null) {
                return -1;
            }
            if (db == null) {
                return 1;
            }
            return da.compareTo(db);
        });
        return comments;
    }

    public int countByPost(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM `" + COMMENTS_TABLE + "` WHERE post_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return 0;
                }
                return rs.getInt("total");
            }
        }
    }

    private String normalizeContent(String value) {
        if (value == null) {
            return "";
        }
        String trimmed = value.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private void ensureSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS `" + COMMENTS_TABLE + "` (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "post_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "content VARCHAR(500) NOT NULL," +
                "created_at DATETIME NOT NULL," +
                "INDEX idx_post (post_id)," +
                "INDEX idx_post_created (post_id, created_at)" +
                ")";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize post comments table: " + e.getMessage(), e);
        }
    }

    private PostComment mapRow(ResultSet rs) throws SQLException {
        PostComment comment = new PostComment();
        comment.setId(rs.getInt("id"));
        comment.setPostId(rs.getInt("post_id"));
        comment.setUserId(rs.getInt("user_id"));
        comment.setContent(rs.getString("content"));
        Timestamp timestamp = rs.getTimestamp("created_at");
        comment.setCreatedAt(timestamp == null ? null : timestamp.toLocalDateTime());
        return comment;
    }
}
