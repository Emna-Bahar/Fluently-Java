package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class LangueService implements IService<Langue> {

    private Connection cnx = MyDatabase.getInstance().getConnection();

    @Override
    public void ajouter(Langue langue) throws SQLException {
        String sql = "INSERT INTO langue (nom, drapeau, updated_at, description, popularite, date_ajout, is_active) VALUES (?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, langue.getNom());
        ps.setString(2, langue.getDrapeau());
        ps.setTimestamp(3, Timestamp.valueOf(langue.getUpdatedAt()));
        ps.setString(4, langue.getDescription());
        ps.setString(5, langue.getPopularite());
        ps.setDate(6, Date.valueOf(langue.getDateAjout()));
        ps.setBoolean(7, langue.isActive());
        ps.executeUpdate();
        System.out.println("Langue ajoutée avec succès !");
    }

    @Override
    public void modifier(Langue langue) throws SQLException {
        String sql = "UPDATE langue SET nom=?, drapeau=?, updated_at=?, description=?, popularite=?, date_ajout=?, is_active=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, langue.getNom());
        ps.setString(2, langue.getDrapeau());
        ps.setTimestamp(3, Timestamp.valueOf(langue.getUpdatedAt()));
        ps.setString(4, langue.getDescription());
        ps.setString(5, langue.getPopularite());
        ps.setDate(6, Date.valueOf(langue.getDateAjout()));
        ps.setBoolean(7, langue.isActive());
        ps.setInt(8, langue.getId());
        ps.executeUpdate();
        System.out.println("Langue modifiée avec succès !");
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM langue WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Langue supprimée avec succès !");
    }

    @Override
    public List<Langue> recuperer() throws SQLException {
        List<Langue> langues = new ArrayList<>();
        String sql = "SELECT * FROM langue";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Langue l = new Langue();
            l.setId(rs.getInt("id"));
            l.setNom(rs.getString("nom"));
            l.setDrapeau(rs.getString("drapeau"));
            Timestamp updatedAt = rs.getTimestamp("updated_at");
            l.setUpdatedAt(updatedAt != null ? updatedAt.toLocalDateTime() : null);
            l.setDescription(rs.getString("description"));
            l.setPopularite(rs.getString("popularite"));
            Date dateAjout = rs.getDate("date_ajout");
            l.setDateAjout(dateAjout != null ? dateAjout.toLocalDate() : null);
            l.setActive(rs.getBoolean("is_active"));
            langues.add(l);
        }
        return langues;
    }
    // ── Methods used by GroupesController / GroupFormController ──────────────

    /** Returns active langues (used by group views). */
    public List<Langue> recupererToutesLanguesActives() throws SQLException {
        List<Langue> langues = new ArrayList<>();
        String sql = "SELECT id, nom FROM langue WHERE is_active = 1";
        Statement st = cnx.createStatement();
        ResultSet rs = st.executeQuery(sql);
        while (rs.next()) {
            Langue l = new Langue();
            l.setId(rs.getInt("id"));
            l.setNom(rs.getString("nom"));
            l.setActive(true);
            langues.add(l);
        }
        return langues;
    }

    /** Returns a single langue by id (used by group views). */
    public Langue recupererParId(int id) throws SQLException {
        String sql = "SELECT id, nom FROM langue WHERE id = ?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            Langue l = new Langue();
            l.setId(rs.getInt("id"));
            l.setNom(rs.getString("nom"));
            return l;
        }
        return null;
    }

    // Vérifier si une langue existe déjà avec ce nom
    public boolean existsByNom(String nom, Integer excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM langue WHERE LOWER(nom) = LOWER(?)";
        if (excludeId != null) {
            sql += " AND id != ?";
        }
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, nom);
        if (excludeId != null) {
            ps.setInt(2, excludeId);
        }
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return rs.getInt(1) > 0;
        }
        return false;
    }
}