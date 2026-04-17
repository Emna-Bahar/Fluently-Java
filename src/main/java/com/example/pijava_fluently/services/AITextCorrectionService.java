package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.ConfigLoader;
import com.example.pijava_fluently.utils.HttpClientUtil;
import com.example.pijava_fluently.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;
import java.util.regex.*;

public class AITextCorrectionService {

    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL = ConfigLoader.get("ai.model", "llama-3.3-70b-versatile");
    private static final double TEMPERATURE = ConfigLoader.getDouble("ai.temperature", 0.1);
    private static final int MAX_TOKENS = ConfigLoader.getInt("ai.max_tokens", 1500);
    private static final ObjectMapper MAPPER = new ObjectMapper();

    // ── Correction texte libre ────────────────────────────────────
    public Map<String, Object> correctFreeText(String studentText, String expectedTheme,
                                               String langue, String niveau) {
        LoggerUtil.info("Calling Groq API for text correction",
                "langue", langue, "niveau", niveau,
                "textLength", String.valueOf(studentText.length()));

        try {
            String prompt = buildPrompt(studentText, expectedTheme, langue, niveau);
            String rawResponse = callGroq(prompt);
            String cleanJson = cleanJsonResponse(rawResponse);

            LoggerUtil.info("Clean JSON", "json", cleanJson.substring(0, Math.min(200, cleanJson.length())));

            Map<String, Object> result = parseJsonSafely(cleanJson);
            LoggerUtil.info("Groq correction successful", "score", String.valueOf(result.get("score")));
            return result;

        } catch (Exception e) {
            LoggerUtil.error("Error calling Groq API", e);
            return getDefaultCorrectionResult();
        }
    }

    // ── Recommandations IA ────────────────────────────────────────
    public Map<String, Object> generateRecommendations(String prompt) {
        try {
            String rawResponse = callGroq(prompt);
            String cleanJson = cleanJsonResponse(rawResponse);
            return parseJsonSafely(cleanJson);
        } catch (Exception e) {
            LoggerUtil.error("Error generating recommendations", e);
            return getDefaultRecommendations();
        }
    }

    // ── Appel API Groq ────────────────────────────────────────────
    private String callGroq(String userPrompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", MODEL);
        requestBody.put("temperature", TEMPERATURE);
        requestBody.put("max_tokens", MAX_TOKENS);

        List<Map<String, String>> messages = new ArrayList<>();

        Map<String, String> systemMsg = new HashMap<>();
        systemMsg.put("role", "system");
        // Prompt système très strict pour éviter les guillemets dans les strings
        systemMsg.put("content",
                "Tu es un correcteur linguistique. " +
                        "Réponds UNIQUEMENT avec du JSON valide. " +
                        "RÈGLE ABSOLUE : Dans les tableaux 'erreurs' et 'corrections', " +
                        "utilise des tirets (-) au lieu de guillemets pour séparer les parties. " +
                        "N'utilise JAMAIS de guillemets à l'intérieur des chaînes JSON. " +
                        "Commence par { et termine par }. Aucun markdown.");
        messages.add(systemMsg);

        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        requestBody.put("messages", messages);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + ConfigLoader.get("groq.api.key"));
        headers.put("Content-Type", "application/json");

        Map<String, Object> response = HttpClientUtil.postJson(GROQ_API_URL, requestBody, headers);

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            throw new RuntimeException("No response from Groq API");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return (String) message.get("content");
    }

    // ── Nettoyage JSON ────────────────────────────────────────────
    private String cleanJsonResponse(String content) {
        if (content == null) return "{}";

        // 1. Supprimer les blocs markdown
        content = content.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "");
        content = content.trim();

        // 2. Extraire entre { et }
        int start = content.indexOf('{');
        int end = content.lastIndexOf('}');
        if (start == -1 || end == -1 || end <= start) return "{}";
        content = content.substring(start, end + 1);

        // 3. Nettoyer les guillemets problématiques DANS les valeurs de tableaux
        // Pattern: remplacer les guillemets français/typographiques
        content = content.replace('\u201C', ' ').replace('\u201D', ' '); // " "
        content = content.replace('\u2018', ' ').replace('\u2019', ' '); // ' '

        // 4. Fixer les guillemets non échappés dans les valeurs de tableaux
        content = fixUnescapedQuotesInArrays(content);

        return content;
    }

    /**
     * Corrige les guillemets non échappés dans les valeurs JSON des tableaux.
     * Exemple : ["erreur "mot" ici"] → ["erreur mot ici"]
     */
    private String fixUnescapedQuotesInArrays(String json) {
        // Remplacer les patterns du type : "texte "citation" texte"
        // par : "texte citation texte"
        // On cherche des guillemets qui apparaissent au milieu d'une chaîne JSON
        StringBuilder result = new StringBuilder();
        boolean inString = false;
        boolean escaped = false;

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (escaped) {
                result.append(c);
                escaped = false;
                continue;
            }

            if (c == '\\') {
                escaped = true;
                result.append(c);
                continue;
            }

            if (c == '"') {
                if (!inString) {
                    // Ouvrir une chaîne
                    inString = true;
                    result.append(c);
                } else {
                    // Vérifier si c'est la vraie fermeture
                    // (suivie de : , ] } espace newline)
                    int next = i + 1;
                    while (next < json.length() && json.charAt(next) == ' ') next++;
                    char nextChar = next < json.length() ? json.charAt(next) : 0;

                    if (nextChar == ':' || nextChar == ',' || nextChar == ']'
                            || nextChar == '}' || nextChar == 0) {
                        // C'est une vraie fermeture
                        inString = false;
                        result.append(c);
                    } else {
                        // Guillemet intrus → remplacer par espace
                        result.append(' ');
                    }
                }
            } else {
                result.append(c);
            }
        }

        return result.toString();
    }

    // ── Parsing JSON robuste ──────────────────────────────────────
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJsonSafely(String json) {
        // Tentative 1 : Jackson direct
        try {
            return MAPPER.readValue(json, Map.class);
        } catch (Exception e) {
            LoggerUtil.warning("Jackson parse failed, trying regex extraction");
        }

        // Tentative 2 : extraction par regex (fallback robuste)
        Map<String, Object> result = new HashMap<>();
        result.put("score",       extractInt(json, "score",       50));
        result.put("grammaire",   extractInt(json, "grammaire",   50));
        result.put("vocabulaire", extractInt(json, "vocabulaire", 50));
        result.put("coherence",   extractInt(json, "coherence",   50));
        result.put("erreurs",     extractArray(json, "erreurs"));
        result.put("corrections", extractArray(json, "corrections"));
        result.put("commentaire", extractString(json, "commentaire",
                "Correction disponible mais format invalide."));

        LoggerUtil.info("Regex extraction result", "score", String.valueOf(result.get("score")));
        return result;
    }

    private int extractInt(String json, String key, int defaultVal) {
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*(\\d+)");
        Matcher m = p.matcher(json);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); }
            catch (NumberFormatException ignored) {}
        }
        return defaultVal;
    }

    private String extractString(String json, String key, String defaultVal) {
        // Cherche "key": "valeur" (valeur peut contenir des espaces)
        Pattern p = Pattern.compile("\"" + key + "\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher m = p.matcher(json);
        if (m.find()) return m.group(1).replace("\\\"", "\"").replace("\\n", " ");
        return defaultVal;
    }

    private List<String> extractArray(String json, String key) {
        List<String> list = new ArrayList<>();
        // Extraire le contenu du tableau
        Pattern arrayPattern = Pattern.compile(
                "\"" + key + "\"\\s*:\\s*\\[([^\\]]*)]", Pattern.DOTALL);
        Matcher arrayMatcher = arrayPattern.matcher(json);

        if (!arrayMatcher.find()) return list;
        String arrayContent = arrayMatcher.group(1);

        // Extraire chaque élément entre guillemets
        Pattern itemPattern = Pattern.compile("\"((?:[^\"\\\\]|\\\\.)*)\"");
        Matcher itemMatcher = itemPattern.matcher(arrayContent);
        while (itemMatcher.find()) {
            String item = itemMatcher.group(1).replace("\\\"", "\"").trim();
            if (!item.isEmpty()) list.add(item);
        }
        return list;
    }

    // ── Builders de prompt ────────────────────────────────────────
    private String buildPrompt(String studentText, String theme,
                               String langue, String niveau) {
        return "Évalue ce texte d'étudiant (niveau " + niveau + ", thème: " + theme + ", langue: " + langue + ").\n\n" +
                "TEXTE:\n" + studentText + "\n\n" +
                "Réponds avec ce JSON EXACT (pas de guillemets dans les chaînes, " +
                "utilise des tirets pour séparer l'erreur de la correction):\n" +
                "{\n" +
                "  \"score\": 75,\n" +
                "  \"grammaire\": 70,\n" +
                "  \"vocabulaire\": 80,\n" +
                "  \"coherence\": 75,\n" +
                "  \"erreurs\": [\"description erreur 1\", \"description erreur 2\"],\n" +
                "  \"corrections\": [\"correction 1\", \"correction 2\"],\n" +
                "  \"commentaire\": \"commentaire global sans guillemets internes\"\n" +
                "}\n\n" +
                "INTERDIT: guillemets a l interieur des valeurs des chaines. " +
                "Commence par { directement.";
    }

    // ── Defaults ──────────────────────────────────────────────────
    private Map<String, Object> getDefaultCorrectionResult() {
        Map<String, Object> result = new HashMap<>();
        result.put("score", 50);
        result.put("grammaire", 50);
        result.put("vocabulaire", 50);
        result.put("coherence", 50);
        result.put("erreurs", List.of("Correction automatique temporairement indisponible"));
        result.put("corrections", new ArrayList<>());
        result.put("commentaire", "Correction IA temporairement indisponible.");
        return result;
    }

    private Map<String, Object> getDefaultRecommendations() {
        Map<String, Object> result = new HashMap<>();
        result.put("recommandations", List.of(
                Map.of("titre", "Continuez à pratiquer",
                        "description", "La pratique régulière est la clé",
                        "actions", List.of("Passer plus de tests", "Réviser les erreurs"),
                        "priorite", "haute")
        ));
        result.put("message_encouragement", "Continue comme ça ! 💪");
        return result;
    }
}