package com.pidev.Services;

import com.pidev.models.Hackathon;
import com.pidev.models.Sponsor;
import com.pidev.models.SponsorHackathon;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSponsorHackathon implements ICrud<SponsorHackathon> {

    private Connection connection;

    public ServiceSponsorHackathon() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void add(SponsorHackathon sh) {
        String query = "INSERT INTO sponsor_hackathon (sponsor_id, hackathon_id, contribution_type, contribution_value) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sh.getSponsor().getId());
            pst.setInt(2, sh.getHackathon().getId());
            pst.setString(3, sh.getContributionType());
            pst.setDouble(4, sh.getContributionValue());
            pst.executeUpdate();
            System.out.println("SponsorHackathon relation added!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void update(SponsorHackathon sh) {
        String query = "UPDATE sponsor_hackathon SET sponsor_id=?, hackathon_id=?, contribution_type=?, contribution_value=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, sh.getSponsor().getId());
            pst.setInt(2, sh.getHackathon().getId());
            pst.setString(3, sh.getContributionType());
            pst.setDouble(4, sh.getContributionValue());
            pst.setInt(5, sh.getId());
            pst.executeUpdate();
            System.out.println("SponsorHackathon relation updated!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM sponsor_hackathon WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("SponsorHackathon relation deleted!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public List<SponsorHackathon> getAll() {
        List<SponsorHackathon> list = new ArrayList<>();
        String query = "SELECT * FROM sponsor_hackathon";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                list.add(mapResultSetToSponsorHackathon(rs));
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public SponsorHackathon getById(int id) {
        String query = "SELECT * FROM sponsor_hackathon WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToSponsorHackathon(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private SponsorHackathon mapResultSetToSponsorHackathon(ResultSet rs) throws SQLException {
        SponsorHackathon sh = new SponsorHackathon();
        sh.setId(rs.getInt("id"));
        sh.setSponsor(new Sponsor(rs.getInt("sponsor_id")));
        sh.setHackathon(new Hackathon(rs.getInt("hackathon_id")));
        sh.setContributionType(rs.getString("contribution_type"));
        sh.setContributionValue(rs.getDouble("contribution_value"));
        return sh;
    }

    public List<SponsorHackathon> getByHackathon(int hackathonId) {
        List<SponsorHackathon> list = new ArrayList<>();
        String query = "SELECT * FROM sponsor_hackathon WHERE hackathon_id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, hackathonId);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    list.add(mapResultSetToSponsorHackathon(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }
}
