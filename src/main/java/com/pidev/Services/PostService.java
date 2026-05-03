package com.pidev.Services;

import com.pidev.models.Post;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class PostService {

    private final Connection connection;
    private final String tableName;

    public PostService() {
        this.connection = DataSource.getInstance().getConnection();
        this.tableName = resolveTableName();
    }

    public List<Post> findAllNewestFirst() throws SQLException {
        String sql = "SELECT id, description, titre, status, visibility, attached_file, created_at, updated_at, " +
                "likes_counter, group_id_id, author_id_id " +
                "FROM `" + tableName + "` ORDER BY created_at DESC";

        List<Post> posts = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                posts.add(mapRow(rs));
            }
        }
        return posts;
    }

    public List<Post> findAllForFeedRanked() throws SQLException {
        LocalDateTime now = LocalDateTime.now();
        List<Post> posts = findAllNewestFirst();
        Comparator<Post> feedComparator = Comparator
                .comparing((Post post) -> isStaleWithoutReactions(post, now))
                .thenComparing(Comparator.comparingDouble((Post post) -> trendingScore(post, now)).reversed())
                .thenComparing(Post::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()));

        return posts.stream()
                .filter(Objects::nonNull)
                .sorted(feedComparator)
                .toList();
    }

    public void createPost(Post post) throws SQLException {
        String sql = "INSERT INTO `" + tableName + "` (description, titre, status, visibility, attached_file, created_at, updated_at, likes_counter, group_id_id, author_id_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime now = LocalDateTime.now();
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, post.getDescription());
            ps.setString(2, post.getTitre());
            ps.setString(3, post.getStatus());
            ps.setString(4, post.getVisibility());

            if (isBlank(post.getAttachedFile())) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, post.getAttachedFile());
            }

            ps.setTimestamp(6, Timestamp.valueOf(now));
            ps.setTimestamp(7, Timestamp.valueOf(now));
            ps.setInt(8, Math.max(0, post.getLikesCounter()));

            if (post.getGroupId() == null) {
                ps.setNull(9, java.sql.Types.INTEGER);
            } else {
                ps.setInt(9, post.getGroupId());
            }

            ps.setInt(10, post.getAuthorId());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    post.setId(keys.getInt(1));
                }
            }
        }
    }

    public void updatePost(Post post) throws SQLException {
        String sql = "UPDATE `" + tableName + "` SET description = ?, titre = ?, status = ?, visibility = ?, attached_file = ?, " +
                "updated_at = ?, likes_counter = ?, group_id_id = ?, author_id_id = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, post.getDescription());
            ps.setString(2, post.getTitre());
            ps.setString(3, post.getStatus());
            ps.setString(4, post.getVisibility());

            if (isBlank(post.getAttachedFile())) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, post.getAttachedFile());
            }

            ps.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(7, Math.max(0, post.getLikesCounter()));

            if (post.getGroupId() == null) {
                ps.setNull(8, java.sql.Types.INTEGER);
            } else {
                ps.setInt(8, post.getGroupId());
            }

            ps.setInt(9, post.getAuthorId());
            ps.setInt(10, post.getId());
            ps.executeUpdate();
        }
    }

    public void deletePost(int id) throws SQLException {
        deleteFromTableByPostId("post_comments", id);
        deleteFromTableByPostId("post_reactions", id);
        String sql = "DELETE FROM `" + tableName + "` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    public void incrementLike(int id) throws SQLException {
        String sql = "UPDATE `" + tableName + "` SET likes_counter = likes_counter + 1, updated_at = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(2, id);
            ps.executeUpdate();
        }
    }

    private Post mapRow(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setId(rs.getInt("id"));
        post.setDescription(rs.getString("description"));
        post.setTitre(rs.getString("titre"));
        post.setStatus(rs.getString("status"));
        post.setVisibility(rs.getString("visibility"));
        post.setAttachedFile(rs.getString("attached_file"));
        post.setCreatedAt(toLocalDateTime(rs.getTimestamp("created_at")));
        post.setUpdatedAt(toLocalDateTime(rs.getTimestamp("updated_at")));
        post.setLikesCounter(rs.getInt("likes_counter"));

        int groupId = rs.getInt("group_id_id");
        post.setGroupId(rs.wasNull() ? null : groupId);

        post.setAuthorId(rs.getInt("author_id_id"));
        return post;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private double trendingScore(Post post, LocalDateTime now) {
        int reactions = Math.max(0, post.getLikesCounter());
        LocalDateTime createdAt = post.getCreatedAt();
        long ageMinutes = createdAt == null ? 0 : Math.max(0, Duration.between(createdAt, now).toMinutes());

        if (reactions == 0 && ageMinutes > 30) {
            // Keep stale zero-reaction posts visible, but force them to the bottom.
            return -1_000_000.0 - ageMinutes;
        }

        double reactionBoost = reactions * 18.0;
        double freshnessBoost = Math.max(0, 180 - ageMinutes);
        double earlyBonus = ageMinutes <= 30 ? 25.0 : 0.0;
        double decayPenalty = ageMinutes * 0.35;

        return reactionBoost + freshnessBoost + earlyBonus - decayPenalty;
    }

    private boolean isStaleWithoutReactions(Post post, LocalDateTime now) {
        if (post == null) {
            return true;
        }

        int reactions = Math.max(0, post.getLikesCounter());
        LocalDateTime createdAt = post.getCreatedAt();
        long ageMinutes = createdAt == null ? 0 : Math.max(0, Duration.between(createdAt, now).toMinutes());
        return reactions == 0 && ageMinutes > 30;
    }

    private String resolveTableName() {
        if (tableUsable("post")) {
            return "post";
        }
        if (tableUsable("posts")) {
            return "posts";
        }
        throw new IllegalStateException(
                "No usable table found for posts. Expected `post` or `posts`. " +
                        "If you see MySQL error #1932, recreate the broken table."
        );
    }

    private boolean tableExists(String candidate) {
        String sql = "SHOW TABLES LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, candidate);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }

    private boolean tableUsable(String candidate) {
        if (!tableExists(candidate)) {
            return false;
        }

        String sql = "SELECT 1 FROM `" + candidate + "` LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.executeQuery();
            return true;
        } catch (SQLException ignored) {
            return false;
        }
    }

    private void deleteFromTableByPostId(String targetTable, int postId) throws SQLException {
        if (!tableExists(targetTable)) {
            return;
        }

        String postColumn = "post_id";
        if (!columnExists(targetTable, "post_id")) {
            if (columnExists(targetTable, "post_id_id")) {
                postColumn = "post_id_id";
            } else if (columnExists(targetTable, "post")) {
                postColumn = "post";
            } else {
                return;
            }
        }

        String sql = "DELETE FROM `" + targetTable + "` WHERE " + postColumn + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, postId);
            ps.executeUpdate();
        }
    }

    private boolean columnExists(String table, String column) {
        String sql = "SHOW COLUMNS FROM `" + table + "` LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, column);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
        }
    }
}
