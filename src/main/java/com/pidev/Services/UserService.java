package com.pidev.Services;

import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection connection;
    private final String tableName;

    public UserService() {
        this.connection = DataSource.getInstance().getConnection();
        this.tableName = resolveTableName();
    }

    public List<User> findAllForSessionSwitch() throws SQLException {
        String sql = "SELECT id, nom, prenom, email FROM `" + tableName + "` ORDER BY id ASC LIMIT 300";
        List<User> users = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                users.add(user);
            }
        }
        return users;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT id, nom, prenom, email FROM `" + tableName + "` WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        }
    }

    public User findByEmail(String email) throws SQLException {
        if (email == null || email.isBlank()) {
            return null;
        }
        String sql = "SELECT id, nom, prenom, email FROM `" + tableName + "` WHERE LOWER(email) = LOWER(?) LIMIT 1";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, email.trim());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                User user = new User();
                user.setId(rs.getInt("id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setEmail(rs.getString("email"));
                return user;
            }
        }
    }

    private String resolveTableName() {
        if (tableUsable("user")) {
            return "user";
        }
        if (tableUsable("users")) {
            return "users";
        }
        throw new IllegalStateException("No usable user table found. Expected `user` or `users`.");
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
