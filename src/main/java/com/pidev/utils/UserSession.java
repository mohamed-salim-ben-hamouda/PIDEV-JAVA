
package com.pidev.utils;

import com.pidev.models.User;
import com.pidev.utils.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserSession {
    private static User currentUser;

    public static User getCurrentUser() {
        if (currentUser == null) {
            // Simulation : on récupère l'utilisateur ID=1 par défaut s'il n'y a pas de session
            try {
                currentUser = fetchUserById(1);
            } catch (SQLException e) {
                System.err.println("Impossible de récupérer l'utilisateur simulateur : " + e.getMessage());
            }
        }
        return currentUser;
    }

    public static void setCurrentUser(User user) {
        currentUser = user;
    }

    private static User fetchUserById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";
        try (Connection conn = DataSource.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    User user = new User(rs.getInt("id"));
                    user.setNom(rs.getString("nom"));
                    user.setPrenom(rs.getString("prenom"));
                    user.setEmail(rs.getString("email"));
                    return user;
                }
            }
        }
        return null;
    }
}

