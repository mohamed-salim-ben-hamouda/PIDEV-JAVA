package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IMemberActivity;
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
    public ServiceMemberActivity(){
        this.connection= DataSource.getInstance().getConnection();
    }
    @Override
    public void addMemberActivity(MemberActivity m,int activity_id,int user_id){
        String query="Insert INTO member_activity (activity_description,id_activity_id, " +
                "user_id_id) VALUES(?,?,?)";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setString(1,m.getActivityDescription());
            ps.setInt(2,activity_id);
            ps.setInt(3,user_id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }
    public List<MemberActivity> display(int activity_id,int user_id){
        String query = "SELECT id,activity_description FROM member_activity " +
                "WHERE id_activity_id = ? AND user_id_id=?";
        List<MemberActivity> m=new ArrayList<>();
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,activity_id);
            ps.setInt(2,user_id);
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                MemberActivity ma=new MemberActivity();
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
    public boolean findDescription(int activity_id,int user_id){
        String query="SELECT COUNT(*) FROM member_activity WHERE user_id_id=? AND id_activity_id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,user_id);
            ps.setInt(2,activity_id);
            ResultSet rs=ps.executeQuery();
            if(rs.next()){
                return rs.getInt(1)>0;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return false;
    }



}
