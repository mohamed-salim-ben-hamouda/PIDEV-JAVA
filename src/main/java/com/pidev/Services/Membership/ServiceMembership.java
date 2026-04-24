package com.pidev.Services.Membership;

import com.pidev.models.Group;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
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
    public List<User> getAllGroupMembersForGit(int group_id){
        List<User> list = new ArrayList<>();
        String query= "SELECT u.id AS user_id, u.nom, u.prenom, u.git_username " +
                "FROM `user` u " +
                "INNER JOIN `membership` ms ON u.id = ms.user_id_id " +
                "WHERE ms.group_id_id = ?";
        try (PreparedStatement ps = connection.prepareStatement(query)){
            ps.setInt(1,group_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()){
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                user.setGit_username(rs.getString("git_username"));
                list.add(user);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return list;
    }

}
