package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GroupService implements IService<Groupe> {

    private Connection connection;

    public GroupService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Groupe groupe) throws SQLException {
        String query = "INSERT INTO `groupe` (nom, description, capacite, statut, date_creation, id_langue_id, id_niveau_id) VALUES (?, ?, ?, ?, NOW(), ?, ?)";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, groupe.getNom());
            statement.setString(2, groupe.getDescription());
            statement.setInt(3, groupe.getCapacite());
            statement.setString(4, groupe.getStatut());
            statement.setInt(5, groupe.getIdLangueId());
            statement.setInt(6, groupe.getIdNiveauId());
            statement.executeUpdate();
            System.out.println("✅ Groupe ajouté avec succès !");
        }
    }

    @Override
    public void modifier(Groupe groupe) throws SQLException {
        String query = "UPDATE `groupe` SET nom = ?, description = ?, capacite = ?, statut = ?, id_langue_id = ?, id_niveau_id = ? WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, groupe.getNom());
            statement.setString(2, groupe.getDescription());
            statement.setInt(3, groupe.getCapacite());
            statement.setString(4, groupe.getStatut());
            statement.setInt(5, groupe.getIdLangueId());
            statement.setInt(6, groupe.getIdNiveauId());
            statement.setInt(7, groupe.getId());
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Groupe modifié avec succès !");
            } else {
                System.out.println("⚠️ Aucun groupe trouvé avec cet ID.");
            }
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String deleteMessagesQuery = "DELETE FROM `message` WHERE id_groupe_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(deleteMessagesQuery)) {
            statement.setInt(1, id);
            statement.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Note: No messages to delete or table doesn't exist");
        }
        
        String query = "DELETE FROM `groupe` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            int rowsAffected = statement.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Groupe supprimé avec succès !");
            } else {
                System.out.println("⚠️ Aucun groupe trouvé avec cet ID.");
            }
        }
    }

    @Override
    public List<Groupe> recuperer() throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String query = "SELECT * FROM `groupe`";
        try (Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(query)) {
            while (resultSet.next()) {
                Groupe groupe = new Groupe();
                groupe.setId(resultSet.getInt("id"));
                groupe.setNom(resultSet.getString("nom"));
                groupe.setDescription(resultSet.getString("description"));
                groupe.setCapacite(resultSet.getInt("capacite"));
                groupe.setStatut(resultSet.getString("statut"));
                groupe.setDateCreation(resultSet.getTimestamp("date_creation"));
                groupe.setIdLangueId(resultSet.getInt("id_langue_id"));
                groupe.setIdNiveauId(resultSet.getInt("id_niveau_id"));
                groupes.add(groupe);
            }
        }
        return groupes;
    }

    public Groupe recupererParId(int id) throws SQLException {
        String query = "SELECT * FROM `groupe` WHERE id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    Groupe groupe = new Groupe();
                    groupe.setId(resultSet.getInt("id"));
                    groupe.setNom(resultSet.getString("nom"));
                    groupe.setDescription(resultSet.getString("description"));
                    groupe.setCapacite(resultSet.getInt("capacite"));
                    groupe.setStatut(resultSet.getString("statut"));
                    groupe.setDateCreation(resultSet.getTimestamp("date_creation"));
                    groupe.setIdLangueId(resultSet.getInt("id_langue_id"));
                    groupe.setIdNiveauId(resultSet.getInt("id_niveau_id"));
                    return groupe;
                }
            }
        }
        return null;
    }

    public List<Groupe> rechercherParNom(String nom) throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String query = "SELECT * FROM `groupe` WHERE nom LIKE ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, "%" + nom + "%");
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Groupe groupe = new Groupe();
                    groupe.setId(resultSet.getInt("id"));
                    groupe.setNom(resultSet.getString("nom"));
                    groupe.setDescription(resultSet.getString("description"));
                    groupe.setCapacite(resultSet.getInt("capacite"));
                    groupe.setStatut(resultSet.getString("statut"));
                    groupe.setDateCreation(resultSet.getTimestamp("date_creation"));
                    groupe.setIdLangueId(resultSet.getInt("id_langue_id"));
                    groupe.setIdNiveauId(resultSet.getInt("id_niveau_id"));
                    groupes.add(groupe);
                }
            }
        }
        return groupes;
    }

    public List<Groupe> recupererParStatut(String statut) throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String query = "SELECT * FROM `groupe` WHERE statut = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, statut);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Groupe groupe = new Groupe();
                    groupe.setId(resultSet.getInt("id"));
                    groupe.setNom(resultSet.getString("nom"));
                    groupe.setDescription(resultSet.getString("description"));
                    groupe.setCapacite(resultSet.getInt("capacite"));
                    groupe.setStatut(resultSet.getString("statut"));
                    groupe.setDateCreation(resultSet.getTimestamp("date_creation"));
                    groupe.setIdLangueId(resultSet.getInt("id_langue_id"));
                    groupe.setIdNiveauId(resultSet.getInt("id_niveau_id"));
                    groupes.add(groupe);
                }
            }
        }
        return groupes;
    }

    public List<Groupe> recupererParLangue(int idLangue) throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String query = "SELECT * FROM `groupe` WHERE id_langue_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idLangue);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Groupe groupe = new Groupe();
                    groupe.setId(resultSet.getInt("id"));
                    groupe.setNom(resultSet.getString("nom"));
                    groupe.setDescription(resultSet.getString("description"));
                    groupe.setCapacite(resultSet.getInt("capacite"));
                    groupe.setStatut(resultSet.getString("statut"));
                    groupe.setDateCreation(resultSet.getTimestamp("date_creation"));
                    groupe.setIdLangueId(resultSet.getInt("id_langue_id"));
                    groupe.setIdNiveauId(resultSet.getInt("id_niveau_id"));
                    groupes.add(groupe);
                }
            }
        }
        return groupes;
    }

    public List<Groupe> recupererParNiveau(int idNiveau) throws SQLException {
        List<Groupe> groupes = new ArrayList<>();
        String query = "SELECT * FROM `groupe` WHERE id_niveau_id = ?";
        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setInt(1, idNiveau);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    Groupe groupe = new Groupe();
                    groupe.setId(resultSet.getInt("id"));
                    groupe.setNom(resultSet.getString("nom"));
                    groupe.setDescription(resultSet.getString("description"));
                    groupe.setCapacite(resultSet.getInt("capacite"));
                    groupe.setStatut(resultSet.getString("statut"));
                    groupe.setDateCreation(resultSet.getTimestamp("date_creation"));
                    groupe.setIdLangueId(resultSet.getInt("id_langue_id"));
                    groupe.setIdNiveauId(resultSet.getInt("id_niveau_id"));
                    groupes.add(groupe);
                }
            }
        }
        return groupes;
    }

    public boolean existsByNom(String nom, Integer excludeId) throws SQLException {
        String query = "SELECT COUNT(*) FROM `groupe` WHERE LOWER(TRIM(nom)) = LOWER(TRIM(?))";
        if (excludeId != null) {
            query += " AND id <> ?";
        }

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            statement.setString(1, nom);
            if (excludeId != null) {
                statement.setInt(2, excludeId);
            }

            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() && resultSet.getInt(1) > 0;
            }
        }
    }
}
