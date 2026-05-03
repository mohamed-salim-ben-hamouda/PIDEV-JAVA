package com.pidev.Services;

import com.pidev.models.Challenge;
import com.pidev.models.Hackathon;
import com.pidev.models.Participation;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceParticipation implements ICrud<Participation> {

    private Connection connection;

    public ServiceParticipation() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public boolean add(Participation p) {
        String query = "INSERT INTO participation (status, payment_status, payment_ref, registred_at, hackathon_id, group_id_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, p.getStatus());
            pst.setString(2, p.getPaymentStatus());
            pst.setString(3, p.getPaymentRef());
            pst.setTimestamp(4, Timestamp.valueOf(p.getRegisteredAt()));
            pst.setInt(5, p.getHackathon().getId());
            pst.setInt(6, resolveValidGroupId(p.getGroupId()));
            pst.executeUpdate();
            System.out.println("Participation added!");
            return true;
        } catch (SQLException e) {
            if ("23000".equals(e.getSQLState())) {
                System.err.println("Participation insert failed: invalid foreign key value for group_id_id.");
            }
            System.err.println(e.getMessage());
            return false;
        }
    }

    @Override
    public boolean update(Participation p) {
        String query = "UPDATE participation SET status=?, payment_status=?, payment_ref=?, registred_at=?, hackathon_id=?, group_id_id=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, p.getStatus());
            pst.setString(2, p.getPaymentStatus());
            pst.setString(3, p.getPaymentRef());
            pst.setTimestamp(4, Timestamp.valueOf(p.getRegisteredAt()));
            pst.setInt(5, p.getHackathon().getId());
            pst.setInt(6, resolveValidGroupId(p.getGroupId()));
            pst.setInt(7, p.getId());
            pst.executeUpdate();
            System.out.println("Participation updated!");
            return true;
        } catch (SQLException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM participation WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Participation deleted!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public List<Participation> getAll() {
        List<Participation> list = new ArrayList<>();
        String query = "SELECT * FROM participation";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToParticipation(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public Participation getById(int id) {
        String query = "SELECT * FROM participation WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToParticipation(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private Participation mapResultSetToParticipation(ResultSet rs) throws SQLException {
        Participation p = new Participation();
        p.setId(rs.getInt("id"));
        p.setStatus(rs.getString("status"));
        p.setPaymentStatus(rs.getString("payment_status"));
        p.setPaymentRef(rs.getString("payment_ref"));
        p.setRegisteredAt(rs.getTimestamp("registred_at").toLocalDateTime());
        
        // We only set the ID for the hackathon to avoid circular dependency or complex joins here
        // If needed, we can use ServiceHackathon to load it fully
        Hackathon h = new Hackathon();
        h.setId(rs.getInt("hackathon_id"));
        p.setHackathon(h);
        
        int groupId = rs.getInt("group_id_id");
        if (!rs.wasNull()) {
            p.setGroupId(groupId);
        }
        
        return p;
    }

    private int resolveValidGroupId(Integer requestedGroupId) throws SQLException {
        if (requestedGroupId != null && groupExists(requestedGroupId)) {
            return requestedGroupId;
        }

        Integer fallbackGroupId = fetchAnyExistingGroupId();
        if (fallbackGroupId != null) {
            return fallbackGroupId;
        }

        throw new SQLException("No existing group found. Cannot insert participation with required group_id_id.");
    }

    private boolean groupExists(int groupId) throws SQLException {
        String query = "SELECT id FROM `group` WHERE id = ? LIMIT 1";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, groupId);
            try (ResultSet rs = pst.executeQuery()) {
                return rs.next();
            }
        }
    }

    private Integer fetchAnyExistingGroupId() throws SQLException {
        String query = "SELECT id FROM `group` ORDER BY id ASC LIMIT 1";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            if (rs.next()) {
                return rs.getInt("id");
            }
            return null;
        }
    }
    @Override
    public List<Participation> display(){
        return null;
    }

}
