package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IMemberActivity;
import com.pidev.models.Activity;
import com.pidev.models.MemberActivity;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;

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


}
