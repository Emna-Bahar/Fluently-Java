package com.example.pijava_fluently.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MistralService {

    // Remplacez par VOTRE clé API Mistral
    // Obtenez une clé gratuite sur : https://console.mistral.ai/
    private static final String API_KEY = "KaqstOkl7MldWhWPWbo4Zi9qaDX7bQvV";
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    public String genererCours(String langue, String theme, String grammaire,
                               String vocabulaire, int niveauDifficulte) {

        String prompt = construirePrompt(langue, theme, grammaire, vocabulaire, niveauDifficulte);
        return appelerMistral(prompt);
    }

    private String construirePrompt(String langue, String theme, String grammaire,
                                    String vocabulaire, int niveauDifficulte) {

        String niveauTexte;
        switch (niveauDifficulte) {
            case 1: niveauTexte = "Débutant (très simple)"; break;
            case 2: niveauTexte = "Débutant intermédiaire"; break;
            case 3: niveauTexte = "Intermédiaire"; break;
            case 4: niveauTexte = "Avancé"; break;
            case 5: niveauTexte = "Expert (très avancé)"; break;
            default: niveauTexte = "Intermédiaire";
        }

        return String.format("""
            Tu es un professeur expert en %s. Tu dois créer un cours personnalisé pour un étudiant.
            
            📋 INFORMATIONS POUR LE COURS :
            - Langue à enseigner : %s
            - Thème du cours : %s
            - Point de grammaire : %s
            - Vocabulaire souhaité : %s
            - Niveau de difficulté : %s (sur 5)
            
            📝 STRUCTURE ATTENDUE DU COURS (à respecter strictement) :
            
            🎯 1. INTRODUCTION
            📝 2. VOCABULAIRE (15-20 mots)
            📖 3. GRAMMAIRE
            💬 4. DIALOGUE / MISE EN SITUATION
            ✏️ 5. EXERCICES (4 exercices)
            ✅ 6. CORRECTION DES EXERCICES
            💡 7. ASTUCES ET CONSEILS
            🚀 8. POUR ALLER PLUS LOIN
            
            Utilise la langue %s pour tout le cours.
            """,
                langue, langue, theme, grammaire, vocabulaire, niveauTexte, langue);
    }

    private String appelerMistral(String prompt) {
        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "mistral-tiny"); // ou mistral-small, mistral-medium
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2000);

            Request request = new Request.Builder()
                    .url(API_URL)
                    .addHeader("Authorization", "Bearer " + API_KEY)
                    .addHeader("Content-Type", "application/json")
                    .post(RequestBody.create(
                            MediaType.parse("application/json"),
                            requestBody.toString()
                    ))
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return "❌ Erreur API Mistral : " + response.code() + " - " + response.message();
                }

                String responseBody = response.body().string();
                JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray choices = jsonResponse.getAsJsonArray("choices");
                JsonObject firstChoice = choices.get(0).getAsJsonObject();
                JsonObject messageResponse = firstChoice.getAsJsonObject("message");

                return messageResponse.get("content").getAsString();
            }

        } catch (IOException e) {
            e.printStackTrace();
            return "❌ Erreur lors de l'appel à Mistral : " + e.getMessage();
        }
    }
    public String chercherDefinition(String mot, String langue) {
        String prompt = "Tu es un dictionnaire. Donne moi la définition du mot '" + mot + "' en " + langue +
                ". Réponds UNIQUEMENT au format suivant (sans texte supplémentaire):\n" +
                "📖 MOT: " + mot.toUpperCase() + "\n" +
                "📝 DÉFINITION: [définition claire et simple]\n" +
                "📌 EXEMPLE: [une phrase d'exemple avec le mot]\n" +
                "🔗 SYNONYMES: [2-3 synonymes séparés par des virgules]";

        return appelerMistral(prompt);
    }
}