package com.example.pijava_fluently.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
public class WikipediaService {

    private static final String WIKIPEDIA_API = "https://fr.wikipedia.org/api/rest_v1/page/summary/";
    private static final String WIKIDATA_API = "https://www.wikidata.org/wiki/Special:EntityData/";

    // Récupère les informations d'une langue depuis Wikipedia
    public Map<String, Object> getLangueInfo(String nomLangue) {
        Map<String, Object> info = new HashMap<>();

        try {
            // Encoder le nom de la langue pour l'URL
            String encodedName = URLEncoder.encode(nomLangue, StandardCharsets.UTF_8);
            String urlString = WIKIPEDIA_API + encodedName;

            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "FluentlyApp/1.0");

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                // Parser le JSON
                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();

                // Extraire les informations
                if (json.has("extract")) {
                    info.put("description", json.get("extract").getAsString());
                }
                if (json.has("extract_html")) {
                    info.put("description_html", json.get("extract_html").getAsString());
                }

                // URL de la page Wikipedia
                if (json.has("content_urls") && json.getAsJsonObject("content_urls").has("desktop")) {
                    JsonObject desktop = json.getAsJsonObject("content_urls").getAsJsonObject("desktop");
                    if (desktop.has("page")) {
                        info.put("url", desktop.get("page").getAsString());
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("Erreur Wikipedia pour " + nomLangue + ": " + e.getMessage());
            // Fallback sur données locales
            info.put("description", getLocalDescription(nomLangue));
        }

        // Ajouter les données locales (statistiques fixes)
        info.putAll(getLocalStats(nomLangue));

        return info;
    }

    // Données locales (statistiques que Wikipedia ne donne pas directement)
    private Map<String, Object> getLocalStats(String nomLangue) {
        Map<String, Object> stats = new HashMap<>();

        switch (nomLangue.toLowerCase()) {
            case "anglais" -> {
                stats.put("locuteurs", "1,456 milliards");
                stats.put("famille", "Indo-européenne > Germanique");
                stats.put("ecriture", "Latin");
                stats.put("difficulte", 2);
                stats.put("pays", "Royaume-Uni, États-Unis, Canada, Australie, Inde, Irlande, Nouvelle-Zélande, Afrique du Sud...");
                stats.put("funfact", "L'anglais est la langue officielle de 59 pays et est la langue internationale des affaires, de l'aviation et d'Internet !");
            }
            case "français" -> {
                stats.put("locuteurs", "321 millions");
                stats.put("famille", "Indo-européenne > Roman");
                stats.put("ecriture", "Latin");
                stats.put("difficulte", 1);
                stats.put("pays", "France, Belgique, Suisse, Canada (Québec), Sénégal, Côte d'Ivoire, Madagascar, Cameroun...");
                stats.put("funfact", "Le français était la langue diplomatique par excellence au XVIIIe siècle et reste une langue officielle de l'ONU, de l'UE et du CIO !");
            }
            case "espagnol" -> {
                stats.put("locuteurs", "543 millions");
                stats.put("famille", "Indo-européenne > Roman");
                stats.put("ecriture", "Latin");
                stats.put("difficulte", 2);
                stats.put("pays", "Espagne, Mexique, Argentine, Colombie, Pérou, Venezuela, Chili, Équateur, Guatemala...");
                stats.put("funfact", "L'espagnol est la 2ème langue maternelle la plus parlée au monde après le mandarin !");
            }
            case "allemand" -> {
                stats.put("locuteurs", "135 millions");
                stats.put("famille", "Indo-européenne > Germanique");
                stats.put("ecriture", "Latin");
                stats.put("difficulte", 3);
                stats.put("pays", "Allemagne, Autriche, Suisse, Luxembourg, Liechtenstein, Belgique...");
                stats.put("funfact", "L'allemand possède des mots composés très longs comme 'Donaudampfschifffahrtsgesellschaftskapitän' !");
            }
            case "arabe" -> {
                stats.put("locuteurs", "422 millions");
                stats.put("famille", "Afro-asiatique > Sémitique");
                stats.put("ecriture", "Arabe");
                stats.put("difficulte", 5);
                stats.put("pays", "Égypte, Arabie Saoudite, Maroc, Algérie, Tunisie, Irak, Syrie, Jordanie, Liban, Émirats...");
                stats.put("funfact", "L'arabe s'écrit de droite à gauche et a 28 lettres. La calligraphie arabe est considérée comme un art majeur !");
            }
            default -> {
                stats.put("locuteurs", "Donnée non disponible");
                stats.put("famille", "Non classifiée");
                stats.put("ecriture", "Variable");
                stats.put("difficulte", 3);
                stats.put("pays", "Information non disponible");
                stats.put("funfact", "Chaque langue ouvre une nouvelle fenêtre sur le monde et sa culture !");
            }
        }
        return stats;
    }

    private String getLocalDescription(String nomLangue) {
        return switch (nomLangue.toLowerCase()) {
            case "anglais" -> "L'anglais est une langue germanique occidentale originaire d'Angleterre. C'est la langue la plus parlée au monde en nombre total de locuteurs.";
            case "français" -> "Le français est une langue romane parlée principalement en France, Belgique, Suisse, Canada et dans de nombreux pays d'Afrique.";
            case "espagnol" -> "L'espagnol est une langue romane parlée en Espagne et dans la plupart des pays d'Amérique latine.";
            case "allemand" -> "L'allemand est une langue germanique parlée principalement en Allemagne, Autriche, Suisse et au Luxembourg.";
            case "arabe" -> "L'arabe est une langue sémitique parlée dans le monde arabe. C'est la langue liturgique de l'Islam.";
            default -> "Découvrez cette langue fascinante et commencez votre apprentissage sur Fluently !";
        };
    }


    // Ajoutez cette méthode pour rechercher une langue sur Wikidata
    public Map<String, Object> getLangueInfoFromWikidata(String nomLangue) {
        Map<String, Object> info = new HashMap<>();

        try {
            // 1. D'abord, chercher l'ID Wikidata de la langue
            String searchUrl = "https://www.wikidata.org/w/api.php?action=wbsearchentities&search="
                    + URLEncoder.encode(nomLangue, StandardCharsets.UTF_8)
                    + "&language=fr&format=json&type=item";

            URL url = new URL(searchUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "FluentlyApp/1.0");

            if (conn.getResponseCode() == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();

                JsonObject json = JsonParser.parseString(response.toString()).getAsJsonObject();
                JsonArray search = json.getAsJsonArray("search");

                if (search.size() > 0) {
                    String entityId = search.get(0).getAsJsonObject().get("id").getAsString();
                    info.put("wikidata_id", entityId);

                    // 2. Récupérer les données de l'entité
                    String entityUrl = "https://www.wikidata.org/wiki/Special:EntityData/" + entityId + ".json";
                    URL entityConn = new URL(entityUrl);
                    HttpURLConnection conn2 = (HttpURLConnection) entityConn.openConnection();
                    conn2.setRequestMethod("GET");
                    conn2.setRequestProperty("User-Agent", "FluentlyApp/1.0");

                    if (conn2.getResponseCode() == 200) {
                        BufferedReader reader2 = new BufferedReader(new InputStreamReader(conn2.getInputStream()));
                        StringBuilder response2 = new StringBuilder();
                        while ((line = reader2.readLine()) != null) {
                            response2.append(line);
                        }
                        reader2.close();

                        JsonObject data = JsonParser.parseString(response2.toString()).getAsJsonObject();
                        JsonObject entities = data.getAsJsonObject("entities");
                        JsonObject entity = entities.getAsJsonObject(entityId);

                        if (entity.has("claims")) {
                            JsonObject claims = entity.getAsJsonObject("claims");

                            // Nombre de locuteurs (P1098)
                            if (claims.has("P1098")) {
                                JsonArray arr = claims.getAsJsonArray("P1098");
                                if (arr.size() > 0) {
                                    JsonObject mainsnak = arr.get(0).getAsJsonObject().getAsJsonObject("mainsnak");
                                    if (mainsnak.has("datavalue")) {
                                        String value = mainsnak.getAsJsonObject("datavalue")
                                                .getAsJsonObject("value")
                                                .get("amount").getAsString();
                                        info.put("locuteurs", value.replace("+", "") + " millions");
                                    }
                                }
                            }

                            // Famille linguistique (P279)
                            if (claims.has("P279")) {
                                info.put("famille", "Récupérée de Wikidata");
                            }

                            // Système d'écriture (P282)
                            if (claims.has("P282")) {
                                info.put("ecriture", "Récupéré de Wikidata");
                            }
                        }
                    }
                }
            }

            // Si on n'a pas trouvé sur Wikidata, utiliser les données locales par défaut
            if (info.isEmpty()) {
                info.putAll(getDefaultStats(nomLangue));
            }

        } catch (Exception e) {
            System.err.println("Erreur Wikidata pour " + nomLangue + ": " + e.getMessage());
            info.putAll(getDefaultStats(nomLangue));
        }

        return info;
    }

    private Map<String, Object> getDefaultStats(String nomLangue) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("locuteurs", "Donnée non disponible");
        stats.put("famille", "Non classifiée");
        stats.put("ecriture", "Variable");
        stats.put("difficulte", 3);
        stats.put("pays", "Information non disponible");
        stats.put("funfact", "Chaque langue ouvre une nouvelle fenêtre sur le monde et sa culture !");
        return stats;
    }
}