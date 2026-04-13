package com.pidev.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;

    private Connection connection;

    private final String USER = "root";
    private final String PASSWORD = "";
        private final String[] URLS = {
           // "jdbc:mysql://localhost:3306/gestion_cours",
            "jdbc:mysql://localhost:3306/pidev"
        };

    private DataSource() {
        openConnection();
    }

    public synchronized Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                openConnection();
            }
        } catch (SQLException e) {
            connection = null;
        }
        return connection;
    }

    private void openConnection() {
        connection = null;
        for (String url : URLS) {
            try {
                connection = DriverManager.getConnection(url, USER, PASSWORD);
                System.out.println("Connection established successfully: " + url);
                return;
            } catch (SQLException e) {
                System.err.println("Database connection failed for " + url + ": " + e.getMessage());
            }
        }
    }

    public static DataSource getInstance() {
        if (instance == null) {
            instance = new DataSource();
        }
        return instance;
    }
}
