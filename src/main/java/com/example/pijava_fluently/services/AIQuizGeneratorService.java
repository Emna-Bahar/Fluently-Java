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
            List<ReponseGeneree> reponses  // vide pour oral/texte_libre
    ) {}

    public record ReponseGeneree(
            String  contenu,
            boolean isCorrecte
    ) {}

    /**
     * @param type "qcm" | "oral" | "texte_libre" | "mixte"
     */
    public List<QuestionGeneree> generer(String theme, String langue,
                                         String niveau, int nombre,
                                         String type) {
        String prompt = buildPrompt(theme, langue, niveau, nombre, type);

        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", MODEL);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 4000);

            List<Map<String, String>> messages = new ArrayList<>();
            Map<String, String> sys = new HashMap<>();
            sys.put("role", "system");
            sys.put("content",
                    "Tu es un créateur de quiz éducatifs. " +
                            "Réponds UNIQUEMENT avec du JSON valide. " +
                            "Commence par [ et termine par ]. " +
                            "Pas de texte avant ou après.");
            messages.add(sys);

            Map<String, String> usr = new HashMap<>();
            usr.put("role", "user");
            usr.put("content", prompt);
            messages.add(usr);

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
                return fallback(theme);

            @SuppressWarnings("unchecked")
            Map<String, Object> message =
                    (Map<String, Object>) choices.get(0).get("message");
            String content = (String) message.get("content");
            return parseQuestions(cleanJson(content));

        } catch (Exception e) {
            LoggerUtil.error("Erreur génération quiz IA", e);
            return fallback(theme);
        }
    }

    private String buildPrompt(String theme, String langue,
                               String niveau, int nombre, String type) {

        String exampleQCM = """
            {
              "type": "qcm",
              "enonce": "Comment dit-on bonjour en anglais ?",
              "scoreMax": 2,
              "reponses": [
                {"contenu": "Hello", "correcte": true},
                {"contenu": "Goodbye", "correcte": false},
                {"contenu": "Please", "correcte": false},
                {"contenu": "Sorry", "correcte": false}
              ]
            }""";

        String exampleOral = """
            {
              "type": "oral",
              "enonce": "Répétez : Je m'appelle Marie et j'ai vingt ans.",
              "scoreMax": 2,
              "reponses": []
            }""";

        String exampleTexteLibre = """
            {
              "type": "texte_libre",
              "enonce": "Décrivez votre journée habituelle en 3-4 phrases.",
              "scoreMax": 5,
              "reponses": []
            }""";

        String exemples = switch (type) {
            case "qcm"         -> exampleQCM;
            case "oral"        -> exampleOral;
            case "texte_libre" -> exampleTexteLibre;
            default            -> exampleQCM + ",\n" + exampleOral + ",\n"
                    + exampleTexteLibre;
        };

        String instructionType = switch (type) {
            case "qcm"         ->
                    "Génère UNIQUEMENT des questions QCM avec 4 réponses dont 1 seule correcte.";
            case "oral"        ->
                    "Génère UNIQUEMENT des questions ORALES : phrases courtes à répéter. " +
                            "L'énoncé est la phrase exacte que l'étudiant doit prononcer.";
            case "texte_libre" ->
                    "Génère UNIQUEMENT des questions à rédiger librement. " +
                            "L'énoncé est le sujet de rédaction. scoreMax entre 5 et 10.";
            default            ->
                    "Génère un mélange : " + (nombre/3 + 1) + " QCM, " +
                            (nombre/3 + 1) + " oral, " + (nombre/3) + " texte_libre.";
        };

        return "Génère exactement " + nombre + " question(s) sur le thème \"" +
                theme + "\" en " + langue + " (niveau " + niveau + ").\n\n" +
                instructionType + "\n\n" +
                "Format JSON EXACT :\n[\n" + exemples + "\n]\n\n" +
                "RÈGLES STRICTES :\n" +
                "- Pour QCM : exactement 4 réponses, 1 seule correcte=true\n" +
                "- Pour oral : reponses = [] (tableau vide)\n" +
                "- Pour texte_libre : reponses = [] (tableau vide), scoreMax entre 5-10\n" +
                "- Les questions doivent être adaptées au niveau " + niveau + "\n" +
                "- Commence par [ directement. Aucun texte avant ou après.";
    }

    @SuppressWarnings("unchecked")
    private List<QuestionGeneree> parseQuestions(String json) {
        List<QuestionGeneree> result = new ArrayList<>();
        try {
            List<Map<String, Object>> rawList =
                    MAPPER.readValue(json, List.class);

            for (Map<String, Object> q : rawList) {
                String enonce = (String) q.getOrDefault("enonce", "");
                String typeQ  = (String) q.getOrDefault("type", "qcm");
                if (enonce.isEmpty()) continue;

                // Score max
                Object scoreObj = q.get("scoreMax");
                int scoreMax = 2;
                if (scoreObj instanceof Number) {
                    scoreMax = ((Number) scoreObj).intValue();
                }

                // Réponses (seulement pour QCM)
                List<ReponseGeneree> reps = new ArrayList<>();
                Object repObj = q.get("reponses");
                if (repObj instanceof List && "qcm".equals(typeQ)) {
                    for (Object r : (List<?>) repObj) {
                        if (r instanceof Map) {
                            Map<String, Object> rm = (Map<String, Object>) r;
                            String contenu = (String) rm.getOrDefault("contenu", "");
                            Object corr    = rm.get("correcte");
                            boolean correcte = corr instanceof Boolean
                                    ? (Boolean) corr
                                    : "true".equals(String.valueOf(corr));
                            if (!contenu.isEmpty())
                                reps.add(new ReponseGeneree(contenu, correcte));
                        }
                    }
                    // S'assurer qu'il y a au moins 1 bonne réponse
                    long nbCorr = reps.stream()
                            .filter(ReponseGeneree::isCorrecte).count();
                    if (nbCorr == 0 && !reps.isEmpty())
                        reps.set(0, new ReponseGeneree(reps.get(0).contenu(), true));
                }

                result.add(new QuestionGeneree(enonce, typeQ, scoreMax, reps));
            }
        } catch (Exception e) {
            LoggerUtil.error("Erreur parsing questions IA", e);
        }
        return result.isEmpty() ? fallback("") : result;
    }

    private String cleanJson(String raw) {
        if (raw == null) return "[]";
        raw = raw.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "").trim();
        int s = raw.indexOf('['), e = raw.lastIndexOf(']');
        return (s != -1 && e > s) ? raw.substring(s, e + 1) : "[]";
    }

    private List<QuestionGeneree> fallback(String theme) {
        return List.of(new QuestionGeneree(
                "Erreur de génération — réessayez",
                "qcm", 2,
                List.of(
                        new ReponseGeneree("Option A", true),
                        new ReponseGeneree("Option B", false),
                        new ReponseGeneree("Option C", false),
                        new ReponseGeneree("Option D", false)
                )
        ));
    }
}