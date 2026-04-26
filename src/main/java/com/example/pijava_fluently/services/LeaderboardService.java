package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.*;

public class LeaderboardService {

    public record EntreeLeaderboard(
            String prenom,
            String nom,
            int    duelsJoues,
            int    duelsGagnes,
            int    totalPoints,
            double tauxVictoire
    ) {}

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    /**
     * Retourne le classement basé sur test_passage avec statut 'duel_gagne'
     * On réutilise test_passage avec un statut spécial.
     */
    public List<EntreeLeaderboard> getLeaderboard() throws SQLException {
        String sql = """
            SELECT u.prenom, u.nom,
                   COUNT(*) as duels_joues,
                   SUM(CASE WHEN tp.statut = 'duel_gagne' THEN 1 ELSE 0 END) as duels_gagnes,
                   SUM(tp.score) as total_points
            FROM test_passage tp
            JOIN user u ON tp.user_id = u.id
            WHERE tp.statut IN ('duel_gagne', 'duel_perdu', 'duel_egalite')
            GROUP BY u.id, u.prenom, u.nom
            ORDER BY duels_gagnes DESC, total_points DESC
            LIMIT 20
            """;

        List<EntreeLeaderboard> result = new ArrayList<>();
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                int joues  = rs.getInt("duels_joues");
                int gagnes = rs.getInt("duels_gagnes");
                result.add(new EntreeLeaderboard(
                        rs.getString("prenom"),
                        rs.getString("nom"),
                        joues, gagnes,
                        rs.getInt("total_points"),
                        joues > 0 ? (double) gagnes / joues * 100 : 0
                ));
            }
        }
        return result;
    }

    /** Sauvegarde le résultat d'un duel */
    public void sauvegarderResultatDuel(int userId, int testId,
                                        int score, int scoreMax,
                                        boolean gagne, boolean egalite)
            throws SQLException {
        String statut = egalite ? "duel_egalite"
                : gagne  ? "duel_gagne"
                : "duel_perdu";
        double pct = scoreMax > 0 ? (double) score / scoreMax * 100 : 0;

        String sql = """
            INSERT INTO test_passage
              (date_debut, date_fin, resultat, score, score_max,
               statut, temps_passe, test_id, user_id)
            VALUES (NOW(), NOW(), ?, ?, ?, ?, 0, ?, ?)
            """;
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setDouble(1, pct);
            ps.setInt(2, score);
            ps.setInt(3, scoreMax);
            ps.setString(4, statut);
            ps.setInt(5, testId);
            ps.setInt(6, userId);
            ps.executeUpdate();
        }
    }
}