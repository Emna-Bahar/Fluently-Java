package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.LoggerUtil;

import java.text.Normalizer;
import java.util.regex.Pattern;

public class SpeechEvaluationService {

    /**
     * Calcule la similarité entre deux textes (algorithme Levenshtein)
     * Retourne un score entre 0 et 1 (1 = identique)
     */
    public double calculateSimilarity(String spoken, String expected) {
        spoken = normalize(spoken);
        expected = normalize(expected);

        if (spoken.equals(expected)) {
            return 1.0;
        }

        int levenshteinDistance = levenshteinDistance(spoken, expected);
        int maxLength = Math.max(spoken.length(), expected.length());

        if (maxLength == 0) {
            return 1.0;
        }

        return 1.0 - ((double) levenshteinDistance / maxLength);
    }

    /**
     * Calcule la distance de Levenshtein entre deux chaînes
     */
    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) {
            dp[i][0] = i;
        }

        for (int j = 0; j <= b.length(); j++) {
            dp[0][j] = j;
        }

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                int cost = (a.charAt(i - 1) == b.charAt(j - 1)) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(
                                dp[i - 1][j] + 1,
                                dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost
                );
            }
        }

        return dp[a.length()][b.length()];
    }

    /**
     * Normalise un texte (minuscules, sans ponctuation, sans accents)
     */
    public String normalize(String text) {
        if (text == null) {
            return "";
        }

        // Convertir en minuscules
        String normalized = text.toLowerCase();

        // Supprimer les accents
        normalized = Normalizer.normalize(normalized, Normalizer.Form.NFD);
        normalized = Pattern.compile("\\p{InCombiningDiacriticalMarks}+").matcher(normalized).replaceAll("");

        // Garder uniquement lettres, chiffres et espaces
        normalized = normalized.replaceAll("[^\\p{L}\\p{N}\\s]", "");

        // Remplacer les espaces multiples par un seul espace
        normalized = normalized.replaceAll("\\s+", " ");

        return normalized.trim();
    }

    /**
     * Évalue une réponse orale et retourne le statut
     * @return 'correct', 'partial', ou 'incorrect'
     */
    public String evaluateAnswer(String spoken, String expected) {
        double similarity = calculateSimilarity(spoken, expected);

        LoggerUtil.info("Speech evaluation - Similarity: " + similarity);

        if (similarity >= 0.85) {
            return "correct"; // 100% des points
        } else if (similarity >= 0.60) {
            return "partial"; // 50% des points
        } else {
            return "incorrect"; // 0 point
        }
    }

    /**
     * Calcule le score pour une réponse orale
     */
    public double calculateScore(String status, double maxScore) {
        switch (status) {
            case "correct":
                return maxScore;
            case "partial":
                return maxScore * 0.5;
            case "incorrect":
                return 0.0;
            default:
                return 0.0;
        }
    }

    /**
     * Vérifie si la réponse orale est acceptable
     */
    public boolean isAcceptable(String spoken, String expected) {
        String status = evaluateAnswer(spoken, expected);
        return status.equals("correct") || status.equals("partial");
    }
}