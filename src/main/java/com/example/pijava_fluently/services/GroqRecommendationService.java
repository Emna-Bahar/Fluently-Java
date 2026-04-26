package com.example.pijava_fluently.services;

import com.example.pijava_fluently.entites.Objectif;
import com.example.pijava_fluently.utils.ConfigLoader;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GroqRecommendationService {

    private String API_KEY;
    private String API_URL;
    private String MODEL;

    private final OkHttpClient client;
    private final Gson gson;

    private boolean useFallback = true;

    public GroqRecommendationService() {
        this.client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        this.gson = new Gson();

        // Charger la configuration
        loadConfiguration();
    }

    private void loadConfiguration() {
        API_KEY = ConfigLoader.getGroqApiKey();
        API_URL = ConfigLoader.getGroqApiUrl();
        MODEL = ConfigLoader.getGroqModel();

        // Vérifier si la clé API est valide
        if (API_KEY == null || API_KEY.isEmpty() || API_KEY.equals("VOTRE_CLE_API_GROQ_ICI")) {
            System.out.println("⚠️ Pas de clé API Groq configurée. Utilisation du mode fallback.");
            useFallback = true;
        } else {
            System.out.println("✅ Clé API Groq chargée. Mode API activé.");
            useFallback = false;
        }
    }

    public List<ObjectifRecommendation> getRecommendations(String userInterests, String niveau, String langueCible) {
        // Mode fallback - retourne des recommandations personnalisées
        if (useFallback) {
            System.out.println("📚 Mode fallback actif - Génération de recommandations basées sur les intérêts");
            return getFallbackRecommendations(userInterests, niveau, langueCible);
        }

        try {
            String prompt = buildPrompt(userInterests, niveau, langueCible);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL);
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2000);

            JsonArray messages = new JsonArray();

            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content",
                    "Tu es un expert en pédagogie et en apprentissage des langues. " +
                            "Tu dois recommander des objectifs d'apprentissage pertinents et réalistes. " +
                            "Réponds UNIQUEMENT au format JSON suivant, sans aucun texte avant ou après : " +
                            "{\"objectifs\": [{\"titre\": \"...\", \"description\": \"...\", \"duree_estimee_jours\": 30}]}");
            messages.add(systemMessage);

            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt);
            messages.add(userMessage);

            requestBody.add("messages", messages);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(gson.toJson(requestBody), MediaType.parse("application/json")))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful() && response.body() != null) {
                    String jsonResponse = response.body().string();
                    System.out.println("✅ Réponse API reçue");
                    return parseRecommendations(jsonResponse);
                } else {
                    System.err.println("❌ Erreur API - Code: " + response.code());
                    return getFallbackRecommendations(userInterests, niveau, langueCible);
                }
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur: " + e.getMessage());
            return getFallbackRecommendations(userInterests, niveau, langueCible);
        }
    }

    private String buildPrompt(String userInterests, String niveau, String langueCible) {
        return String.format("""
            L'utilisateur a les caractéristiques suivantes :
            - Centres d'intérêt : %s
            - Niveau actuel : %s
            - Langue cible : %s
            
            Propose 4 objectifs d'apprentissage personnalisés, variés et progressifs.
            Chaque objectif doit avoir :
            - Un titre court et accrocheur (max 50 caractères)
            - Une description détaillée (2-3 phrases)
            - Une durée estimée réaliste en jours (entre 7 et 60 jours)
            """, userInterests, niveau, langueCible);
    }

    private List<ObjectifRecommendation> parseRecommendations(String jsonResponse) {
        List<ObjectifRecommendation> recommendations = new ArrayList<>();

        try {
            JsonObject root = gson.fromJson(jsonResponse, JsonObject.class);

            if (!root.has("choices") || root.getAsJsonArray("choices").isEmpty()) {
                return null;
            }

            String content = root.getAsJsonArray("choices")
                    .get(0).getAsJsonObject()
                    .getAsJsonObject("message")
                    .get("content").getAsString();

            content = cleanJsonResponse(content);

            JsonObject data = gson.fromJson(content, JsonObject.class);

            if (!data.has("objectifs")) {
                return null;
            }

            JsonArray objectifs = data.getAsJsonArray("objectifs");

            for (int i = 0; i < objectifs.size(); i++) {
                JsonObject obj = objectifs.get(i).getAsJsonObject();
                ObjectifRecommendation rec = new ObjectifRecommendation();

                rec.setTitre(obj.has("titre") ? obj.get("titre").getAsString() : "Objectif " + (i+1));
                rec.setDescription(obj.has("description") ? obj.get("description").getAsString() : "Description non disponible");
                rec.setDureeEstimeeJours(obj.has("duree_estimee_jours") ? obj.get("duree_estimee_jours").getAsInt() : 30);

                recommendations.add(rec);
            }

        } catch (Exception e) {
            System.err.println("Erreur parsing JSON: " + e.getMessage());
            return null;
        }

        return recommendations.isEmpty() ? null : recommendations;
    }

    private String cleanJsonResponse(String content) {
        if (content.startsWith("```json")) {
            content = content.substring(7);
        }
        if (content.startsWith("```")) {
            content = content.substring(3);
        }
        if (content.endsWith("```")) {
            content = content.substring(0, content.length() - 3);
        }
        return content.trim();
    }

    private List<ObjectifRecommendation> getFallbackRecommendations(String userInterests, String niveau, String langueCible) {
        List<ObjectifRecommendation> fallback = new ArrayList<>();

        String langue = (langueCible != null && !langueCible.isEmpty()) ? langueCible : "français";
        String interests = (userInterests != null && !userInterests.isEmpty()) ? userInterests.toLowerCase() : "général";

        if (interests.contains("cuisine") || interests.contains("food")) {
            fallback.add(new ObjectifRecommendation(
                    "Vocabulaire culinaire en " + langue,
                    "Apprenez le vocabulaire de la cuisine : ingrédients, ustensiles et techniques culinaires. Pratiquez avec des recettes.",
                    21
            ));
        } else if (interests.contains("voyage") || interests.contains("travel")) {
            fallback.add(new ObjectifRecommendation(
                    "Préparer son voyage en " + langue,
                    "Maîtrisez le vocabulaire du voyage : réservation, demander son chemin, commander au restaurant.",
                    28
            ));
        } else if (interests.contains("technologie") || interests.contains("tech")) {
            fallback.add(new ObjectifRecommendation(
                    "Vocabulaire technologique en " + langue,
                    "Apprenez le lexique de l'informatique et des nouvelles technologies.",
                    25
            ));
        } else {
            fallback.add(new ObjectifRecommendation(
                    "Maîtriser les bases de la conversation en " + langue,
                    "Apprenez les phrases essentielles pour une conversation simple.",
                    21
            ));
        }

        fallback.add(new ObjectifRecommendation(
                "Enrichir son vocabulaire en " + langue,
                "Mémorisez 100 nouveaux mots répartis en 5 thèmes quotidiens.",
                30
        ));

        fallback.add(new ObjectifRecommendation(
                "Améliorer sa compréhension orale en " + langue,
                "Écoutez 15 minutes de contenu audio chaque jour.",
                28
        ));

        fallback.add(new ObjectifRecommendation(
                "Pratiquer l'écriture quotidienne en " + langue,
                "Tenez un journal intime : écrivez 5 à 10 phrases par jour.",
                30
        ));

        return fallback;
    }

    public static class ObjectifRecommendation {
        private String titre;
        private String description;
        private int dureeEstimeeJours;

        public ObjectifRecommendation() {}

        public ObjectifRecommendation(String titre, String description, int dureeEstimeeJours) {
            this.titre = titre;
            this.description = description;
            this.dureeEstimeeJours = dureeEstimeeJours;
        }

        public String getTitre() { return titre; }
        public void setTitre(String titre) { this.titre = titre; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public int getDureeEstimeeJours() { return dureeEstimeeJours; }
        public void setDureeEstimeeJours(int dureeEstimeeJours) { this.dureeEstimeeJours = dureeEstimeeJours; }

        public Objectif toObjectif(int userId) {
            LocalDate dateDebut = LocalDate.now();
            LocalDate dateFin = dateDebut.plusDays(dureeEstimeeJours);

            // Tronquer la description à 250 caractères maximum
            String safeDescription = description;
            if (safeDescription == null) {
                safeDescription = "Objectif d'apprentissage personnalisé";
            }
            if (safeDescription.length() > 250) {
                safeDescription = safeDescription.substring(0, 247) + "...";
            }

            String safeTitre = titre;
            if (safeTitre == null) {
                safeTitre = "Objectif personnalisé";
            }
            if (safeTitre.length() > 50) {
                safeTitre = safeTitre.substring(0, 47) + "...";
            }

            return new Objectif(safeTitre, safeDescription, dateDebut, dateFin, "En cours", userId);
        }
    }
}