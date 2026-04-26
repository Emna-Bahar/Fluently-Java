package com.example.pijava_fluently.services;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

public class DictionaryService {

    private static final String ENDPOINT = "https://api.api-ninjas.com/v1/dictionary";
    private static final String API_KEY  = com.example.pijava_fluently.utils.AppConfig.get("API_NINJAS_KEY");

    public record DictionaryResult(String word, String definition, boolean found) {}

    public DictionaryResult lookup(String word) {
        if (word == null || word.isBlank()) return new DictionaryResult(word, null, false);

        HttpURLConnection connection = null;
        try {
            String encoded = URLEncoder.encode(word.trim(), StandardCharsets.UTF_8);
            URL url = new URL(ENDPOINT + "?word=" + encoded);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("X-Api-Key", API_KEY);
            connection.setRequestProperty("User-Agent", "FluentlyApp/1.0");
            connection.setConnectTimeout(4000);
            connection.setReadTimeout(7000);

            if (connection.getResponseCode() != 200) return new DictionaryResult(word, null, false);

            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) sb.append(line);

            // Response: {"word":"hello","definition":"...","valid":true}
            JsonObject json = JsonParser.parseString(sb.toString()).getAsJsonObject();
            boolean valid = json.has("valid") && json.get("valid").getAsBoolean();
            String definition = valid ? json.get("definition").getAsString() : null;
            return new DictionaryResult(word, definition, valid);

        } catch (Exception e) {
            return new DictionaryResult(word, null, false);
        } finally {
            if (connection != null) connection.disconnect();
        }
    }
}
