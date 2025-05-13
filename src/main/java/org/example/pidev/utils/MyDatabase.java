package org.example.pidev.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private final String URL = "jdbc:mysql://localhost:3306/autolinkdb";

    private final String USER = "root";
    private final String PSW = "";

    private Connection myConnection;

    private static MyDatabase instance;

    private MyDatabase() {
        try {
            // Load the MySQL JDBC driver
            Class.forName("com.mysql.cj.jdbc.Driver");
            myConnection = DriverManager.getConnection(URL, USER, PSW);
            System.out.println("Database connection established successfully");
        } catch (ClassNotFoundException e) {
            System.out.println("MySQL JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.out.println("Database connection failed: " + e.getMessage());
        }
    }

    public Connection getMyConnection() {
        try {
            // Check if connection is closed or null
            if (myConnection == null || myConnection.isClosed()) {
                System.out.println("Reconnecting to database...");
                myConnection = DriverManager.getConnection(URL, USER, PSW);
            }
            return myConnection;
        } catch (SQLException e) {
            System.out.println("Error getting database connection: " + e.getMessage());
            return null;
        }
    }

    public Connection getConnection() {
        return myConnection;
    }


    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }
}
