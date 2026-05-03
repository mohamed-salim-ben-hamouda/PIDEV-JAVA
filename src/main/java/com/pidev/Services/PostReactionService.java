package com.pidev.Services;

import com.pidev.models.ReactionType;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.Map;

public class PostReactionService {

    private static final String REACTION_TABLE = "post_reactions";
    private final Connection connection;
    private final String postsTableName;

    public PostReactionService() {
        this.connection = DataSource.getInstance().getConnection();
        this.postsTableName = resolvePostsTableName();
        ensureSchema();
    }

    public void setReaction(int postId, int userId, ReactionType type) throws SQLException {
        if (type == null) {
            throw new IllegalArgumentException("Reaction type is required.");
        }

        String sql = "INSERT INTO `" + REACTION_TABLE + "` (post_id, user_id, reaction_type, reacted_at) " +
                "VALUES (?, ?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE reaction_type = VALUES(reaction_type), reacted_at = VALUES(reacted_at)";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.setString(3, type.code());
            ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            ps.executeUpdate();
        }
        syncLegacyLikeCounter(postId);
    }

    public void removeReaction(int postId, int userId) throws SQLException {
        String sql = "DELETE FROM `" + REACTION_TABLE + "` WHERE post_id = ? AND user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            ps.executeUpdate();
        }
        syncLegacyLikeCounter(postId);
    }

    public ReactionType findUserReaction(int postId, int userId) throws SQLException {
        String sql = "SELECT reaction_type FROM `" + REACTION_TABLE + "` WHERE post_id = ? AND user_id = ? LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.setInt(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return ReactionType.fromCode(rs.getString("reaction_type"));
            }
        }
    }

    public Map<ReactionType, Integer> countByPost(int postId) throws SQLException {
        Map<ReactionType, Integer> counts = new EnumMap<>(ReactionType.class);
        for (ReactionType type : ReactionType.values()) {
            counts.put(type, 0);
        }

        String sql = "SELECT reaction_type, COUNT(*) AS total FROM `" + REACTION_TABLE + "` WHERE post_id = ? GROUP BY reaction_type";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ReactionType type = ReactionType.fromCode(rs.getString("reaction_type"));
                    if (type != null) {
                        counts.put(type, rs.getInt("total"));
                    }
                }
            }
        }
        return counts;
    }

    public int totalReactions(int postId) throws SQLException {
        String sql = "SELECT COUNT(*) AS total FROM `" + REACTION_TABLE + "` WHERE post_id = ?";
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

    private void ensureSchema() {
        String sql = "CREATE TABLE IF NOT EXISTS `" + REACTION_TABLE + "` (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "post_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "reaction_type VARCHAR(20) NOT NULL," +
                "reacted_at DATETIME NOT NULL," +
                "UNIQUE KEY uq_post_user (post_id, user_id)," +
                "INDEX idx_post (post_id)" +
                ")";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.execute();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize reactions table: " + e.getMessage(), e);
        }
    }

    private void syncLegacyLikeCounter(int postId) throws SQLException {
        String sql = "UPDATE `" + postsTableName + "` p " +
                "SET p.likes_counter = (SELECT COUNT(*) FROM `" + REACTION_TABLE + "` r WHERE r.post_id = p.id) " +
                "WHERE p.id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        }
    }

    private String resolvePostsTableName() {
        if (tableUsable("posts")) {
            return "posts";
        }
        if (tableUsable("post")) {
            return "post";
        }
        throw new IllegalStateException("No usable post table found. Expected `posts` or `post`.");
    }

    private boolean tableUsable(String candidate) {
        String sql = "SELECT 1 FROM `" + candidate + "` LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeQuery();
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }
}
