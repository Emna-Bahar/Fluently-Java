package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Reservation;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Reservationservice {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ── CRUD ───────────────────────────────────────────────────────

    public void ajouter(Reservation r) throws SQLException {
        String sql = "INSERT INTO reservation (date_reservation, statut, id_session_id, id_user_id) " +
                "VALUES (?, ?, ?, ?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(r.getDateReservation()));
        ps.setString(2, r.getStatut());
        ps.setInt(3, r.getIdSessionId());
        ps.setInt(4, r.getIdUserId());
        ps.executeUpdate();
        System.out.println("Réservation ajoutée avec succès !");
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM reservation WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Réservation supprimée avec succès !");
    }

    /** Professeur : accepte une réservation en attente */
    public void accepter(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE reservation SET statut='acceptée', date_confirmation=NOW() WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Réservation acceptée !");
    }

    /** Professeur : refuse une réservation en attente */
    public void refuser(int id, String commentaire) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE reservation SET statut='refusée', commentaire=?, date_confirmation=NOW() WHERE id=?");
        ps.setString(1, commentaire);
        ps.setInt(2, id);
        ps.executeUpdate();
        System.out.println("Réservation refusée !");
    }

    /** Étudiant : annule sa propre réservation */
    public void annuler(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "UPDATE reservation SET statut='annulée' WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
        System.out.println("Réservation annulée !");
    }

    /** Toutes les réservations */
    public List<Reservation> recuperer() throws SQLException {
        List<Reservation> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery(
                "SELECT * FROM reservation ORDER BY date_reservation DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /** Réservations pour une session donnée (vue professeur) */
    public List<Reservation> recupererParSession(int idSession) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM reservation WHERE id_session_id=? ORDER BY date_reservation DESC");
        ps.setInt(1, idSession);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /** Réservations d'un étudiant donné */
    public List<Reservation> recupererParEtudiant(int idUser) throws SQLException {
        List<Reservation> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM reservation WHERE id_user_id=? ORDER BY date_reservation DESC");
        ps.setInt(1, idUser);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    /** Vérifie si un étudiant a déjà réservé une session */
    public boolean dejaReserve(int idUser, int idSession) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM reservation WHERE id_user_id=? AND id_session_id=? " +
                        "AND statut NOT IN ('refusée','annulée')");
        ps.setInt(1, idUser);
        ps.setInt(2, idSession);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1) > 0;
        return false;
    }

    // ── Mapping ResultSet → Reservation ───────────────────────────
    private Reservation map(ResultSet rs) throws SQLException {
        Reservation r = new Reservation();
        r.setId(rs.getInt("id"));
        r.setDateReservation(rs.getDate("date_reservation").toLocalDate());
        r.setStatut(rs.getString("statut"));
        r.setIdSessionId(rs.getInt("id_session_id"));
        r.setIdUserId(rs.getInt("id_user_id"));
        byte presence = rs.getByte("presence");
        r.setPresence(rs.wasNull() ? null : presence == 1);
        r.setCommentaire(rs.getString("commentaire"));
        Timestamp ts = rs.getTimestamp("date_confirmation");
        r.setDateConfirmation(ts != null ? ts.toLocalDateTime() : null);
        return r;
    }
}