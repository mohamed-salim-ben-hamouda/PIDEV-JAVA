package com.pidev.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;
    private Connection connection;

    private final String USER = EnvConfig.get("DB_USER", "root");
    private final String PASSWORD = EnvConfig.get("DB_PASSWORD", "");
    private final String URL = EnvConfig.get("DB_URL", "jdbc:mysql://localhost:3306/pidev");

    private DataSource() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver"); // IMPORTANT

            connection = DriverManager.getConnection(URL, USER, PASSWORD);

            if (connection != null && !connection.isClosed()) {
                System.out.println("✅ Database connection SUCCESS");
            }

        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL Driver not found");
        } catch (SQLException e) {
            System.err.println("❌ DB CONNECTION FAILED: " + e.getMessage());
            connection = null;
        }
    }

    public Connection getConnection() {
        return connection;
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }
}