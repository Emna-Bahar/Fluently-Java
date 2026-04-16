package com.example.pijava_fluently.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class ConfigLoader {

    private static Properties properties = null;

    private static void loadConfig() {
        properties = new Properties();

        // Debug: Lister tous les fichiers dans le classpath
        System.out.println("=== DEBUG CONFIG LOADER ===");

        // Méthode 1: Chemin normal
        String normalPath = "com/example/pijava_fluently/config.properties";
        System.out.println("Recherche avec: " + normalPath);

        try (InputStream input = ConfigLoader.class.getClassLoader()
                .getResourceAsStream(normalPath)) {

            if (input == null) {
                System.err.println("❌ Fichier non trouvé au chemin: " + normalPath);

                // Méthode 2: Essayer avec un slash au début
                String altPath = "/com/example/pijava_fluently/config.properties";
                System.out.println("Essai avec: " + altPath);

                try (InputStream input2 = ConfigLoader.class.getResourceAsStream(altPath)) {
                    if (input2 == null) {
                        System.err.println("❌ Fichier non trouvé non plus avec: " + altPath);

                        // Méthode 3: Essayer depuis la racine des ressources
                        System.out.println("Essai depuis la racine des ressources...");
                        try (InputStream input3 = ConfigLoader.class.getClassLoader()
                                .getResourceAsStream("config.properties")) {
                            if (input3 == null) {
                                System.err.println("❌ Fichier introuvable partout !");
                                System.err.println("Vérifiez que le fichier existe exactement à:");
                                System.err.println("src/main/resources/com/example/pijava_fluently/config.properties");
                            } else {
                                properties.load(input3);
                                System.out.println("✅ Configuration chargée depuis la racine !");
                            }
                        }
                    } else {
                        properties.load(input2);
                        System.out.println("✅ Configuration chargée depuis getResourceAsStream avec slash !");
                    }
                }
            } else {
                properties.load(input);
                System.out.println("✅ Configuration chargée avec succès depuis le chemin normal !");
            }

            if (properties != null && !properties.isEmpty()) {
                System.out.println("Propriétés chargées: " + properties.stringPropertyNames());
            }

        } catch (IOException e) {
            System.err.println("❌ Erreur lors du chargement: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        if (properties == null) {
            loadConfig();
        }
        return properties != null ? properties.getProperty(key) : null;
    }

    public static String getMistralApiKey() {
        return get("MISTRAL_API_KEY");
    }

    public static String getYouTubeApiKey() {
        return get("YOUTUBE_API_KEY");
    }
}