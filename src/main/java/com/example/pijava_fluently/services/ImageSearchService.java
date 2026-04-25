package com.example.pijava_fluently.services;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class ImageSearchService {

    private final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    // Version améliorée pour récupérer des images depuis Wikipedia
    public String telechargerImageDepuisWikipedia(String query, String outputPath) {
        try {
            // Essayer d'abord avec la langue française
            String imageUrl = rechercherImageWikipedia(query, "fr");
            if (imageUrl == null) {
                // Sinon essayer avec Wikipedia en anglais
                imageUrl = rechercherImageWikipedia(query, "en");
            }

            if (imageUrl != null) {
                return telechargerImage(imageUrl, outputPath);
            }
        } catch (Exception e) {
            System.err.println("Erreur recherche Wikipedia: " + e.getMessage());
        }
        return null;
    }

    private String rechercherImageWikipedia(String query, String lang) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

            // 1. D'abord, chercher l'article le plus pertinent
            String searchUrl = "https://" + lang + ".wikipedia.org/w/api.php?action=query&list=search&srsearch="
                    + encodedQuery + "&format=json&origin=*&srlimit=3";

            Request searchRequest = new Request.Builder().url(searchUrl)
                    .addHeader("User-Agent", "FluentlyApp/1.0")
                    .build();

            try (Response searchResponse = client.newCall(searchRequest).execute()) {
                if (searchResponse.isSuccessful()) {
                    String jsonData = searchResponse.body().string();
                    JSONObject json = new JSONObject(jsonData);
                    JSONObject queryObj = json.getJSONObject("query");
                    JSONArray searchResults = queryObj.getJSONArray("search");

                    if (searchResults.length() > 0) {
                        // Essayer chaque résultat jusqu'à trouver une image
                        for (int i = 0; i < searchResults.length(); i++) {
                            String pageTitle = searchResults.getJSONObject(i).getString("title");
                            String encodedTitle = URLEncoder.encode(pageTitle, StandardCharsets.UTF_8.toString());

                            // 2. Récupérer l'image principale de l'article
                            String imageUrl = "https://" + lang + ".wikipedia.org/w/api.php?action=query&titles="
                                    + encodedTitle + "&prop=pageimages&format=json&pithumbsize=500&origin=*";

                            Request imageRequest = new Request.Builder().url(imageUrl)
                                    .addHeader("User-Agent", "FluentlyApp/1.0")
                                    .build();

                            try (Response imageResponse = client.newCall(imageRequest).execute()) {
                                if (imageResponse.isSuccessful()) {
                                    String imageJson = imageResponse.body().string();
                                    JSONObject imageJsonObj = new JSONObject(imageJson);
                                    JSONObject pages = imageJsonObj.getJSONObject("query").getJSONObject("pages");

                                    for (String key : pages.keySet()) {
                                        JSONObject page = pages.getJSONObject(key);
                                        if (page.has("thumbnail")) {
                                            String thumbnailUrl = page.getJSONObject("thumbnail").getString("source");
                                            if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                                                System.out.println("✅ Image trouvée pour: " + query);
                                                return thumbnailUrl;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur recherche " + lang + ": " + e.getMessage());
        }
        return null;
    }

    private String telechargerImage(String imageUrl, String outputPath) {
        try {
            Request request = new Request.Builder().url(imageUrl)
                    .addHeader("User-Agent", "FluentlyApp/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    byte[] imageBytes = response.body().bytes();
                    try (FileOutputStream fos = new FileOutputStream(outputPath)) {
                        fos.write(imageBytes);
                    }
                    System.out.println("✅ Image téléchargée: " + outputPath);
                    return outputPath;
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur téléchargement: " + e.getMessage());
        }
        return null;
    }

    // Nouvelle méthode: Utiliser Wikimedia Commons (plus d'images libres)
    public String telechargerImageDepuisWikimedia(String query, String outputPath) {
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8.toString());

            // Rechercher sur Wikimedia Commons
            String commonsUrl = "https://commons.wikimedia.org/w/api.php?action=query&generator=search&gsrsearch="
                    + encodedQuery + "&gsrlimit=5&prop=imageinfo&iiprop=url&iiurlwidth=500&format=json&origin=*";

            Request request = new Request.Builder().url(commonsUrl)
                    .addHeader("User-Agent", "FluentlyApp/1.0")
                    .build();

            try (Response response = client.newCall(request).execute()) {
                if (response.isSuccessful()) {
                    String jsonData = response.body().string();
                    JSONObject json = new JSONObject(jsonData);

                    if (json.has("query") && json.getJSONObject("query").has("pages")) {
                        JSONObject pages = json.getJSONObject("query").getJSONObject("pages");

                        for (String key : pages.keySet()) {
                            JSONObject page = pages.getJSONObject(key);
                            if (page.has("imageinfo")) {
                                JSONArray imageInfo = page.getJSONArray("imageinfo");
                                if (imageInfo.length() > 0) {
                                    String imageUrl = imageInfo.getJSONObject(0).getString("url");
                                    if (imageUrl != null && !imageUrl.isEmpty()) {
                                        return telechargerImage(imageUrl, outputPath);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur Wikimedia: " + e.getMessage());
        }
        return null;
    }

    // Méthode principale qui combine toutes les sources
    public String rechercherEtTelechargerImage(String query, String outputPath) {
        // Essayer Wikipedia d'abord
        String result = telechargerImageDepuisWikipedia(query, outputPath);

        // Si Wikipedia échoue, essayer Wikimedia Commons
        if (result == null) {
            result = telechargerImageDepuisWikimedia(query, outputPath);
        }

        return result;
    }
}