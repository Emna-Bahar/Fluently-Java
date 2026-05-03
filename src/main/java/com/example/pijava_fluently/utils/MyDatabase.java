package com.example.pijava_fluently.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDatabase {

    private static final String URL = "jdbc:mysql://10.206.162.141:3306/fluently";
    private static final String USER = "fluently_user";
    private static final String PASSWORD = "MotDePasseSecurise123!";
    private Connection connection;
    private static MyDatabase instance;


    private MyDatabase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connexion réussie !");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null)
            instance = new MyDatabase();
        return instance;
    }

    public Connection getConnection() {
        return connection;
    }
    // Dans MyDatabase.java — ajoute cette méthode
    public void reconnect() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
            // Même URL que dans ton constructeur
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("[MyDatabase] Reconnexion réussie.");
        } catch (SQLException e) {
            System.err.println("[MyDatabase] Erreur reconnexion : " + e.getMessage());
        }
    }
}