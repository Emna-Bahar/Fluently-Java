package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NiveauService {

    private Connection connection;

    public NiveauService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    public List<Niveau> recupererNiveauxParLangue(int idLangue) throws SQLException {
        List<Niveau> niveaux = new ArrayList<>();
        String query = "SELECT id, titre, difficulte FROM `niveau` WHERE id_langue_id = ? ORDER BY ordre";
        
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idLangue);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Niveau niveau = new Niveau();
                    niveau.setId(resultSet.getInt("id"));
                    niveau.setTitre(resultSet.getString("titre"));
                    niveau.setDifficulte(resultSet.getString("difficulte"));
                    niveaux.add(niveau);
                }
            }
        }
        return niveaux;
    }

    public Niveau recupererParId(int id) throws SQLException {
        String query = "SELECT id, titre, description, difficulte, ordre, id_langue_id FROM `niveau` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Niveau niveau = new Niveau();
                    niveau.setId(resultSet.getInt("id"));
                    niveau.setTitre(resultSet.getString("titre"));
                    
                    // Handle potentially missing columns
                    try {
                        niveau.setDescription(resultSet.getString("description"));
                    } catch (SQLException e) {
                        niveau.setDescription("");
                    }
                    
                    niveau.setDifficulte(resultSet.getString("difficulte"));
                    niveau.setOrdre(resultSet.getInt("ordre"));
                    niveau.setIdLangueId(resultSet.getInt("id_langue_id"));
                    return niveau;
                }
            }
        }
        return null;
    }
}
