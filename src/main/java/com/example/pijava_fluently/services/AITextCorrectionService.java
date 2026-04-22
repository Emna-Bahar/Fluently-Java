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
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 2000);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content",
                    "Tu es un conseiller pedagogique. " +
                            "Tu reponds UNIQUEMENT en JSON valide. " +
                            "TOUTES les valeurs des chaines DOIVENT etre entre guillemets doubles. " +
                            "Exemple valide: {\"titre\": \"Mon titre\", \"score\": 75}. " +
                            "Commence par { et termine par }. Aucun texte avant ou apres.");
            messages.add(systemMsg);

            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", prompt);
            messages.add(userMsg);

            requestBody.put("messages", messages);

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + ConfigLoader.get("groq.api.key"));
            headers.put("Content-Type", "application/json");

            Map<String, Object> response =
                    HttpClientUtil.postJson(GROQ_API_URL, requestBody, headers);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");

            if (choices == null || choices.isEmpty())
                return getDefaultRecommendations();

            @SuppressWarnings("unchecked")
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            LoggerUtil.info("Raw AI response",
                    "content", content.substring(0, Math.min(300, content.length())));

            String cleanJson = cleanJsonResponse(content);
            LoggerUtil.info("Clean JSON",
                    "preview", cleanJson.substring(0, Math.min(200, cleanJson.length())));

            // Tentative de parsing Jackson
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> result = MAPPER.readValue(cleanJson, Map.class);
                if (result.containsKey("recommandations")) {
                    LoggerUtil.info("AI recommendations parsed successfully");
                    return result;
                }
            } catch (Exception e) {
                LoggerUtil.warning("Jackson failed: " + e.getMessage());
            }

            // Fallback : parser manuellement les recommandations
            return parseRecommendationsManually(cleanJson);

        } catch (Exception e) {
            LoggerUtil.error("Error generating recommendations", e);
            return getDefaultRecommendations();
        }
    }
    /**
     * Parse manuellement le JSON de recommandations quand Jackson échoue.
     * Gère les cas où l'IA oublie des guillemets.
     */
    private Map<String, Object> parseRecommendationsManually(String json) {
        LoggerUtil.info("Attempting manual recommendations parse");

        List<Map<String, Object>> recommandations = new ArrayList<>();

        // Extraire tous les blocs {...} à l'intérieur du tableau recommandations
        // On cherche d'abord le tableau
        int arrayStart = json.indexOf("\"recommandations\"");
        if (arrayStart == -1) {
            arrayStart = json.indexOf("recommandations");
        }

        if (arrayStart != -1) {
            int bracketOpen = json.indexOf('[', arrayStart);
            int bracketClose = json.lastIndexOf(']');

            if (bracketOpen != -1 && bracketClose != -1 && bracketClose > bracketOpen) {
                String arrayContent = json.substring(bracketOpen + 1, bracketClose);

                // Extraire chaque bloc d'objet {}
                List<String> blocks = extractObjectBlocks(arrayContent);

                for (String block : blocks) {
                    Map<String, Object> rec = new HashMap<>();

                    // Extraire titre
                    String titre = extractValueFlexible(block, "titre");
                    rec.put("titre", titre.isEmpty() ? "Conseil" : titre);

                    // Extraire description
                    String description = extractValueFlexible(block, "description");
                    rec.put("description", description.isEmpty()
                            ? "Continuez vos efforts." : description);

                    // Extraire priorite
                    String priorite = extractValueFlexible(block, "priorite");
                    rec.put("priorite", priorite.isEmpty() ? "moyenne" : priorite);

                    // Extraire actions
                    List<String> actions = extractActionsFlexible(block);
                    if (actions.isEmpty()) {
                        actions = List.of("Pratiquer régulièrement",
                                "Revoir les erreurs");
                    }
                    rec.put("actions", actions);

                    if (!titre.isEmpty()) {
                        recommandations.add(rec);
                    }
                }
            }
        }

        // Extraire message_encouragement
        String msg = extractValueFlexible(json, "message_encouragement");
        if (msg.isEmpty()) msg = "Continue comme ça ! 💪";

        if (!recommandations.isEmpty()) {
            LoggerUtil.info("Manual parse success",
                    "count", String.valueOf(recommandations.size()));
            Map<String, Object> result = new HashMap<>();
            result.put("recommandations", recommandations);
            result.put("message_encouragement", msg);
            return result;
        }

        LoggerUtil.warning("Manual parse found no recommendations, using default");
        return getDefaultRecommendations();
    }

    /**
     * Extrait les blocs {} d'une chaîne (niveau 1 uniquement).
     */
    private List<String> extractObjectBlocks(String content) {
        List<String> blocks = new ArrayList<>();
        int depth = 0;
        int start = -1;

        for (int i = 0; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                if (depth == 0) start = i;
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0 && start != -1) {
                    blocks.add(content.substring(start, i + 1));
                    start = -1;
                }
            }
        }
        return blocks;
    }

    /**
     * Extrait une valeur de façon flexible :
     * gère "key": "value", "key": value, key: "value", key: value.
     */
    private String extractValueFlexible(String json, String key) {
        // Pattern 1 : "key": "value" (standard)
        java.util.regex.Pattern p1 = java.util.regex.Pattern.compile(
                "\"?" + key + "\"?\\s*:\\s*\"([^\"]+)\"");
        java.util.regex.Matcher m1 = p1.matcher(json);
        if (m1.find()) return m1.group(1).trim();

        // Pattern 2 : "key": value_sans_guillemets (jusqu'à , ou } ou \n)
        java.util.regex.Pattern p2 = java.util.regex.Pattern.compile(
                "\"?" + key + "\"?\\s*:\\s*([^\"\\[\\]{},\\n]+)");
        java.util.regex.Matcher m2 = p2.matcher(json);
        if (m2.find()) {
            String val = m2.group(1).trim();
            // Ignorer si c'est un chiffre (score etc.)
            if (!val.matches("\\d+")) return val;
        }

        return "";
    }

    /**
     * Extrait les actions d'un bloc de façon flexible.
     */
    private List<String> extractActionsFlexible(String block) {
        List<String> actions = new ArrayList<>();

        // Trouver le tableau actions
        int actionsIdx = block.indexOf("\"actions\"");
        if (actionsIdx == -1) actionsIdx = block.indexOf("actions");
        if (actionsIdx == -1) return actions;

        int arrStart = block.indexOf('[', actionsIdx);
        int arrEnd   = block.indexOf(']', actionsIdx);
        if (arrStart == -1 || arrEnd == -1 || arrEnd <= arrStart) return actions;

        String arrContent = block.substring(arrStart + 1, arrEnd);

        // Pattern 1 : "action en guillemets"
        java.util.regex.Pattern pQuoted =
                java.util.regex.Pattern.compile("\"([^\"]+)\"");
        java.util.regex.Matcher mQuoted = pQuoted.matcher(arrContent);
        while (mQuoted.find()) {
            String action = mQuoted.group(1).trim();
            if (!action.isEmpty()) actions.add(action);
        }

        // Si aucune action avec guillemets, essayer sans guillemets
        if (actions.isEmpty()) {
            String[] parts = arrContent.split(",");
            for (String part : parts) {
                String cleaned = part.replaceAll("[\\[\\]\"'{}]", "").trim();
                if (!cleaned.isEmpty()) actions.add(cleaned);
            }
        }

        return actions;
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
        return """
Évalue ce texte d'étudiant selon une grille précise. Sois STRICT — un texte hors sujet doit avoir un score bas.

THÈME IMPOSÉ : "%s"
LANGUE : %s
NIVEAU : %s

TEXTE DE L'ÉTUDIANT :
\"\"\"%s\"\"\"

GRILLE D'ÉVALUATION (total 30 points, convertis en score /100) :
1. Respect du sujet (0-4 pts) : le texte répond-il AU SUJET IMPOSÉ ?
   - Hors sujet total = 0 pts. Partiellement = 2 pts. Respecté = 4 pts.
2. Cohérence (0-4 pts) : les idées s'enchaînent-elles logiquement ?
3. Expression des idées (0-4 pts) : présentation de faits et opinions clairs.
4. Vocabulaire - étendue (0-3 pts) : richesse du vocabulaire utilisé.
5. Vocabulaire - maîtrise (0-3 pts) : mots utilisés correctement.
6. Grammaire - orthographe lexicale (0-2 pts) :
   0-4 fautes=2pts, 5-6 fautes=1.5pts, 7-8 fautes=1pt, 9-10=0.5pt, >10=0pt
7. Grammaire - orthographe grammaticale (0-2 pts) : même barème.
8. Structure des phrases (0-2 pts) : phrases correctement construites.
9. Mise en page / longueur (0-2 pts) : texte suffisamment développé.
10. Lisibilité (0-2 pts) : texte clair et compréhensible.
11. Capacité à présenter des faits (0-2 pts).

RÈGLE ABSOLUE : si le texte est hors sujet, le score total NE PEUT PAS dépasser 30/100.
Un texte de 3 mots ne peut pas dépasser 20/100 même sans fautes.

Réponds UNIQUEMENT avec ce JSON (pas de guillemets dans les valeurs) :
{
  "score": nombre entre 0 et 100,
  "grammaire": nombre entre 0 et 100,
  "vocabulaire": nombre entre 0 et 100,
  "coherence": nombre entre 0 et 100,
  "respect_sujet": nombre entre 0 et 100,
  "erreurs": ["erreur 1", "erreur 2"],
  "corrections": ["correction 1", "correction 2"],
  "commentaire": "commentaire global de 2-3 phrases en %s"
}

Commence par { directement.
""".formatted(theme, langue, niveau, studentText, langue);
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