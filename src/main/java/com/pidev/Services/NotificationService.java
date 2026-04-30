package com.pidev.Services;

import com.pidev.models.Notif;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationService {
    private final Connection connection;

    public NotificationService() {
        this.connection = DataSource.getInstance().getConnection();
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS notif (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "user_id INT NOT NULL," +
                "message LONGTEXT NOT NULL," +
                "is_read TINYINT(4) DEFAULT 0," +
                "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            System.err.println("Error creating notif table: " + e.getMessage());
        }
    }

    public void addNotification(Notif notification) throws SQLException {
        String sql = "INSERT INTO notif (user_id, message, is_read, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, notification.getUser_id());
            ps.setString(2, notification.getMessage());
            ps.setBoolean(3, notification.isIs_read());
            ps.setTimestamp(4, Timestamp.valueOf(notification.getCreated_at()));
            ps.executeUpdate();
        }
    }

    public List<Notif> getNotificationsForUser(int userId) throws SQLException {
        List<Notif> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notif WHERE user_id = ? ORDER BY created_at DESC";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Notif n = new Notif();
                    n.setId(rs.getInt("id"));
                    n.setUser_id(rs.getInt("user_id"));
                    n.setMessage(rs.getString("message"));
                    n.setIs_read(rs.getBoolean("is_read"));
                    n.setCreated_at(rs.getTimestamp("created_at").toLocalDateTime());
                    notifications.add(n);
                }
            }
        }
        return notifications;
    }

    public int getUnreadCount(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM notif WHERE user_id = ? AND is_read = 0";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    public void markAsRead(int notificationId) throws SQLException {
        String sql = "UPDATE notif SET is_read = 1 WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }

    public void markAllAsRead(int userId) throws SQLException {
        String sql = "UPDATE notif SET is_read = 1 WHERE user_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    public void deleteNotification(int notificationId) throws SQLException {
        String sql = "DELETE FROM notif WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, notificationId);
            ps.executeUpdate();
        }
    }
}
