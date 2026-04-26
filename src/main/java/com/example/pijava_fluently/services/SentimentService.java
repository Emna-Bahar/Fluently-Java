package com.example.pijava_fluently.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class SentimentService {

    private static final String ENDPOINT = "https://api.api-ninjas.com/v1/sentiment";
    private static final String API_KEY  = com.example.pijava_fluently.utils.AppConfig.get("API_NINJAS_KEY");

    /** Returns "positive", "negative", "neutral", or null if unavailable. */
    public String analyze(String text) {
        if (text == null || text.isBlank()) return "neutral";

        HttpURLConnection connection = null;
        try {
            String encoded = URLEncoder.encode(text, StandardCharsets.UTF_8);
            URL url = new URL(ENDPOINT + "?text=" + encoded);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Api-Key", API_KEY);
            connection.setRequestProperty("User-Agent", "FluentlyApp/1.0");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(7000);

            int responseCode = connection.getResponseCode();
            InputStream stream = (responseCode >= 200 && responseCode < 300)
                    ? connection.getInputStream()
                    : connection.getErrorStream();
            String body = readResponse(stream);

            if (responseCode < 200 || responseCode >= 300) return null;

            // Response: {"sentiment":"positive","score":0.5,"text":"..."}
            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            return json.has("sentiment") ? json.get("sentiment").getAsString().toLowerCase() : null;

        } catch (Exception e) {
            System.err.println("[SentimentService] " + e.getMessage());
            return null;
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private String readResponse(InputStream inputStream) {
        if (inputStream == null) return "";
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);
            return sb.toString();
        } catch (Exception ignored) {
            return "";
        }
    }
}
