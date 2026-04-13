package com.pidev.Services;

import com.pidev.models.Group;
import com.pidev.models.Membership;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MembershipService {

    private final Connection connection;
    private final String membershipTable;
    private final String userColumn;
    private final String groupColumn;
    private final String activeColumn;
    private final String achievementColumn;

    public MembershipService() {
        this.connection = DataSource.getInstance().getConnection();
        this.membershipTable = resolveMembershipTable();
        this.userColumn = resolveColumn(membershipTable, "user_id_id", "user_id");
        this.groupColumn = resolveColumn(membershipTable, "group_id_id", "group_id");
        this.activeColumn = resolveColumn(membershipTable, "is_active", "active");
        this.achievementColumn = resolveColumn(membershipTable, "achievement_unlocked", "achievement");
    }

    public List<Membership> findByUser(int userId) throws SQLException {
        String sql = "SELECT id, " + userColumn + ", " + groupColumn + ", role, contribution_score, " + achievementColumn + ", " + activeColumn +
                " FROM `" + membershipTable + "` WHERE " + userColumn + " = ? ORDER BY id DESC";

        List<Membership> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapMembership(rs));
                }
            }
        }
        return items;
    }

    public List<Membership> findByGroup(int groupId) throws SQLException {
        String sql = "SELECT id, " + userColumn + ", " + groupColumn + ", role, contribution_score, " + achievementColumn + ", " + activeColumn +
                " FROM `" + membershipTable + "` WHERE " + groupColumn + " = ? ORDER BY id DESC";

        List<Membership> items = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    items.add(mapMembership(rs));
                }
            }
        }
        return items;
    }

    public Optional<Membership> findOne(int userId, int groupId) throws SQLException {
        String sql = "SELECT id, " + userColumn + ", " + groupColumn + ", role, contribution_score, " + achievementColumn + ", " + activeColumn +
                " FROM `" + membershipTable + "` WHERE " + userColumn + " = ? AND " + groupColumn + " = ? LIMIT 1";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, groupId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapMembership(rs));
                }
            }
        }
        return Optional.empty();
    }

    public Membership create(Membership membership) throws SQLException {
        String sql = "INSERT INTO `" + membershipTable + "` (" + userColumn + ", " + groupColumn + ", role, contribution_score, " + achievementColumn + ", " + activeColumn + ") " +
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, membership.getUserId());
            ps.setInt(2, membership.getGroupId());
            ps.setString(3, membership.getRole());
            ps.setDouble(4, membership.getContributionScore());

            if (membership.getAchievementUnlocked() == null || membership.getAchievementUnlocked().isBlank()) {
                ps.setNull(5, java.sql.Types.VARCHAR);
            } else {
                ps.setString(5, membership.getAchievementUnlocked());
            }

            ps.setBoolean(6, membership.isActive());
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    membership.setId(keys.getInt(1));
                }
            }
        }

        return membership;
    }

    public Membership addIfMissing(int userId, int groupId, String role) throws SQLException {
        Optional<Membership> existing = findOne(userId, groupId);
        if (existing.isPresent()) {
            return existing.get();
        }

        Membership membership = new Membership();
        membership.setUserId(userId);
        membership.setGroupId(groupId);
        membership.setRole(role);
        membership.setContributionScore(0);
        membership.setAchievementUnlocked(null);
        membership.setActive(true);
        return create(membership);
    }

    public boolean removeById(int membershipId) throws SQLException {
        String sql = "DELETE FROM `" + membershipTable + "` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, membershipId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeByUserAndGroup(int userId, int groupId) throws SQLException {
        String sql = "DELETE FROM `" + membershipTable + "` WHERE " + userColumn + " = ? AND " + groupColumn + " = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, groupId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean setRole(int membershipId, String role) throws SQLException {
        String sql = "UPDATE `" + membershipTable + "` SET role = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, role);
            ps.setInt(2, membershipId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Group> findUserGroups(int userId, GroupService groupService) throws SQLException {
        List<Membership> memberships = findByUser(userId);
        List<Group> groups = new ArrayList<>();
        for (Membership membership : memberships) {
            if (membership.getGroupId() == null) {
                continue;
            }
            groupService.findById(membership.getGroupId()).ifPresent(groups::add);
        }
        return groups;
    }

    private Membership mapMembership(ResultSet rs) throws SQLException {
        Membership membership = new Membership();
        membership.setId(rs.getInt("id"));

        int userId = rs.getInt(userColumn);
        membership.setUserId(rs.wasNull() ? null : userId);

        int groupId = rs.getInt(groupColumn);
        membership.setGroupId(rs.wasNull() ? null : groupId);

        membership.setRole(rs.getString("role"));
        membership.setContributionScore(rs.getDouble("contribution_score"));
        membership.setAchievementUnlocked(rs.getString(achievementColumn));
        membership.setActive(rs.getBoolean(activeColumn));
        return membership;
    }

    private String resolveMembershipTable() {
        if (tableExists("membership")) {
            return "membership";
        }
        if (tableExists("memberships")) {
            return "memberships";
        }
        return "membership";
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
}
