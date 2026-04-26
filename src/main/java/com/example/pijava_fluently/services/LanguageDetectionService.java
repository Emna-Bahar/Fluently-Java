package com.example.pijava_fluently.services;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class LanguageDetectionService {

    private static final String ENDPOINT = "https://ws.detectlanguage.com/0.2/detect";
    private static final String API_KEY  = com.example.pijava_fluently.utils.AppConfig.get("DETECTLANGUAGE_KEY");

    // Maps ISO 639-1 codes returned by the API to the English names used in LANG_NAME_MAP
    public String detect(String text) {
        if (text == null || text.isBlank()) return null;

        HttpURLConnection connection = null;
        try {
            URL url = new URL(ENDPOINT);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", "Bearer " + API_KEY);
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestProperty("User-Agent", "FluentlyApp/1.0");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(7000);
            connection.setDoOutput(true);

            byte[] body = ("q=" + java.net.URLEncoder.encode(text, StandardCharsets.UTF_8)).getBytes(StandardCharsets.UTF_8);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(body);
            }

            int code = connection.getResponseCode();
            if (code != 200) return null;

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            // Response: {"data":{"detections":[{"language":"fr","isReliable":true,"confidence":8.27}]}}
            JsonObject root = JsonParser.parseString(sb.toString()).getAsJsonObject();
            JsonArray detections = root.getAsJsonObject("data").getAsJsonArray("detections");

            if (detections.isEmpty()) return null;
            String langCode = detections.get(0).getAsJsonObject().get("language").getAsString();
            return isoToName(langCode);

        } catch (Exception e) {
            System.out.println("[DEBUG] LanguageDetection error: " + e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String isoToName(String code) {
        return switch (code.toLowerCase()) {
            case "fr" -> "French";
            case "en" -> "English";
            case "es" -> "Spanish";
            case "de" -> "German";
            case "ar" -> "Arabic";
            case "it" -> "Italian";
            case "pt" -> "Portuguese";
            case "zh" -> "Chinese";
            case "ja" -> "Japanese";
            case "ru" -> "Russian";
            case "ko" -> "Korean";
            case "tr" -> "Turkish";
            case "nl" -> "Dutch";
            default   -> null;
        };
    }
}
