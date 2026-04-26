package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.entites.Tache;
import com.example.pijava_fluently.utils.ConfigLoader;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Service de génération de tâches par IA (Groq — LLaMA 3, 100% gratuit).
 */
public class AITaskGeneratorService {

    private final OkHttpClient httpClient;
    private final ObjectMapper mapper;
    private final String GROQ_API_KEY;
    private final String GROQ_API_URL;
    private final String MODEL;

    public AITaskGeneratorService() {
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.mapper = new ObjectMapper();

        // Charger la configuration depuis config.properties
        this.GROQ_API_KEY = ConfigLoader.getGroqApiKey();
        this.GROQ_API_URL = ConfigLoader.getGroqApiUrl();
        this.MODEL = ConfigLoader.getGroqModel();

        if (GROQ_API_KEY == null || GROQ_API_KEY.isEmpty() || GROQ_API_KEY.equals("VOTRE_CLE_API_GROQ_ICI")) {
            System.err.println("⚠️ ATTENTION: Clé API Groq non configurée !");
            System.err.println("Ajoutez groq.api.key dans src/main/resources/config.properties");
        } else {
            System.out.println("✅ Configuration API Groq chargée avec succès");
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MODÈLE DE RÉSULTAT
    // ════════════════════════════════════════════════════════════════════════

    public static class AIGeneratedTask {
        public String    titre;
        public String    description;
        public String    statut      = "À faire";
        public String    priorite;
        public int       dureeJours;
        public String    pourquoi;
        public String    mediaType;
        public String    mediaTitre;
        public String    mediaAuteur;
        public List<String> sousTaches = new ArrayList<>();

        public Tache toTache(int idObjectif) {
            LocalDate dateLimite = LocalDate.now().plusDays(dureeJours > 0 ? dureeJours : 14);
            String safeTitre = titre != null && titre.length() > 50 ? titre.substring(0, 47) + "..." : titre;
            String safeDesc = description != null && description.length() > 255 ? description.substring(0, 252) + "..." : description;
            String safePriorite = priorite != null ? priorite : "Normale";
            return new Tache(safeTitre, safeDesc, dateLimite, statut, safePriorite, idObjectif);
        }
    }

    public static class UserLearningProfile {
        public String niveauLangue;
        public List<String> interests;
        public double tauxCompletion;
        public double tauxEchec;
        public String patternPrefere;
        public int    nombreTachesTotal;
        public int    nombreTachesTerminees;
        public int    nombreTachesBloquees;

        public UserLearningProfile(String niveau, List<String> interests,
                                   double completion, double echec,
                                   String pattern, int total, int terminees, int bloquees) {
            this.niveauLangue      = niveau;
            this.interests         = interests;
            this.tauxCompletion    = completion;
            this.tauxEchec         = echec;
            this.patternPrefere    = pattern;
            this.nombreTachesTotal = total;
            this.nombreTachesTerminees = terminees;
            this.nombreTachesBloquees  = bloquees;
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  MÉTHODE PRINCIPALE
    // ════════════════════════════════════════════════════════════════════════

    public List<AIGeneratedTask> generateTasks(Objectif objectif) throws IOException {
        // Créer un profil par défaut si besoin
        UserLearningProfile profile = new UserLearningProfile(
                "B1",
                List.of("général"),
                50, 0, "pratique régulière", 0, 0, 0
        );
        return generateTasks(objectif, profile);
    }

    public List<AIGeneratedTask> generateTasks(Objectif objectif, UserLearningProfile profile) throws IOException {
        String systemPrompt = buildSystemPrompt();
        String userPrompt   = buildUserPrompt(objectif, profile);

        String jsonBody = buildRequestBody(systemPrompt, userPrompt);

        Request request = new Request.Builder()
                .url(GROQ_API_URL)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + GROQ_API_KEY)
                .post(RequestBody.create(jsonBody, MediaType.parse("application/json")))
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "No response body";
                System.err.println("Erreur API: " + response.code() + " - " + errorBody);
                return getDefaultTasks();
            }
            String responseBody = response.body().string();
            return parseResponse(responseBody);
        } catch (Exception e) {
            System.err.println("Exception lors de l'appel API: " + e.getMessage());
            return getDefaultTasks();
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CONSTRUCTION DU PROMPT
    // ════════════════════════════════════════════════════════════════════════

    private String buildSystemPrompt() {
        return """
                Tu es un assistant expert en apprentissage des langues.
                Tu génères des tâches d'apprentissage personnalisées en JSON.
                
                RÈGLES STRICTES :
                - Réponds UNIQUEMENT avec un tableau JSON valide, aucun texte avant ou après.
                - Génère exactement 3 tâches progressives.
                - Adapte la difficulté au niveau de langue de l'utilisateur.
                - Tiens compte du taux d'échec : si > 40%, simplifie les tâches.
                
                FORMAT JSON OBLIGATOIRE :
                {
                  "taches": [
                    {
                      "titre": "string",
                      "description": "string",
                      "priorite": "Basse|Normale|Haute|Urgente",
                      "dureeJours": 7,
                      "pourquoi": "string",
                      "sousTaches": ["sous-tache 1", "sous-tache 2"]
                    }
                  ]
                }
                """;
    }

    private String buildUserPrompt(Objectif objectif, UserLearningProfile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Génère 3 tâches pour cet objectif d'apprentissage :\n\n");
        sb.append("OBJECTIF : ").append(objectif.getTitre()).append("\n");
        if (objectif.getDescription() != null && !objectif.getDescription().isBlank()) {
            sb.append("Description : ").append(objectif.getDescription()).append("\n");
        }
        sb.append("\nPROFIL UTILISATEUR :\n");
        sb.append("- Niveau de langue    : ").append(profile.niveauLangue).append("\n");
        sb.append("- Centres d'intérêt  : ").append(String.join(", ", profile.interests)).append("\n");
        sb.append("- Taux de complétion : ").append(String.format("%.0f", profile.tauxCompletion)).append("%\n");
        sb.append("- Taux d'échec       : ").append(String.format("%.0f", profile.tauxEchec)).append("%\n");
        sb.append("- Pattern préféré    : ").append(profile.patternPrefere).append("\n");

        if (profile.tauxEchec > 40) {
            sb.append("\nATTENTION : Taux d'échec élevé. Génère des tâches plus simples et progressives.\n");
        }

        return sb.toString();
    }

    private String buildRequestBody(String systemPrompt, String userPrompt) {
        String escapedSystem = escapeJson(systemPrompt);
        String escapedUser = escapeJson(userPrompt);

        return String.format("""
                {
                  "model": "%s",
                  "messages": [
                    {"role": "system", "content": "%s"},
                    {"role": "user", "content": "%s"}
                  ],
                  "temperature": 0.7,
                  "max_tokens": 2000
                }
                """, MODEL, escapedSystem, escapedUser);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PARSING DE LA RÉPONSE
    // ════════════════════════════════════════════════════════════════════════

    private List<AIGeneratedTask> parseResponse(String responseBody) throws IOException {
        List<AIGeneratedTask> tasks = new ArrayList<>();

        try {
            JsonNode root = mapper.readTree(responseBody);
            String content = root.path("choices").get(0).path("message").path("content").asText();

            // Nettoyer le JSON
            content = content.trim();
            if (content.startsWith("```json")) {
                content = content.substring(7);
            }
            if (content.startsWith("```")) {
                content = content.substring(3);
            }
            if (content.endsWith("```")) {
                content = content.substring(0, content.length() - 3);
            }
            content = content.trim();

            JsonNode data = mapper.readTree(content);

            // Chercher le tableau de tâches
            JsonNode tasksNode = null;
            if (data.has("taches")) {
                tasksNode = data.get("taches");
            } else if (data.isArray()) {
                tasksNode = data;
            }

            if (tasksNode != null && tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    AIGeneratedTask task = new AIGeneratedTask();
                    task.titre = getString(taskNode, "titre", "Tâche générée");
                    task.description = getString(taskNode, "description", "");
                    task.priorite = normalizePriorite(getString(taskNode, "priorite", "Normale"));
                    task.dureeJours = taskNode.has("dureeJours") ? taskNode.get("dureeJours").asInt(14) : 14;
                    task.pourquoi = getString(taskNode, "pourquoi", "");
                    task.mediaType = getString(taskNode, "mediaType", null);
                    task.mediaTitre = getString(taskNode, "mediaTitre", null);
                    task.mediaAuteur = getString(taskNode, "mediaAuteur", null);

                    JsonNode sousTachesNode = taskNode.path("sousTaches");
                    if (sousTachesNode.isArray()) {
                        for (JsonNode st : sousTachesNode) {
                            task.sousTaches.add(st.asText());
                        }
                    }
                    tasks.add(task);
                }
            }

            if (tasks.isEmpty()) {
                tasks = getDefaultTasks();
            }

        } catch (Exception e) {
            System.err.println("Erreur parsing JSON: " + e.getMessage());
            return getDefaultTasks();
        }

        return tasks;
    }

    private List<AIGeneratedTask> getDefaultTasks() {
        List<AIGeneratedTask> defaultTasks = new ArrayList<>();

        AIGeneratedTask task1 = new AIGeneratedTask();
        task1.titre = "📚 Découvrir le vocabulaire de base";
        task1.description = "Apprenez 20 mots essentiels liés à votre objectif avec des flashcards. Utilisez Quizlet ou Anki pour mémoriser.";
        task1.priorite = "Normale";
        task1.dureeJours = 7;
        defaultTasks.add(task1);

        AIGeneratedTask task2 = new AIGeneratedTask();
        task2.titre = "🎧 Pratiquer avec des dialogues audio";
        task2.description = "Écoutez et répétez des dialogues simples pour améliorer votre compréhension orale.";
        task2.priorite = "Normale";
        task2.dureeJours = 7;
        defaultTasks.add(task2);

        AIGeneratedTask task3 = new AIGeneratedTask();
        task3.titre = "✍️ Rédiger un court texte";
        task3.description = "Écrivez un paragraphe de 5-10 phrases en utilisant les nouveaux mots appris.";
        task3.priorite = "Basse";
        task3.dureeJours = 7;
        defaultTasks.add(task3);

        return defaultTasks;
    }

    // ════════════════════════════════════════════════════════════════════════
    //  UTILITAIRES
    // ════════════════════════════════════════════════════════════════════════

    private String getString(JsonNode node, String field, String defaultVal) {
        JsonNode n = node.path(field);
        if (n.isMissingNode() || n.isNull()) return defaultVal;
        return n.asText(defaultVal);
    }

    private String normalizePriorite(String raw) {
        if (raw == null) return "Normale";
        return switch (raw.toLowerCase()) {
            case "basse", "low" -> "Basse";
            case "haute", "high" -> "Haute";
            case "urgente", "urgent" -> "Urgente";
            default -> "Normale";
        };
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "")
                .replace("\t", "\\t");
    }
}