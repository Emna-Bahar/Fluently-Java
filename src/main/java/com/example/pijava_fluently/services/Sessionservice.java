package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Sessionservice {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ── CRUD ───────────────────────────────────────────────────────

    public void ajouter(Session s) throws SQLException {
        String sql = "INSERT INTO session (date_heure, statut, lien_reunion, id_group_id, id_user_id, " +
                "duree, prix, description, capacite_max) VALUES (?,?,?,?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, s.getDateHeure() != null ? Timestamp.valueOf(s.getDateHeure()) : null);
        ps.setString(2, s.getStatut());
        ps.setString(3, s.getLienReunion());
        ps.setInt(4, s.getIdGroupId());
        ps.setInt(5, s.getIdUserId());
        ps.setObject(6, s.getDuree());
        ps.setObject(7, s.getPrix());
        ps.setString(8, s.getDescription());
        ps.setObject(9, s.getCapaciteMax());
        ps.executeUpdate();
        System.out.println("Session ajoutée avec succès !");
    }

    public void modifier(Session s) throws SQLException {
        String sql = "UPDATE session SET date_heure=?, statut=?, lien_reunion=?, id_group_id=?, " +
                "id_user_id=?, duree=?, prix=?, description=?, capacite_max=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setTimestamp(1, s.getDateHeure() != null ? Timestamp.valueOf(s.getDateHeure()) : null);
        ps.setString(2, s.getStatut());
        ps.setString(3, s.getLienReunion());
        ps.setInt(4, s.getIdGroupId());
        ps.setInt(5, s.getIdUserId());
        ps.setObject(6, s.getDuree());
        ps.setObject(7, s.getPrix());
        ps.setString(8, s.getDescription());
        ps.setObject(9, s.getCapaciteMax());
        ps.setInt(10, s.getId());
        ps.executeUpdate();
        System.out.println("Session modifiée avec succès !");
    }

    public void supprimer(int id) throws SQLException {
        // Supprimer d'abord les réservations associées (contrainte FK)
        PreparedStatement psResa = cnx.prepareStatement("DELETE FROM reservation WHERE id_session_id=?");
        psResa.setInt(1, id);
        psResa.executeUpdate();
        // Puis supprimer la session
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM session WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Session supprimée avec succès !");
    }

    /** Toutes les sessions */
    public List<Session> recuperer() throws SQLException {
        List<Session> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM session ORDER BY date_heure DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }
    public List<Session> recupererDisponibles() throws SQLException {
        List<Session> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM session " +
                        "WHERE statut IN ('planifiée', 'en cours') " +
                        "AND date_heure > NOW() " +
                        "ORDER BY date_heure ASC"
        );
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }
    public List<Session> recupererParProfesseur(int idUser) throws SQLException {
        List<Session> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement()
                .executeQuery("SELECT * FROM session ORDER BY date_heure DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }
    /**
     * Sessions disponibles pour un étudiant :
     * - Statut : planifiée ou en cours
     * - Date dans le futur (strictement après maintenant)
     */


    /** Compte le nombre de réservations acceptées pour une session */
    public int compterReservationsAcceptees(int idSession) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM reservation WHERE id_session_id=? AND statut='acceptée'");
        ps.setInt(1, idSession);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    // ── Mapping ResultSet → Session ────────────────────────────────
    private Session map(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("date_heure");
        if (ts != null) s.setDateHeure(ts.toLocalDateTime());
        s.setStatut(rs.getString("statut"));
        s.setLienReunion(rs.getString("lien_reunion"));
        s.setIdGroupId(rs.getInt("id_group_id"));
        s.setIdUserId(rs.getInt("id_user_id"));
        int rating = rs.getInt("rating");
        s.setRating(rs.wasNull() ? null : rating);
        int duree = rs.getInt("duree");
        s.setDuree(rs.wasNull() ? null : duree);
        double prix = rs.getDouble("prix");
        s.setPrix(rs.wasNull() ? null : prix);
        s.setDescription(rs.getString("description"));
        int cap = rs.getInt("capacite_max");
        s.setCapaciteMax(rs.wasNull() ? null : cap);
        return s;
    }


}