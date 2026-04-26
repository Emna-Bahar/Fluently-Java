package com.example.pijava_fluently.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Chargeur de configuration unifié pour tout le projet Fluently.
 * Supporte toutes les clés API et paramètres nécessaires.
 */
public class ConfigLoader {

    private static final String CONFIG_FILE = "config.properties";
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
     * Charge le fichier de configuration
     */
    private static void loadConfig() {
        properties = new Properties();

        if (debugMode) {
            System.out.println("=== DEBUG CONFIG LOADER ===");
        }

        // Méthode 1: Chemin normal via ClassLoader
        String normalPath = CONFIG_FILE;
        if (debugMode) {
            System.out.println("Recherche avec: " + normalPath);
        }

        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(normalPath)) {

            if (input == null) {
                if (debugMode) {
                    System.err.println("❌ Fichier non trouvé au chemin: " + normalPath);
                }

                // Méthode 2: Essayer avec un slash au début
                String altPath = "/" + CONFIG_FILE;
                if (debugMode) {
                    System.out.println("Essai avec: " + altPath);
                }

                try (InputStream input2 = ConfigLoader.class.getResourceAsStream(altPath)) {
                    if (input2 == null) {
                        if (debugMode) {
                            System.err.println("❌ Fichier non trouvé non plus avec: " + altPath);
                            System.err.println("Vérifiez que le fichier existe exactement à:");
                            System.err.println("src/main/resources/" + CONFIG_FILE);
                        }
                        return;
                    } else {
                        properties.load(input2);
                        if (debugMode) {
                            System.out.println("✅ Configuration chargée depuis getResourceAsStream avec slash !");
                        }
                    }
                }
            } else {
                properties.load(input);
                if (debugMode) {
                    System.out.println("✅ Configuration chargée avec succès depuis le chemin normal !");
                }
            }

            if (debugMode && properties != null && !properties.isEmpty()) {
                System.out.println("Propriétés chargées: " + properties.stringPropertyNames());
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement de la configuration: " + e.getMessage());
            if (debugMode) {
                e.printStackTrace();
            }
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
     */
    public static String get(String key) {
        ensureLoaded();
        return properties != null ? properties.getProperty(key) : null;
    }

    /**
     * Récupère une propriété avec une valeur par défaut
     */
    public static String get(String key, String defaultValue) {
        String value = get(key);
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    /**
     * Récupère une propriété entière
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
     */
    public static boolean getBoolean(String key, boolean defaultValue) {
        String value = get(key);
        if (value == null || value.isEmpty()) {
            return defaultValue;
        }
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Module Test) ====================

    public static String getGroqApiKey() {
        String apiKey = get("groq.api.key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("VOTRE_CLE_API_GROQ_ICI")) {
            System.err.println("⚠️ Clé API Groq non configurée dans config.properties");
            return "";
        }
        return apiKey;
    }

    public static String getGroqApiUrl() {
        return get("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    }

    public static String getGroqModel() {
        return get("groq.model", "llama-3.3-70b-versatile");
    }

    public static String getWhisperApiUrl() {
        return get("whisper.api.url", "https://api.groq.com/openai/v1/audio/transcriptions");
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Module Mistral) ====================

    public static String getMistralApiKey() {
        return get("MISTRAL_API_KEY", "");
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Module YouTube) ====================

    public static String getYouTubeApiKey() {
        return get("YOUTUBE_API_KEY", "");
    }

    // ==================== MÉTHODES SPÉCIFIQUES (Admin/Duel) ====================

    public static String getBestLocalIP() {
        return get("server.local_ip", "");
    }

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
     * Vérifie si la configuration globale est valide
     */
    public static boolean isConfigValid() {
        return properties != null && !properties.isEmpty();
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