package com.example.pijava_fluently;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL = "jdbc:mysql://localhost:3306/fluently";
    private static final String USER = "root";
    private static final String PASSWORD = ""; // ton mot de passe MySQL

    private static Connection connection = null;

    public static Connection getConnection() {
        if (connection == null) {
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion réussie à la base de données !");
            } catch (SQLException e) {
                System.err.println("❌ Erreur de connexion : " + e.getMessage());
            }
        }
        return connection;
    }
}
