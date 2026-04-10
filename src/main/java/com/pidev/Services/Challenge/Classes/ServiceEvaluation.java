package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IEvaluation;
import com.pidev.models.Activity;
import com.pidev.models.Evaluation;
import com.pidev.utils.DataSource;

import javax.management.Query;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ServiceEvaluation implements IEvaluation {
    Connection connection;
    public ServiceEvaluation(){
        this.connection= DataSource.getInstance().getConnection();
    }
    public void StartEvaluation(Evaluation e, Activity a){
        String query="INSERT INTO evaluation (activity_id_id,status) " +
                "VALUES (?,?)";
        try (PreparedStatement ps= connection.prepareStatement(query)){
            ps.setInt(1,a.getId());
            ps.setString(2, "in_progress");
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public boolean isEvaluation(int activity_id){
        String query="SELECT COUNT(*) FROM evaluation WHERE activity_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)){
            ps.setInt(1,activity_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()){
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }
    public void updateEvaluation(Evaluation e){
        String query="UPDATE evaluation SET group_score=? , feedback=? , status=? WHERE id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setDouble(1,e.getGroupScore());
            ps.setString(2,e.getFeedback());
            ps.setString(3,"finished");
            ps.setLong(4,e.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    public Evaluation findEvaluation(int activity_id){
        String query="SELECT * FROM evaluation WHERE activity_id_id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,activity_id);
            ResultSet rs= ps.executeQuery();
            if(rs.next()){
                Evaluation e =new Evaluation();
                e.setId(rs.getLong("id"));
                e.setGroupScore(rs.getDouble("group_score"));
                e.setFeedback(rs.getString("feedback"));

                return e;
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return null;
    }
}
