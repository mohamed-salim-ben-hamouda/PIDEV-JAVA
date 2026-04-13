package com.pidev.Services;

import com.pidev.models.CvApplication;
import com.pidev.models.Cv;
import com.pidev.models.Offer;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CvApplicationService {
    private final Connection connection;

    public CvApplicationService() {
        this.connection = DataSource.getInstance().getConnection();
        if (this.connection == null) {
            throw new IllegalStateException("Connexion MySQL indisponible");
        }
        ensureTableExists();
    }

    private void ensureTableExists() {
        String sql = "CREATE TABLE IF NOT EXISTS cv_application (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "offer_id INT NOT NULL," +
                "cv_id INT NOT NULL," +
                "status VARCHAR(20) DEFAULT 'PENDING'," +
                "applied_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP," +
                "FOREIGN KEY (offer_id) REFERENCES offer(id) ON DELETE CASCADE," +
                "FOREIGN KEY (cv_id) REFERENCES cv(id) ON DELETE CASCADE" +
                ")";
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating cv_application table: " + e.getMessage());
        }
    }

    public void postuler(CvApplication application) throws SQLException {
        String sql = "INSERT INTO cv_application (offer_id, cv_id, status, applied_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, application.getOffer().getId());
            ps.setInt(2, application.getCv().getId());
            ps.setString(3, application.getStatus());
            ps.setTimestamp(4, Timestamp.valueOf(application.getAppliedAt()));
            ps.executeUpdate();
        }
    }

    public List<CvApplication> getApplicationsByOffer(int offerId) throws SQLException {
        String sql = "SELECT a.*, cv.nom_cv, cv.langue FROM cv_application a " +
                "JOIN cv ON a.cv_id = cv.id " +
                "WHERE a.offer_id = ?";
        List<CvApplication> list = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, offerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CvApplication app = new CvApplication();
                    app.setId(rs.getInt("id"));
                    app.setOffer(new Offer(rs.getInt("offer_id")));

                    Cv cv = new Cv();
                    cv.setId(rs.getInt("cv_id"));
                    cv.setNomCv(rs.getString("nom_cv"));
                    cv.setLangue(rs.getString("langue"));
                    app.setCv(cv);

                    app.setStatus(rs.getString("status"));
                    app.setAppliedAt(rs.getTimestamp("applied_at").toLocalDateTime());
                    list.add(app);
                }
            }
        }
        return list;
    }

    public void updateStatus(int applicationId, String status) throws SQLException {
        String sql = "UPDATE cv_application SET status = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, applicationId);
            ps.executeUpdate();
        }
    }
}
