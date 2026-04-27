package com.example.pijava_fluently.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Chargeur de configuration unifié pour tout le projet Fluently.
 * Supporte toutes les clés API et paramètres nécessaires.
 *
 * Chemin du fichier: src/main/resources/config.properties
 *
 * @version 2.0 (fusionné)
 */
public class ConfigLoader {

    private static final String CONFIG_FILE_SINGLE = "config.properties";
    private static final String CONFIG_FILE_PACKAGE = "/com/example/pijava_fluently/config.properties";
    private static Properties properties = null;
    private static boolean debugMode = false;

    // Constructeur privé pour empêcher l'instanciation
    private ConfigLoader() {}

    /**
     * Active/Désactive le mode debug (affiche les logs de chargement)
     */
    public static void setDebugMode(boolean enabled) {
        debugMode = enabled;
    }

    /**
     * Charge le fichier de configuration avec fallback sur plusieurs chemins
     */
    private static void loadConfig() {
        properties = new Properties();

        if (debugMode) {
            System.out.println("=== DEBUG CONFIG LOADER ===");
        }

        boolean loaded = false;

        // Chemin 1: Via ClassLoader (src/main/resources/config.properties)
        if (!loaded) {
            try (InputStream input = ConfigLoader.class.getClassLoader()
                    .getResourceAsStream(CONFIG_FILE_SINGLE)) {
                if (input != null) {
                    properties.load(input);
                    loaded = true;
                    if (debugMode) {
                        System.out.println("✅ Configuration chargée depuis: " + CONFIG_FILE_SINGLE);
                    }
                } else if (debugMode) {
                    System.out.println("❌ Fichier non trouvé: " + CONFIG_FILE_SINGLE);
                }
            } catch (IOException e) {
                if (debugMode) {
                    System.err.println("Erreur lecture: " + e.getMessage());
                }
            }
        }

        // Chemin 2: Via getResourceAsStream avec slash (/com/example/...)
        if (!loaded) {
            try (InputStream input = ConfigLoader.class.getResourceAsStream(CONFIG_FILE_PACKAGE)) {
                if (input != null) {
                    properties.load(input);
                    loaded = true;
                    if (debugMode) {
                        System.out.println("✅ Configuration chargée depuis: " + CONFIG_FILE_PACKAGE);
                    }
                } else if (debugMode) {
                    System.out.println("❌ Fichier non trouvé: " + CONFIG_FILE_PACKAGE);
                }
            } catch (IOException e) {
                if (debugMode) {
                    System.err.println("Erreur lecture: " + e.getMessage());
                }
            }
        }

        if (!loaded && debugMode) {
            System.err.println("⚠️ config.properties non trouvé! Copiez config.properties.example et remplissez vos clés.");
        }

        if (debugMode && properties != null && !properties.isEmpty()) {
            System.out.println("Propriétés chargées: " + properties.stringPropertyNames());
            System.out.println("=============================\n");
        }
    }

    /**
     * Vérifie que les propriétés sont chargées
     */
    private static void ensureLoaded() {
        if (properties == null) {
            loadConfig();
        }
    }

    // ==================== MÉTHODES GÉNÉRIQUES ====================

    /**
     * Récupère une propriété par sa clé
     * @param key Clé de la propriété
     * @return Valeur de la propriété ou chaîne vide si non trouvée
     */
    public static String get(String key) {
        ensureLoaded();
        String value = properties != null ? properties.getProperty(key) : null;
        return value != null ? value : "";
    }

    /**
     * Récupère une propriété avec une valeur par défaut
     * @param key Clé de la propriété
     * @param defaultValue Valeur par défaut si non trouvée
     * @return Valeur de la propriété ou defaultValue
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Récupère une propriété entière
     * @param key Clé de la propriété
     * @param defaultValue Valeur par défaut
     * @return Valeur entière ou defaultValue
     */
    public static int getInt(String key, int defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Récupère une propriété double
     * @param key Clé de la propriété
     * @param defaultValue Valeur par défaut
     * @return Valeur double ou defaultValue
     */
    public static double getDouble(String key, double defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    /**
     * Récupère une propriété booléenne
     * @param key Clé de la propriété
     * @param defaultValue Valeur par défaut
     * @return Valeur booléenne ou defaultValue
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value) || "yes".equalsIgnoreCase(value);
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Module Test - Groq/Whisper) ====================

    /**
     * Récupère la clé API Groq pour les appels LLM et Whisper
     */
    public static String getGroqApiKey() {
        String apiKey = get("groq.api.key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("VOTRE_CLE_API_GROQ_ICI")) {
            if (debugMode) {
                System.err.println("⚠️ Clé API Groq non configurée dans config.properties");
            }
            return "";
        }
        return apiKey;
    }

    /**
     * Récupère l'URL de l'API Groq
     */
    public static String getGroqApiUrl() {
        return get("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    }

    /**
     * Récupère le modèle Groq à utiliser
     */
    public static String getGroqModel() {
        return get("groq.model", "llama-3.3-70b-versatile");
    }

    /**
     * Récupère l'URL de l'API Whisper (transcription audio)
     */
    public static String getWhisperApiUrl() {
        return get("whisper.api.url", "https://api.groq.com/openai/v1/audio/transcriptions");
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Module Mistral) ====================

    /**
     * Récupère la clé API Mistral
     */
    public static String getMistralApiKey() {
        return get("MISTRAL_API_KEY", "");
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Module YouTube) ====================

    /**
     * Récupère la clé API YouTube
     */
    public static String getYouTubeApiKey() {
        return get("YOUTUBE_API_KEY", "");
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Admin/Duel) ====================

    /**
     * Récupère la meilleure IP locale pour le duel
     */
    public static String getBestLocalIP() {
        return get("server.local_ip", "");
    }

    /**
     * Récupère le port pour le duel
     */
    public static int getDuelPort() {
        return getInt("duel.port", 9090);
    }

    // ==================== MÉTHODES DE VALIDATION ====================

    /**
     * Vérifie si la configuration Groq est valide
     */
    public static boolean isGroqConfigValid() {
        String apiKey = getGroqApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("VOTRE_CLE_API_GROQ_ICI");
    }

    /**
     * Vérifie si la configuration Mistral est valide
     */
    public static boolean isMistralConfigValid() {
        String apiKey = getMistralApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("VOTRE_CLE_API_MISTRAL_ICI");
    }

    /**
     * Vérifie si la configuration YouTube est valide
     */
    public static boolean isYouTubeConfigValid() {
        String apiKey = getYouTubeApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("VOTRE_CLE_API_YOUTUBE_ICI");
    }

    /**
     * Vérifie si la configuration Google Sheets est valide
     */
    public static boolean isGoogleSheetsConfigValid() {
        String clientId = get("google.sheets.client.id", "");
        String clientSecret = get("google.sheets.client.secret", "");
        return !clientId.isEmpty() && !clientId.equals("VOTRE_GOOGLE_CLIENT_ID")
                && !clientSecret.isEmpty() && !clientSecret.equals("VOTRE_GOOGLE_CLIENT_SECRET");
    }

    /**
     * Vérifie si la configuration globale est valide
     */
    public static boolean isConfigValid() {
        return properties != null && !properties.isEmpty();
    }

    /**
     * Vérifie si une configuration de reconnaissance vocale est disponible
     */
    public static boolean isSpeechRecognitionAvailable() {
        return isGroqConfigValid();
    }

    /**
     * Affiche le statut des configurations (utile pour le debug)
     */
    public static void printConfigStatus() {
        System.out.println("\n=== CONFIGURATION STATUS ===");
        System.out.println("Fichier chargé: " + (properties != null && !properties.isEmpty()));
        System.out.println("Groq API: " + (isGroqConfigValid() ? "✅ Configurée" : "❌ Non configurée"));
        System.out.println("Mistral API: " + (isMistralConfigValid() ? "✅ Configurée" : "❌ Non configurée"));
        System.out.println("YouTube API: " + (isYouTubeConfigValid() ? "✅ Configurée" : "❌ Non configurée"));
        System.out.println("Google Sheets: " + (isGoogleSheetsConfigValid() ? "✅ Configurée" : "❌ Non configurée"));
        System.out.println("Reconnaissance vocale: " + (isSpeechRecognitionAvailable() ? "✅ Disponible" : "❌ Non disponible"));
        System.out.println("=============================\n");
    }

    /**
     * Recharge manuellement la configuration
     */
    public static void reload() {
        properties = null;
        loadConfig();
    }
}