package com.pidev.Services;

import com.pidev.models.Hackathon;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceHackathon implements ICrud<Hackathon> {

    private Connection connection;

    public ServiceHackathon() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void add(Hackathon h) {
        String query = "INSERT INTO hackathon (title, theme, description, rules, start_at, end_at, " +
                "registration_open_at, registration_close_at, fee, max_teams, team_size_max, location, " +
                "cover_url, status, created_at, creator_id_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, h.getTitle());
            pst.setString(2, h.getTheme());
            pst.setString(3, h.getDescription());
            pst.setString(4, h.getRules());
            pst.setTimestamp(5, Timestamp.valueOf(h.getStartAt()));
            pst.setTimestamp(6, Timestamp.valueOf(h.getEndAt()));
            pst.setTimestamp(7, Timestamp.valueOf(h.getRegistrationOpenAt()));
            pst.setTimestamp(8, Timestamp.valueOf(h.getRegistrationCloseAt()));
            pst.setDouble(9, h.getFee());
            pst.setInt(10, h.getMaxTeams());
            pst.setInt(11, h.getTeamSizeMax());
            pst.setString(12, h.getLocation());
            pst.setString(13, h.getCoverUrl());
            pst.setString(14, h.getStatus());
            pst.setTimestamp(15, Timestamp.valueOf(h.getCreatedAt()));
            pst.setInt(16, h.getCreator().getId());
            pst.executeUpdate();
            System.out.println("Hackathon added!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void update(Hackathon h) {
        String query = "UPDATE hackathon SET title=?, theme=?, description=?, rules=?, start_at=?, end_at=?, " +
                "registration_open_at=?, registration_close_at=?, fee=?, max_teams=?, team_size_max=?, location=?, " +
                "cover_url=?, status=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, h.getTitle());
            pst.setString(2, h.getTheme());
            pst.setString(3, h.getDescription());
            pst.setString(4, h.getRules());
            pst.setTimestamp(5, Timestamp.valueOf(h.getStartAt()));
            pst.setTimestamp(6, Timestamp.valueOf(h.getEndAt()));
            pst.setTimestamp(7, Timestamp.valueOf(h.getRegistrationOpenAt()));
            pst.setTimestamp(8, Timestamp.valueOf(h.getRegistrationCloseAt()));
            pst.setDouble(9, h.getFee());
            pst.setInt(10, h.getMaxTeams());
            pst.setInt(11, h.getTeamSizeMax());
            pst.setString(12, h.getLocation());
            pst.setString(13, h.getCoverUrl());
            pst.setString(14, h.getStatus());
            pst.setInt(15, h.getId());
            pst.executeUpdate();
            System.out.println("Hackathon updated!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM hackathon WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Hackathon deleted!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public List<Hackathon> getAll() {
        List<Hackathon> list = new ArrayList<>();
        String query = "SELECT * FROM hackathon";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToHackathon(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public Hackathon getById(int id) {
        String query = "SELECT * FROM hackathon WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToHackathon(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private Hackathon mapResultSetToHackathon(ResultSet rs) throws SQLException {
        Hackathon h = new Hackathon();
        h.setId(rs.getInt("id"));
        h.setTitle(rs.getString("title"));
        h.setTheme(rs.getString("theme"));
        h.setDescription(rs.getString("description"));
        h.setRules(rs.getString("rules"));
        h.setStartAt(rs.getTimestamp("start_at").toLocalDateTime());
        h.setEndAt(rs.getTimestamp("end_at").toLocalDateTime());
        h.setRegistrationOpenAt(rs.getTimestamp("registration_open_at").toLocalDateTime());
        h.setRegistrationCloseAt(rs.getTimestamp("registration_close_at").toLocalDateTime());
        h.setFee(rs.getDouble("fee"));
        h.setMaxTeams(rs.getInt("max_teams"));
        h.setTeamSizeMax(rs.getInt("team_size_max"));
        h.setLocation(rs.getString("location"));
        h.setCoverUrl(rs.getString("cover_url"));
        h.setStatus(rs.getString("status"));
        h.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        h.setCreator(new User(rs.getInt("creator_id_id")));
        return h;
    }
}
