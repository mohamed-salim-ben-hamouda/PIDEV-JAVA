package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IActivity;
import com.pidev.models.Activity;
import com.pidev.models.Challenge;
import com.pidev.models.Group;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class ServiceActivity implements IActivity {
    Connection connection;

    public ServiceActivity() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void StartActivity(Activity a, Challenge c, Group g) {
        String query = "INSERT INTO ACTIVITY (id_challenge_id,group_id_id,status) " +
                "VALUES (?,?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, c.getId());
            ps.setInt(2, g.getId());
            ps.setString(3,"in_progress");
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }
    @Override
    public Activity findActivityInprogress(int id){
        String query = "SELECT a.status, a.id_challenge_id, a.group_id_id, c.* FROM activity a " +
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

}
