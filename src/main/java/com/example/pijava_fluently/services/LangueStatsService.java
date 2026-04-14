package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.Test;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.entites.User_progress;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// LangueStatsService.java - Service complet
public class LangueStatsService {

    private final LangueService langueService = new LangueService();
    private final TestPassageService testPassageService = new TestPassageService();
    private final UserProgressService userProgressService = new UserProgressService();

    // Récupère les statistiques complètes pour une langue
    public Map<String, Object> getStatsCompletes(int langueId, String nomLangue) {
        Map<String, Object> stats = new HashMap<>();

        // 1. Données Wikipedia (externes)
        Map<String, Object> wikiData = getWikipediaData(nomLangue);
        stats.putAll(wikiData);

        // 2. Données d'utilisation (internes)
        Map<String, Object> appData = getAppData(langueId);
        stats.putAll(appData);

        // 3. Tendance (comparaison avec autres langues)
        stats.put("tendance", getTendance(langueId));

        return stats;
    }
    private final TestService testService = new TestService();

    // Données d'utilisation de l'application - Version simplifiée
    private Map<String, Object> getAppData(int langueId) {
        Map<String, Object> data = new HashMap<>();

        try {
            // Nombre d'étudiants ayant étudié cette langue
            List<User_progress> progresses = userProgressService.recuperer();
            long nbEtudiants = progresses.stream()
                    .filter(p -> p.getLangueId() == langueId)
                    .map(User_progress::getUserId)
                    .distinct()
                    .count();
            data.put("nbEtudiants", nbEtudiants);

            // Taux de complétion moyen
            double tauxCompletion = progresses.stream()
                    .filter(p -> p.getLangueId() == langueId)
                    .mapToInt(p -> p.getDernierNumeroCours())
                    .average()
                    .orElse(0);
            data.put("tauxCompletion", Math.round(tauxCompletion * 100.0) / 100.0);

            // ⭐ Pour les tests, on utilise une approche sans getById
            // On récupère d'abord tous les tests pour avoir la correspondance ID -> langueId
            List<Test> tousLesTests = testService.recuperer();

            // Créer une map des IDs de tests par langue
            List<Integer> testIdsPourLangue = new ArrayList<>();
            for (Test test : tousLesTests) {
                if (test.getLangueId() == langueId) {
                    testIdsPourLangue.add(test.getId());
                }
            }

            // Maintenant filtrer les passages
            List<TestPassage> passages = testPassageService.recuperer();
            long nbTests = passages.stream()
                    .filter(p -> testIdsPourLangue.contains(p.getTestId()))
                    .count();
            data.put("nbTests", nbTests);

            // Score moyen
            double scoreMoyen = passages.stream()
                    .filter(p -> testIdsPourLangue.contains(p.getTestId()))
                    .filter(p -> p.getScoreMax() > 0)
                    .mapToDouble(p -> (double) p.getScore() / p.getScoreMax() * 100)
                    .average()
                    .orElse(0);
            data.put("scoreMoyen", Math.round(scoreMoyen));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return data;
    }

    // Données Wikipedia
    private Map<String, Object> getWikipediaData(String nomLangue) {
        Map<String, Object> data = new HashMap<>();

        // Données locales (fallback si API non accessible)
        switch (nomLangue.toLowerCase()) {
            case "anglais" -> {
                data.put("locuteurs", "1,456 milliards");
                data.put("famille", "Indo-européenne > Germanique");
                data.put("ecriture", "Latin");
                data.put("difficulte", 2);
                data.put("description", "L'anglais est une langue germanique occidentale originaire d'Angleterre.");
                data.put("pays", "Royaume-Uni, États-Unis, Canada, Australie, Inde...");
                data.put("funfact", "L'anglais est la langue officielle de 59 pays !");
            }
            case "français" -> {
                data.put("locuteurs", "321 millions");
                data.put("famille", "Indo-européenne > Roman");
                data.put("ecriture", "Latin");
                data.put("difficulte", 1);
                data.put("description", "Le français est une langue romane parlée en France, Belgique, Suisse, Canada...");
                data.put("pays", "France, Belgique, Suisse, Canada, Sénégal...");
                data.put("funfact", "Le français était la langue diplomatique par excellence au XVIIIe siècle !");
            }
            case "espagnol" -> {
                data.put("locuteurs", "543 millions");
                data.put("famille", "Indo-européenne > Roman");
                data.put("ecriture", "Latin");
                data.put("difficulte", 2);
                data.put("description", "L'espagnol est une langue romane parlée en Espagne et en Amérique latine.");
                data.put("pays", "Espagne, Mexique, Argentine, Colombie, Pérou...");
                data.put("funfact", "L'espagnol est la 2ème langue maternelle la plus parlée au monde !");
            }
            case "allemand" -> {
                data.put("locuteurs", "135 millions");
                data.put("famille", "Indo-européenne > Germanique");
                data.put("ecriture", "Latin");
                data.put("difficulte", 3);
                data.put("description", "L'allemand est une langue germanique parlée en Allemagne, Autriche, Suisse.");
                data.put("pays", "Allemagne, Autriche, Suisse, Luxembourg, Belgique...");
                data.put("funfact", "L'allemand possède des mots composés très longs comme 'Donaudampfschifffahrtsgesellschaftskapitän' !");
            }
            case "arabe" -> {
                data.put("locuteurs", "422 millions");
                data.put("famille", "Afro-asiatique > Sémitique");
                data.put("ecriture", "Arabe");
                data.put("difficulte", 5);
                data.put("description", "L'arabe est une langue sémitique parlée dans le monde arabe.");
                data.put("pays", "Égypte, Arabie Saoudite, Maroc, Algérie, Tunisie...");
                data.put("funfact", "L'arabe s'écrit de droite à gauche et a 28 lettres !");
            }
            default -> {
                data.put("locuteurs", "Donnée non disponible");
                data.put("famille", "Non classifiée");
                data.put("ecriture", "Variable");
                data.put("difficulte", 3);
                data.put("description", "Informations non disponibles pour cette langue.");
                data.put("pays", "Non spécifié");
                data.put("funfact", "Découvrez cette langue par vous-même !");
            }
        }
        return data;
    }

    // Tendance de popularité dans l'app
    private String getTendance(int langueId) {
        try {
            List<User_progress> progresses = userProgressService.recuperer();
            long nbTotal = progresses.stream()
                    .filter(p -> p.getLangueId() == langueId)
                    .count();

            if (nbTotal > 10) return "📈 Très populaire";
            if (nbTotal > 5) return "📈 Populaire";
            if (nbTotal > 2) return "📊 Moyenne";
            return "📉 Peu étudiée";
        } catch (SQLException e) {
            return "📊 Donnée non disponible";
        }
    }

    // Récupère le classement des langues les plus étudiées
    public List<Map<String, Object>> getClassementLangues() {
        List<Map<String, Object>> classement = new ArrayList<>();

        try {
            List<Langue> langues = langueService.recuperer();
            List<User_progress> progresses = userProgressService.recuperer();

            for (Langue langue : langues) {
                Map<String, Object> stat = new HashMap<>();
                stat.put("id", langue.getId());
                stat.put("nom", langue.getNom());
                stat.put("drapeau", getDrapeauEmoji(langue.getNom()));

                long nbEtudiants = progresses.stream()
                        .filter(p -> p.getLangueId() == langue.getId())
                        .map(User_progress::getUserId)
                        .distinct()
                        .count();
                stat.put("nbEtudiants", nbEtudiants);

                double tauxCompletion = progresses.stream()
                        .filter(p -> p.getLangueId() == langue.getId())
                        .mapToInt(p -> p.getDernierNumeroCours())
                        .average()
                        .orElse(0);
                stat.put("tauxCompletion", Math.round(tauxCompletion * 100.0) / 100.0);

                classement.add(stat);
            }

            // Trier par nombre d'étudiants (décroissant)
            classement.sort((a, b) ->
                    Long.compare((long)b.get("nbEtudiants"), (long)a.get("nbEtudiants")));

        } catch (SQLException e) {
            e.printStackTrace();
        }
        return classement;
    }

    private String getDrapeauEmoji(String nom) {
        return switch (nom.toLowerCase()) {
            case "anglais" -> "🇬🇧";
            case "français" -> "🇫🇷";
            case "espagnol" -> "🇪🇸";
            case "allemand" -> "🇩🇪";
            case "arabe" -> "🇸🇦";
            case "italien" -> "🇮🇹";
            case "chinois" -> "🇨🇳";
            default -> "🌍";
        };
    }
}
