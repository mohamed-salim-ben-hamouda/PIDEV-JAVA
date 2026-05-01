package com.pidev.Services;

import com.pidev.models.MotivationLetter;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MotivationLetterService {
    private final Connection connection;

    public MotivationLetterService() {
        this.connection = DataSource.getInstance().getConnection();
        if (this.connection == null) {
            throw new IllegalStateException("Connexion MySQL indisponible");
        }
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS motivation_letter (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "content TEXT NOT NULL," +
                "cv_id INT NOT NULL," +
                "offer_id INT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (cv_id) REFERENCES cv(id) ON DELETE CASCADE," +
                "FOREIGN KEY (offer_id) REFERENCES offer(id) ON DELETE CASCADE" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating motivation_letter table: " + e.getMessage());
        }
    }

    public void save(MotivationLetter letter) throws SQLException {
        // Check if a letter already exists for this CV and offer to avoid duplicates
        MotivationLetter existing = getByCvAndOffer(letter.getCvId(), letter.getOfferId());
        if (existing != null) {
            update(letter);
            return;
        }

        String sql = "INSERT INTO motivation_letter (content, cv_id, offer_id, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, letter.getContent());
            ps.setInt(2, letter.getCvId());
            ps.setInt(3, letter.getOfferId());
            ps.setTimestamp(4, Timestamp.valueOf(letter.getCreatedAt()));
            ps.executeUpdate();

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    letter.setId(rs.getInt(1));
                }
            }
        }
    }

    public void update(MotivationLetter letter) throws SQLException {
        String sql = "UPDATE motivation_letter SET content = ?, created_at = ? WHERE cv_id = ? AND offer_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, letter.getContent());
            ps.setTimestamp(2, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(3, letter.getCvId());
            ps.setInt(4, letter.getOfferId());
            ps.executeUpdate();
        }
    }

    public MotivationLetter getByCvAndOffer(int cvId, int offerId) throws SQLException {
        String sql = "SELECT * FROM motivation_letter WHERE cv_id = ? AND offer_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cvId);
            ps.setInt(2, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToLetter(rs);
                }
            }
        }
        return null;
    }

    public List<MotivationLetter> getByCv(int cvId) throws SQLException {
        String sql = "SELECT * FROM motivation_letter WHERE cv_id = ?";
        List<MotivationLetter> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, cvId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToLetter(rs));
                }
            }
        }
        return list;
    }

    private MotivationLetter mapResultSetToLetter(ResultSet rs) throws SQLException {
        MotivationLetter letter = new MotivationLetter();
        letter.setId(rs.getInt("id"));
        letter.setContent(rs.getString("content"));
        letter.setCvId(rs.getInt("cv_id"));
        letter.setOfferId(rs.getInt("offer_id"));
        letter.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        return letter;
    }
}
