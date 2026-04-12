package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IMemberActivity;
import com.pidev.models.Activity;
import com.pidev.models.Group;
import com.pidev.models.MemberActivity;
import com.pidev.models.User;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceMemberActivity implements IMemberActivity {
    Connection connection;

    public ServiceMemberActivity() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void addMemberActivity(MemberActivity m, int activity_id, int user_id) {
        String query = "Insert INTO member_activity (activity_description,id_activity_id, " +
                "user_id_id) VALUES(?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, m.getActivityDescription());
            ps.setInt(2, activity_id);
            ps.setInt(3, user_id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public List<MemberActivity> display(int activity_id, int user_id) {
        String query = "SELECT id,activity_description FROM member_activity " +
                "WHERE id_activity_id = ? AND user_id_id=?";
        List<MemberActivity> m = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activity_id);
            ps.setInt(2, user_id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                MemberActivity ma = new MemberActivity();
                ma.setId(rs.getInt("id"));
                ma.setActivityDescription(rs.getString("activity_description"));
                m.add(ma);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return m;
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM member_activity WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting member activity: " + e.getMessage(), e);
        }
    }

    @Override
    public void update(MemberActivity m) {
        String query = "UPDATE member_activity SET activity_description=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, m.getActivityDescription());
            ps.setInt(2, m.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating member activity: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean findDescription(int activity_id, int user_id) {
        String query = "SELECT COUNT(*) FROM member_activity WHERE user_id_id=? AND id_activity_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, user_id);
            ps.setInt(2, activity_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }



    public List<MemberActivity> getAllGroupMembersForActivity(Group group, Activity activity) {
        List<MemberActivity> list = new ArrayList<>();
        String query = "SELECT u.id AS user_id, u.nom, u.prenom, " +
                "ma.id AS ma_id, ma.indiv_score, ma.activity_description " + // Use your actual column name here
                "FROM `user` u " +
                "INNER JOIN `membership` ms ON u.id = ms.user_id_id " +
                "LEFT JOIN `member_activity` ma ON u.id = ma.user_id_id AND ma.id_activity_id = ? " +
                "WHERE ms.group_id_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activity.getId());
            ps.setInt(2, group.getId());
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                User user = new User();
                user.setId(rs.getInt("user_id"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));

                MemberActivity ma = new MemberActivity();
                ma.setUser(user);
                ma.setActivity(activity);

                int maId = rs.getInt("ma_id");
                if (maId != 0) {
                    ma.setId(maId);
                    ma.setIndivScore(rs.getDouble("indiv_score"));
                    ma.setActivityDescription(rs.getString("activity_description"));
                } else {
                    ma.setId(-1);
                    ma.setIndivScore(0.0);
                    ma.setActivityDescription("No submission recorded.");
                }
                list.add(ma);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

    public void updateIndivScore(MemberActivity m,double score) {
        String query="UPDATE member_activity SET indiv_score=? WHERE id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setDouble(1,score);
            ps.setInt(2,m.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public boolean IsIndivScore(MemberActivity m){
        String query="SELECT indiv_score FROM member_activity WHERE id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,m.getId());
            ResultSet rs=ps.executeQuery();
            if(rs.next()){
                Object score = rs.getObject("indiv_score");
                return score != null;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return false;
    }
    public double SelectIndivScore(int user_id,int activity_id){
        String query="SELECT indiv_score FROM member_activity " +
                "WHERE id_activity_id=? AND user_id_id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,activity_id);
            ps.setInt(2,user_id);
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                double score = rs.getDouble("indiv_score");
                if(rs.wasNull()){
                    return 0;
                }
               return score;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;
    }
    public List<MemberActivity> SelectMActivity(int activity_id) {
        List<MemberActivity> list = new ArrayList<>();
        String query = "SELECT ma.*, u.nom, u.prenom " +
                "FROM member_activity ma " +
                "JOIN user u ON ma.user_id_id = u.id " +
                "WHERE ma.id_activity_id = ?";

        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activity_id);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                MemberActivity m_activity = new MemberActivity();
                m_activity.setId(rs.getInt("id"));

                String desc = rs.getString("activity_description");
                m_activity.setActivityDescription(desc != null ? desc : ""); 

                User u = new User();
                u.setNom(rs.getString("nom"));
                u.setPrenom(rs.getString("prenom"));

                m_activity.setUser(u);
                list.add(m_activity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return list;
    }
}
