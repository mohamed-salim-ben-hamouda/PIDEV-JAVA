package com.pidev.Services;

import com.pidev.models.Group;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GroupService {

    private final Connection connection;
    private final String groupTable;
    private final String leaderColumn;

    public GroupService() {
        this.connection = DataSource.getInstance().getConnection();
        this.groupTable = resolveGroupTable();
        this.leaderColumn = resolveColumn(groupTable, "leader_id_id", "leader_id");
    }

    public List<Group> findAll() throws SQLException {
        String sql = "SELECT id, name, description, creation_date, type, level, " + leaderColumn + ", max_members, rating_score, icon " +
                "FROM `" + groupTable + "` ORDER BY creation_date DESC";

        List<Group> groups = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                groups.add(mapGroup(rs));
            }
        }
        return groups;
    }

    public Optional<Group> findById(int id) throws SQLException {
        String sql = "SELECT id, name, description, creation_date, type, level, " + leaderColumn + ", max_members, rating_score, icon " +
                "FROM `" + groupTable + "` WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapGroup(rs));
                }
            }
        }
        return Optional.empty();
    }

    public List<Group> findByLeaderId(int leaderId) throws SQLException {
        String sql = "SELECT id, name, description, creation_date, type, level, " + leaderColumn + ", max_members, rating_score, icon " +
                "FROM `" + groupTable + "` WHERE " + leaderColumn + " = ? ORDER BY creation_date DESC";

        List<Group> groups = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, leaderId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    groups.add(mapGroup(rs));
                }
            }
        }
        return groups;
    }

    public Group create(Group group) throws SQLException {
        String sql = "INSERT INTO `" + groupTable + "` (name, description, creation_date, type, level, " + leaderColumn + ", max_members, rating_score, icon) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";

        LocalDateTime created = group.getCreationDate() == null ? LocalDateTime.now() : group.getCreationDate();
        group.setCreationDate(created);

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, group.getName());
            ps.setString(2, group.getDescription());
            ps.setTimestamp(3, Timestamp.valueOf(created));
            ps.setString(4, group.getType());
            ps.setString(5, group.getLevel());
            ps.setInt(6, group.getLeaderId());
            ps.setInt(7, group.getMaxMembers() == null ? 0 : group.getMaxMembers());
            ps.setDouble(8, group.getRatingScore());
            ps.setString(9, group.getIcon());

            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    group.setId(keys.getInt(1));
                }
            }
        }

        return group;
    }

    public boolean update(Group group) throws SQLException {
        String sql = "UPDATE `" + groupTable + "` SET name = ?, description = ?, type = ?, level = ?, " +
                leaderColumn + " = ?, max_members = ?, rating_score = ?, icon = ? WHERE id = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, group.getName());
            ps.setString(2, group.getDescription());
            ps.setString(3, group.getType());
            ps.setString(4, group.getLevel());
            ps.setInt(5, group.getLeaderId());
            ps.setInt(6, group.getMaxMembers() == null ? 0 : group.getMaxMembers());
            ps.setDouble(7, group.getRatingScore());
            ps.setString(8, group.getIcon());
            ps.setInt(9, group.getId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean deleteWithDependencies(int groupId) throws SQLException {
        connection.setAutoCommit(false);
        try {
            deleteMembershipsByGroup(groupId);
            deletePostsDependencies(groupId);

            String sql = "DELETE FROM `" + groupTable + "` WHERE id = ?";
            boolean deleted;
            try (PreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, groupId);
                deleted = ps.executeUpdate() > 0;
            }

            connection.commit();
            return deleted;
        } catch (SQLException e) {
            connection.rollback();
            throw e;
        } finally {
            connection.setAutoCommit(true);
        }
    }

    public List<User> searchUsers(String term) throws SQLException {
        if (term == null || term.trim().isEmpty()) {
            return List.of();
        }

        String sql = "SELECT id, nom, prenom, email FROM `user` " +
                "WHERE LOWER(nom) LIKE LOWER(?) OR LOWER(prenom) LIKE LOWER(?) OR LOWER(email) LIKE LOWER(?) " +
                "ORDER BY id DESC LIMIT 20";

        String pattern = "%" + term.trim() + "%";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, pattern);
            ps.setString(2, pattern);
            ps.setString(3, pattern);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    User user = new User();
                    user.setId(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setEmail(rs.getString("email"));
                    users.add(user);
                }
            }
        }
        return users;
    }

    private Group mapGroup(ResultSet rs) throws SQLException {
        Group group = new Group();
        group.setId(rs.getInt("id"));
        group.setName(rs.getString("name"));
        group.setDescription(rs.getString("description"));
        group.setCreationDate(toLocalDateTime(rs.getTimestamp("creation_date")));
        group.setType(rs.getString("type"));
        group.setLevel(rs.getString("level"));

        int leaderId = rs.getInt(leaderColumn);
        group.setLeaderId(rs.wasNull() ? null : leaderId);

        int max = rs.getInt("max_members");
        group.setMaxMembers(rs.wasNull() ? null : max);

        group.setRatingScore(rs.getDouble("rating_score"));
        group.setIcon(rs.getString("icon"));
        return group;
    }

    private LocalDateTime toLocalDateTime(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime();
    }

    private String resolveGroupTable() {
        if (tableExists("group")) {
            return "group";
        }
        if (tableExists("groups")) {
            return "groups";
        }
        return "group";
    }

    private String resolveColumn(String table, String firstChoice, String fallback) {
        if (columnExists(table, firstChoice)) {
            return firstChoice;
        }
        return fallback;
    }

    private boolean tableExists(String table) {
        String sql = "SHOW TABLES LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, table);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ignored) {
            return false;
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

    private void deleteMembershipsByGroup(int groupId) throws SQLException {
        String membershipTable = tableExists("membership") ? "membership" : "memberships";
        String groupFk = columnExists(membershipTable, "group_id_id") ? "group_id_id" : "group_id";

        String sql = "DELETE FROM `" + membershipTable + "` WHERE " + groupFk + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ps.executeUpdate();
        }
    }

    private void deletePostsDependencies(int groupId) throws SQLException {
        String postTable = tableExists("post") ? "post" : (tableExists("posts") ? "posts" : null);
        if (postTable == null) {
            return;
        }

        String groupFk = columnExists(postTable, "group_id_id") ? "group_id_id" : "group_id";
        List<Integer> postIds = new ArrayList<>();

        String postSql = "SELECT id FROM `" + postTable + "` WHERE " + groupFk + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(postSql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    postIds.add(rs.getInt("id"));
                }
            }
        }

        if (postIds.isEmpty()) {
            return;
        }

        deleteByPostIds("reactions", postIds);
        deleteByPostIds("reaction", postIds);
        deleteByPostIds("commentaires", postIds);
        deleteByPostIds("commentaire", postIds);

        String deletePostsSql = "DELETE FROM `" + postTable + "` WHERE " + groupFk + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(deletePostsSql)) {
            ps.setInt(1, groupId);
            ps.executeUpdate();
        }
    }

    private void deleteByPostIds(String table, List<Integer> postIds) throws SQLException {
        if (!tableExists(table) || postIds.isEmpty()) {
            return;
        }

        String postColumn = columnExists(table, "post_id_id") ? "post_id_id" :
                (columnExists(table, "post_id") ? "post_id" :
                        (columnExists(table, "post") ? "post" : null));

        if (postColumn == null) {
            return;
        }

        StringBuilder sql = new StringBuilder("DELETE FROM `" + table + "` WHERE " + postColumn + " IN (");
        for (int i = 0; i < postIds.size(); i++) {
            if (i > 0) {
                sql.append(",");
            }
            sql.append("?");
        }
        sql.append(")");

        try (PreparedStatement ps = connection.prepareStatement(sql.toString())) {
            for (int i = 0; i < postIds.size(); i++) {
                ps.setInt(i + 1, postIds.get(i));
            }
            ps.executeUpdate();
        }
    }
}
