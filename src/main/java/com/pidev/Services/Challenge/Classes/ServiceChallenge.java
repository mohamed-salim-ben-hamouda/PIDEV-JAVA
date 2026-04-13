package com.pidev.Services.Challenge.Classes;

import com.pidev.utils.DataSource;

import com.pidev.Services.ICrud;
import com.pidev.Services.Challenge.Interfaces.IChallenge;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import com.pidev.models.Challenge;

import javax.management.Query;
import java.sql.Timestamp;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.ArrayList;


public class ServiceChallenge implements ICrud<Challenge>, IChallenge {
    Connection connection;

    public ServiceChallenge() {
        this.connection = DataSource.getInstance().getConnection();
    }

    @Override
    public void add(Challenge c) {
        String query = "INSERT INTO challenge (title, description, target_skill, difficulty," +
                "min_group_nbr, max_group_nbr, dead_line, created_at,content, creator_id, course_id) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
        try (PreparedStatement st = connection.prepareStatement(query)) {
            st.setString(1, c.getTitle());
            st.setString(2, c.getDescription());
            st.setString(3, c.getTargetSkill());
            st.setString(4, c.getDifficulty());
            st.setInt(5, c.getMinGroupNbr());
            st.setInt(6, c.getMaxGroupNbr());
            st.setTimestamp(7, java.sql.Timestamp.valueOf(c.getDeadLine()));
            st.setTimestamp(8, java.sql.Timestamp.valueOf(c.getCreatedAt()));
            st.setString(9, c.getContent());

            st.setInt(10, 1);
            st.setInt(11, 1);
            st.executeUpdate();


        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void update(Challenge c) {
        String query="UPDATE challenge SET title=?,description=?,target_skill=?," +
                "difficulty=?,min_group_nbr=?,max_group_nbr=?,dead_line=?,content=? WHERE id=?";
        try(PreparedStatement ps=connection.prepareStatement(query)){
            ps.setString(1,c.getTitle());
            ps.setString(2,c.getDescription());
            ps.setString(3,c.getTargetSkill());
            ps.setString(4,c.getDifficulty());
            ps.setInt(5,c.getMinGroupNbr());
            ps.setInt(6,c.getMaxGroupNbr());
            if (c.getDeadLine() != null) {
                ps.setTimestamp(7, Timestamp.valueOf(c.getDeadLine()));
            } else {
                ps.setNull(7, Types.TIMESTAMP);
            }
            ps.setString(8, c.getContent());
            ps.setInt(9,c.getId());
            ps.executeUpdate();
        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM challenge WHERE id= ?";
        try(PreparedStatement st =connection.prepareStatement(query)){
            st.setInt(1, id);
            st.executeUpdate();

        }catch (SQLException e){
            System.out.println(e.getMessage());
        }
    }

    @Override
    public List<Challenge> display() {
        List<Challenge> challenges = new ArrayList<>();
        String query = "SELECT id, title, description, target_skill, difficulty, min_group_nbr, max_group_nbr, dead_line, created_at, content FROM challenge ORDER BY created_at DESC";

        try (PreparedStatement st = connection.prepareStatement(query);
             ResultSet rs = st.executeQuery()) {
            while (rs.next()) {
                Challenge c = new Challenge();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                c.setDescription(rs.getString("description"));
                c.setTargetSkill(rs.getString("target_skill"));
                c.setDifficulty(rs.getString("difficulty"));
                c.setMinGroupNbr(rs.getInt("min_group_nbr"));
                c.setMaxGroupNbr(rs.getInt("max_group_nbr"));

                java.sql.Timestamp deadLine = rs.getTimestamp("dead_line");
                if (deadLine != null) {
                    c.setDeadLine(deadLine.toLocalDateTime());
                }

                java.sql.Timestamp createdAt = rs.getTimestamp("created_at");
                if (createdAt != null) {
                    c.setCreatedAt(createdAt.toLocalDateTime());
                }

                c.setContent(rs.getString("content"));
                challenges.add(c);
            }
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }

        return challenges;
    }

    @Override
    public List<Challenge> findChallengeWithActivities()
    {
        List<Challenge> c=new ArrayList<>();
        String query="SELECT DISTINCT c.id , c.title , c.description FROM challenge c " +
                "JOIN activity a ON a.id_challenge_id=c.id";
        try(PreparedStatement ps=connection.prepareStatement(query)){
            ResultSet rs =ps.executeQuery();
            while(rs.next()){
                Challenge ch = new Challenge();
                ch.setId(rs.getInt("id"));
                ch.setTitle(rs.getString("title"));
                ch.setDescription(rs.getString("description"));
                c.add(ch);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return c;
    }
    public List<Challenge> displayALL(){
        List<Challenge> list = new ArrayList<>();
        String query="SELECT * FROM challenge";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ResultSet rs=ps.executeQuery();
            while(rs.next()){
                Challenge c = new Challenge();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                c.setDescription(rs.getString("description"));
                c.setTargetSkill(rs.getString("target_skill"));
                c.setDifficulty(rs.getString("difficulty"));
                c.setMinGroupNbr(rs.getInt("min_group_nbr"));
                c.setMaxGroupNbr(rs.getInt("max_group_nbr"));
                c.setDeadLine(rs.getTimestamp("dead_line").toLocalDateTime());
                list.add(c);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return list;
    }
    public List<Challenge> searchChallenge(String query) {
        List<Challenge> list = new ArrayList<>();
        String sql = "SELECT * FROM challenge WHERE LOWER(title) LIKE ? " +
                "OR LOWER(description) LIKE ? OR LOWER(target_skill) LIKE ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            String searchPattern = "%" + query.toLowerCase() + "%";
            ps.setString(1, searchPattern);
            ps.setString(2, searchPattern);
            ps.setString(3, searchPattern);

            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Challenge c = new Challenge();
                c.setId(rs.getInt("id"));
                c.setTitle(rs.getString("title"));
                c.setDescription(rs.getString("description"));
                c.setTargetSkill(rs.getString("target_skill"));
                c.setDifficulty(rs.getString("difficulty"));
                c.setMinGroupNbr(rs.getInt("min_group_nbr"));
                c.setMaxGroupNbr(rs.getInt("max_group_nbr"));
                if (rs.getDate("dead_line") != null) {
                    c.setDeadLine(rs.getTimestamp("dead_line").toLocalDateTime());
                }
                list.add(c);
            }
        } catch (SQLException e) {
            System.err.println("Search failed: " + e.getMessage());
        }
        return list;
    }

}
