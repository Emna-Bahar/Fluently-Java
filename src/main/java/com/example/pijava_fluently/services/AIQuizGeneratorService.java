package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.ConfigLoader;
import com.example.pijava_fluently.utils.HttpClientUtil;
import com.example.pijava_fluently.utils.LoggerUtil;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.*;

public class AIQuizGeneratorService {

    private static final String GROQ_API_URL =
            "https://api.groq.com/openai/v1/chat/completions";
    private static final String MODEL =
            ConfigLoader.get("ai.model", "llama-3.3-70b-versatile");
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public record QuestionGeneree(
            String enonce,
            String type,
            int    scoreMax,
            List<ReponseGeneree> reponses
    ) {}

    public record ReponseGeneree(
            String  contenu,
            boolean isCorrecte
    ) {}

    /**
     * Génère des questions QCM pour un test donné.
     * @param theme     sujet des questions (ex: "conjugaison présent")
     * @param langue    langue du test (ex: "Français")
     * @param niveau    niveau CECRL (ex: "A1", "B2")
     * @param nombre    nombre de questions à générer (1-10)
     */
    public List<QuestionGeneree> generer(String theme, String langue,
                                         String niveau, int nombre) {
        String prompt = buildPrompt(theme, langue, niveau, nombre);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 3000);

            List<Map<String, String>> messages = new ArrayList<>();

            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            // INSTRUCTION TRÈS STRICTE pour les guillemets
            sys.put("content",
                    "Tu es un créateur de quiz éducatifs.\n" +
                            "RÈGLE ABSOLUE : TOUTES les valeurs textuelles DOIVENT être entre guillemets doubles.\n" +
                            "Exemple CORRECT : {\"enonce\": \"Quel verbe utiliser?\", \"reponses\": [{\"contenu\": \"aller\", \"correcte\": true}]}\n" +
                            "Exemple INCORRECT : {\"enonce\": Quel verbe utiliser?, \"reponses\": [{\"contenu\": aller, \"correcte\": true}]}\n" +
                            "Réponds UNIQUEMENT avec un JSON valide. Commence par [ et termine par ].\n" +
                            "Pas de texte avant ou après le JSON.");
            messages.add(sys);

            Map<String, String> user = new HashMap<>();
            user.put("role", "user");
            user.put("content", prompt);
            messages.add(user);

            requestBody.put("messages", messages);

            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization",
                    "Bearer " + ConfigLoader.get("groq.api.key"));
            headers.put("Content-Type", "application/json");

            Map<String, Object> response =
                    HttpClientUtil.postJson(GROQ_API_URL, requestBody, headers);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> choices =
                    (List<Map<String, Object>>) response.get("choices");
            if (choices == null || choices.isEmpty())
                return getDefaultQuestions(theme, langue);

            @SuppressWarnings("unchecked")
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");

            return parseQuestions(cleanJson(content));

        } catch (Exception e) {
            LoggerUtil.error("Erreur génération quiz IA", e);
            return getDefaultQuestions(theme, langue);
        }
    }

    private String buildPrompt(String theme, String langue,
                               String niveau, int nombre) {
        return "Génère exactement " + nombre + " questions QCM sur le thème \"" +
                theme + "\" en " + langue + " (niveau " + niveau + ").\n\n" +
                "Chaque question doit avoir exactement 4 réponses dont UNE seule correcte.\n" +
                "Les questions doivent être adaptées au niveau " + niveau + ".\n\n" +
                "⚠️ IMPORTANT : TOUTES les valeurs textuelles DOIVENT être entre guillemets doubles !\n\n" +
                "Réponds avec ce format JSON EXACT :\n" +
                "[\n" +
                "  {\n" +
                "    \"enonce\": \"La question ici\",\n" +
                "    \"reponses\": [\n" +
                "      {\"contenu\": \"Réponse A\", \"correcte\": true},\n" +
                "      {\"contenu\": \"Réponse B\", \"correcte\": false},\n" +
                "      {\"contenu\": \"Réponse C\", \"correcte\": false},\n" +
                "      {\"contenu\": \"Réponse D\", \"correcte\": false}\n" +
                "    ]\n" +
                "  }\n" +
                "]\n\n" +
                "Commence par [ directement. Pas de texte avant ou après.";
    }

    @SuppressWarnings("unchecked")
    private List<QuestionGeneree> parseQuestions(String rawJson) {
        List<QuestionGeneree> result = new ArrayList<>();

        // Nettoyage avancé du JSON avant parsing
        String cleanedJson = sanitizeJson(rawJson);

        try {
            List<Map<String, Object>> rawList =
                    MAPPER.readValue(cleanedJson, List.class);

            for (Map<String, Object> q : rawList) {
                String enonce = extractStringValue(q, "enonce");
                if (enonce.isEmpty()) continue;

                List<ReponseGeneree> reps = new ArrayList<>();
                Object repObj = q.get("reponses");
                if (repObj instanceof List) {
                    for (Object r : (List<?>) repObj) {
                        if (r instanceof Map) {
                            Map<String, Object> rm = (Map<String, Object>) r;
                            String contenu = extractStringValue(rm, "contenu");
                            boolean correcte = extractBooleanValue(rm, "correcte");
                            if (!contenu.isEmpty())
                                reps.add(new ReponseGeneree(contenu, correcte));
                        }
                    }
                }

                // Vérifier qu'il y a exactement une bonne réponse
                long nbCorrectes = reps.stream()
                        .filter(ReponseGeneree::isCorrecte).count();
                if (nbCorrectes == 0 && !reps.isEmpty()) {
                    reps.set(0, new ReponseGeneree(reps.get(0).contenu(), true));
                }

                result.add(new QuestionGeneree(enonce, "qcm", 2, reps));
            }
        } catch (Exception e) {
            LoggerUtil.error("Erreur parsing questions IA", e);
            // Tentative de récupération manuelle
            return parseQuestionsManually(cleanedJson);
        }

        return result.isEmpty() ? getDefaultQuestions("", "") : result;
    }

    /**
     * Sanitize le JSON avant parsing
     */
    private String sanitizeJson(String json) {
        if (json == null || json.isEmpty()) return "[]";

        // Ajouter des guillemets autour des valeurs qui n'en ont pas
        // Pattern: "key": valeur_sans_guillemets (qui n'est ni true/false/null ni un nombre)
        StringBuilder result = new StringBuilder();
        boolean inKey = false;
        boolean inString = false;
        boolean afterColon = false;
        StringBuilder currentToken = new StringBuilder();

        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);

            if (c == '"' && (i == 0 || json.charAt(i-1) != '\\')) {
                inString = !inString;
                result.append(c);
            } else if (inString) {
                result.append(c);
            } else if (c == ':' && !inString) {
                afterColon = true;
                currentToken = new StringBuilder();
                result.append(c);
            } else if (afterColon) {
                if (c == ',' || c == '}' || c == ']' || c == ' ') {
                    String token = currentToken.toString().trim();
                    if (!token.isEmpty() &&
                            !token.equals("true") && !token.equals("false") &&
                            !token.equals("null") && !token.matches("-?\\d+(\\.\\d+)?")) {
                        // Valeur sans guillemets, on les ajoute
                        result.append('"').append(token).append('"');
                    } else if (!token.isEmpty()) {
                        result.append(token);
                    }
                    result.append(c);
                    afterColon = false;
                    currentToken = new StringBuilder();
                } else {
                    currentToken.append(c);
                }
            } else {
                result.append(c);
            }
        }

        // Nettoyer les guillemets en trop
        String sanitized = result.toString();
        sanitized = sanitized.replaceAll(",\\s*}", "}");
        sanitized = sanitized.replaceAll(",\\s*]", "]");

        return sanitized;
    }

    private String extractStringValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return "";
        if (val instanceof String) return (String) val;
        return String.valueOf(val);
    }

    private boolean extractBooleanValue(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return false;
        if (val instanceof Boolean) return (Boolean) val;
        if (val instanceof String) return "true".equalsIgnoreCase((String) val);
        return false;
    }

    /**
     * Parsing manuel de secours
     */
    private List<QuestionGeneree> parseQuestionsManually(String json) {
        List<QuestionGeneree> result = new ArrayList<>();

        try {
            // Extraire chaque bloc de question
            String[] questionBlocks = json.split("\\{[\\s\n]*\"enonce\"");

            for (String block : questionBlocks) {
                if (block.trim().isEmpty()) continue;

                // Extraire l'énoncé
                String enonce = "";
                int enonceStart = block.indexOf("\"enonce\"");
                if (enonceStart != -1) {
                    int colonPos = block.indexOf(':', enonceStart);
                    if (colonPos != -1) {
                        int quoteStart = block.indexOf('"', colonPos + 1);
                        int quoteEnd = block.indexOf('"', quoteStart + 1);
                        if (quoteStart != -1 && quoteEnd != -1) {
                            enonce = block.substring(quoteStart + 1, quoteEnd);
                        } else {
                            // Sans guillemets
                            int commaPos = block.indexOf(',', colonPos + 1);
                            if (commaPos == -1) commaPos = block.indexOf('}', colonPos + 1);
                            if (commaPos != -1) {
                                enonce = block.substring(colonPos + 1, commaPos).trim();
                            }
                        }
                    }
                }

                if (enonce.isEmpty()) continue;

                // Extraire les réponses
                List<ReponseGeneree> reponses = new ArrayList<>();
                int reponsesStart = block.indexOf("\"reponses\"");
                if (reponsesStart != -1) {
                    int arrayStart = block.indexOf('[', reponsesStart);
                    int arrayEnd = block.lastIndexOf(']');
                    if (arrayStart != -1 && arrayEnd != -1 && arrayEnd > arrayStart) {
                        String repsContent = block.substring(arrayStart + 1, arrayEnd);
                        String[] repBlocks = repsContent.split("\\{[\\s\n]*\"contenu\"");

                        for (String rBlock : repBlocks) {
                            if (rBlock.trim().isEmpty()) continue;

                            String contenu = "";
                            boolean correcte = false;

                            // Extraire contenu
                            int contenuStart = rBlock.indexOf("\"contenu\"");
                            if (contenuStart != -1) {
                                int colonPos = rBlock.indexOf(':', contenuStart);
                                if (colonPos != -1) {
                                    int quoteStart = rBlock.indexOf('"', colonPos + 1);
                                    int quoteEnd = rBlock.indexOf('"', quoteStart + 1);
                                    if (quoteStart != -1 && quoteEnd != -1) {
                                        contenu = rBlock.substring(quoteStart + 1, quoteEnd);
                                    } else {
                                        int commaPos = rBlock.indexOf(',', colonPos + 1);
                                        if (commaPos == -1) commaPos = rBlock.indexOf('}', colonPos + 1);
                                        if (commaPos != -1) {
                                            contenu = rBlock.substring(colonPos + 1, commaPos).trim();
                                        }
                                    }
                                }
                            }

                            // Extraire correcte
                            int correctStart = rBlock.indexOf("\"correcte\"");
                            if (correctStart != -1) {
                                int colonPos = rBlock.indexOf(':', correctStart);
                                if (colonPos != -1) {
                                    int valStart = colonPos + 1;
                                    int valEnd = rBlock.indexOf(',', valStart);
                                    if (valEnd == -1) valEnd = rBlock.indexOf('}', valStart);
                                    if (valEnd != -1) {
                                        String val = rBlock.substring(valStart, valEnd).trim();
                                        correcte = val.equals("true") || val.equals("1");
                                    }
                                }
                            }

                            if (!contenu.isEmpty()) {
                                reponses.add(new ReponseGeneree(contenu, correcte));
                            }
                        }
                    }
                }

                if (!reponses.isEmpty()) {
                    // S'assurer qu'il y a une bonne réponse
                    boolean hasCorrect = reponses.stream().anyMatch(ReponseGeneree::isCorrecte);
                    if (!hasCorrect && !reponses.isEmpty()) {
                        reponses.set(0, new ReponseGeneree(reponses.get(0).contenu(), true));
                    }
                    result.add(new QuestionGeneree(enonce, "qcm", 2, reponses));
                }
            }
        } catch (Exception e) {
            LoggerUtil.error("Erreur parsing manuel", e);
        }

        return result.isEmpty() ? getDefaultQuestions("", "") : result;
    }

    private String cleanJson(String raw) {
        if (raw == null) return "[]";
        raw = raw.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
        raw = raw.replaceAll(",\\s*}", "}").replaceAll(",\\s*]", "]");
        int start = raw.indexOf('[');
        int end   = raw.lastIndexOf(']');
        if (start != -1 && end != -1 && end > start)
            return raw.substring(start, end + 1);
        return "[]";
    }

    private List<QuestionGeneree> getDefaultQuestions(String theme, String langue) {
        return List.of(new QuestionGeneree(
                "Erreur de génération — veuillez réessayer",
                "qcm", 2,
                List.of(
                        new ReponseGeneree("Réponse A", true),
                        new ReponseGeneree("Réponse B", false),
                        new ReponseGeneree("Réponse C", false),
                        new ReponseGeneree("Réponse D", false)
                )
        ));
    }
}