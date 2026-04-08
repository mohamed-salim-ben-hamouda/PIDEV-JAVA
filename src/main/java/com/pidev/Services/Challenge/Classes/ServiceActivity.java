package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IActivity;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import com.pidev.utils.DataSource;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;


public class ServiceActivity implements IActivity {
    Connection connection;

    public ServiceActivity() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void StartActivity(Activity a, Challenge c, Group g) {
        String query = "INSERT INTO ACTIVITY (id_challenge_id,group_id_id,status) " +
                "VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, c.getId());
            ps.setInt(2, g.getId());
            ps.setString(3,"in_progress");
            ps.executeUpdate();

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    a.setId(keys.getInt(1));
                } else {
                    throw new SQLException("Activity created but no generated ID was returned.");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public Activity findActivityInprogress(int id){
        String query = "SELECT a.id,a.status, a.id_challenge_id, a.group_id_id, c.* FROM activity a " +
                "JOIN challenge c ON c.id = a.id_challenge_id " +
                "JOIN membership m ON m.group_id_id = a.group_id_id " +
                "WHERE m.user_id_id = ? " +
                "AND a.status = ?";
        try(PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,id);
            ps.setString(2,"in_progress");
            ResultSet rs = ps.executeQuery();
            if(rs.next()){
                Activity activity = new Activity();
                activity.setStatus(rs.getString("status"));
                activity.setId(rs.getInt("id"));
                Group Group = new Group();
                Group.setId(rs.getInt("group_id_id"));
                activity.setGroup(Group);
                Challenge challenge = new Challenge();
                challenge.setId(rs.getInt("id_challenge_id"));
                challenge.setTitle(rs.getString("title"));
                challenge.setDescription(rs.getString("description"));
                challenge.setDifficulty(rs.getString("difficulty"));

                activity.setChallenge(challenge);

                return activity;

            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
    @Override
    public boolean isUserLeader(int group_id,int user_id){
        String query="SELECT role FROM membership WHERE group_id_id=? AND user_id_id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)) {
            ps.setInt(1,group_id);
            ps.setInt(2,user_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String role = rs.getString("role");
                return "leader".equalsIgnoreCase(role);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        return false;
    }
    @Override
    public void submissionfile(Activity a){
        String query="UPDATE activity SET submission_file=? ,submission_date=?, status=? WHERE id=?";
        try(PreparedStatement ps=connection.prepareStatement(query)){
            ps.setString(1,a.getSubmissionFile());
            Timestamp now = new Timestamp(System.currentTimeMillis());
            ps.setTimestamp(2, now);
            ps.setString(3,"submitted");
            ps.setInt(4,a.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public boolean isActivityPassedByGrp(int group_id,int challenge_id){
        String query="SELECT status FROM activity WHERE group_id_id=? AND id_challenge_id=?";
        try(PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,group_id);
            ps.setInt(2,challenge_id);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){
                String status =rs.getString("status");
                return "in_progress".equalsIgnoreCase(status);
            }else{
                return true;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    public List<Group> findGroupsInActivity(int id_challenge){
        List<Group> gr = new ArrayList<>();
        String query = "SELECT DISTINCT g.id, g.name FROM `group` g " +
                "JOIN activity a ON a.group_id_id = g.id " +
                "WHERE a.id_challenge_id = ?";
        try(PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,id_challenge);
            ResultSet rs=ps.executeQuery();
            while (rs.next()){
                Group g=new Group();
                g.setId(rs.getInt("id"));
                g.setName(rs.getString("name"));
                gr.add(g);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return gr;
    }


}
