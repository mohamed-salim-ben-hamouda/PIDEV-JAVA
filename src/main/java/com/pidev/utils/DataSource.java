package com.pidev.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DataSource {

    private static DataSource instance;

    private Connection connection;

    private final String USER = EnvConfig.get("DB_USER", "root");
    private final String PASSWORD = EnvConfig.get("DB_PASSWORD", "");
    private final String URL = EnvConfig.get("DB_URL", "jdbc:mysql://localhost:3306/skill_bridge");

    private DataSource() {
        try {
            //etablisement de connection
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connection established successfully.");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
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
