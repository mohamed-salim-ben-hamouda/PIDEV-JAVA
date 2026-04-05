package com.pidev.Services.Membership;

import com.pidev.models.Group;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceMembership implements Imembership {
    Connection connection;
    public ServiceMembership() {
        this.connection = DataSource.getInstance().getConnection();
    }


    @Override
    public List<Group> FindAdminGroups(int user_id) {
        String query = "SELECT g.id, g.name FROM membership m " +
                "JOIN `group` g ON g.id = m.group_id_id " +
                "WHERE m.role = ? AND m.user_id_id = ?";
        List<Group> adminGrp = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, "leader");
            ps.setInt(2,user_id);
            try(ResultSet rs=ps.executeQuery()){
                while(rs.next()){
                    Group group = new Group();
                    group.setId(rs.getInt("id"));     // Button needs this
                    group.setName(rs.getString("name")); // Label needs this
                    adminGrp.add(group);                }
            }
            return adminGrp;

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
