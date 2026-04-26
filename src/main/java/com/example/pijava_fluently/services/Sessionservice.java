package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Session;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class Sessionservice {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ══════════════════════════════════════════════════════════════
    // CRUD DE BASE
    // ══════════════════════════════════════════════════════════════

    public void ajouter(Session s) throws SQLException {
        String sql = "INSERT INTO session (date_heure, statut, lien_reunion, id_group_id, id_user_id, " +
                "duree, prix, description, capacite_max, nom) VALUES (?,?,?,?,?,?,?,?,?,?)";
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
        ps.setString(10, s.getNom());
        ps.executeUpdate();
    }

    public void modifier(Session s) throws SQLException {
        String sql = "UPDATE session SET date_heure=?, statut=?, lien_reunion=?, id_group_id=?, " +
                "id_user_id=?, duree=?, prix=?, description=?, capacite_max=?, nom=? WHERE id=?";
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
        ps.setString(10, s.getNom());
        ps.setInt(11, s.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement psResa = cnx.prepareStatement("DELETE FROM reservation WHERE id_session_id=?");
        psResa.setInt(1, id);
        psResa.executeUpdate();
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM session WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Session> recuperer() throws SQLException {
        List<Session> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement()
                .executeQuery("SELECT * FROM session ORDER BY date_heure DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Session> recupererDisponibles() throws SQLException {
        List<Session> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM session WHERE statut IN ('planifiée','en cours') ORDER BY date_heure ASC");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Session> recupererToutesEtudiant() throws SQLException {
        List<Session> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM session WHERE statut IN ('planifiée','en cours','terminée') " +
                        "ORDER BY date_heure DESC");
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Session> recupererToutesEtudiantSansResas(int userId) throws SQLException {
        List<Session> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT * FROM session " +
                        "WHERE statut IN ('planifiée','en cours','terminée') " +
                        "AND id NOT IN (" +
                        "   SELECT id_session_id FROM reservation " +
                        "   WHERE id_user_id = ? AND statut NOT IN ('refusée','annulée')" +
                        ") ORDER BY date_heure DESC"
        );
        ps.setInt(1, userId);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public List<Session> recupererParProfesseur(int idUser) throws SQLException {
        List<Session> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement()
                .executeQuery("SELECT * FROM session ORDER BY date_heure DESC");
        while (rs.next()) list.add(map(rs));
        return list;
    }

    public Session recupererParId(int id) throws SQLException {
        try (PreparedStatement ps = cnx.prepareStatement("SELECT * FROM session WHERE id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return map(rs);
        }
        return null;
    }

    public int compterReservationsAcceptees(int idSession) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement(
                "SELECT COUNT(*) FROM reservation WHERE id_session_id=? AND statut='acceptée'");
        ps.setInt(1, idSession);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) return rs.getInt(1);
        return 0;
    }

    // ══════════════════════════════════════════════════════════════
    // SYSTÈME DE RATING INTELLIGENT
    // ══════════════════════════════════════════════════════════════

    public void enregistrerRatingAvance(int sessionId, int noteEtudiant) throws SQLException {
        Session s = recupererParId(sessionId);
        if (s == null) throw new SQLException("Session introuvable : " + sessionId);

        int nbAcceptees = compterReservationsAcceptees(sessionId);
        double scorePondere = calculerScorePondere(noteEtudiant, nbAcceptees, s.getCapaciteMax());

        int scoreAStocker = (int) Math.round(scorePondere);
        scoreAStocker = Math.max(1, Math.min(5, scoreAStocker));

        try (PreparedStatement ps = cnx.prepareStatement(
                "UPDATE session SET rating = ? WHERE id = ?")) {
            ps.setInt(1, scoreAStocker);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
    }

    public double calculerScorePondere(int note, int nbAcceptees, Integer capaciteMax) {
        if (capaciteMax == null || capaciteMax == 0) return note;
        double taux = Math.min(1.0, (double) nbAcceptees / capaciteMax);
        return note * (0.7 + 0.3 * taux);
    }

    public String[] getClassificationQualite(double scorePondere) {
        if (scorePondere >= 4.5) return new String[]{"Excellente",  "⭐", "#ECFDF5", "#059669"};
        if (scorePondere >= 3.5) return new String[]{"Bonne",       "👍", "#EFF6FF", "#3B82F6"};
        if (scorePondere >= 2.5) return new String[]{"Moyenne",     "😐", "#FFFBEB", "#D97706"};
        return                          new String[]{"À améliorer", "⚠️", "#FFF1F2", "#E11D48"};
    }

    public Object[] verifierAnomalieNote(int groupId, int noteDonnee) throws SQLException {
        String sql = "SELECT rating FROM session " +
                "WHERE id_group_id = ? AND statut = 'terminée' AND rating IS NOT NULL";
        List<Double> notes = new ArrayList<>();
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) notes.add(rs.getDouble(1));
        }

        if (notes.size() < 3) return new Object[]{false, "", 0.0};

        double somme = 0;
        for (double n : notes) somme += n;
        double moyenne = somme / notes.size();

        double varianceSum = 0;
        for (double n : notes) varianceSum += Math.pow(n - moyenne, 2);
        double ecartType = Math.sqrt(varianceSum / notes.size());

        if (ecartType < 0.01) return new Object[]{false, "", 0.0};

        double zScore = Math.abs(noteDonnee - moyenne) / ecartType;

        if (zScore > 2.0) {
            String msg = String.format(
                    "Attention : ta note (%d★) est très différente\n" +
                            "de la moyenne du groupe (%.1f★).\nEs-tu sûr de ta note ?",
                    noteDonnee, moyenne);
            return new Object[]{true, msg, zScore};
        }

        return new Object[]{false, "", zScore};
    }

    public double[] getReputationGroupe(int groupId) throws SQLException {
        String sql = "SELECT s.id, s.rating, s.capacite_max, " +
                "(SELECT COUNT(*) FROM reservation r " +
                " WHERE r.id_session_id = s.id AND r.statut = 'acceptée') AS nb_acc " +
                "FROM session s WHERE s.id_group_id = ? AND s.statut = 'terminée'";

        List<Double> scoresPonderes = new ArrayList<>();
        int nbTerminees = 0;
        int nbNotees = 0;

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                nbTerminees++;
                int rating = rs.getInt("rating");
                boolean wasNull = rs.wasNull();
                if (!wasNull && rating > 0) {
                    nbNotees++;
                    int nbAcc = rs.getInt("nb_acc");
                    int capMax = rs.getInt("capacite_max");
                    double score = calculerScorePondere(rating, nbAcc, capMax == 0 ? null : capMax);
                    scoresPonderes.add(score);
                }
            }
        }

        if (nbTerminees == 0) return new double[]{0, 0, 0, 0, 0};

        double taux = (double) nbNotees / nbTerminees;
        double scoreMoyen = scoresPonderes.isEmpty() ? 0 :
                scoresPonderes.stream().mapToDouble(d -> d).average().orElse(0);
        double reputation = scoreMoyen * (0.6 + 0.4 * taux);

        return new double[]{reputation, nbTerminees, nbNotees, taux, scoreMoyen};
    }

    public Object[] getStatistiquesSession(int sessionId) throws SQLException {
        Session s = recupererParId(sessionId);
        if (s == null) return new Object[]{
                0.0,
                new String[]{"Non notée", "☆", "#F8FAFC", "#94A3B8"},
                new double[]{0, 0, 0, 0, 0},
                "☆☆☆☆☆"
        };

        int nbAcc = compterReservationsAcceptees(sessionId);
        double scorePondere = 0;
        String[] classification;
        String etoiles;

        if (s.getRating() != null && s.getRating() > 0) {
            scorePondere = calculerScorePondere(s.getRating(), nbAcc, s.getCapaciteMax());
            classification = getClassificationQualite(scorePondere);
            int nbPlein = (int) Math.round(scorePondere);
            int nbVide = 5 - Math.max(0, nbPlein);
            etoiles = "★".repeat(Math.max(0, Math.min(5, nbPlein))) +
                    "☆".repeat(Math.max(0, Math.min(5, nbVide)));
        } else {
            classification = new String[]{"Non notée", "☆", "#F8FAFC", "#94A3B8"};
            etoiles = "☆☆☆☆☆";
        }

        double[] reputation = getReputationGroupe(s.getIdGroupId());
        return new Object[]{scorePondere, classification, reputation, etoiles};
    }

    public double getAverageRating(int groupId) throws SQLException {
        String sql = "SELECT AVG(rating) FROM session WHERE id_group_id = ? AND rating IS NOT NULL";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, groupId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        }
        return 0.0;
    }

    public void updateRating(int sessionId, int rating) throws SQLException {
        enregistrerRatingAvance(sessionId, rating);
    }

    // ══════════════════════════════════════════════════════════════
    // MAPPING ET NOUVELLES MÉTHODES POUR JITSI
    // ══════════════════════════════════════════════════════════════

    private Session map(ResultSet rs) throws SQLException {
        Session s = new Session();
        s.setId(rs.getInt("id"));
        Timestamp ts = rs.getTimestamp("date_heure");
        if (ts != null) s.setDateHeure(ts.toLocalDateTime());
        s.setStatut(rs.getString("statut"));
        s.setLienReunion(rs.getString("lien_reunion"));
        s.setIdGroupId(rs.getInt("id_group_id"));
        s.setIdUserId(rs.getInt("id_user_id"));
        int rating = rs.getInt("rating"); s.setRating(rs.wasNull() ? null : rating);
        int duree = rs.getInt("duree");  s.setDuree(rs.wasNull() ? null : duree);
        double prix = rs.getDouble("prix"); s.setPrix(rs.wasNull() ? null : prix);
        s.setDescription(rs.getString("description"));
        int cap = rs.getInt("capacite_max"); s.setCapaciteMax(rs.wasNull() ? null : cap);
        s.setNom(rs.getString("nom"));
        return s;
    }

    public int getLastInsertId() throws SQLException {
        String sql = "SELECT LAST_INSERT_ID()";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        }
        return -1;
    }

    public void mettreAJourLienReunion(int sessionId, String lien) throws SQLException {
        String sql = "UPDATE session SET lien_reunion = ? WHERE id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, lien);
            ps.setInt(2, sessionId);
            ps.executeUpdate();
        }
    }
}