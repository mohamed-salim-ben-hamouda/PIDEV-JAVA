package com.pidev.Services;

import com.pidev.models.Sponsor;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceSponsor implements ICrud<Sponsor> {

    private Connection connection;

    public ServiceSponsor() {
        connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void add(Sponsor sponsor) {
        String query = "INSERT INTO sponsor (name, description, logo_url, website_url, created_at, creator_id_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, sponsor.getName());
            pst.setString(2, sponsor.getDescription());
            pst.setString(3, sponsor.getLogoUrl());
            pst.setString(4, sponsor.getWebsiteUrl());
            pst.setTimestamp(5, Timestamp.valueOf(sponsor.getCreatedAt()));
            pst.setInt(6, sponsor.getCreator().getId());
            pst.executeUpdate();
            System.out.println("Sponsor added!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void update(Sponsor sponsor) {
        String query = "UPDATE sponsor SET name=?, description=?, logo_url=?, website_url=? WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setString(1, sponsor.getName());
            pst.setString(2, sponsor.getDescription());
            pst.setString(3, sponsor.getLogoUrl());
            pst.setString(4, sponsor.getWebsiteUrl());
            pst.setInt(5, sponsor.getId());
            pst.executeUpdate();
            System.out.println("Sponsor updated!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM sponsor WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            pst.executeUpdate();
            System.out.println("Sponsor deleted!");
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
    }

    @Override
    public List<Sponsor> getAll() {
        List<Sponsor> list = new ArrayList<>();
        String query = "SELECT * FROM sponsor";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(query)) {
            while (rs.next()) {
                Sponsor s = new Sponsor();
                s.setId(rs.getInt("id"));
                s.setName(rs.getString("name"));
                s.setDescription(rs.getString("description"));
                s.setLogoUrl(rs.getString("logo_url"));
                s.setWebsiteUrl(rs.getString("website_url"));
                s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                s.setCreator(new User(rs.getInt("creator_id_id")));
                list.add(s);
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return list;
    }

    @Override
    public Sponsor getById(int id) {
        String query = "SELECT * FROM sponsor WHERE id=?";
        try (PreparedStatement pst = connection.prepareStatement(query)) {
            pst.setInt(1, id);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) {
                    Sponsor s = new Sponsor();
                    s.setId(rs.getInt("id"));
                    s.setName(rs.getString("name"));
                    s.setDescription(rs.getString("description"));
                    s.setLogoUrl(rs.getString("logo_url"));
                    s.setWebsiteUrl(rs.getString("website_url"));
                    s.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    s.setCreator(new User(rs.getInt("creator_id_id")));
                    return s;
                }
            }
        } catch (SQLException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }
}
