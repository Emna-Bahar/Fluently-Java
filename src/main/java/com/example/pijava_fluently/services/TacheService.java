package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TacheService {

    private final Connection cnx = MyDatabase.getInstance().getConnection();

    // ═══════════════════════════════════════════════════════════════════
    //  CRUD OPÉRATIONS
    // ═══════════════════════════════════════════════════════════════════

    public void ajouter(Tache t) throws SQLException {
        String sql = "INSERT INTO tache (titre, description, date_limite, statut, priorite, id_objectif_id) VALUES (?,?,?,?,?,?)";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setDate(3, Date.valueOf(t.getDateLimite()));
        ps.setString(4, toSymfonyStatut(t.getStatut()));
        ps.setString(5, toSymfonyPriorite(t.getPriorite()));
        ps.setInt(6, t.getIdObjectifId());
        ps.executeUpdate();
    }

    public void modifier(Tache t) throws SQLException {
        String sql = "UPDATE tache SET titre=?, description=?, date_limite=?, statut=?, priorite=?, id_objectif_id=? WHERE id=?";
        PreparedStatement ps = cnx.prepareStatement(sql);
        ps.setString(1, t.getTitre());
        ps.setString(2, t.getDescription());
        ps.setDate(3, Date.valueOf(t.getDateLimite()));
        ps.setString(4, toSymfonyStatut(t.getStatut()));
        ps.setString(5, toSymfonyPriorite(t.getPriorite()));
        ps.setInt(6, t.getIdObjectifId());
        ps.setInt(7, t.getId());
        ps.executeUpdate();
    }

    public void supprimer(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("DELETE FROM tache WHERE id=?");
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public List<Tache> recuperer() throws SQLException {
        List<Tache> list = new ArrayList<>();
        ResultSet rs = cnx.createStatement().executeQuery("SELECT * FROM tache");
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    public List<Tache> recupererParObjectif(int idObjectif) throws SQLException {
        List<Tache> list = new ArrayList<>();
        PreparedStatement ps = cnx.prepareStatement("SELECT * FROM tache WHERE id_objectif_id = ?");
        ps.setInt(1, idObjectif);
        ResultSet rs = ps.executeQuery();
        while (rs.next()) {
            list.add(map(rs));
        }
        return list;
    }

    public Tache recupererParId(int id) throws SQLException {
        PreparedStatement ps = cnx.prepareStatement("SELECT * FROM tache WHERE id = ?");
        ps.setInt(1, id);
        ResultSet rs = ps.executeQuery();
        if (rs.next()) {
            return map(rs);
        }
        return null;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  MAPPING RésultatSet → Objet Tache (avec normalisation)
    // ═══════════════════════════════════════════════════════════════════

    private Tache map(ResultSet rs) throws SQLException {
        Tache t = new Tache();
        t.setId(rs.getInt("id"));
        t.setTitre(rs.getString("titre"));
        t.setDescription(rs.getString("description"));

        Date dateLimite = rs.getDate("date_limite");
        if (dateLimite != null) {
            t.setDateLimite(dateLimite.toLocalDate());
        }

        // ✅ Normalisation Symfony → JavaFX
        t.setStatut(fromSymfonyStatut(rs.getString("statut")));
        t.setPriorite(fromSymfonyPriorite(rs.getString("priorite")));
        t.setIdObjectifId(rs.getInt("id_objectif_id"));

        return t;
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONVERSION STATUTS : Symfony ↔ JavaFX
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Convertit un statut Symfony (base de données) vers le format JavaFX (affichage)
     *
     * @param symfonyStatut Valeur en base : "a_faire", "en_cours", "terminee", "annulee"
     * @return Valeur JavaFX : "À faire", "En cours", "Terminée", "Annulée"
     */
    private String fromSymfonyStatut(String symfonyStatut) {
        if (symfonyStatut == null) return "À faire";

        switch (symfonyStatut.toLowerCase()) {
            case "a_faire":
            case "afaire":
            case "à_faire":
                return "À faire";
            case "en_cours":
            case "encours":
                return "En cours";
            case "terminee":
            case "terminée":
                return "Terminée";
            case "annulee":
            case "annulée":
                return "Annulée";
            default:
                System.err.println("⚠️ Statut Symfony inconnu: " + symfonyStatut + " → valeur par défaut 'À faire'");
                return "À faire";
        }
    }

    /**
     * Convertit un statut JavaFX (affichage) vers le format Symfony (base de données)
     *
     * @param javafxStatut Valeur JavaFX : "À faire", "En cours", "Terminée", "Annulée"
     * @return Valeur Symfony : "a_faire", "en_cours", "terminee", "annulee"
     */
    private String toSymfonyStatut(String javafxStatut) {
        if (javafxStatut == null) return "a_faire";

        switch (javafxStatut) {
            case "À faire":
                return "a_faire";
            case "En cours":
                return "en_cours";
            case "Terminée":
                return "terminee";
            case "Annulée":
                return "annulee";
            default:
                System.err.println("⚠️ Statut JavaFX inconnu: " + javafxStatut + " → valeur par défaut 'a_faire'");
                return "a_faire";
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    //  CONVERSION PRIORITÉS : Symfony ↔ JavaFX
    // ═══════════════════════════════════════════════════════════════════

    /**
     * Convertit une priorité Symfony (base de données) vers le format JavaFX (affichage)
     *
     * @param symfonyPriorite Valeur en base : "basse", "normale", "haute", "urgente"
     * @return Valeur JavaFX : "Basse", "Normale", "Haute", "Urgente"
     */
    private String fromSymfonyPriorite(String symfonyPriorite) {
        if (symfonyPriorite == null) return "Normale";

        switch (symfonyPriorite.toLowerCase()) {
            case "basse":
                return "Basse";
            case "normale":
                return "Normale";
            case "haute":
                return "Haute";
            case "urgente":
                return "Urgente";
            default:
                System.err.println("⚠️ Priorité Symfony inconnue: " + symfonyPriorite + " → valeur par défaut 'Normale'");
                return "Normale";
        }
    }

    /**
     * Convertit une priorité JavaFX (affichage) vers le format Symfony (base de données)
     *
     * @param javafxPriorite Valeur JavaFX : "Basse", "Normale", "Haute", "Urgente"
     * @return Valeur Symfony : "basse", "normale", "haute", "urgente"
     */
    private String toSymfonyPriorite(String javafxPriorite) {
        if (javafxPriorite == null) return "normale";

        switch (javafxPriorite) {
            case "Basse":
                return "basse";
            case "Normale":
                return "normale";
            case "Haute":
                return "haute";
            case "Urgente":
                return "urgente";
            default:
                System.err.println("⚠️ Priorité JavaFX inconnue: " + javafxPriorite + " → valeur par défaut 'normale'");
                return "normale";
        }
    }
}