package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LangueService {

    private Connection connection;

    public LangueService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public List<Langue> recupererToutesLanguesActives() throws SQLException {
        List<Langue> langues = new ArrayList<>();
        String query = "SELECT id, nom, drapeau FROM `langue` WHERE is_active = 1 ORDER BY nom";
        
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Langue langue = new Langue();
                langue.setId(resultSet.getInt("id"));
                langue.setNom(resultSet.getString("nom"));
                langue.setDrapeau(resultSet.getString("drapeau"));
                langues.add(langue);
            }
        }
        return langues;
    }

    public Langue recupererParId(int id) throws SQLException {
        String query = "SELECT id, nom, drapeau, description, popularite, date_ajout, is_active FROM `langue` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Langue langue = new Langue();
                    langue.setId(resultSet.getInt("id"));
                    langue.setNom(resultSet.getString("nom"));
                    langue.setDrapeau(resultSet.getString("drapeau"));
                    langue.setDescription(resultSet.getString("description"));
                    langue.setPopularite(resultSet.getString("popularite"));
                    langue.setDateAjout(resultSet.getTimestamp("date_ajout"));
                    // For tinyint in MySQL, getInt returns 0 or 1
                    langue.setActive(resultSet.getInt("is_active") == 1);
                    // update_at column doesn't exist, skip it
                    return langue;
                }
            }
        }
        return null;
    }
}
