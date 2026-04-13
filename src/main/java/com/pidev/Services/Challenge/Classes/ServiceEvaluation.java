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
import java.util.ArrayList;
import java.util.List;

public class ServiceEvaluation implements IEvaluation {
    Connection connection;

    public ServiceEvaluation() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public void StartEvaluation(Evaluation e, Activity a) {
        String query = "INSERT INTO evaluation (activity_id_id,status) " +
                "VALUES (?,?)";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, a.getId());
            ps.setString(2, "in_progress");
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public boolean isEvaluation(int activity_id) {
        String query = "SELECT COUNT(*) FROM evaluation WHERE activity_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activity_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    public void updateEvaluation(Evaluation e) {
        String query = "UPDATE evaluation SET group_score=? , feedback=? , status=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setDouble(1, e.getGroupScore());
            ps.setString(2, e.getFeedback());
            ps.setString(3, "finished");
            ps.setLong(4, e.getId());
            ps.executeUpdate();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public Evaluation findEvaluation(int activity_id) {
        String query = "SELECT * FROM evaluation WHERE activity_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, activity_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Evaluation e = new Evaluation();
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

    public double SelectGrpScore(int evaluation_id) {
        String query = "SELECT group_score FROM evaluation WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, evaluation_id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                double score = rs.getDouble("group_score");
                if (rs.wasNull()) {
                    return 0;
                }
                return score;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return 0;

    }

    public void delete(long evaluation_id){
        String query = "DELETE FROM evaluation WHERE id= ?";
        try (PreparedStatement ps = connection.prepareStatement(query)){
            ps.setLong(1,evaluation_id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
    public List<Evaluation> displayAll(){
        String query="SELECT * FROM evaluation";
        List<Evaluation> list = new ArrayList<>();
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                Evaluation e = new Evaluation();
                e.setId(rs.getLong("id"));
                e.setGroupScore(rs.getDouble("group_score"));
                e.setFeedback(rs.getString("feedback"));
                e.setStatus(rs.getString("status"));
                int activityId = rs.getInt("activity_id_id");
                if (activityId > 0) {
                    Activity a = new Activity();
                    a.setId(activityId);
                    e.setActivity(a);
                }
                list.add(e);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }

}
