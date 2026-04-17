package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Langue;
import com.example.pijava_fluently.entites.TestPassage;
import com.example.pijava_fluently.entites.User;
import com.example.pijava_fluently.utils.ConfigLoader;
import com.example.pijava_fluently.utils.LoggerUtil;

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

public class PerformanceAnalyzerService {

    private final TestPassageService  testPassageService;
    private final AITextCorrectionService aiService;

    private static final double EXCELLENT = ConfigLoader.getDouble("performance.excellent", 90);
    private static final double VERY_GOOD = ConfigLoader.getDouble("performance.very_good", 80);
    private static final double GOOD      = ConfigLoader.getDouble("performance.good",      70);
    private static final double FAIR      = ConfigLoader.getDouble("performance.fair",      60);
    private static final double AVERAGE   = ConfigLoader.getDouble("performance.average",   50);

    public PerformanceAnalyzerService() {
        this.testPassageService = new TestPassageService();
        this.aiService          = new AITextCorrectionService();
    }

    // ─────────────────────────────────────────────────────────────
    //  1. Analyse principale
    // ─────────────────────────────────────────────────────────────
    public Map<String, Object> analyzeUserPerformance(User user, Langue langue)
            throws SQLException {

        List<TestPassage> passages =
                testPassageService.recupererParUserEtLangue(user.getId(), langue.getId());

        // Garder uniquement les passages terminés
        passages = passages.stream()
                .filter(p -> "termine".equals(p.getStatut()))
                .collect(Collectors.toList());

        LoggerUtil.info("Passages found",
                "userId",   String.valueOf(user.getId()),
                "langue",   langue.getNom(),
                "count",    String.valueOf(passages.size()));

        if (passages.isEmpty()) {
            return emptyResult();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("competences",   calculateCompetences(passages));
        result.put("stats_globales", calculateGlobalStats(passages));
        result.put("progression",   calculateProgression(passages));
        result.put("has_data",      true);
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  2. Compétences (basées sur resultat qui est déjà en %)
    // ─────────────────────────────────────────────────────────────
    private Map<String, Object> calculateCompetences(List<TestPassage> passages) {
        // resultat est déjà un pourcentage (0.0 - 100.0)
        List<Double> scores = passages.stream()
                .map(TestPassage::getResultat)
                .collect(Collectors.toList());

        double moyenne = scores.stream()
                .mapToDouble(Double::doubleValue)
                .average().orElse(0);

        // On distribue le même score global sur les 4 compétences
        // (affinement possible si on analyse question par question)
        Map<String, Object> result = new LinkedHashMap<>();
        for (String comp : new String[]{"grammaire", "vocabulaire", "comprehension", "oral"}) {
            Map<String, Object> d = new HashMap<>();
            d.put("score",  round1(moyenne));
            d.put("count",  passages.size());
            d.put("niveau", determineNiveau(moyenne));
            result.put(comp, d);
        }

        LoggerUtil.info("Competences calculated", "moyenne", String.valueOf(moyenne));
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  3. Stats globales
    // ─────────────────────────────────────────────────────────────
    private Map<String, Object> calculateGlobalStats(List<TestPassage> passages) {
        List<Double> scores = passages.stream()
                .map(TestPassage::getResultat)
                .collect(Collectors.toList());

        double avg  = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double best = scores.stream().mapToDouble(Double::doubleValue).max().orElse(0);
        double last = scores.get(0).doubleValue(); // trié DESC → premier = plus récent
        long   temps = passages.stream()
                .mapToLong(TestPassage::getTempsPasse).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("tests_passes",       passages.size());
        stats.put("score_moyen",        round1(avg));
        stats.put("meilleur_score",     round1(best));
        stats.put("dernier_score",      round1(last));
        stats.put("temps_total",        temps);
        stats.put("temps_total_heures", round1(temps / 3600.0));
        stats.put("progression",        computeTrend(scores));

        LoggerUtil.info("Stats calculated",
                "avg",  String.valueOf(avg),
                "best", String.valueOf(best),
                "last", String.valueOf(last));
        return stats;
    }

    // ─────────────────────────────────────────────────────────────
    //  4. Progression dans le temps
    // ─────────────────────────────────────────────────────────────
    private List<Map<String, Object>> calculateProgression(List<TestPassage> passages) {
        // Les passages arrivent en DESC (plus récent en premier), on inverse pour le graphique
        List<TestPassage> chrono = new ArrayList<>(passages);
        Collections.reverse(chrono);

        List<Map<String, Object>> prog = new ArrayList<>();
        for (TestPassage p : chrono) {
            Map<String, Object> point = new HashMap<>();
            String date = p.getDateDebut() != null
                    ? p.getDateDebut().toLocalDate().toString() : "?";
            point.put("date",  date);
            point.put("score", round1(p.getResultat()));
            point.put("test",  "Test #" + p.getTestId());
            prog.add(point);
        }
        return prog;
    }

    // ─────────────────────────────────────────────────────────────
    //  5. Recommandations IA
    // ─────────────────────────────────────────────────────────────
    public Map<String, Object> generateAIRecommendations(User user, Langue langue,
                                                         Map<String, Object> analysis) {
        @SuppressWarnings("unchecked")
        Map<String, Object> competences =
                (Map<String, Object>) analysis.get("competences");
        @SuppressWarnings("unchecked")
        Map<String, Object> stats =
                (Map<String, Object>) analysis.get("stats_globales");

        String prompt = buildRecommendationPrompt(user, langue, competences, stats);

        try {
            Map<String, Object> result = aiService.generateRecommendations(prompt);
            return parseAIRecommendations(result);
        } catch (Exception e) {
            LoggerUtil.error("Error generating AI recommendations", e);
            return generateDefaultRecommendations(competences);
        }
    }

    private String buildRecommendationPrompt(User user, Langue langue,
                                             Map<String, Object> competences,
                                             Map<String, Object> stats) {
        StringBuilder comps = new StringBuilder();
        for (Map.Entry<String, Object> e : competences.entrySet()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> d = (Map<String, Object>) e.getValue();
            comps.append(e.getKey()).append(": ")
                    .append(d.get("score")).append("% (")
                    .append(d.get("niveau")).append(")\n");
        }

        // Prompt très strict avec exemple concret
        return "Genere 3 recommandations pedagogiques en JSON STRICTEMENT VALIDE.\n\n" +
                "Contexte etudiant:\n" +
                "- Prenom: " + user.getPrenom() + "\n" +
                "- Langue: " + langue.getNom() + "\n" +
                "- Tests passes: " + stats.get("tests_passes") + "\n" +
                "- Score moyen: " + stats.get("score_moyen") + "%\n" +
                "- Competences: " + comps.toString().replace("\n", ", ") + "\n\n" +
                "REPONDS EXACTEMENT AVEC CE FORMAT JSON (guillemets obligatoires partout):\n" +
                "{\n" +
                "  \"recommandations\": [\n" +
                "    {\n" +
                "      \"titre\": \"Ameliorer la grammaire\",\n" +
                "      \"description\": \"Revoir les bases grammaticales\",\n" +
                "      \"actions\": [\"Faire des exercices chaque jour\", \"Relire les corrections\"],\n" +
                "      \"priorite\": \"haute\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"titre\": \"Enrichir le vocabulaire\",\n" +
                "      \"description\": \"Apprendre 5 mots nouveaux par jour\",\n" +
                "      \"actions\": [\"Utiliser des flashcards\", \"Lire des textes en " +
                langue.getNom() + "\"],\n" +
                "      \"priorite\": \"moyenne\"\n" +
                "    },\n" +
                "    {\n" +
                "      \"titre\": \"Pratiquer a l oral\",\n" +
                "      \"description\": \"S exprimer regulierement pour gagner en fluidite\",\n" +
                "      \"actions\": [\"Repasser les tests oraux\", \"Ecouter des podcasts\"],\n" +
                "      \"priorite\": \"basse\"\n" +
                "    }\n" +
                "  ],\n" +
                "  \"message_encouragement\": \"Bravo " + user.getPrenom() +
                ", continuez ainsi!\"\n" +
                "}\n\n" +
                "ADAPTE le contenu au profil ci-dessus. " +
                "RESPECTE le format JSON avec guillemets doubles partout. " +
                "Commence par { directement.";
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseAIRecommendations(Map<String, Object> aiResult) {
        if (aiResult.containsKey("recommandations")) {
            return aiResult;
        }
        // Fallback si la clé n'existe pas
        return generateDefaultRecommendations(new HashMap<>());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> generateDefaultRecommendations(
            Map<String, Object> competences) {

        List<Map<String, Object>> recs = new ArrayList<>();

        Map<String, Object> rec1 = new HashMap<>();
        rec1.put("titre",       "Pratiquez régulièrement");
        rec1.put("description", "La régularité est la clé du progrès en langues.");
        rec1.put("actions",     List.of("10 min par jour", "Repasser les tests ratés"));
        rec1.put("priorite",    "haute");
        recs.add(rec1);

        Map<String, Object> rec2 = new HashMap<>();
        rec2.put("titre",       "Révisez vos erreurs");
        rec2.put("description", "Analysez vos réponses incorrectes après chaque test.");
        rec2.put("actions",     List.of("Relire les corrections", "Noter les mots difficiles"));
        rec2.put("priorite",    "moyenne");
        recs.add(rec2);

        Map<String, Object> result = new HashMap<>();
        result.put("recommandations",        recs);
        result.put("message_encouragement",  "Continue comme ça ! 💪");
        return result;
    }

    // ─────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────
    private String determineNiveau(double score) {
        if (score >= EXCELLENT) return "Excellent";
        if (score >= VERY_GOOD) return "Très bien";
        if (score >= GOOD)      return "Bien";
        if (score >= FAIR)      return "Assez bien";
        if (score >= AVERAGE)   return "Moyen";
        return "À améliorer";
    }

    private String computeTrend(List<Double> scores) {
        if (scores.size() < 2) return "stable";
        // scores est en DESC → on inverse pour avoir l'ordre chronologique
        List<Double> chrono = new ArrayList<>(scores);
        Collections.reverse(chrono);

        int half   = (int) Math.ceil(chrono.size() / 2.0);
        double avg1 = chrono.subList(0, half).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);
        double avg2 = chrono.subList(half, chrono.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(0);

        if (avg2 > avg1 + 5)  return "progression";
        if (avg2 < avg1 - 5)  return "regression";
        return "stable";
    }

    private double round1(double v) {
        return Math.round(v * 10) / 10.0;
    }

    private Map<String, Object> emptyResult() {
        Map<String, Object> r = new HashMap<>();
        r.put("competences",    new HashMap<>());
        r.put("stats_globales", emptyStats());
        r.put("progression",    new ArrayList<>());
        r.put("has_data",       false);
        return r;
    }

    private Map<String, Object> emptyStats() {
        Map<String, Object> s = new HashMap<>();
        s.put("tests_passes",       0);
        s.put("score_moyen",        0.0);
        s.put("meilleur_score",     0.0);
        s.put("dernier_score",      0.0);
        s.put("temps_total",        0L);
        s.put("temps_total_heures", 0.0);
        s.put("progression",        "stable");
        return s;
    }
}