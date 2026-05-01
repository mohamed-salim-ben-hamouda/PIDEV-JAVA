package com.pidev.Services;

import com.pidev.models.*;
import com.pidev.utils.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class AdminLookupService {
    private final Connection connection;

    public AdminLookupService() {
        this.connection = DataSource.getInstance().getConnection();
    }

    public List<Course> findAllCourses() throws SQLException {
        String sql = "SELECT id, title FROM course ORDER BY title ASC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Course> result = new ArrayList<>();
            while (rs.next()) {
                Course course = new Course(rs.getInt("id"));
                course.setTitle(rs.getString("title"));
                result.add(course);
            }
            return result;
        }
    }

    public List<Chapter> findAllChapters() throws SQLException {
        String sql = "SELECT id, title FROM chapter ORDER BY chapter_order ASC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Chapter> result = new ArrayList<>();
            while (rs.next()) {
                Chapter chapter = new Chapter(rs.getInt("id"));
                chapter.setTitle(rs.getString("title"));
                result.add(chapter);
            }
            return result;
        }
    }

    public List<Quiz> findAllQuizzes() throws SQLException {
        String sql = "SELECT id, title FROM quiz ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Quiz> result = new ArrayList<>();
            while (rs.next()) {
                Quiz quiz = new Quiz(rs.getInt("id"));
                quiz.setTitle(rs.getString("title"));
                result.add(quiz);
            }
            return result;
        }
    }

    public List<Question> findAllQuestions() throws SQLException {
        String sql = "SELECT id, content FROM question ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<Question> result = new ArrayList<>();
            while (rs.next()) {
                Question question = new Question(rs.getInt("id"));
                question.setContent(rs.getString("content"));
                result.add(question);
            }
            return result;
        }
    }

    public List<User> findAllUsers() throws SQLException {
        String sql = "SELECT id, email, nom, prenom FROM user ORDER BY id DESC";
        try (PreparedStatement statement = requireConnection().prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            List<User> result = new ArrayList<>();
            while (rs.next()) {
                User user = new User(rs.getInt("id"));
                user.setEmail(rs.getString("email"));
                user.setNom(rs.getString("nom"));
                user.setPrenom(rs.getString("prenom"));
                result.add(user);
            }
            return result;
        }
    }

    private Connection requireConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Database connection is not available. Check DataSource URL/user/password and MySQL server.");
        }
        return connection;
    }
}
