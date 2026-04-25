package com.example.pijava_fluently.services;

import com.example.pijava_fluently.utils.ConfigLoader;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class MistralService {

    // Récupérer la clé API depuis config.properties
    private static final String API_KEY = ConfigLoader.getMistralApiKey();
    private static final String API_URL = "https://api.mistral.ai/v1/chat/completions";

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    // Vérifier si la clé API est disponible
    static {
        if (API_KEY == null || API_KEY.isEmpty()) {
            System.err.println("⚠️ ATTENTION: Clé API Mistral non configurée dans config.properties !");
        }
    }

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
        ✅ 6. CORRECTION DES EXERCICES (réponses détaillées pour les 4 exercices)
        💡 7. ASTUCES ET CONSEILS
        🚀 8. POUR ALLER PLUS LOIN
        
        IMPORTANT: 
        - La section EXERCICES doit contenir 4 exercices variés (QCM, trous, traduction, conjugaison)
        - La section CORRECTION DES EXERCICES doit donner TOUTES les réponses avec explications
        - Utilise la langue %s pour tout le cours.
        """,
                langue, langue, theme, grammaire, vocabulaire, niveauTexte, langue);
    }

    private String appelerMistral(String prompt) {
        // Vérifier si la clé API est configurée
        if (API_KEY == null || API_KEY.isEmpty()) {
            return "❌ Clé API Mistral non configurée. Veuillez configurer config.properties";
        }

        try {
            JsonObject message = new JsonObject();
            message.addProperty("role", "user");
            message.addProperty("content", prompt);

            JsonArray messages = new JsonArray();
            messages.add(message);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", "mistral-tiny");
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
    // Ajoutez cette méthode dans MistralService.java
    public String genererFlashcards(String langue, String promptUtilisateur, String niveau) {
        String systemPrompt = String.format("""
        Tu es un professeur expert en %s. Tu dois générer des flashcards éducatives en %s UNIQUEMENT.
        
        L'utilisateur demande: "%s"
        Niveau: %s
        
        Génère EXACTEMENT 5 flashcards. Chaque flashcard doit être au format suivant, sans texte supplémentaire:
        
        ===
        QUESTION: [La question posée à l'étudiant en %s]
        OPTION1: [Choix 1 en %s]
        OPTION2: [Choix 2 en %s]
        OPTION3: [Choix 3 en %s]
        OPTION4: [Choix 4 en %s]
        REPONSE: [numéro de 1 à 4]
        EXPLICATION: [Explication détaillée en %s de la réponse]
        ===
        
        IMPORTANT: 
        - Toute la question, les options et l'explication DOIVENT être en %s
        - Ne mets rien d'autre que ces 5 flashcards
        - Sépare chaque flashcard par "==="
        - Les flashcards doivent couvrir: grammaire, vocabulaire, conjugaison, culture
        - Adapte-toi au niveau %s
        """, langue, langue, promptUtilisateur, niveau,
                langue, langue, langue, langue, langue, langue, langue, niveau);

        return appelerMistral(systemPrompt);
    }

    public String explorerCulture(String pays, String categorie, String langue) {
        String prompt = String.format("""
    Tu es un guide touristique expert. Tu dois présenter des éléments culturels.
    
    Pays: %s
    Catégorie: %s
    Langue: %s
    
    Pour CHAQUE élément, donne une adresse TRÈS PRÉCISE avec le NOM du lieu.
    
    FORMAT EXACT:
    ===
    NOM: [nom exact du lieu/plat/tradition]
    TYPE: [lieu|cuisine|tradition|art]
    DESCRIPTION: [description en 2-3 phrases]
    ADRESSE: [adresse COMPLÈTE - ex: "Champ de Mars, 5 Avenue Anatole France, 75007 Paris" ou "Quartier Latin, Paris 5e" ou "Place de la Comédie, Montpellier"]
    HORAIRES: [horaires ou "Toute l'année" ou "Variable"]
    ===
    
    EXEMPLES D'ADRESSES PRÉCISES:
    - Pour la Tour Eiffel: "Champ de Mars, 5 Avenue Anatole France, 75007 Paris"
    - Pour le Louvre: "Rue de Rivoli, 75001 Paris"
    - Pour le Croissant (plat): "Dans toutes les boulangeries françaises, originaire de Vienne (Autriche)"
    
    IMPORTANT: 
    - Utilise la langue %s
    - Sois TRÈS PRÉCIS sur les adresses
    - Ne mets pas seulement "Paris" ou "France"
    """, pays, categorie, langue, langue);

        return appelerMistral(prompt);
    }

    public String genererPuzzleEtymologique(String langue, String niveau) {
        String prompt = String.format("""
    Tu es un professeur de linguistique. Crée un puzzle étymologique pour un étudiant de niveau %s en langue %s.
    
    Choisis un mot intéressant de la langue %s. Découpe-le en ses morphèmes (préfixe(s), racine, suffixe(s)).
    
    Réponds EXACTEMENT au format suivant:
    
    MOT COMPLET: [le mot complet]
    DÉFINITION: [définition claire du mot]
    MORCEAUX: [morceau1]|[morceau2]|[morceau3]
    
    Exemple pour "impossible" en français:
    MOT COMPLET: impossible
    DÉFINITION: Qui ne peut être fait ou réalisé
    MORCEAUX: im|pos|sible
    
    Exemple pour "reconstruction" en français:
    MOT COMPLET: reconstruction
    DÉFINITION: Action de reconstruire quelque chose
    MORCEAUX: re|con|struc|tion
    
    IMPORTANT:
    - Utilise la langue %s pour tout
    - Les morceaux doivent être séparés par |
    - Le mot doit être adapté au niveau %s
    """, niveau, langue, langue, langue, niveau);

        return appelerMistral(prompt);
    }

}