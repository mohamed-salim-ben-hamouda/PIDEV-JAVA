package com.pidev.Services;

import com.pidev.models.User;
import com.pidev.utils.DataSource;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserService implements ICrud<User> {

    private Connection connection;

    public UserService() {
        connection = DataSource.getInstance().getConnection();
        try {
            if (connection != null && !connection.isClosed()) {
                DatabaseMetaData meta = connection.getMetaData();
                System.out.println("Debug: UserService connected to: " + meta.getURL());
            } else {
                System.err.println("Debug: UserService connection is NULL or CLOSED!");
            }
        } catch (SQLException e) {
            System.err.println("Debug: Error checking connection metadata: " + e.getMessage());
        }
    }

    @Override
    //ajouter user
    public boolean add(User user) {
        // Hash password before saving
        String hashedPasswd = BCrypt.hashpw(user.getPasswd(), BCrypt.gensalt());
        user.setPasswd(hashedPasswd);

        String query = "INSERT INTO user (nom, prenom, email, passwd, date_naissance, type, date_inscrit, is_active, photo) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getPasswd());
            if (user.getDateNaissance() != null) {
                pst.setDate(5, Date.valueOf(user.getDateNaissance()));
            } else {
                pst.setNull(5, Types.DATE);
            }
            pst.setString(6, user.getRole() != null ? user.getRole().name() : null);
            pst.setTimestamp(7, user.getDateInscrit() != null ? Timestamp.valueOf(user.getDateInscrit()) : Timestamp.valueOf(LocalDateTime.now()));
            pst.setBoolean(8, user.isActive());
            pst.setString(9, user.getPhoto());
            pst.executeUpdate();
            System.out.println("User added successfully!");
            return true;
        } catch (SQLException e) {
            System.err.println("Error adding user: " + e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(User user) {
        // Only hash if password changed (not already hashed)
        if (!user.getPasswd().startsWith("$2a$") && !user.getPasswd().startsWith("$2b$") && !user.getPasswd().startsWith("$2y$")) {
            user.setPasswd(BCrypt.hashpw(user.getPasswd(), BCrypt.gensalt()));
        }

        String query = "UPDATE user SET nom = ?, prenom = ?, email = ?, passwd = ?, date_naissance = ?, type = ?, date_inscrit = ?, is_active = ?, photo = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, user.getNom());
            pst.setString(2, user.getPrenom());
            pst.setString(3, user.getEmail());
            pst.setString(4, user.getPasswd());
            if (user.getDateNaissance() != null) {
                pst.setDate(5, Date.valueOf(user.getDateNaissance()));
            } else {
                pst.setNull(5, Types.DATE);
            }
            pst.setString(6, user.getRole() != null ? user.getRole().name() : null);
            pst.setTimestamp(7, user.getDateInscrit() != null ? Timestamp.valueOf(user.getDateInscrit()) : Timestamp.valueOf(LocalDateTime.now()));
            pst.setInt(8, user.isActive() ? 1 : 0);
            pst.setString(9, user.getPhoto());
            pst.setInt(10, user.getId());
            pst.executeUpdate();
            System.out.println("User updated successfully!");
            return true;
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("User deleted successfully!");
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
        }
    }

    @Override
    public List<User> getAll() {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user"; // All users
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (Exception e) {
            System.err.println("Error in getAll(): " + e.getMessage());
        }
        return users;
    }

    public List<User> getActiveUsers() {
        return getUsersWithStatus(true);
    }

    public List<User> getArchivedUsers() {
        return getUsersWithStatus(false);
    }

    private List<User> getUsersWithStatus(boolean isActive) {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user WHERE is_active = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            // Using setInt for better compatibility with different SQL drivers treating BOOLEAN as TINYINT
            pst.setInt(1, isActive ? 1 : 0);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    users.add(mapResultSetToUser(rs));
                }
            }
            System.out.println("Debug: Fetched " + users.size() + " users with status " + (isActive ? "active" : "archived"));
        } catch (SQLException e) {
            System.err.println("Error fetching users with status " + isActive + ": " + e.getMessage());
        }
        return users;
    }

    @Override
    public User getById(int id) {
        String query = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user by id: " + e.getMessage());
        }
        return null;
    }

    public User login(String email, String password) {
        String query = "SELECT * FROM user WHERE LOWER(TRIM(email)) = LOWER(TRIM(?))";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    // Try to find password column dynamicallly
                    String storedPass = null;
                    try { 
                        storedPass = rs.getString("passwd"); 
                    } catch (Exception e) { 
                        try {
                            storedPass = rs.getString("password");
                        } catch (Exception e2) {
                            System.err.println("CRITICAL: Password column (passwd/password) not found in database!");
                            return null;
                        }
                    }

                    if (storedPass == null) return null;
                    storedPass = storedPass.trim();
                    
                    // Check if it's BCrypt
                    boolean isHashed = storedPass.startsWith("$2a$") || storedPass.startsWith("$2b$") || storedPass.startsWith("$2y$");
                    
                    if (isHashed) {
                        try {
                            if (BCrypt.checkpw(password, storedPass)) {
                                return mapResultSetToUser(rs);
                            } else {
                                System.out.println("Debug: Password mismatch for hashed user " + email);
                            }
                        } catch (Exception e) {
                            System.err.println("BCrypt check failed: " + e.getMessage());
                        }
                    } else {
                        // Plaintext fallback
                        if (password.equals(storedPass)) {
                            System.out.println("Debug: Legacy login success, migrating " + email + " to hash...");
                            User user = mapResultSetToUser(rs);
                            user.setPasswd(password);
                            update(user);
                            return user;
                        } else {
                            System.out.println("Debug: Plaintext password mismatch for " + email);
                        }
                    }
                } else {
                    System.out.println("Debug: No user found in database with email: [" + email.trim().toLowerCase() + "]");
                }
            }
        } catch (SQLException e) {
            System.err.println("SQL Error during login: " + e.getMessage());
        }
        return null;
    }

    public boolean isEmailExists(String email) {
        String query = "SELECT count(*) FROM user WHERE email = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking email: " + e.getMessage());
        }
        return false;
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        try { user.setId(rs.getInt("id")); } catch (Exception e) {}
        try { user.setNom(rs.getString("nom")); } catch (Exception e) {}
        try { user.setPrenom(rs.getString("prenom")); } catch (Exception e) {}
        try { user.setEmail(rs.getString("email")); } catch (Exception e) {}
        
        // Dynamic password column
        String pass = null;
        try { pass = rs.getString("passwd"); } catch (Exception e) {
            try { pass = rs.getString("password"); } catch (Exception e2) {}
        }
        user.setPasswd(pass);
        
        try {
            Date dbDate = rs.getDate("date_naissance");
            if (dbDate != null) user.setDateNaissance(dbDate.toLocalDate());
        } catch (Exception e) {}
        
        try {
            String roleStr = rs.getString("type");
            if (roleStr != null) user.setRole(User.Role.valueOf(roleStr.trim().toUpperCase()));
        } catch (Exception e) {}
        
        try {
            Timestamp ts = rs.getTimestamp("date_inscrit");
            if (ts != null) user.setDateInscrit(ts.toLocalDateTime());
        } catch (Exception e) {}
        
        try { 
            Object activeObj = rs.getObject("is_active");
            if (activeObj instanceof Boolean) {
                user.setActive((Boolean) activeObj);
            } else if (activeObj instanceof Number) {
                user.setActive(((Number) activeObj).intValue() == 1);
            } else if (activeObj != null) {
                // Fallback for strings like "true", "1", etc.
                String s = activeObj.toString().trim();
                user.setActive(s.equalsIgnoreCase("true") || s.equals("1") || s.equalsIgnoreCase("active"));
            }
        } catch (Exception e) {
            System.err.println("Mapping error for is_active: " + e.getMessage());
        }
        try { user.setConnected(rs.getBoolean("is_connected")); } catch (Exception e) {}
        try { user.setPhoto(rs.getString("photo")); } catch (Exception e) {}
        
        return user;
    }

    public void setConnectedStatus(int userId, boolean status) {
        String query = "UPDATE user SET is_connected = ? WHERE id = ?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setBoolean(1, status);
            pst.setInt(2, userId);
            pst.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error updating connection status: " + e.getMessage());
        }
    }

    public int getTotalUsersCount() {
        String query = "SELECT count(*) FROM user WHERE is_active = 1";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (Exception e) {
            System.err.println("Error in getTotalUsersCount(): " + e.getMessage());
        }
        return 0;
    }

    public int getConnectedUsersCount() {
        String query = "SELECT count(*) FROM user WHERE is_connected = 1 AND is_active = 1";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting connected users: " + e.getMessage());
        }
        return 0;
    }
}
