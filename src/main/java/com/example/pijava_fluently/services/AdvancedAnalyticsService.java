package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.*;
import com.example.pijava_fluently.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

public class AdvancedAnalyticsService {

    private final TestPassageService passageService;
    private final QuestionService questionService;
    private final AITextCorrectionService aiService;

    public AdvancedAnalyticsService() {
        this.passageService = new TestPassageService();
        this.questionService = new QuestionService();
        this.aiService = new AITextCorrectionService();
    }

    // ═══════════════════════════════════════════════════════════════════
    // 1. PRÉDICTION DE SCORE (basée sur l'historique + tendance)
    // ═══════════════════════════════════════════════════════════════════
    public PredictionResult predireScore(int userId, int langueId) {
        try {
            List<TestPassage> passages = passageService.recupererParUserEtLangue(userId, langueId);
            if (passages.size() < 2) return null;

            // Extraire les scores et dates
            List<Double> scores = passages.stream()
                    .map(TestPassage::getResultat)
                    .collect(Collectors.toList());

            // Calculer la moyenne
            double moyenne = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);

            // Calculer la tendance (régression linéaire simple)
            double pente = calculerPente(passages);
            String tendance = pente > 5 ? "progression" : (pente < -5 ? "régression" : "stable");

            // Prédiction = moyenne + (pente * 0.5)
            double prediction = moyenne + (pente * 0.5);
            prediction = Math.min(100, Math.max(0, prediction));

            // Intervalle de confiance
            double ecartType = calculerEcartType(scores);
            double marge = ecartType * 0.8;

            return new PredictionResult(prediction, marge, tendance, scores.size());

        } catch (Exception e) {
            LoggerUtil.error("Erreur prédiction score", e);
            return null;
        }
    }

    private double calculerPente(List<TestPassage> passages) {
        if (passages.size() < 2) return 0;
        int n = passages.size();
        double sumX = 0, sumY = 0, sumXY = 0, sumX2 = 0;
        for (int i = 0; i < n; i++) {
            double x = i;
            double y = passages.get(i).getResultat();
            sumX += x;
            sumY += y;
            sumXY += x * y;
            sumX2 += x * x;
        }
        double denom = n * sumX2 - sumX * sumX;
        if (denom == 0) return 0;
        return (n * sumXY - sumX * sumY) / denom;
    }

    private double calculerEcartType(List<Double> valeurs) {
        double moyenne = valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double sommeCarres = valeurs.stream().mapToDouble(v -> Math.pow(v - moyenne, 2)).sum();
        return Math.sqrt(sommeCarres / valeurs.size());
    }

    // ═══════════════════════════════════════════════════════════════════
    // 2. ANALYSE DE FRAUDE POTENTIELLE (basée sur comportement)
    // ═══════════════════════════════════════════════════════════════════
    public FraudAnalysis analyserRisqueFraude(int userId) {
        try {
            List<TestPassage> passages = passageService.recuperer();
            passages.removeIf(p -> p.getUserId() != userId);

            // Utiliser un tableau pour pouvoir modifier la valeur dans la lambda
            final int[] scoreRisque = {0};  // ← Tableau pour mutabilité
            List<String> facteurs = new ArrayList<>();

            // Facteur 1: Reprises fréquentes du même test (> 3 fois)
            Map<Integer, Long> testCount = passages.stream()
                    .collect(Collectors.groupingBy(TestPassage::getTestId, Collectors.counting()));
            for (Map.Entry<Integer, Long> entry : testCount.entrySet()) {
                if (entry.getValue() > 3) {
                    scoreRisque[0] += 25;
                    facteurs.add("Reprises fréquentes du test #" + entry.getKey() + " (" + entry.getValue() + " fois)");
                }
            }

            // Facteur 2: Score anormalement élevé (écart > 30% par rapport à la moyenne)
            if (!passages.isEmpty()) {
                double moyenneScores = passages.stream().mapToDouble(TestPassage::getResultat).average().orElse(0);
                final double moyenneFinale = moyenneScores;  // Variable finale pour la lambda

                passages.stream()
                        .filter(p -> p.getResultat() > moyenneFinale + 30)
                        .findFirst()
                        .ifPresent(p -> {
                            scoreRisque[0] += 30;
                            facteurs.add("Score anormalement élevé (" + p.getResultat() + "% vs moyenne " + String.format("%.1f", moyenneFinale) + "%)");
                        });
            }

            // Facteur 3: Analyse des temps de réponse (optionnel)
            // ...

            int scoreFinal = Math.min(100, scoreRisque[0]);
            String niveau = scoreFinal >= 70 ? "ÉLEVÉ" : (scoreFinal >= 40 ? "MOYEN" : "FAIBLE");
            String couleur = scoreFinal >= 70 ? "🔴" : (scoreFinal >= 40 ? "🟡" : "🟢");

            return new FraudAnalysis(scoreFinal, niveau, couleur, facteurs);

        } catch (Exception e) {
            LoggerUtil.error("Erreur analyse fraude", e);
            return new FraudAnalysis(0, "FAIBLE", "🟢", new ArrayList<>());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 3. COURBE D'APPRENTISSAGE (données déjà disponibles)
    // ═══════════════════════════════════════════════════════════════════
    public List<PointProgression> getProgression(int userId, int langueId) {
        try {
            List<TestPassage> passages = passageService.recupererParUserEtLangue(userId, langueId);
            List<PointProgression> points = new ArrayList<>();
            for (TestPassage p : passages) {
                points.add(new PointProgression(
                        p.getDateDebut().toLocalDate().toString(),
                        p.getResultat()
                ));
            }
            return points;
        } catch (Exception e) {
            LoggerUtil.error("Erreur progression", e);
            return new ArrayList<>();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 4. HEATMAP DES COMPÉTENCES (basée sur les types de questions)
    // ═══════════════════════════════════════════════════════════════════
    public CompetencesHeatmap getCompetencesHeatmap(int userId, int langueId) {
        try {
            List<TestPassage> passages = passageService.recupererParUserEtLangue(userId, langueId);
            if (passages.isEmpty()) return new CompetencesHeatmap();

            // Simulation basée sur les types de tests
            // En réalité, on analyserait chaque question individuellement
            List<Double> scoresGrammaire = new ArrayList<>();
            List<Double> scoresVocabulaire = new ArrayList<>();
            List<Double> scoresComprehension = new ArrayList<>();
            List<Double> scoresOral = new ArrayList<>();

            for (TestPassage p : passages) {
                // Simuler une répartition (à améliorer avec les vrais scores par question)
                scoresGrammaire.add(p.getResultat());
                scoresVocabulaire.add(p.getResultat());
                scoresComprehension.add(p.getResultat());
                // Pour l'oral, on pourrait avoir des données séparées
                scoresOral.add(p.getResultat() * 0.7); // Simulation
            }

            return new CompetencesHeatmap(
                    moyenne(scoresGrammaire),
                    moyenne(scoresVocabulaire),
                    moyenne(scoresComprehension),
                    moyenne(scoresOral)
            );

        } catch (Exception e) {
            LoggerUtil.error("Erreur heatmap", e);
            return new CompetencesHeatmap();
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 5. MEILLEUR CRÉNEAU HORAIRE
    // ═══════════════════════════════════════════════════════════════════
    public HoraireAnalysis getMeilleurCreneau(int userId) {
        try {
            List<TestPassage> passages = passageService.recuperer();
            passages.removeIf(p -> p.getUserId() != userId);

            Map<String, List<Double>> scoresParCreneau = new HashMap<>();
            scoresParCreneau.put("Matin (6h-12h)", new ArrayList<>());
            scoresParCreneau.put("Après-midi (12h-18h)", new ArrayList<>());
            scoresParCreneau.put("Soirée (18h-23h)", new ArrayList<>());
            scoresParCreneau.put("Nuit (23h-6h)", new ArrayList<>());

            for (TestPassage p : passages) {
                LocalDateTime date = p.getDateDebut();
                if (date == null) continue;
                int heure = date.getHour();
                String creneau;
                if (heure >= 6 && heure < 12) creneau = "Matin (6h-12h)";
                else if (heure >= 12 && heure < 18) creneau = "Après-midi (12h-18h)";
                else if (heure >= 18 && heure < 23) creneau = "Soirée (18h-23h)";
                else creneau = "Nuit (23h-6h)";

                scoresParCreneau.get(creneau).add(p.getResultat());
            }

            String meilleur = "";
            double meilleurScore = 0;
            for (Map.Entry<String, List<Double>> entry : scoresParCreneau.entrySet()) {
                double moyenne = entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0);
                if (moyenne > meilleurScore && !entry.getValue().isEmpty()) {
                    meilleurScore = moyenne;
                    meilleur = entry.getKey();
                }
            }

            return new HoraireAnalysis(meilleur, meilleurScore, scoresParCreneau);

        } catch (Exception e) {
            LoggerUtil.error("Erreur analyse horaire", e);
            return new HoraireAnalysis("", 0, new HashMap<>());
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 6. SCORE DE CONFIANCE
    // ═══════════════════════════════════════════════════════════════════
    public ConfianceAnalysis getScoreConfiance(int userId) {
        try {
            List<TestPassage> passages = passageService.recuperer();
            passages.removeIf(p -> p.getUserId() != userId);

            if (passages.isEmpty()) return new ConfianceAnalysis(50, "Non évalué");

            // Facteurs de confiance:
            // - Temps de réponse (plus rapide = plus confiant)
            // - Score élevé
            // - Progression constante

            double tempsMoyen = passages.stream().mapToInt(TestPassage::getTempsPasse).average().orElse(0);
            double scoreMoyen = passages.stream().mapToDouble(TestPassage::getResultat).average().orElse(0);

            // Plus le temps est court par rapport à la moyenne (15 min = 900 sec), plus l'étudiant est confiant
            double tempsConfiance = Math.max(0, 100 - (tempsMoyen / 900.0) * 100);
            double scoreConfiance = scoreMoyen;

            double confiance = (tempsConfiance * 0.3 + scoreConfiance * 0.7);
            confiance = Math.min(100, Math.max(0, confiance));

            String niveauConfiance;
            if (confiance >= 70) niveauConfiance = "Très confiant";
            else if (confiance >= 50) niveauConfiance = "Modérément confiant";
            else if (confiance >= 30) niveauConfiance = "Peu confiant";
            else niveauConfiance = "Manque de confiance";

            return new ConfianceAnalysis(confiance, niveauConfiance);

        } catch (Exception e) {
            LoggerUtil.error("Erreur analyse confiance", e);
            return new ConfianceAnalysis(50, "Non évalué");
        }
    }

    // ═══════════════════════════════════════════════════════════════════
    // 7. ANALYSE IA AVEC GROQ (le plus important pour WOW)
    // ═══════════════════════════════════════════════════════════════════
    public String genererAnalyseIA(int userId, String prenom, String langue) {
        try {
            // Récupérer toutes les données
            List<TestPassage> passages = passageService.recuperer();
            passages.removeIf(p -> p.getUserId() != userId);

            if (passages.isEmpty()) {
                return "Passez quelques tests pour recevoir une analyse personnalisée !";
            }

            // Calculer les métriques
            double scoreMoyen = passages.stream().mapToDouble(TestPassage::getResultat).average().orElse(0);
            double progression = calculerPente(passages);
            FraudAnalysis fraude = analyserRisqueFraude(userId);
            HoraireAnalysis horaire = getMeilleurCreneau(userId);
            CompetencesHeatmap competences = getCompetencesHeatmap(userId, 1);

            // Construire le prompt pour Groq
            String prompt = construirePromptIA(prenom, langue, scoreMoyen, progression, fraude, horaire, competences);

            // Appeler Groq - génère une Map avec clé "recommandations" contenant une List
            Map<String, Object> response = aiService.generateRecommendations(prompt);

            LoggerUtil.info("Raw AI response", "content", response.toString());

            // La réponse est de la forme: { "recommandations": [ { "titre": "...", ... } ] }
            // ou directement avec "analyse"

            // Vérifier si la réponse contient directement une clé "analyse"
            if (response.containsKey("analyse")) {
                Object analyseObj = response.get("analyse");
                if (analyseObj instanceof String) {
                    return (String) analyseObj;
                }
            }

            // Sinon, chercher dans la première recommandation
            if (response.containsKey("recommandations")) {
                Object recsObj = response.get("recommandations");
                if (recsObj instanceof List) {
                    List<?> recs = (List<?>) recsObj;
                    if (!recs.isEmpty()) {
                        Object firstRec = recs.get(0);
                        if (firstRec instanceof Map) {
                            Map<?, ?> firstRecMap = (Map<?, ?>) firstRec;
                            if (firstRecMap.containsKey("description")) {
                                return (String) firstRecMap.get("description");
                            }
                            if (firstRecMap.containsKey("titre")) {
                                return (String) firstRecMap.get("titre");
                            }
                        }
                    }
                }
            }

            // Fallback: essayer de parser le JSON manuellement depuis la clé "content" si présente
            if (response.containsKey("content")) {
                String content = (String) response.get("content");
                String cleaned = cleanAndExtractAnalysis(content);
                if (!cleaned.isEmpty()) {
                    return cleaned;
                }
            }

            return genererAnalyseFallback(prenom, scoreMoyen, progression, horaire.meilleurCreneau);

        } catch (Exception e) {
            LoggerUtil.error("Erreur analyse IA", e);
            return genererAnalyseFallback(prenom, 0, 0, "");
        }
    }

    /**
     * Extrait l'analyse d'un contenu JSON brut
     */
    private String cleanAndExtractAnalysis(String content) {
        if (content == null || content.isEmpty()) return "";

        try {
            // Chercher la clé "analyse" dans le JSON
            int idxAnalyse = content.indexOf("\"analyse\"");
            if (idxAnalyse != -1) {
                int colonPos = content.indexOf(':', idxAnalyse);
                if (colonPos != -1) {
                    int quoteStart = content.indexOf('"', colonPos + 1);
                    if (quoteStart != -1) {
                        int quoteEnd = content.indexOf('"', quoteStart + 1);
                        if (quoteEnd != -1) {
                            String analyse = content.substring(quoteStart + 1, quoteEnd);
                            // Nettoyer les caractères d'échappement
                            analyse = analyse.replace("\\\"", "\"")
                                    .replace("\\n", "\n")
                                    .replace("\\'", "'");
                            if (!analyse.isEmpty()) {
                                return analyse;
                            }
                        }
                    }
                }
            }

            // Fallback: enlever les balises JSON et retourner le texte
            String cleaned = content.replaceAll("[{}\"\\[\\]]", "")
                    .replace("analyse:", "")
                    .trim();
            if (!cleaned.isEmpty()) {
                return cleaned;
            }

        } catch (Exception e) {
            LoggerUtil.warning("Erreur extraction analyse", e.getMessage());
        }

        return "";
    }

    private String construirePromptIA(String prenom, String langue, double scoreMoyen,
                                      double progression, FraudAnalysis fraude,
                                      HoraireAnalysis horaire, CompetencesHeatmap competences) {
        String progressionTexte = progression > 5 ? "en progression" : (progression < -5 ? "en régression" : "stable");

        // Compétence la plus faible
        double minComp = Math.min(competences.grammaire, Math.min(competences.vocabulaire,
                Math.min(competences.comprehension, competences.oral)));
        String faibleCompetence = "";
        if (minComp == competences.grammaire) faibleCompetence = "la grammaire";
        else if (minComp == competences.vocabulaire) faibleCompetence = "le vocabulaire";
        else if (minComp == competences.comprehension) faibleCompetence = "la compréhension";
        else if (minComp == competences.oral) faibleCompetence = "l'expression orale";

        return String.format("""
        Tu es un coach pédagogique expert en apprentissage des langues.
        
        Voici les données de l'étudiant %s qui apprend le %s :
        
        - Score moyen : %.1f%%
        - Tendance : %s
        - Risque de fraude : %s (%s)
        - Meilleur moment pour réviser : %s (score moyen %.0f%%)
        
        Compétences :
        - Grammaire : %.0f%%
        - Vocabulaire : %.0f%%
        - Compréhension : %.0f%%
        - Expression orale : %.0f%%
        
        Génère une analyse personnalisée en français de 2-3 phrases qui :
        1. Donne un feedback positif
        2. Identifie la compétence la plus faible (%s)
        3. Donne un conseil pratique
        4. Termine par une phrase d'encouragement
        
        Réponds UNIQUEMENT avec le texte de l'analyse, pas de JSON, pas de markdown.
        """,
                prenom, langue, scoreMoyen, progressionTexte,
                fraude.niveau, fraude.couleur,
                horaire.meilleurCreneau, horaire.meilleurScore,
                competences.grammaire, competences.vocabulaire,
                competences.comprehension, competences.oral,
                faibleCompetence
        );
    }

    private String genererAnalyseFallback(String prenom, double scoreMoyen, double progression, String meilleurCreneau) {
        String progressionTexte = progression > 0 ? "en progression" : (progression < 0 ? "en régression" : "stable");
        return String.format("""
            💪 %s, votre score moyen est de %.0f%% et vous êtes %s.
            %s
            Continuez vos efforts, vous allez y arriver ! 🎯
            """,
                prenom, scoreMoyen, progressionTexte,
                meilleurCreneau.isEmpty() ? "" : "Le meilleur moment pour réviser est " + meilleurCreneau + "."
        );
    }

    // ═══════════════════════════════════════════════════════════════════
    // CLASSES INTERNES (DTO)
    // ═══════════════════════════════════════════════════════════════════
    public static class PredictionResult {
        public final double prediction;
        public final double marge;
        public final String tendance;
        public final int nbTests;

        public PredictionResult(double prediction, double marge, String tendance, int nbTests) {
            this.prediction = prediction;
            this.marge = marge;
            this.tendance = tendance;
            this.nbTests = nbTests;
        }
    }

    public static class FraudAnalysis {
        public final int score;
        public final String niveau;
        public final String couleur;
        public final List<String> facteurs;

        public FraudAnalysis(int score, String niveau, String couleur, List<String> facteurs) {
            this.score = score;
            this.niveau = niveau;
            this.couleur = couleur;
            this.facteurs = facteurs;
        }
    }

    public static class PointProgression {
        public final String date;
        public final double score;

        public PointProgression(String date, double score) {
            this.date = date;
            this.score = score;
        }
    }

    public static class CompetencesHeatmap {
        public final double grammaire;
        public final double vocabulaire;
        public final double comprehension;
        public final double oral;

        public CompetencesHeatmap() {
            this(0, 0, 0, 0);
        }

        public CompetencesHeatmap(double grammaire, double vocabulaire, double comprehension, double oral) {
            this.grammaire = grammaire;
            this.vocabulaire = vocabulaire;
            this.comprehension = comprehension;
            this.oral = oral;
        }
    }

    public static class HoraireAnalysis {
        public final String meilleurCreneau;
        public final double meilleurScore;
        public final Map<String, List<Double>> scoresParCreneau;

        public HoraireAnalysis(String meilleurCreneau, double meilleurScore, Map<String, List<Double>> scoresParCreneau) {
            this.meilleurCreneau = meilleurCreneau;
            this.meilleurScore = meilleurScore;
            this.scoresParCreneau = scoresParCreneau;
        }
    }

    public static class ConfianceAnalysis {
        public final double score;
        public final String niveau;

        public ConfianceAnalysis(double score, String niveau) {
            this.score = score;
            this.niveau = niveau;
        }
    }

    private double moyenne(List<Double> valeurs) {
        return valeurs.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }
}