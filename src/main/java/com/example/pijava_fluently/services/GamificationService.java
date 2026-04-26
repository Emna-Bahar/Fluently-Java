package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class GamificationService {

    // ─── Modèle interne ───────────────────────────────────────────────────────

    public record Niveau(String label, String icone, String couleur, int prochaineEtape) {}

    public record Badge(String icone, String nom, String description, String couleur, boolean obtenu) {}

    public record GamificationResult(
            int totalPoints,
            int pointsTaches,
            int pointsObjectifs,
            int nbTachesTerminees,
            int nbObjectifsCompletes,
            Niveau niveau,
            int progression,          // 0-100 (% vers prochain niveau)
            List<Badge> tousBadges,
            List<Badge> badgesObtenus
    ) {}

    // ─── Calcul principal ────────────────────────────────────────────────────

    /**
     * Calcule la gamification pour un utilisateur donné.
     *
     * @param userId  identifiant de l'utilisateur
     * @param objectifService  service objectifs
     * @param tacheService     service tâches
     */
    public GamificationResult calculer(int userId,
                                       ObjectifService objectifService,
                                       TacheService tacheService) {
        int nbTachesTerminees = 0;
        int nbObjectifsCompletes = 0;

        try {
            // Objectifs de l'utilisateur
            List<Objectif> objectifs = objectifService.recuperer().stream()
                    .filter(o -> o.getIdUserId() == userId)
                    .collect(Collectors.toList());

            // Compter objectifs complétés ("Terminé" en français)
            nbObjectifsCompletes = (int) objectifs.stream()
                    .filter(o -> "Terminé".equals(o.getStatut()))
                    .count();

            // Tâches terminées à travers tous les objectifs de l'utilisateur
            for (Objectif o : objectifs) {
                List<Tache> taches = tacheService.recupererParObjectif(o.getId());
                nbTachesTerminees += (int) taches.stream()
                        .filter(t -> "Terminée".equals(t.getStatut()))
                        .count();
            }
        } catch (SQLException ignored) {}

        int pointsTaches    = nbTachesTerminees    * 10;
        int pointsObjectifs = nbObjectifsCompletes * 50;
        int totalPoints     = pointsTaches + pointsObjectifs;

        // ── Niveau ──────────────────────────────────────────────────────────
        Niveau niveau;
        if      (totalPoints >= 1000) niveau = new Niveau("Légende",       "🦁", "#8e44ad", 1000);
        else if (totalPoints >= 500)  niveau = new Niveau("Expert",        "🌟", "#f39c12", 1000);
        else if (totalPoints >= 100)  niveau = new Niveau("Intermédiaire", "🚀", "#3498db", 500);
        else if (totalPoints >= 10)   niveau = new Niveau("Débutant",      "🌱", "#2ecc71", 100);
        else                          niveau = new Niveau("Novice",        "🐣", "#95a5a6", 10);

        int progression = (int) Math.min(100,
                Math.round((totalPoints / (double) niveau.prochaineEtape()) * 100));

        // ── Badges ──────────────────────────────────────────────────────────
        final int nbT = nbTachesTerminees;
        final int nbO = nbObjectifsCompletes;

        List<Badge> tousBadges = List.of(
                new Badge("🎯", "Premier pas",     "1 tâche complétée",     "#2ecc71", nbT >= 1),
                new Badge("⚡", "En route",         "5 tâches complétées",   "#3498db", nbT >= 5),
                new Badge("🔥", "Productif",        "10 tâches complétées",  "#e67e22", nbT >= 10),
                new Badge("🤖", "Machine",          "25 tâches complétées",  "#9b59b6", nbT >= 25),
                new Badge("🏅", "Objectif atteint", "1 objectif complété",   "#f1c40f", nbO >= 1),
                new Badge("🏆", "Ambitieux",        "3 objectifs complétés", "#e74c3c", nbO >= 3),
                new Badge("👑", "Champion",         "5 objectifs complétés", "#f39c12", nbO >= 5)
        );

        List<Badge> badgesObtenus = tousBadges.stream()
                .filter(Badge::obtenu)
                .collect(Collectors.toList());

        return new GamificationResult(
                totalPoints, pointsTaches, pointsObjectifs,
                nbTachesTerminees, nbObjectifsCompletes,
                niveau, progression,
                tousBadges, badgesObtenus
        );
    }
}