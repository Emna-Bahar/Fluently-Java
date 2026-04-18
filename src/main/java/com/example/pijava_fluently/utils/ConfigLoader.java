package com.example.pijava_fluently.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static final String CONFIG_FILE = "config.properties";
    private static Properties properties = null;

    private ConfigLoader() {
        // Constructeur privé pour empêcher l'instanciation
    }

    /**
     * Charge le fichier de configuration
     */
    private static void loadConfig() {
        properties = new Properties();
        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {

            if (input == null) {
                System.err.println("⚠️ Fichier " + CONFIG_FILE + " non trouvé dans le classpath");
                System.err.println("📌 Créez le fichier src/main/resources/config.properties");
                return;
            }

            properties.load(input);
            System.out.println("✅ Configuration chargée avec succès");

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement de la configuration: " + e.getMessage());
        }
    }

    /**
     * Récupère une propriété par sa clé
     */
    public static String getProperty(String key) {
        if (properties == null) {
            loadConfig();
        }

        if (properties != null) {
            return properties.getProperty(key);
        }
        return null;
    }

    /**
     * Récupère une propriété avec une valeur par défaut
     */
    public static String getProperty(String key, String defaultValue) {
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * Récupère la clé API Groq
     */
    public static String getGroqApiKey() {
        String apiKey = getProperty("groq.api.key");
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("VOTRE_CLE_API_GROQ_ICI")) {
            System.err.println("⚠️ Clé API Groq non configurée dans config.properties");
            return "";
        }
        return apiKey;
    }

    /**
     * Récupère l'URL de l'API Groq
     */
    public static String getGroqApiUrl() {
        return getProperty("groq.api.url", "https://api.groq.com/openai/v1/chat/completions");
    }

    /**
     * Récupère le modèle Groq
     */
    public static String getGroqModel() {
        return getProperty("groq.model", "llama-3.3-70b-versatile");
    }

    /**
     * Vérifie si la configuration est valide
     */
    public static boolean isConfigValid() {
        String apiKey = getGroqApiKey();
        return apiKey != null && !apiKey.isEmpty() && !apiKey.equals("VOTRE_CLE_API_GROQ_ICI");
    }
}