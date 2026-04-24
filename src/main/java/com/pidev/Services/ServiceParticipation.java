package com.pidev.Services;

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
    public void add(Participation p) {
        String query = "INSERT INTO participation (status, payment_status, payment_ref, registred_at, hackathon_id, group_id_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, p.getStatus());
            pst.setString(2, p.getPaymentStatus());
            pst.setString(3, p.getPaymentRef());
            pst.setTimestamp(4, Timestamp.valueOf(p.getRegisteredAt()));
            pst.setInt(5, p.getHackathon().getId());
            if (p.getGroupId() != null) {
                pst.setInt(6, p.getGroupId());
            } else {
                pst.setNull(6, Types.INTEGER);
            }
            pst.executeUpdate();
            System.out.println("Participation added!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void update(Participation p) {
        String query = "UPDATE participation SET status=?, payment_status=?, payment_ref=?, registred_at=?, hackathon_id=?, group_id_id=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, p.getStatus());
            pst.setString(2, p.getPaymentStatus());
            pst.setString(3, p.getPaymentRef());
            pst.setTimestamp(4, Timestamp.valueOf(p.getRegisteredAt()));
            pst.setInt(5, p.getHackathon().getId());
            if (p.getGroupId() != null) {
                pst.setInt(6, p.getGroupId());
            } else {
                pst.setNull(6, Types.INTEGER);
            }
            pst.setInt(7, p.getId());
            pst.executeUpdate();
            System.out.println("Participation updated!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
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
}
