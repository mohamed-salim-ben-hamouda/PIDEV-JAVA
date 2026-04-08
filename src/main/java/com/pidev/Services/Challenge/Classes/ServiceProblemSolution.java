package com.pidev.Services.Challenge.Classes;

import com.pidev.Services.Challenge.Interfaces.IProblemSolution;
import com.pidev.models.ProblemSolution;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class ServiceProblemSolution implements IProblemSolution {
    Connection connection;
    public ServiceProblemSolution(){
        this.connection= DataSource.getInstance().getConnection();
    }
    @Override
    public int addProblem(ProblemSolution p, int a) {
        String query = "INSERT INTO problem_solution (problem_description, activity_id_id) VALUES (?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(query, java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, p.getProblemDescription());
            ps.setInt(2, a);
            ps.executeUpdate();
            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    int generatedId = rs.getInt(1);
                    p.setId(generatedId);
                    return generatedId;
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error adding problem: " + e.getMessage(), e);
        }
        return -1;
    }
    @Override
    public void addSolutionGrp(ProblemSolution p){
        String query="UPDATE  problem_solution SET group_solution=? where id=?";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setString(1,p.getGroupSolution());
            ps.setInt(2,p.getId());
            ps.executeUpdate();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public List<ProblemSolution> displayProblems(int act){
        List<ProblemSolution> p=new ArrayList<>();
        String query="SELECT * FROM problem_solution WHERE activity_id_id=? AND group_solution IS NULL ";
        try (PreparedStatement ps=connection.prepareStatement(query)){
            ps.setInt(1,act);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ProblemSolution prob = new ProblemSolution();
                prob.setId(rs.getInt("id"));
                prob.setProblemDescription(rs.getString("problem_description"));
                p.add(prob);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }
    @Override
    public List<ProblemSolution> display(int activity_id){
        List<ProblemSolution> p = new ArrayList<>();
        String query = "SELECT * FROM problem_solution WHERE activity_id_id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)){
            ps.setInt(1,activity_id);
            ResultSet rs = ps.executeQuery();
            while(rs.next()){
                ProblemSolution prob = new ProblemSolution();
                prob.setId(rs.getInt("id"));
                prob.setProblemDescription(rs.getString("problem_description"));
                prob.setGroupSolution(rs.getString("group_solution"));
                p.add(prob);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return p;
    }

    @Override
    public void update(ProblemSolution p) {
        String query = "UPDATE problem_solution SET problem_description=?, group_solution=? WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setString(1, p.getProblemDescription());
            ps.setString(2, p.getGroupSolution());
            ps.setInt(3, p.getId());
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error updating problem/solution: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(int id) {
        String query = "DELETE FROM problem_solution WHERE id=?";
        try (PreparedStatement ps = connection.prepareStatement(query)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (Exception e) {
            throw new RuntimeException("Error deleting problem/solution: " + e.getMessage(), e);
        }
    }
}
